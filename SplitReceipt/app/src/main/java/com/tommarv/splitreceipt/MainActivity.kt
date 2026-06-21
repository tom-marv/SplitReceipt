package com.tommarv.splitreceipt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
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
            
            var showUpdateDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                checkUpdate { showUpdateDialog = true }
            }

            SplitReceiptTheme(darkTheme = isDarkMode) {
                if (showUpdateDialog) {
                    UpdateDialog(
                        onDismiss = { showUpdateDialog = false },
                        onUpdate = {
                            showUpdateDialog = false
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=$packageName")
                                setPackage("com.android.vending")
                            }
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to browser
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                            }
                        }
                    )
                }
                SplitReceiptApp(viewModel)
            }
        }
    }

    private fun checkUpdate(onUpdateAvailable: () -> Unit) {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                onUpdateAvailable()
            }
        }
    }
}

@Composable
fun UpdateDialog(onDismiss: () -> Unit, onUpdate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiornamento Disponibile") },
        text = { Text("È disponibile una nuova versione dell'app sullo store. Vuoi aggiornare ora?") },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text("AGGIORNA")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CONTINUA")
            }
        }
    )
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
                onNavigateToReport = { navController.navigate(Screen.Report.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
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
