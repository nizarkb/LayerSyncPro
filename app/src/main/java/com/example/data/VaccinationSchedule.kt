package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(tableName = "vaccination_schedules")
@JsonClass(generateAdapter = true)
data class VaccinationSchedule(
    @PrimaryKey @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "kandangName") val kandangName: String,     // "Semua Kandang", "Kandang A", etc.
    @Json(name = "vaccineName") val vaccineName: String,     // e.g., "ND-IB Clone", "Gumboro", "AI (Avian Influenza)"
    @Json(name = "plannedDate") val plannedDate: String,     // YYYY-MM-DD
    @Json(name = "actualDate") val actualDate: String? = null, // YYYY-MM-DD if completed
    @Json(name = "method") val method: String,          // e.g., "Air Minum", "Suntik", "Tetes Mata", "Tetes Mulut", "Spray", "Tusuk Sayap"
    @Json(name = "status") val status: String = "Pending", // "Pending" or "Completed"
    @Json(name = "notes") val notes: String = "",
    @Json(name = "ageWeeks") val ageWeeks: Int? = null,   // Expected age in weeks if based on schedule
    @Json(name = "lastUpdated") val lastUpdated: Long = System.currentTimeMillis()
)
