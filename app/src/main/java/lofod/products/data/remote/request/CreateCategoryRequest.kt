package lofod.products.data.remote.request

data class CreateCategoryRequest(
    val parentId: String?,
    val name: String,
    val imageId: String?,
    val customFields: List<CustomFieldDefinitionDto> = emptyList(),
)
