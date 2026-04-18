package net.divlight.peekt

import android.content.Context
import net.divlight.peekt.core.PeektConfig
import net.divlight.peekt.core.PeektRecorder
import okhttp3.Interceptor

/**
 * API-compatible stub for Peekt. Does not record or persist HTTP traffic; delegates to the OkHttp chain
 * transparently.
 *
 * Depend on `library-no-op` in release builds to avoid debug recording overhead.
 */
class Peekt private constructor(
    val recorder: PeektRecorder,
    private val okhttpInterceptor: Interceptor,
) {
    /**
     * OkHttp [Interceptor] that does not capture or store request or response data.
     */
    fun interceptor(): Interceptor = okhttpInterceptor

    companion object {
        /**
         * Returns a [Peekt] that performs no recording in this module.
         *
         * [context] and [config] match the full library signature for compatibility but are unused by this stub.
         */
        @Suppress("UNUSED_PARAMETER")
        fun create(context: Context, config: PeektConfig = PeektConfig()): Peekt {
            return Peekt(NoOpPeektRecorder, NoOpInterceptor)
        }
    }
}
