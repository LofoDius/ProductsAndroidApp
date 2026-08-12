package lofod.products.data.repository

import lofod.products.data.local.SessionDataStore
import lofod.products.data.local.SessionTokenHolder
import lofod.products.data.remote.AuthApi
import lofod.products.data.remote.request.AuthCredentialsRequest
import lofod.products.data.remote.response.UserSummaryResponse
import lofod.products.domain.UserSession
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionDataStore: SessionDataStore,
    private val sessionTokenHolder: SessionTokenHolder
) {
    val tokenFlow = sessionDataStore.tokenFlow

    /**
     * Register then auto-login: API register does not issue a session header,
     * so we immediately login to obtain a token and land in the catalog.
     */
    suspend fun register(username: String, password: String): UserSession {
        val response = authApi.register(AuthCredentialsRequest(username, password))
        response.bodyOrThrow()
        return login(username, password)
    }

    suspend fun login(username: String, password: String): UserSession {
        val response = authApi.login(AuthCredentialsRequest(username, password))
        val user = response.bodyOrThrow()
        val rawToken = response.headers()["Authorization"]
            ?: throw IllegalStateException("Login succeeded but Authorization header is missing")
        val token = rawToken.removePrefix("Bearer ").trim()
        if (token.isBlank()) {
            throw IllegalStateException("Login succeeded but session token is empty")
        }
        persistToken(token)
        return UserSession(userId = user.userId, username = user.username, token = token)
    }

    suspend fun logout() {
        try {
            authApi.logout()
        } finally {
            clearSession()
        }
    }

    suspend fun me(): UserSummaryResponse {
        return authApi.me().bodyOrThrow()
    }

    suspend fun hasSession(): Boolean = !sessionDataStore.getToken().isNullOrBlank()

    suspend fun restoreSessionIfValid(): Boolean {
        val token = sessionDataStore.getToken()
        if (token.isNullOrBlank()) {
            sessionTokenHolder.token = null
            return false
        }
        sessionTokenHolder.token = token
        return try {
            me()
            true
        } catch (_: HttpException) {
            clearSession()
            false
        } catch (_: Exception) {
            // Network blip with a stored token: keep local session and allow catalog entry.
            true
        }
    }

    suspend fun clearSession() {
        sessionTokenHolder.token = null
        sessionDataStore.clearToken()
    }

    private suspend fun persistToken(token: String) {
        sessionTokenHolder.token = token
        sessionDataStore.saveToken(token)
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (isSuccessful) {
            return body() ?: throw IllegalStateException("Empty response body")
        }
        throw HttpException(this)
    }
}
