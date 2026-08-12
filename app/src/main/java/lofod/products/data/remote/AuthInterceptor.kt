package lofod.products.data.remote

import lofod.products.data.local.SessionTokenHolder
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionTokenHolder: SessionTokenHolder,
    private val sessionExpiredNotifier: SessionExpiredNotifier
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionTokenHolder.token
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        if (response.code == 401 && !isPublicAuthPath(request.url.encodedPath)) {
            sessionTokenHolder.token = null
            sessionExpiredNotifier.notifySessionExpired()
        }

        return response
    }

    private fun isPublicAuthPath(encodedPath: String): Boolean {
        val path = encodedPath.trimEnd('/')
        return path.endsWith("/auth/login") || path.endsWith("/auth/register")
    }
}
