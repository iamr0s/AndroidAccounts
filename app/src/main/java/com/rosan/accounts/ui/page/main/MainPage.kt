package com.rosan.accounts.ui.page.main

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.rosan.accounts.ui.page.account_manager.AccountManagerPage
import com.rosan.accounts.ui.page.user_manager.UserManagerPage

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainPage() {
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(
        navController = navController,
        startDestination = MainScreen.UserManager.route
    ) {
        composable(route = MainScreen.UserManager.route) {
            UserManagerPage(
                navController = navController
            )
        }
        composable(
            route = MainScreen.AccountManager.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentScope.SlideDirection.Up,
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentScope.SlideDirection.Down,
                )
            }
        ) {
            val userId = it.arguments?.getInt("id")
            if (userId == null) {
                navController.navigateUp()
                return@composable
            }
            AccountManagerPage(
                userId = userId,
                navController = navController
            )
        }
    }
}