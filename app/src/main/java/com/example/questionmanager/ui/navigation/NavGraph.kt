package com.example.questionmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.questionmanager.ui.screen.detail.DetailScreen
import com.example.questionmanager.ui.screen.drilldown.DrillDownScreen
import com.example.questionmanager.ui.screen.home.HomeScreen
import com.example.questionmanager.ui.screen.input.InputScreen
import com.example.questionmanager.ui.screen.search.SearchScreen
import com.example.questionmanager.ui.screen.settings.SettingsScreen

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Input : Screen("input")
    data object Detail : Screen("detail/{questionId}") {
        fun createRoute(questionId: Long) = "detail/$questionId"
    }
    data object DrillDown : Screen("drilldown/{questionId}") {
        fun createRoute(questionId: Long) = "drilldown/$questionId"
    }
    data object Search : Screen("search")
    data object Settings : Screen("settings")
}

/**
 * 导航图
 */
@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // 首页 - 问题列表
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { questionId ->
                    navController.navigate(Screen.Detail.createRoute(questionId))
                },
                onNavigateToInput = {
                    navController.navigate(Screen.Input.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 问题输入页
        composable(Screen.Input.route) {
            InputScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 条目详情页
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("questionId") { type = NavType.LongType }
            )
        ) {
            DetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { questionId ->
                    navController.navigate(Screen.Detail.createRoute(questionId))
                },
                onNavigateToDrillDown = { questionId ->
                    navController.navigate(Screen.DrillDown.createRoute(questionId))
                }
            )
        }

        // 深挖选择页
        composable(
            route = Screen.DrillDown.route,
            arguments = listOf(
                navArgument("questionId") { type = NavType.LongType }
            )
        ) {
            DrillDownScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 搜索页
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { questionId ->
                    navController.navigate(Screen.Detail.createRoute(questionId))
                }
            )
        }

        // 设置页
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


