package com.tommarv.splitreceipt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tommarv.splitreceipt.viewmodel.SplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemsScreen(
    viewModel: SplitViewModel,
    onNavigateBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Voci") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Aggiungi Nuova Voce", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("Nome") },
                            modifier = Modifier.weight(1.5f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { newItemPrice = it },
                            label = { Text("Prezzo") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val price = newItemPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
                                viewModel.addItem(newItemName, price)
                                newItemName = ""
                                newItemPrice = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // IMPORTANT: Use key to uniquely identify items in the list
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        name = item.name,
                        price = item.price,
                        onDelete = { viewModel.removeItem(item.id) },
                        onUpdate = { name, price -> viewModel.updateItem(item.id, name, price) }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    name: String,
    price: Double,
    onDelete: () -> Unit,
    onUpdate: (String, Double) -> Unit
) {
    // Re-sync local state when the source data changes
    var editName by remember(name) { mutableStateOf(name) }
    var editPrice by remember(price) { mutableStateOf(price.toString()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = editName,
                onValueChange = { 
                    editName = it
                    onUpdate(it, editPrice.replace(",", ".").toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.weight(1.5f),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = editPrice,
                onValueChange = { 
                    editPrice = it
                    onUpdate(editName, it.replace(",", ".").toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
