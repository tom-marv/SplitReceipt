package com.tommarv.splitreceipt.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommarv.splitreceipt.ui.EmptyState
import com.tommarv.splitreceipt.viewmodel.SplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: SplitViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val people by viewModel.people.collectAsState()
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showShareMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var savePlace by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(viewModel.t("save_to_log")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text(viewModel.t("event_name")) },
                        placeholder = { Text(viewModel.t("event_name_hint")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = savePlace,
                        onValueChange = { savePlace = it },
                        label = { Text("Luogo") },
                        placeholder = { Text(viewModel.t("place_hint")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCurrentSplit(saveName, savePlace)
                    showSaveDialog = false
                    saveName = ""
                    savePlace = ""
                }) {
                    Text(viewModel.t("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(viewModel.t("cancel"))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.t("final_summary_title").uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = viewModel.t("back"))
                    }
                },
                actions = {
                    if (people.isNotEmpty()) {
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Default.Save, contentDescription = viewModel.t("save"), tint = Color.White)
                        }
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = viewModel.t("share"), tint = Color.White)
                        }
                        
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(viewModel.t("share_synthetic")) },
                                leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    shareText(context, viewModel.generateSyntheticSummary(), viewModel.t("synthetic_chooser"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(viewModel.t("share_full")) },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    shareText(context, viewModel.generateFullSummary(), viewModel.t("full_chooser"))
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF004691), // Always SofaBlue
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (people.isEmpty()) {
            EmptyState(
                icon = Icons.Default.QueryStats,
                message = viewModel.t("no_data_to_show"),
                subMessage = viewModel.t("assign_desc"),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(people) { person ->
                    val total = viewModel.calculateTotalForPerson(person.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(person.id) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(person.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(viewModel.t("total_due"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "€ ${String.format("%.2f", total)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF004691),
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (isDarkMode) Color(0xFF64B5F6).copy(alpha = 0.5f) else Color(0xFF004691).copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: String,
    viewModel: SplitViewModel,
    onNavigateBack: () -> Unit
) {
    val people by viewModel.people.collectAsState()
    val person = people.find { it.id == personId }
    val assignedItems = viewModel.getItemsForPerson(personId)
    val total = viewModel.calculateTotalForPerson(personId)
    val discountPerPerson = viewModel.getDiscountPerPerson()
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showShareMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.name?.uppercase() ?: viewModel.t("detail"), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = viewModel.t("back"))
                    }
                },
                actions = {
                    IconButton(onClick = { showShareMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = viewModel.t("share"), tint = Color.White)
                    }
                    
                    DropdownMenu(
                        expanded = showShareMenu,
                        onDismissRequest = { showShareMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(viewModel.t("only_total")) },
                            leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null) },
                            onClick = {
                                showShareMenu = false
                                shareText(context, viewModel.generatePersonSyntheticSummary(personId), "${viewModel.t("send_total_to")} ${person?.name}")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(viewModel.t("total_with_details")) },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            onClick = {
                                showShareMenu = false
                                shareText(context, viewModel.generatePersonFullSummary(personId), "${viewModel.t("send_detail_to")} ${person?.name}")
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF004691), // Always SofaBlue
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(viewModel.t("cost_detail"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF004691))
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(assignedItems) { (item, share) ->
                    ListItem(
                        headlineContent = { Text(item.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { 
                            if (item.assignedPersonIds.size > 1) {
                                Text("${viewModel.t("divided_between")} ${item.assignedPersonIds.size} ${viewModel.t("people_count")}", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text(viewModel.t("full_share"), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        trailingContent = {
                            Text("€ ${String.format("%.2f", share)}", fontWeight = FontWeight.Bold)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
                
                if (discountPerPerson > 0) {
                    item {
                        ListItem(
                            headlineContent = { Text(viewModel.t("discount_applied"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(viewModel.t("divided_equally"), style = MaterialTheme.typography.labelSmall) },
                            trailingContent = {
                                Text("- € ${String.format("%.2f", discountPerPerson)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF004691) else MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.t("total").uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                    Text("€ ${String.format("%.2f", total)}", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun shareText(context: android.content.Context, text: String, chooserTitle: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}
