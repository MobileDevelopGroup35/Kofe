package com.l4kt.kofe.navigation

/**
 * Sealed class defining all navigation routes in the app
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object CafeList : Screen("cafe_list/{matchId}") {
        fun createRoute(matchId: String) = "cafe_list/$matchId"
    }
    object Meetup : Screen("meetup/{matchId}") {
        fun createRoute(matchId: String) = "meetup/$matchId"
    }
}

