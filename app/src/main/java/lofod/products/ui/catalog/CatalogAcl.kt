package lofod.products.ui.catalog

import lofod.products.data.remote.model.CategoryRole
import lofod.products.data.remote.response.CategoryResponse

object CatalogConstants {
    const val ROOT_ID = "-1"
    const val ROOT_NAME = "Все категории"
}

fun syntheticRoot(children: List<CategoryResponse>): CategoryResponse =
    CategoryResponse(
        name = CatalogConstants.ROOT_NAME,
        categoryId = CatalogConstants.ROOT_ID,
        parentId = null,
        subcategoriesAmount = children.size,
        cardsAmount = 0,
        subcategories = children,
        imageId = null,
        role = CategoryRole.OWNER
    )

fun CategoryResponse.isSyntheticRoot(): Boolean =
    categoryId == CatalogConstants.ROOT_ID

fun CategoryResponse.canManageCategory(): Boolean =
    !isSyntheticRoot() && role == CategoryRole.OWNER

fun CategoryResponse.canManageMembers(): Boolean =
    !isSyntheticRoot() && role == CategoryRole.OWNER

fun CategoryResponse.canCreateSubcategory(): Boolean =
    isSyntheticRoot() || role == CategoryRole.OWNER

fun CategoryResponse.canEditCards(): Boolean =
    !isSyntheticRoot()
