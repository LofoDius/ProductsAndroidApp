package lofod.products.ui.catalog

import lofod.products.data.remote.response.CategoryResponse
import lofod.products.ui.common.findCategoryById

private const val BREADCRUMB_SEPARATOR = " ● "

fun categoryBreadcrumb(categoryId: String, root: CategoryResponse): String? {
    val tree = root.subcategories
    val target = findCategoryById(categoryId, tree) ?: return null
    val names = ArrayDeque<String>()
    var current: CategoryResponse? = target
    while (current != null && !current.isSyntheticRoot()) {
        names.addFirst(current.name)
        val parentId = current.parentId
        if (parentId == null || parentId == CatalogConstants.ROOT_ID) break
        current = findCategoryById(parentId, tree) ?: return null
    }
    if (names.isEmpty()) return null
    return names.joinToString(BREADCRUMB_SEPARATOR)
}
