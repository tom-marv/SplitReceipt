package com.tommarv.splitreceipt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ASSEGNA VOCI", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
                icon = Icons.Default.PersonAdd,
                message = "Nessun partecipante trovato",
                subMessage = "Aggiungi i tuoi amici nella sezione 'Persone' per iniziare a dividere il conto.",
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Top Action Row
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.assignAllToAll() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Dividi tutto", style = MaterialTheme.typography.labelMedium)
                            }
                            
                            OutlinedButton(
                                onClick = { viewModel.clearAllAssignments() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.HighlightOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Rimuovi tutto", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { 
                                discountText = it
                                viewModel.updateDiscount(it.replace(",", ".").toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text("Sconto", fontSize = 12.sp) },
                            prefix = { Text("€ ") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("€ ${String.format("%.2f", item.price)}", 
                                        style = MaterialTheme.typography.titleMedium, 
                                        color = if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF004691), 
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
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
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }

                                    items(people) { person ->
                                        val isAssigned = item.assignedPersonIds.contains(person.id)
                                        FilterChip(
                                            selected = isAssigned,
                                            onClick = { viewModel.toggleAssignment(item.id, person.id) },
                                            label = { Text(person.name) },
                                            leadingIcon = if (isAssigned) {
                                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else {
                                                { Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                                
                                if (item.assignedPersonIds.isNotEmpty()) {
                                    val splitPrice = item.price / item.assignedPersonIds.size
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            " Quota: € ${String.format("%.2f", splitPrice)} ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color(0xFFBBDEFB) else Color(0xFF004691)
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
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, subMessage: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(20.dp).size(40.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))
            Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(subMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        }
    }
}
