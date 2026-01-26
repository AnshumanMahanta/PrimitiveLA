package com.example.primitivela

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
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

        // Initialize Database
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "attendance-db"
        ).build()
        val dao = db.attendanceDao()

        setContent {
            PrimitiveLATheme {
                // 0 = Dashboard, 1 = Scanner
                var currentScreen by remember { mutableIntStateOf(0) }
                var activeEventId by remember { mutableIntStateOf(-1) }

                // Watch the database for changes
                val events by dao.getAllEvents().collectAsState(initial = emptyList())

                if (currentScreen == 0) {
                    MainDashboard(
                        events = events,
                        onCreateEvent = { name ->
                            lifecycleScope.launch {
                                val id = dao.insertEvent(Event(name = name))
                                activeEventId = id.toInt()
                                currentScreen = 1
                            }
                        },
                        onEventClick = { event ->
                            activeEventId = event.id
                            currentScreen = 1
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
                        }
                    )
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

// Global helper function for sharing files
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