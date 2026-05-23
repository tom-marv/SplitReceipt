package com.tommarv.splitreceipt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tommarv.splitreceipt.viewmodel.SplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    viewModel: SplitViewModel,
    onNavigateBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val people by viewModel.people.collectAsState()
    val discount by viewModel.discount.collectAsState()
    var discountText by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assegna Voci") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        if (people.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aggiungi dei partecipanti prima di assegnare le voci.")
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Top Action Row
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.assignAllToAll() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tutto diviso tutti", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { 
                                discountText = it
                                viewModel.updateDiscount(it.replace(",", ".").toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text("Sconto (€)", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(0.8f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text("€ ${String.format("%.2f", item.price)}", style = MaterialTheme.typography.titleMedium)
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                Text("Assegna a:", style = MaterialTheme.typography.labelMedium)
                                
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    item {
                                        val isAllSelected = item.assignedPersonIds.size == people.size
                                        FilterChip(
                                            selected = isAllSelected,
                                            onClick = { viewModel.assignToAll(item.id) },
                                            label = { Text("TUTTI") },
                                            leadingIcon = {
                                                Icon(
                                                    if (isAllSelected) Icons.Default.CheckCircle else Icons.Default.Groups,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }

                                    items(people) { person ->
                                        val isAssigned = item.assignedPersonIds.contains(person.id)
                                        FilterChip(
                                            selected = isAssigned,
                                            onClick = { viewModel.toggleAssignment(item.id, person.id) },
                                            label = { Text(person.name) },
                                            leadingIcon = if (isAssigned) {
                                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                            } else {
                                                { Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                            }
                                        )
                                    }
                                }
                                
                                if (item.assignedPersonIds.isNotEmpty()) {
                                    val splitPrice = item.price / item.assignedPersonIds.size
                                    Text(
                                        "Quota per persona: € ${String.format("%.2f", splitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
