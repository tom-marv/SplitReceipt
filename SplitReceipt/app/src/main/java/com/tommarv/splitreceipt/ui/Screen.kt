package com.tommarv.splitreceipt.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val labelKey: String? = null, val icon: ImageVector? = null) {
    object Home : Screen("home", "home", Icons.Default.Receipt)
    object Scan : Screen("scan")
    object EditItems : Screen("edit_items")
    object Participants : Screen("participants")
    object Assignment : Screen("assignment")
    object Report : Screen("report")
    object History : Screen("history", "history", Icons.Default.History)
    object Settings : Screen("settings", "settings", Icons.Default.Settings)
    object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: String) = "person_detail/$personId"
    }
}
