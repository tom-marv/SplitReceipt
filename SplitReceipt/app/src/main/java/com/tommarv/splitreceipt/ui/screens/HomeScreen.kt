package com.tommarv.splitreceipt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommarv.splitreceipt.viewmodel.SplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SplitViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToParticipants: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToAssignment: () -> Unit,
    onNavigateToReport: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(viewModel.t("reset_all")) },
            text = { Text(viewModel.t("reset_desc")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) {
                    Text(viewModel.t("reset"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(viewModel.t("cancel"))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SplitReceipt", 
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Tema",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Pulisci tutto", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF004691), // Always SofaBlue
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernCardButton(
                        viewModel.t("start_scan"), 
                        viewModel.t("extract_ai"),
                        Icons.Default.CameraEnhance,
                        Color(0xFF32A852), // Always SofaAccent Green
                        onNavigateToScan,
                        iconColor = Color.White
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CompactCardButton(viewModel.t("people"), viewModel.t("who_participated"), Icons.Default.Group, Modifier.weight(1f), onNavigateToParticipants)
                        CompactCardButton(viewModel.t("items"), viewModel.t("check_prices"), Icons.Default.ListAlt, Modifier.weight(1f), onNavigateToItems)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(viewModel.t("processing"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF004691))

                    ModernCardButton(
                        viewModel.t("split_assign"), 
                        viewModel.t("select_who_pays"),
                        Icons.Default.Balance,
                        MaterialTheme.colorScheme.surfaceVariant,
                        onNavigateToAssignment,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF004691)
                    )

                    ModernCardButton(
                        viewModel.t("view_result"), 
                        viewModel.t("final_bills"),
                        Icons.Default.Receipt, 
                        MaterialTheme.colorScheme.primaryContainer,
                        onNavigateToReport,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF004691)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernCardButton(
    title: String, 
    desc: String,
    icon: ImageVector, 
    containerColor: Color,
    onClick: () -> Unit,
    textColor: Color = Color.White,
    iconColor: Color = Color.White
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = textColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun CompactCardButton(desc: String, title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), lineHeight = 12.sp)
            }
        }
    }
}
