package lofod.products.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lofod.products.data.remote.response.CardResponse
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.data.repository.CategoryRepository
import lofod.products.ui.common.ErrorMapper
import lofod.products.ui.common.findCategoryById
import javax.inject.Inject

data class CatalogUiState(
    val isBootLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val bootError: String? = null,
    val actionError: String? = null,
    val root: CategoryResponse? = null,
    val currentCategory: CategoryResponse? = null,
    val cards: List<CardResponse> = emptyList(),
    val isCardsLoading: Boolean = false,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val isSearchMode: Boolean = false,
    val expandedCardId: String? = null,
    val categoryPendingDelete: CategoryResponse? = null,
    val cardPendingDelete: CardResponse? = null
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    init {
        loadTree(isBoot = true)
    }

    fun retryBoot() {
        loadTree(isBoot = true)
    }

    fun refresh() {
        val currentId = _state.value.currentCategory?.categoryId ?: CatalogConstants.ROOT_ID
        loadTree(isBoot = false, selectCategoryId = currentId)
    }

    fun selectCategory(category: CategoryResponse) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    currentCategory = category,
                    isSearchMode = false,
                    searchOpen = false,
                    searchQuery = "",
                    expandedCardId = null,
                    actionError = null
                )
            }
            if (category.isSyntheticRoot()) {
                _state.update { it.copy(cards = emptyList(), isCardsLoading = false) }
            } else {
                loadCards(category.categoryId)
            }
        }
    }

    fun navigateBack() {
        val current = _state.value.currentCategory ?: return
        val root = _state.value.root ?: return
        val parentId = current.parentId
        val parent = when {
            parentId == null || parentId == CatalogConstants.ROOT_ID -> root
            else -> findCategoryById(parentId, root.subcategories) ?: root
        }
        selectCategory(parent)
    }

    fun toggleCardExpanded(cardId: String) {
        _state.update {
            it.copy(expandedCardId = if (it.expandedCardId == cardId) null else cardId)
        }
    }

    fun openSearch() {
        _state.update { it.copy(searchOpen = true) }
    }

    fun closeSearch() {
        val current = _state.value.currentCategory
        _state.update {
            it.copy(
                searchOpen = false,
                searchQuery = "",
                isSearchMode = false,
                actionError = null
            )
        }
        if (current == null || current.isSyntheticRoot()) {
            _state.update { it.copy(cards = emptyList()) }
        } else {
            loadCards(current.categoryId)
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, actionError = null) }
        if (query.isBlank()) {
            _state.update { it.copy(isSearchMode = false, cards = emptyList()) }
            val current = _state.value.currentCategory
            if (current != null && !current.isSyntheticRoot()) {
                loadCards(current.categoryId)
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCardsLoading = true, isSearchMode = true) }
            try {
                val results = categoryRepository.search(query)
                _state.update {
                    it.copy(cards = results, isCardsLoading = false, actionError = null)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCardsLoading = false,
                        actionError = ErrorMapper.toMessage(e)
                    )
                }
            }
        }
    }

    fun onCategoryFormReturned() {
        refresh()
    }

    fun onCardFormReturned() {
        val query = _state.value.searchQuery
        if (_state.value.isSearchMode && query.isNotBlank()) {
            onSearchQueryChange(query)
        } else {
            val current = _state.value.currentCategory
            if (current != null && !current.isSyntheticRoot()) {
                loadCards(current.categoryId)
            }
        }
        refreshTreeOnly()
    }

    fun requestDeleteCategory(category: CategoryResponse) {
        _state.update { it.copy(categoryPendingDelete = category) }
    }

    fun cancelDeleteCategory() {
        _state.update { it.copy(categoryPendingDelete = null) }
    }

    fun confirmDeleteCategory() {
        val target = _state.value.categoryPendingDelete ?: return
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(target.categoryId)
                _state.update { it.copy(categoryPendingDelete = null, actionError = null) }
                val parentId = target.parentId
                loadTree(
                    isBoot = false,
                    selectCategoryId = parentId ?: CatalogConstants.ROOT_ID
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        categoryPendingDelete = null,
                        actionError = ErrorMapper.toMessage(e)
                    )
                }
            }
        }
    }

    fun requestDeleteCard(card: CardResponse) {
        _state.update { it.copy(cardPendingDelete = card) }
    }

    fun cancelDeleteCard() {
        _state.update { it.copy(cardPendingDelete = null) }
    }

    fun confirmDeleteCard() {
        val target = _state.value.cardPendingDelete ?: return
        viewModelScope.launch {
            try {
                categoryRepository.deleteCard(target.categoryId, target.cardId)
                _state.update { it.copy(cardPendingDelete = null, actionError = null) }
                val current = _state.value.currentCategory
                if (current != null && !current.isSyntheticRoot()) {
                    loadCards(current.categoryId)
                }
                refreshTreeOnly()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        cardPendingDelete = null,
                        actionError = ErrorMapper.toMessage(e)
                    )
                }
            }
        }
    }

    fun clearActionError() {
        _state.update { it.copy(actionError = null) }
    }

    private fun loadTree(isBoot: Boolean, selectCategoryId: String? = null) {
        viewModelScope.launch {
            if (isBoot) {
                _state.update {
                    it.copy(isBootLoading = true, bootError = null)
                }
            } else {
                _state.update { it.copy(isRefreshing = true, actionError = null) }
            }
            try {
                val children = categoryRepository.getCategories()
                val root = syntheticRoot(children)
                val targetId = selectCategoryId
                    ?: _state.value.currentCategory?.categoryId
                    ?: CatalogConstants.ROOT_ID
                val selected = if (targetId == CatalogConstants.ROOT_ID) {
                    root
                } else {
                    findCategoryById(targetId, root.subcategories) ?: root
                }
                _state.update {
                    it.copy(
                        isBootLoading = false,
                        isRefreshing = false,
                        bootError = null,
                        root = root,
                        currentCategory = selected
                    )
                }
                if (selected.isSyntheticRoot()) {
                    _state.update { it.copy(cards = emptyList(), isCardsLoading = false) }
                } else if (!_state.value.isSearchMode) {
                    loadCards(selected.categoryId)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isBootLoading = false,
                        isRefreshing = false,
                        bootError = if (isBoot || it.root == null) {
                            ErrorMapper.toMessage(e)
                        } else {
                            it.bootError
                        },
                        actionError = if (!isBoot && it.root != null) {
                            ErrorMapper.toMessage(e)
                        } else {
                            it.actionError
                        }
                    )
                }
            }
        }
    }

    private fun refreshTreeOnly() {
        viewModelScope.launch {
            try {
                val children = categoryRepository.getCategories()
                val root = syntheticRoot(children)
                val currentId = _state.value.currentCategory?.categoryId ?: CatalogConstants.ROOT_ID
                val selected = if (currentId == CatalogConstants.ROOT_ID) {
                    root
                } else {
                    findCategoryById(currentId, root.subcategories) ?: root
                }
                _state.update { it.copy(root = root, currentCategory = selected) }
            } catch (_: Exception) {
                // Keep existing tree; card mutation already succeeded.
            }
        }
    }

    private fun loadCards(categoryId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isCardsLoading = true) }
            try {
                val cards = categoryRepository.getCategoryCards(categoryId)
                _state.update {
                    it.copy(cards = cards, isCardsLoading = false, actionError = null)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCardsLoading = false,
                        cards = emptyList(),
                        actionError = ErrorMapper.toMessage(e)
                    )
                }
            }
        }
    }
}
