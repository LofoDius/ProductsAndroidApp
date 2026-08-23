package lofod.products.data.remote.response

import lofod.products.data.remote.model.CategoryRole

data class SearchResponse(
    val categories: List<CategorySearchHit> = emptyList(),
    val cards: List<CardResponse> = emptyList(),
)

data class CategorySearchHit(
    val categoryId: String,
    val name: String,
    val parentId: String?,
    val imageId: String?,
    val role: CategoryRole,
)
