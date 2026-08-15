package lofod.products.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import lofod.products.ui.auth.LoginScreen
import lofod.products.ui.auth.RegisterScreen
import lofod.products.ui.card.CardFormScreen
import lofod.products.ui.catalog.CatalogScreen
import lofod.products.ui.category.CategoryFormScreen
import lofod.products.ui.members.MembersScreen
import lofod.products.ui.session.SessionBootstrapState
import lofod.products.ui.session.SessionNavEvent
import lofod.products.ui.session.SessionViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val bootstrapState by sessionViewModel.bootstrapState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        sessionViewModel.navEvents.collect { event ->
            when (event) {
                SessionNavEvent.ToLogin -> {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    when (bootstrapState) {
        SessionBootstrapState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        SessionBootstrapState.Authenticated,
        SessionBootstrapState.Unauthenticated -> {
            val startDestination = if (bootstrapState == SessionBootstrapState.Authenticated) {
                Routes.CATALOG
            } else {
                Routes.LOGIN
            }

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                        onNavigateToCatalog = {
                            navController.navigate(Routes.CATALOG) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onNavigateToLogin = { navController.popBackStack() },
                        onNavigateToCatalog = {
                            navController.navigate(Routes.CATALOG) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.CATALOG) { entry ->
                    val cardFormSaved by entry.savedStateHandle
                        .getStateFlow(Routes.KEY_CARD_FORM_SAVED, false)
                        .collectAsStateWithLifecycle()
                    val categoryFormSaved by entry.savedStateHandle
                        .getStateFlow(Routes.KEY_CATEGORY_FORM_SAVED, false)
                        .collectAsStateWithLifecycle()
                    CatalogScreen(
                        sessionViewModel = sessionViewModel,
                        onCreateCard = { categoryId ->
                            navController.navigate(Routes.cardCreate(categoryId))
                        },
                        onEditCard = { categoryId, cardId ->
                            navController.navigate(Routes.cardEdit(categoryId, cardId))
                        },
                        onOpenMembers = { categoryId ->
                            navController.navigate(Routes.categoryMembers(categoryId))
                        },
                        cardFormSaved = cardFormSaved,
                        onCardFormSavedConsumed = {
                            entry.savedStateHandle[Routes.KEY_CARD_FORM_SAVED] = false
                        },
                        onCreateCategory = { parentId ->
                            navController.navigate(Routes.categoryCreate(parentId))
                        },
                        onEditCategory = { categoryId ->
                            navController.navigate(Routes.categoryEdit(categoryId))
                        },
                        categoryFormSaved = categoryFormSaved,
                        onCategoryFormSavedConsumed = {
                            entry.savedStateHandle[Routes.KEY_CATEGORY_FORM_SAVED] = false
                        }
                    )
                }
                composable(
                    route = Routes.CATEGORY_MEMBERS,
                    arguments = listOf(
                        navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType }
                    )
                ) {
                    MembersScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.CARD_CREATE,
                    arguments = listOf(
                        navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType }
                    )
                ) {
                    CardFormScreen(
                        onSaved = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Routes.KEY_CARD_FORM_SAVED, true)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.CARD_EDIT,
                    arguments = listOf(
                        navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType },
                        navArgument(Routes.ARG_CARD_ID) { type = NavType.StringType }
                    )
                ) {
                    CardFormScreen(
                        onSaved = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Routes.KEY_CARD_FORM_SAVED, true)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.CATEGORY_CREATE,
                    arguments = listOf(
                        navArgument(Routes.ARG_PARENT_ID) { type = NavType.StringType }
                    )
                ) {
                    CategoryFormScreen(
                        onSaved = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Routes.KEY_CATEGORY_FORM_SAVED, true)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.CATEGORY_EDIT,
                    arguments = listOf(
                        navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType }
                    )
                ) {
                    CategoryFormScreen(
                        onSaved = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Routes.KEY_CATEGORY_FORM_SAVED, true)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
