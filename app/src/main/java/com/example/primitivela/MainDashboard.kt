package com.example.primitivela // 1. Add your package name

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

// 2. This is the crucial import to connect your data classes
import com.example.primitivela.Event

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    events: List<Event>,
    onCreateEvent: (String) -> Unit,
    onEventClick: (Event) -> Unit,
    onExportClick: (Event, String) -> Unit
) {
    // ... rest of your code stays exactly the same
    var showDialog by remember { mutableStateOf(false) }
    var eventName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Event Attendance") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Event")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No events yet. Tap + to start.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(events) { event ->
                    EventItem(
                        event = event,
                        onClick = { onEventClick(event) },
                        onExport = { format -> onExportClick(event, format) }
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Create New Event") },
                text = {
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        label = { Text("Event Name (e.g. Morning Shift)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (eventName.isNotBlank()) {
                            onCreateEvent(eventName)
                            eventName = ""
                            showDialog = false
                        }
                    }) { Text("Start Scanning") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun EventItem(
    event: Event,
    onClick: () -> Unit,
    onExport: (String) -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(event.createdAt))
    var showExportMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.name, style = MaterialTheme.typography.titleLarge)
                Text(text = "Created: $date", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Export Button
            Box {
                IconButton(onClick = { showExportMenu = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Export")
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export as .CSV") },
                        onClick = {
                            onExport("csv")
                            showExportMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as .TXT") },
                        onClick = {
                            onExport("txt")
                            showExportMenu = false
                        }
                    )
                }
            }
        }
    }
}