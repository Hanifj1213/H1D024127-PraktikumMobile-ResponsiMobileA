package com.hajun.layar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hajun.layar.data.ShowRepository
import com.hajun.layar.ui.detail.DetailScreen
import com.hajun.layar.ui.detail.DetailViewModel
import com.hajun.layar.ui.home.HomeScreen
import com.hajun.layar.ui.home.HomeViewModel

@Composable
fun LayarApp(repository: ShowRepository) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(route = "home") {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(repository),
            )
            val state by homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = state,
                onQueryChanged = homeViewModel::onQueryChanged,
                onSearch = homeViewModel::searchNow,
                onRetry = homeViewModel::retry,
                onGenreSelected = homeViewModel::onGenreSelected,
                onPreviousPage = homeViewModel::previousPage,
                onNextPage = homeViewModel::nextPage,
                onRetryPage = homeViewModel::retryPage,
                onShuffleSuggestions = homeViewModel::shuffleSuggestions,
                onShowClick = { showId ->
                    navController.navigate("detail/$showId") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = "detail/{showId}",
            arguments = listOf(navArgument("showId") { type = NavType.IntType }),
        ) {
            val detailViewModel: DetailViewModel = viewModel(
                factory = DetailViewModel.factory(repository),
            )
            val state by detailViewModel.uiState.collectAsStateWithLifecycle()
            DetailScreen(
                uiState = state,
                onBack = { navController.popBackStack() },
                onRetry = detailViewModel::retry,
            )
        }
    }
}
