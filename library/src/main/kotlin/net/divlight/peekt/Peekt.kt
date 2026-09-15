package net.divlight.peekt

import android.content.Context
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.divlight.peekt.core.ClearingStrategy
import net.divlight.peekt.core.PeektConfig
import net.divlight.peekt.core.PeektRecorder
import net.divlight.peekt.datastore.PeektDatabase
import net.divlight.peekt.interceptor.PeektInterceptor
import net.divlight.peekt.recorder.RealPeektRecorder
import okhttp3.Interceptor

/**
 * Application-facing entry point that wires the HTTP recorder, OkHttp [Interceptor], and local storage.
 *
 * Call [create] once (typically in `Application` or your DI graph) and install the same [Interceptor] instance on
 * every [okhttp3.OkHttpClient] that should be recorded.
 */
class Peekt private constructor(
    val recorder: PeektRecorder,
    private val okhttpInterceptor: Interceptor,
) {
    /**
     * Returns the OkHttp [Interceptor] that records traffic; must be registered on clients that should be observed.
     */
    fun interceptor(): Interceptor = okhttpInterceptor

    companion object {
        private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Creates a [Peekt] with a default [PeektConfig], backed by the app-private Room database.
         *
         * @param context Any [Context]; the database is created under the application context.
         * @param config Redaction rules, body size limits, and other recording behavior.
         */
        fun create(context: Context, config: PeektConfig = PeektConfig()): Peekt {
            val dao = PeektDatabase.create(context).httpTransactionDao()
            when (config.clearingStrategy) {
                ClearingStrategy.OnLaunch -> {
                    val cutoffMillis = processStartEpochMillis()
                    ioScope.launch { dao.deleteStartedBefore(cutoffMillis) }
                }
                ClearingStrategy.Never -> Unit
            }
            val recorder = RealPeektRecorder(dao)
            val interceptor = PeektInterceptor(dao, config)
            return Peekt(recorder, interceptor)
        }

        /**
         * Wall-clock millis corresponding to when this process started.
         */
        private fun processStartEpochMillis(): Long {
            val elapsedSinceStart = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
            return System.currentTimeMillis() - elapsedSinceStart
        }
    }
}
