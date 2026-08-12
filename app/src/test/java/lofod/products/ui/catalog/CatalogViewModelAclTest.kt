package lofod.products.ui.catalog

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import lofod.products.MainDispatcherRule
import lofod.products.data.remote.model.CategoryRole
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.data.repository.CategoryRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelAclTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)

    @Test
    fun ownerCategory_canOpenMembersAndCreateCard() = runTest(mainDispatcherRule.dispatcher) {
        val owner = category(id = "owned", role = CategoryRole.OWNER)
        coEvery { categoryRepository.getCategories() } returns listOf(owner)
        coEvery { categoryRepository.getCategoryCards("owned") } returns emptyList()

        val viewModel = CatalogViewModel(categoryRepository)
        viewModel.selectCategory(owner)

        viewModel.openMembers()
        viewModel.openCreateCard()

        assertTrue(viewModel.state.value.showMembers)
        assertTrue(viewModel.state.value.showCardForm)
        assertNull(viewModel.state.value.cardFormTarget)
    }

    @Test
    fun memberCategory_canCreateCardButNotOpenMembers() = runTest(mainDispatcherRule.dispatcher) {
        val member = category(id = "shared", role = CategoryRole.MEMBER)
        coEvery { categoryRepository.getCategories() } returns listOf(member)
        coEvery { categoryRepository.getCategoryCards("shared") } returns emptyList()

        val viewModel = CatalogViewModel(categoryRepository)
        viewModel.selectCategory(member)

        viewModel.openMembers()
        assertFalse(viewModel.state.value.showMembers)

        viewModel.openCreateCard()
        assertTrue(viewModel.state.value.showCardForm)
    }

    @Test
    fun syntheticRoot_blocksCreateCardAndMembers() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { categoryRepository.getCategories() } returns emptyList()

        val viewModel = CatalogViewModel(categoryRepository)

        assertTrue(viewModel.state.value.currentCategory!!.isSyntheticRoot())

        viewModel.openMembers()
        viewModel.openCreateCard()

        assertFalse(viewModel.state.value.showMembers)
        assertFalse(viewModel.state.value.showCardForm)
    }

    private fun category(
        id: String,
        role: CategoryRole
    ): CategoryResponse = CategoryResponse(
        name = "Category $id",
        categoryId = id,
        parentId = CatalogConstants.ROOT_ID,
        subcategoriesAmount = 0,
        cardsAmount = 0,
        subcategories = emptyList(),
        imageId = null,
        role = role
    )
}
