package lofod.products.ui.catalog

import lofod.products.data.remote.model.CategoryRole
import lofod.products.data.remote.response.CategoryResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI visibility flags derived from category role (OWNER vs MEMBER).
 * Task T08 names `canEditCategory`; implementation uses [canManageCategory].
 */
class CatalogAclTest {

    @Test
    fun ownerFlags_allowCategoryMembersAndCards() {
        val owner = category(role = CategoryRole.OWNER)

        assertTrue(owner.canManageCategory())
        assertTrue(owner.canManageMembers())
        assertTrue(owner.canEditCards())
        assertTrue(owner.canCreateSubcategory())
    }

    @Test
    fun memberFlags_allowCardsOnly() {
        val member = category(role = CategoryRole.MEMBER)

        assertFalse(member.canManageCategory())
        assertFalse(member.canManageMembers())
        assertTrue(member.canEditCards())
        assertFalse(member.canCreateSubcategory())
    }

    @Test
    fun syntheticRoot_hidesOwnerActionsButAllowsCreateSubcategory() {
        val root = syntheticRoot(emptyList())

        assertTrue(root.isSyntheticRoot())
        assertFalse(root.canManageCategory())
        assertFalse(root.canManageMembers())
        assertFalse(root.canEditCards())
        assertTrue(root.canCreateSubcategory())
    }

    private fun category(
        role: CategoryRole,
        id: String = "cat-1",
        parentId: String? = CatalogConstants.ROOT_ID
    ): CategoryResponse = CategoryResponse(
        name = "Category",
        categoryId = id,
        parentId = parentId,
        subcategoriesAmount = 0,
        cardsAmount = 0,
        subcategories = emptyList(),
        imageId = null,
        role = role
    )
}
