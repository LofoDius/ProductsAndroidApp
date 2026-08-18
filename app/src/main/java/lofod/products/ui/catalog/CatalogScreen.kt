package lofod.products.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import lofod.products.data.remote.response.CardResponse
import lofod.products.data.remote.response.CategoryResponse
import lofod.products.data.repository.CategoryRepository
import lofod.products.ui.common.loadCardImageBitmap
import lofod.products.ui.common.loadCategoryImageBitmap
import lofod.products.ui.session.SessionViewModel

@Composable
fun CatalogScreen(
    sessionViewModel: SessionViewModel,
    isUpdateAvailable: Boolean,
    onUpdateApp: () -> Unit,
    onCreateCard: (categoryId: String) -> Unit,
    onEditCard: (categoryId: String, cardId: String) -> Unit,
    onOpenMembers: (categoryId: String) -> Unit,
    cardFormSaved: Boolean,
    onCardFormSavedConsumed: () -> Unit,
    onCreateCategory: (parentId: String) -> Unit,
    onEditCategory: (categoryId: String) -> Unit,
    categoryFormSaved: Boolean,
    onCategoryFormSavedConsumed: () -> Unit,
    catalogViewModel: CatalogViewModel = hiltViewModel()
) {
    val categoryRepository = catalogViewModel.categoryRepository
    val state by catalogViewModel.state.collectAsStateWithLifecycle()
    val isLoggingOut by sessionViewModel.isLoggingOut.collectAsStateWithLifecycle()
    val logoutError by sessionViewModel.logoutError.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(cardFormSaved) {
        if (cardFormSaved) {
            catalogViewModel.onCardFormReturned()
            onCardFormSavedConsumed()
        }
    }

    LaunchedEffect(categoryFormSaved) {
        if (categoryFormSaved) {
            catalogViewModel.onCategoryFormReturned()
            onCategoryFormSavedConsumed()
        }
    }

    val snackbarMessage = state.actionError ?: logoutError
    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        catalogViewModel.clearActionError()
        sessionViewModel.clearLogoutError()
    }

    if (state.isBootLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (state.bootError != null && state.root == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.bootError.orEmpty(),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = catalogViewModel::retryBoot) {
                Text("Повторить")
            }
            TextButton(onClick = sessionViewModel::logout) {
                Text("Выйти")
            }
        }
    } else {
        val root = state.root
        val current = state.currentCategory
        if (root != null && current != null) {
            CatalogMainContent(
                root = root,
                current = current,
                state = state,
                drawerState = drawerState,
                scope = scope,
                snackbarHostState = snackbarHostState,
                isLoggingOut = isLoggingOut,
                isUpdateAvailable = isUpdateAvailable,
                onUpdateApp = onUpdateApp,
                categoryRepository = categoryRepository,
                catalogViewModel = catalogViewModel,
                sessionViewModel = sessionViewModel,
                onCreateCard = {
                    if (current.canEditCards()) {
                        onCreateCard(current.categoryId)
                    }
                },
                onEditCard = { card ->
                    onEditCard(card.categoryId, card.cardId)
                },
                onOpenMembers = { categoryId ->
                    onOpenMembers(categoryId)
                },
                onCreateCategory = {
                    onCreateCategory(current.categoryId)
                },
                onEditCategory = { category ->
                    onEditCategory(category.categoryId)
                }
            )
        }
    }

    val categoryPendingDelete = state.categoryPendingDelete
    if (categoryPendingDelete != null) {
        AlertDialog(
            onDismissRequest = catalogViewModel::cancelDeleteCategory,
            title = { Text("Удалить категорию?") },
            text = {
                Text("«${categoryPendingDelete.name}» будет удалена вместе с вложенными папками и оценками.")
            },
            confirmButton = {
                TextButton(onClick = catalogViewModel::confirmDeleteCategory) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = catalogViewModel::cancelDeleteCategory) {
                    Text("Отмена")
                }
            }
        )
    }

    val cardPendingDelete = state.cardPendingDelete
    if (cardPendingDelete != null) {
        AlertDialog(
            onDismissRequest = catalogViewModel::cancelDeleteCard,
            title = { Text("Удалить оценку?") },
            text = { Text("«${cardPendingDelete.name}» будет удалена.") },
            confirmButton = {
                TextButton(onClick = catalogViewModel::confirmDeleteCard) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = catalogViewModel::cancelDeleteCard) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun CatalogMainContent(
    root: CategoryResponse,
    current: CategoryResponse,
    state: CatalogUiState,
    drawerState: androidx.compose.material3.DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    isLoggingOut: Boolean,
    isUpdateAvailable: Boolean,
    onUpdateApp: () -> Unit,
    categoryRepository: CategoryRepository,
    catalogViewModel: CatalogViewModel,
    sessionViewModel: SessionViewModel,
    onCreateCard: () -> Unit,
    onEditCard: (CardResponse) -> Unit,
    onOpenMembers: (categoryId: String) -> Unit,
    onCreateCategory: () -> Unit,
    onEditCategory: (CategoryResponse) -> Unit
) {
    val loadCategoryImage: suspend (String) -> ImageBitmap? =
        { id -> loadCategoryImageBitmap(categoryRepository, id) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                CategoryDrawerContent(
                    root = root,
                    currentCategory = current,
                    loadCategoryImage = loadCategoryImage,
                    onSelect = {
                        catalogViewModel.selectCategory(it)
                        scope.launch { drawerState.close() }
                    },
                    onBack = catalogViewModel::navigateBack,
                    onCreateCategory = onCreateCategory,
                    onEditCategory = onEditCategory,
                    onDeleteCategory = catalogViewModel::requestDeleteCategory,
                    onOpenMembers = {
                        scope.launch { drawerState.close() }
                        onOpenMembers(current.categoryId)
                    },
                    onLogout = {
                        sessionViewModel.clearLogoutError()
                        sessionViewModel.logout()
                    },
                    isLoggingOut = isLoggingOut,
                    isUpdateAvailable = isUpdateAvailable,
                    onUpdateApp = {
                        scope.launch { drawerState.close() }
                        onUpdateApp()
                    },
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                SearchableTopAppBar(
                    title = if (state.isSearchMode) "Поиск" else current.name,
                    showTitleIcon = !state.isSearchMode && !current.isSyntheticRoot(),
                    titleImageId = current.imageId.takeUnless {
                        state.isSearchMode || current.isSyntheticRoot()
                    },
                    loadCategoryImage = loadCategoryImage,
                    searchOpen = state.searchOpen,
                    searchQuery = state.searchQuery,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenSearch = catalogViewModel::openSearch,
                    onCloseSearch = catalogViewModel::closeSearch,
                    onSearchQueryChange = catalogViewModel::onSearchQueryChange
                )
            },
            floatingActionButton = {
                if (current.canEditCards() && !state.searchOpen) {
                    FloatingActionButton(onClick = onCreateCard) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить оценку")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.isCardsLoading || state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (current.isSyntheticRoot() && !state.isSearchMode) {
                    Text(
                        text = "Выберите категорию в меню слева",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (state.cards.isEmpty()) {
                    Text(
                        text = if (state.isSearchMode) "Ничего не найдено" else "Пока нет оценок",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.cards, key = { it.cardId }) { card ->
                            CardListItem(
                                card = card,
                                expanded = state.expandedCardId == card.cardId,
                                canEdit = true,
                                loadImage = { imageId ->
                                    loadCardImageBitmap(categoryRepository, imageId)
                                },
                                onToggle = { catalogViewModel.toggleCardExpanded(card.cardId) },
                                onEdit = { onEditCard(card) },
                                onDelete = { catalogViewModel.requestDeleteCard(card) }
                            )
                        }
                    }
                }
            }
        }
    }
}
