
package com.l4kt.kofe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.l4kt.kofe.ui.screens.auth.LoginScreen
import com.l4kt.kofe.ui.screens.cafes.CafeListScreen
import com.l4kt.kofe.ui.screens.main.MainScreen
import com.l4kt.kofe.ui.screens.meetup.MeetupScreen

/**
/**
 * Main navigation graph for the app
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Main screen (Home, Matches, Profile tabs)
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToCafeList = { matchId ->
                    navController.navigate(Screen.CafeList.createRoute(matchId))
                },
                onNavigateToMeetup = { matchId ->
                    navController.navigate(Screen.Meetup.createRoute(matchId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // Cafe list screen
        composable(
            route = Screen.CafeList.route,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            CafeListScreen(
                matchId = matchId,
                onCafeSelected = { cafeId ->
                    navController.navigate(Screen.Meetup.createRoute(matchId)) {
                        popUpTo(Screen.Main.route)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Meetup screen
        composable(
            route = Screen.Meetup.route,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            MeetupScreen(
                matchId = matchId,
                onMeetupConfirmed = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToCafeList = {
                    navController.navigate(Screen.CafeList.createRoute(matchId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
 */