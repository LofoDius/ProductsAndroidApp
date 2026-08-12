package lofod.products.data.remote

import lofod.products.data.remote.request.AuthCredentialsRequest
import lofod.products.data.remote.response.UserSummaryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: AuthCredentialsRequest): Response<UserSummaryResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthCredentialsRequest): Response<UserSummaryResponse>

    @DELETE("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/me")
    suspend fun me(): Response<UserSummaryResponse>
}
