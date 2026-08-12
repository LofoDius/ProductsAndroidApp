package lofod.products.data.repository

import android.net.Uri
import lofod.products.data.remote.CategoryApi
import lofod.products.data.remote.bodyOrThrow
import lofod.products.data.remote.ensureSuccess
import lofod.products.data.remote.request.CreateCardRequest
import lofod.products.data.remote.request.CreateCategoryRequest
import lofod.products.data.remote.request.InviteMemberRequest
import lofod.products.data.remote.response.CardResponse
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.data.remote.response.ImageResponse
import lofod.products.data.remote.response.MemberResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi
) {
    suspend fun getCategories(): List<CategoryResponse> = categoryApi.getCategories()

    suspend fun createCategory(request: CreateCategoryRequest): CategoryResponse =
        categoryApi.createCategory(request).bodyOrThrow()

    suspend fun updateCategory(id: String, request: CreateCategoryRequest): CategoryResponse =
        categoryApi.updateCategory(id, request).bodyOrThrow()

    suspend fun deleteCategory(id: String) {
        categoryApi.deleteCategory(id).ensureSuccess()
    }

    suspend fun getCategoryCards(id: String): List<CardResponse> =
        categoryApi.getCategoryCards(id)

    suspend fun createCard(categoryId: String, request: CreateCardRequest): List<CardResponse> =
        categoryApi.createCard(categoryId, request).bodyOrThrow()

    suspend fun updateCard(
        categoryId: String,
        cardId: String,
        request: CreateCardRequest
    ): List<CardResponse> = categoryApi.updateCard(categoryId, cardId, request).bodyOrThrow()

    suspend fun deleteCard(categoryId: String, cardId: String) {
        categoryApi.deleteCard(categoryId, cardId).ensureSuccess()
    }

    suspend fun search(query: String): List<CardResponse> {
        val encoded = Uri.encode(query.trim())
        return categoryApi.search(encoded)
    }

    suspend fun uploadCategoryImage(bytes: ByteArray): String {
        val part = imagePart(bytes)
        return categoryApi.uploadCategoryImage(part).bodyOrThrow().imageId
    }

    suspend fun uploadCardImage(bytes: ByteArray): String {
        val part = imagePart(bytes)
        return categoryApi.uploadCardImage(part).bodyOrThrow().imageId
    }

    suspend fun getCategoryImage(id: String): ImageResponse = categoryApi.getCategoryImage(id)

    suspend fun getCardImage(id: String): ImageResponse = categoryApi.getCardImage(id)

    suspend fun listMembers(categoryId: String): List<MemberResponse> =
        categoryApi.listMembers(categoryId)

    suspend fun inviteMember(categoryId: String, username: String): MemberResponse =
        categoryApi.inviteMember(categoryId, InviteMemberRequest(username.trim())).bodyOrThrow()

    suspend fun removeMember(categoryId: String, userId: String) {
        categoryApi.removeMember(categoryId, userId).ensureSuccess()
    }

    private fun imagePart(bytes: ByteArray): MultipartBody.Part {
        val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "image.jpg", body)
    }
}
