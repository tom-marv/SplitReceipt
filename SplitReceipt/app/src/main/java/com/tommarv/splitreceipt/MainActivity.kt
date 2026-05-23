package com.tommarv.splitreceipt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tommarv.splitreceipt.ui.Screen
import com.tommarv.splitreceipt.ui.screens.*
import com.tommarv.splitreceipt.ui.theme.SplitReceiptTheme
import com.tommarv.splitreceipt.viewmodel.SplitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SplitViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            SplitReceiptTheme(darkTheme = isDarkMode) {
                SplitReceiptApp(viewModel)
            }
        }
    }
}

@Composable
fun SplitReceiptApp(viewModel: SplitViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                onNavigateToParticipants = { navController.navigate(Screen.Participants.route) },
                onNavigateToItems = { navController.navigate(Screen.EditItems.route) },
                onNavigateToAssignment = { navController.navigate(Screen.Assignment.route) },
                onNavigateToReport = { navController.navigate(Screen.Report.route) }
            )
        }
        composable(Screen.Scan.route) {
            ScanScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onScanComplete = { 
                    navController.navigate(Screen.EditItems.route) {
                        popUpTo(Screen.Scan.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.EditItems.route) {
            EditItemsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Participants.route) {
            ParticipantsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Assignment.route) {
            AssignmentScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Report.route) {
            ReportScreen(
                viewModel = viewModel,
                onNavigateToDetail = { personId -> 
                    navController.navigate(Screen.PersonDetail.createRoute(personId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PersonDetail.route) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: return@composable
            PersonDetailScreen(
                personId = personId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
