package com.tommarv.splitreceipt.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tommarv.splitreceipt.viewmodel.SplitViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ParticipantsScreen(
    viewModel: SplitViewModel,
    onNavigateBack: () -> Unit
) {
    val people by viewModel.people.collectAsState()
    val savedNames by viewModel.savedNames.collectAsState()
    
    var newName by remember { mutableStateOf("") }
    var personCount by remember { mutableStateOf("") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Cancella Cronologia?") },
            text = { Text("Tutti i nomi suggeriti verranno eliminati definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("CANCELLA", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PARTECIPANTI", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (savedNames.isNotEmpty()) {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(Icons.Default.HistoryToggleOff, contentDescription = "Pulisci cronologia", tint = Color.White)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Bulk Add Utility
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = personCount,
                        onValueChange = { personCount = it },
                        label = { Text("Num. Persone", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val count = personCount.toIntOrNull() ?: 0
                            if (count > 0) {
                                viewModel.addMultiplePeople(count)
                                personCount = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Genera")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Manual Add
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Aggiungi per nome") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.addPerson(newName)
                        newName = ""
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                }
            }
            
            // Reorderable Saved Names with Deletion
            if (savedNames.isNotEmpty()) {
                Text(
                    "SUGGERITI (Trascina o premi X)", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                
                val listState = rememberLazyListState()
                var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
                var dragOffset by remember { mutableStateOf(0f) }

                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val index = listState.layoutInfo.visibleItemsInfo.find { 
                                        offset.x.toInt() in it.offset..(it.offset + it.size)
                                    }?.index
                                    draggedItemIndex = index
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.x
                                    
                                    val currentDraggedIndex = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                    val targetIndex = listState.layoutInfo.visibleItemsInfo.find { 
                                        (listState.layoutInfo.visibleItemsInfo[currentDraggedIndex.coerceIn(0, listState.layoutInfo.visibleItemsInfo.size - 1)].offset + dragOffset).toInt() in it.offset..(it.offset + it.size)
                                    }?.index

                                    if (targetIndex != null && targetIndex != currentDraggedIndex) {
                                        viewModel.moveName(currentDraggedIndex, targetIndex)
                                        draggedItemIndex = targetIndex
                                        dragOffset = 0f
                                    }
                                },
                                onDragEnd = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                }
                            )
                        }
                ) {
                    itemsIndexed(savedNames, key = { _, name -> name }) { index, name ->
                        val isDragging = index == draggedItemIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = if (isDragging) 8.dp else 0.dp,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = if (isDragging) dragOffset else 0f
                                    scaleX = if (isDragging) 1.1f else 1f
                                    scaleY = if (isDragging) 1.1f else 1f
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.clickable { viewModel.addPerson(name) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(name, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deleteSavedName(name) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Rimuovi", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Text("LISTA CORRENTE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(people, key = { _, person -> person.id }) { _, person ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(person.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(
                                onClick = { viewModel.removePerson(person.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
