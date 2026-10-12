package com.fexed.checklist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fexed.checklist.data.local.ChecklistDao
import com.fexed.checklist.data.preferences.StartPageOption
import com.fexed.checklist.data.preferences.UserPreferences
import com.fexed.checklist.data.repository.ChecklistRepository
import com.fexed.checklist.ui.screens.detail.ChecklistDetailScreen
import com.fexed.checklist.ui.screens.detail.ChecklistDetailViewModel
import com.fexed.checklist.ui.screens.detail.ChecklistDetailViewModelFactory
import com.fexed.checklist.ui.screens.overview.OverviewScreen
import com.fexed.checklist.ui.screens.overview.OverviewViewModel
import com.fexed.checklist.ui.screens.overview.OverviewViewModelFactory

sealed class Screen(val route: String) {
    object Overview : Screen("overview")
    object Detail : Screen("detail/{checklistId}") {
        fun createRoute(checklistId: Long) = "detail/$checklistId"
    }
}

@Composable
fun ChecklistNavGraph(
    navController: NavHostController,
    repository: ChecklistRepository,
    checklistDao: ChecklistDao,
    userPreferences: UserPreferences?,
    initialChecklistId: Long? = null,
    modifier: Modifier = Modifier
) {
    var hasHandledInitialNavigation by remember { mutableStateOf(false) }

    LaunchedEffect(userPreferences, initialChecklistId) {
        if (!hasHandledInitialNavigation) {
            if (initialChecklistId != null) {
                hasHandledInitialNavigation = true
                navController.navigate(Screen.Detail.createRoute(initialChecklistId))
            } else if (userPreferences != null) {
                hasHandledInitialNavigation = true
                if (userPreferences.startPage == StartPageOption.LATEST_OPENED) {
                    val targetId = userPreferences.lastOpenedChecklistId
                        ?: repository.getLatestOpenedChecklist()?.id
                    if (targetId != null) {
                        navController.navigate(Screen.Detail.createRoute(targetId))
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Overview.route,
        modifier = modifier
    ) {
        composable(route = Screen.Overview.route) {
            val overviewViewModel: OverviewViewModel = viewModel(
                factory = OverviewViewModelFactory(repository, checklistDao)
            )
            OverviewScreen(
                viewModel = overviewViewModel,
                onOpenChecklist = { checklistId ->
                    navController.navigate(Screen.Detail.createRoute(checklistId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("checklistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getLong("checklistId") ?: 0L
            val detailViewModel: ChecklistDetailViewModel = viewModel(
                key = "detail_$checklistId",
                factory = ChecklistDetailViewModelFactory(checklistId, repository)
            )
            ChecklistDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
