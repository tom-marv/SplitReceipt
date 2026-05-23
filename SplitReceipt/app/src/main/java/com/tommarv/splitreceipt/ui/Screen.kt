package com.tommarv.splitreceipt.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object EditItems : Screen("edit_items")
    object Participants : Screen("participants")
    object Assignment : Screen("assignment")
    object Report : Screen("report")
    object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: String) = "person_detail/$personId"
    }
}
