package lofod.products.ui.catalog

import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import lofod.products.data.repository.CategoryRepository
import lofod.products.ui.card.CardFormDialog
import lofod.products.ui.card.CardFormViewModel
import lofod.products.ui.category.CategoryFormDialog
import lofod.products.ui.category.CategoryFormViewModel
import lofod.products.ui.members.MembersDialog
import lofod.products.ui.members.MembersViewModel
import lofod.products.ui.session.SessionViewModel

@Composable
fun CatalogScreen(
    sessionViewModel: SessionViewModel,
    catalogViewModel: CatalogViewModel = hiltViewModel(),
    categoryFormViewModel: CategoryFormViewModel = hiltViewModel(),
    cardFormViewModel: CardFormViewModel = hiltViewModel(),
    membersViewModel: MembersViewModel = hiltViewModel()
) {
    val categoryRepository = catalogViewModel.categoryRepository
    val state by catalogViewModel.state.collectAsStateWithLifecycle()
    val isLoggingOut by sessionViewModel.isLoggingOut.collectAsStateWithLifecycle()
    val logoutError by sessionViewModel.logoutError.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.showCategoryForm, state.categoryFormTarget) {
        if (!state.showCategoryForm) return@LaunchedEffect
        val target = state.categoryFormTarget
        if (target != null) {
            categoryFormViewModel.openEdit(target)
        } else {
            categoryFormViewModel.openCreate(state.currentCategory?.categoryId)
        }
    }

    LaunchedEffect(state.showCardForm, state.cardFormTarget) {
        if (!state.showCardForm) return@LaunchedEffect
        val target = state.cardFormTarget
        val categoryId = target?.categoryId
            ?: state.currentCategory?.categoryId
            ?: return@LaunchedEffect
        if (target != null) {
            cardFormViewModel.openEdit(categoryId, target)
        } else {
            cardFormViewModel.openCreate(categoryId)
        }
    }

    LaunchedEffect(state.showMembers) {
        if (!state.showMembers) return@LaunchedEffect
        val current = state.currentCategory ?: return@LaunchedEffect
        membersViewModel.open(current.categoryId, current.name)
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
                isLoggingOut = isLoggingOut,
                logoutError = logoutError,
                categoryRepository = categoryRepository,
                catalogViewModel = catalogViewModel,
                sessionViewModel = sessionViewModel
            )
        }
    }

    CategoryFormDialog(
        viewModel = categoryFormViewModel,
        onSaved = catalogViewModel::onCategorySaved,
        onDismiss = catalogViewModel::closeCategoryForm
    )

    CardFormDialog(
        viewModel = cardFormViewModel,
        onSaved = catalogViewModel::onCardsChanged,
        onDismiss = catalogViewModel::closeCardForm
    )

    MembersDialog(
        viewModel = membersViewModel,
        onDismiss = catalogViewModel::closeMembers
    )

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
    root: lofod.products.data.remote.response.CategoryResponse,
    current: lofod.products.data.remote.response.CategoryResponse,
    state: CatalogUiState,
    drawerState: androidx.compose.material3.DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    isLoggingOut: Boolean,
    logoutError: String?,
    categoryRepository: CategoryRepository,
    catalogViewModel: CatalogViewModel,
    sessionViewModel: SessionViewModel
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                CategoryDrawerContent(
                    root = root,
                    currentCategory = current,
                    onSelect = {
                        catalogViewModel.selectCategory(it)
                        scope.launch { drawerState.close() }
                    },
                    onBack = catalogViewModel::navigateBack,
                    onCreateCategory = catalogViewModel::openCreateCategory,
                    onEditCategory = catalogViewModel::openEditCategory,
                    onDeleteCategory = catalogViewModel::requestDeleteCategory,
                    onOpenMembers = catalogViewModel::openMembers,
                    onLogout = {
                        sessionViewModel.clearLogoutError()
                        sessionViewModel.logout()
                    },
                    isLoggingOut = isLoggingOut
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                SearchableTopAppBar(
                    title = if (state.isSearchMode) "Поиск" else current.name,
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
                    FloatingActionButton(onClick = catalogViewModel::openCreateCard) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить оценку")
                    }
                }
            }
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
                                onEdit = { catalogViewModel.openEditCard(card) },
                                onDelete = { catalogViewModel.requestDeleteCard(card) }
                            )
                        }
                    }
                }

                val error = state.actionError ?: logoutError
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

private suspend fun loadCardImageBitmap(
    repository: CategoryRepository,
    imageId: String
): ImageBitmap? {
    return try {
        val base64 = repository.getCardImage(imageId).image
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
