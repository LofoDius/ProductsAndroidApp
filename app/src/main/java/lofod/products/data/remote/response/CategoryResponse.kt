package lofod.products.data.remote.response

import lofod.products.data.remote.model.CategoryRole
import lofod.products.data.remote.request.CustomFieldDefinitionDto

data class CategoryResponse(
    val name: String,
    val categoryId: String,
    val parentId: String?,
    val subcategoriesAmount: Int,
    val cardsAmount: Int,
    val subcategories: List<CategoryResponse>,
    val imageId: String?,
    val role: CategoryRole,
    val customFields: List<CustomFieldDefinitionDto> = emptyList(),
    val customFieldArchive: List<CustomFieldDefinitionDto> = emptyList(),
)
