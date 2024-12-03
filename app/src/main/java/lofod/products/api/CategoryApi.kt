package lofod.products.api

import lofod.products.api.request.CreateCardRequest
import lofod.products.api.request.CreateCategoryRequest
import lofod.products.api.response.CardResponse
import lofod.products.api.response.CategoryResponse
import lofod.products.api.response.ImageIdResponse
import lofod.products.api.response.ImageResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface CategoryApi {

    @GET("category/tree")
    suspend fun getCategories(): List<CategoryResponse>

    @POST("category")
    suspend fun createCategory(@Body request: CreateCategoryRequest): Response<CategoryResponse>

    @POST("category/image")
    @Multipart
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<ImageIdResponse>

    @GET("category/image/{id}")
    fun getImage(@Path("id") id: String): Call<ImageResponse>

    @PUT("category/{id}")
    fun updateCategory(@Path("id") id: String, @Body request: CreateCategoryRequest): Call<CategoryResponse>

    @DELETE("category/{id}")
    fun deleteCategory(@Path("id") id: String): Call<String>

    @GET("category/{id}/cards")
    fun getCategoryCards(@Path("id") id: String): Call<List<CardResponse>>

    @POST("card/image")
    fun uploadCardImage(@Part image: MultipartBody.Part): Call<ImageIdResponse>

    @GET("card/image/{id}")
    suspend fun getCardImage(@Path("id") id: String): ImageResponse

    @POST("category/{id}/card")
    fun createCard(@Path("id") id: String, @Body request: CreateCardRequest): Call<List<CardResponse>>

    @PUT("category/{id}/card/{cardId}")
    fun updateCard(
        @Path("id") id: String,
        @Path("cardId") cardId: String,
        @Body request: CreateCardRequest
    ): Call<List<CardResponse>>

    @GET("category/{id}/card/{cardId}")
    fun getCard(@Path("id") id: String, @Path("cardId") cardId: String): Call<CardResponse>

    @DELETE("category/{id}/card/{cardId}")
    fun deleteCard(@Path("id") id: String, @Path("cardId") cardId: String): Call<Void>

    @GET("cards/search/{query}")
    fun search(@Path("query") query: String): Call<List<CardResponse>>
}
