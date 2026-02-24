package com.example.primitivela

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    events: List<Event>,
    onCreateEvent: (String) -> Unit,
    onEventClick: (Event) -> Unit,
    onExportClick: (Event, String) -> Unit,
    onDeleteClick: (Event) -> Unit,
    onViewRecordsClick: (Event) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var eventName by remember { mutableStateOf("") }

    // Logic for the light gray background in Night Mode
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF1A1A1A) else Color.White

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            // FIXED TOP SECTION: Stays put while the list scrolls below
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Primitive-LA",
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF121212), // Matte Black
                        titleContentColor = Color.White
                    )
                )
                // THE GREY DIVIDER
                HorizontalDivider(
                    thickness = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.6f)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Event")
            }
        }
    ) { padding ->
        // Content wrapper
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No events yet. Tap + to start.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // contentPadding handles the space between the divider and the first item
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        EventItem(
                            event = event,
                            onClick = { onEventClick(event) },
                            onExport = { format -> onExportClick(event, format) },
                            onDelete = { onDeleteClick(event) },
                            onView = { onViewRecordsClick(event) }
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
}

@Composable
fun EventItem(
    event: Event,
    onClick: () -> Unit,
    onExport: (String) -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(event.createdAt))
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Created: $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        text = { Text("View Scans") },
                        onClick = {
                            onView()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        text = { Text("Export as .CSV") },
                        onClick = {
                            onExport("csv")
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        text = { Text("Export as .TXT") },
                        onClick = {
                            onExport("txt")
                            showMenu = false
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete", color = Color.Red)
                            }
                        },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}