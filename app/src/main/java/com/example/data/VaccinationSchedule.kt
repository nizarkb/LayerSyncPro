package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "vaccination_schedules")
data class VaccinationSchedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kandangName: String,     // "Semua Kandang", "Kandang A", etc.
    val vaccineName: String,     // e.g., "ND-IB Clone", "Gumboro", "AI (Avian Influenza)"
    val plannedDate: String,     // YYYY-MM-DD
    val actualDate: String? = null, // YYYY-MM-DD if completed
    val method: String,          // e.g., "Air Minum", "Suntik", "Tetes Mata", "Tetes Mulut", "Spray", "Tusuk Sayap"
    val status: String = "Pending", // "Pending" or "Completed"
    val notes: String = "",
    val ageWeeks: Int? = null,   // Expected age in weeks if based on schedule
    val lastUpdated: Long = System.currentTimeMillis()
)
