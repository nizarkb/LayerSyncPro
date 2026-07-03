package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(
    tableName = "layer_farm_logs",
    indices = [Index(value = ["kandangName", "date"], unique = true)]
)
@JsonClass(generateAdapter = true)
data class LayerFarmLog(
    @PrimaryKey @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "kandangName") val kandangName: String,     // e.g. "Kandang A", "Kandang B"
    @Json(name = "date") val date: String,            // YYYY-MM-DD
    @Json(name = "eggCount") val eggCount: Int,           // Eggs collected (pieces / butir)
    @Json(name = "eggWeight") val eggWeight: Float,        // Eggs weight in kg
    @Json(name = "feedAmount") val feedAmount: Float,       // Feed consumed in kg
    @Json(name = "chickenDead") val chickenDead: Int,        // Mortality count (ekor)
    @Json(name = "notes") val notes: String = "",
    @Json(name = "isSynced") val isSynced: Boolean = false,
    @Json(name = "lastUpdated") val lastUpdated: Long = System.currentTimeMillis(),
    @Json(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "syncId") val syncId: String? = null
)
