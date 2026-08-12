package lofod.products.data.remote

import lofod.products.data.remote.request.CreateCardRequest
import lofod.products.data.remote.request.CreateCategoryRequest
import lofod.products.data.remote.request.InviteMemberRequest
import lofod.products.data.remote.response.CardResponse
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.data.remote.response.ImageIdResponse
import lofod.products.data.remote.response.ImageResponse
import lofod.products.data.remote.response.MemberResponse
import okhttp3.MultipartBody
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
    suspend fun uploadCategoryImage(@Part image: MultipartBody.Part): Response<ImageIdResponse>

    @GET("category/image/{id}")
    suspend fun getCategoryImage(@Path("id") id: String): ImageResponse

    @PUT("category/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: CreateCategoryRequest
    ): Response<CategoryResponse>

    @DELETE("category/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    @GET("category/{id}/cards")
    suspend fun getCategoryCards(@Path("id") id: String): List<CardResponse>

    @POST("card/image")
    @Multipart
    suspend fun uploadCardImage(@Part image: MultipartBody.Part): Response<ImageIdResponse>

    @GET("card/image/{id}")
    suspend fun getCardImage(@Path("id") id: String): ImageResponse

    @POST("category/{id}/card")
    suspend fun createCard(
        @Path("id") id: String,
        @Body request: CreateCardRequest
    ): Response<List<CardResponse>>

    @PUT("category/{id}/card/{cardId}")
    suspend fun updateCard(
        @Path("id") id: String,
        @Path("cardId") cardId: String,
        @Body request: CreateCardRequest
    ): Response<List<CardResponse>>

    @GET("category/{id}/card/{cardId}")
    suspend fun getCard(
        @Path("id") id: String,
        @Path("cardId") cardId: String
    ): CardResponse

    @DELETE("category/{id}/card/{cardId}")
    suspend fun deleteCard(
        @Path("id") id: String,
        @Path("cardId") cardId: String
    ): Response<Unit>

    @GET("cards/search/{query}")
    suspend fun search(@Path(value = "query", encoded = true) query: String): List<CardResponse>

    @GET("category/{id}/members")
    suspend fun listMembers(@Path("id") id: String): List<MemberResponse>

    @POST("category/{id}/members")
    suspend fun inviteMember(
        @Path("id") id: String,
        @Body request: InviteMemberRequest
    ): Response<MemberResponse>

    @DELETE("category/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") id: String,
        @Path("userId") userId: String
    ): Response<Unit>
}
