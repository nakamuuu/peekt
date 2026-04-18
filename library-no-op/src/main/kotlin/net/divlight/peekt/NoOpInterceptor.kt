package net.divlight.peekt

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that forwards the request unchanged and only calls [Interceptor.Chain.proceed].
 */
internal object NoOpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}
