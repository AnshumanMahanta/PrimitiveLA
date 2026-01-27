package com.example.primitivela

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.primitivela.ui.theme.PrimitiveLATheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "attendance-db"
        ).build()
        val dao = db.attendanceDao()

        setContent {
            PrimitiveLATheme {
                val context = LocalContext.current
                var currentScreen by remember { mutableIntStateOf(0) }
                var activeEventId by remember { mutableIntStateOf(-1) }

                // --- NEW STATES FOR VIEWING RECORDS ---
                var showRecordsDialog by remember { mutableStateOf(false) }
                var recordsToView by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
                var viewingEventName by remember { mutableStateOf("") }

                // 1. Permission State
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    )
                }

                // 2. Permission Launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCameraPermission = isGranted
                    if (!isGranted) {
                        Toast.makeText(context, "Camera permission required to scan", Toast.LENGTH_LONG).show()
                    }
                }

                val events by dao.getAllEvents().collectAsState(initial = emptyList())

                if (currentScreen == 0) {
                    MainDashboard(
                        events = events,
                        onCreateEvent = { name ->
                            if (hasCameraPermission) {
                                lifecycleScope.launch {
                                    val id = dao.insertEvent(Event(name = name))
                                    activeEventId = id.toInt()
                                    currentScreen = 1
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onEventClick = { event ->
                            if (hasCameraPermission) {
                                activeEventId = event.id
                                currentScreen = 1
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onExportClick = { event, format ->
                            lifecycleScope.launch {
                                val records = dao.getRecordsForEvent(event.id)
                                if (records.isNotEmpty()) {
                                    shareEventData(this@MainActivity, event.name, records, format)
                                } else {
                                    Toast.makeText(this@MainActivity, "No scans to export!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeleteClick = { event ->
                            lifecycleScope.launch {
                                dao.deleteEvent(event)
                                Toast.makeText(this@MainActivity, "Event deleted", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewRecordsClick = { event ->
                            // --- LOGIC TO FETCH AND SHOW RECORDS ---
                            lifecycleScope.launch {
                                val records = dao.getRecordsForEvent(event.id)
                                recordsToView = records
                                viewingEventName = event.name
                                showRecordsDialog = true
                            }
                        }
                    )

                    // --- FULLSCREEN VIEW RECORDS DIALOG ---
                    if (showRecordsDialog) {
                        AlertDialog(
                            onDismissRequest = { showRecordsDialog = false },
                            title = { Text("Scans: $viewingEventName") },
                            text = {
                                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                                    if (recordsToView.isEmpty()) {
                                        Text("No IDs scanned yet for this event.")
                                    } else {
                                        LazyColumn {
                                            items(recordsToView) { record ->
                                                Text(
                                                    text = "• ${record.barcodeValue}",
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                // Corrected Divider for Material 3
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showRecordsDialog = false }) {
                                    Text("Done")
                                }
                            }
                        )
                    }

                } else {
                    ScannerScreen(
                        onIdScanned = { barcode ->
                            lifecycleScope.launch {
                                dao.insertRecord(AttendanceRecord(eventId = activeEventId, barcodeValue = barcode))
                            }
                        },
                        onCancel = { currentScreen = 0 }
                    )
                    BackHandler { currentScreen = 0 }
                }
            }
        }
    }
}

fun shareEventData(context: Context, eventName: String, records: List<AttendanceRecord>, format: String) {
    val fileName = "${eventName.replace(" ", "_")}.$format"
    val file = File(context.cacheDir, fileName)

    val content = if (format == "csv") {
        "Scanned_ID\n" + records.joinToString("\n") { it.barcodeValue }
    } else {
        records.joinToString("\n") { it.barcodeValue }
    }

    try {
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Attendance"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}