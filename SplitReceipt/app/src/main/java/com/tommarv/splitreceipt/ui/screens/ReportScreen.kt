package com.tommarv.splitreceipt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tommarv.splitreceipt.viewmodel.SplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: SplitViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val people by viewModel.people.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riepilogo Finale") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
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
                        .clickable { onNavigateToDetail(person.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(person.name, style = MaterialTheme.typography.titleLarge)
                            Text("Totale dovuto", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "€ ${String.format("%.2f", total)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio: ${person?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Dettaglio costi:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(assignedItems) { (item, share) ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { 
                            if (item.assignedPersonIds.size > 1) {
                                Text("Diviso tra ${item.assignedPersonIds.size} persone")
                            } else {
                                Text("Quota intera")
                            }
                        },
                        trailingContent = {
                            Text("€ ${String.format("%.2f", share)}")
                        }
                    )
                }
                
                if (discountPerPerson > 0) {
                    item {
                        ListItem(
                            headlineContent = { Text("Sconto applicato", color = MaterialTheme.colorScheme.error) },
                            supportingContent = { Text("Suddiviso equamente") },
                            trailingContent = {
                                Text("- € ${String.format("%.2f", discountPerPerson)}", color = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }
            
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTALE", style = MaterialTheme.typography.titleLarge)
                    Text("€ ${String.format("%.2f", total)}", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
