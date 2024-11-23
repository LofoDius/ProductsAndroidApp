package lofod.products.api.utils

import lofod.products.Storage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = Storage.token?.let {
            chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $it")
                .build()
        } ?: chain.request()

        return chain.proceed(request)
    }
}