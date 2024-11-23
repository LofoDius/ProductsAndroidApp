package lofod.products.api.request

data class CreateCategoryRequest(
    val parentId: String?,
    val name: String,
    val imageId: String?
)
