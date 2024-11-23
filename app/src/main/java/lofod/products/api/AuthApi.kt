package lofod.products.api

import lofod.products.api.request.CreateSessionRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("session")
    fun createSession(@Body request: CreateSessionRequest): Call<Void>

}
