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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import lofod.products.ui.auth.LoginScreen
import lofod.products.ui.auth.RegisterScreen
import lofod.products.ui.catalog.CatalogScreen
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
                composable(Routes.CATALOG) {
                    CatalogScreen(sessionViewModel = sessionViewModel)
                }
            }
        }
    }
}
