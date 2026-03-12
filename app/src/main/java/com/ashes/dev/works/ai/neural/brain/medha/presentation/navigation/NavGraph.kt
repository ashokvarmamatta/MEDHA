package com.ashes.dev.works.ai.neural.brain.medha.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.about.AboutScreen
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatScreen
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatViewModel
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.logs.LogsScreen
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.settings.SettingsScreen

object Routes {
    const val CHAT = "chat"
    const val ABOUT = "about"
    const val LOGS = "logs"
    const val SETTINGS = "settings"
}

@Composable
fun MedhaNavGraph(viewModel: ChatViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CHAT,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        }
    ) {
        composable(Routes.CHAT) {
            ChatScreen(
                viewModel = viewModel,
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) },
                onNavigateToLogs = { navController.navigate(Routes.LOGS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.LOGS) {
            LogsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
