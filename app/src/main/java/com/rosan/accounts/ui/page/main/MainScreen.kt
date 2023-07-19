package com.rosan.accounts.ui.page.main

sealed class MainScreen(val route: String) {
    object UserManager : MainScreen("user")

    object AccountManager : MainScreen("user/{id}/account") {
        fun builder(id: Int): String =
            "user/$id/account"
    }
}