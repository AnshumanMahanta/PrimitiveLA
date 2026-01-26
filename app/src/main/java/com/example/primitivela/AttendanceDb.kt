package com.example.primitivela // 1. CRITICAL: Ensure this matches your package name

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. The "Note" or Event table
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

// 2. The Scanned Barcodes table
@Entity(tableName = "attendance")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int, // Connects this scan to a specific event
    val barcodeValue: String
)

// 3. The Data Access Object (Commands)
@Dao
interface AttendanceDao {
    @Insert
    suspend fun insertEvent(event: Event): Long

    @Insert
    suspend fun insertRecord(record: AttendanceRecord)

    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM attendance WHERE eventId = :eventId")
    suspend fun getRecordsForEvent(eventId: Int): List<AttendanceRecord>

    @Delete
    suspend fun deleteEvent(event: Event)
}

// 4. The Database Holder
// Added exportSchema = false to simplify the build process
@Database(entities = [Event::class, AttendanceRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}