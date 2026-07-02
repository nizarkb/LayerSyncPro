package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "layer_farm_logs",
    indices = [Index(value = ["kandangName", "date"], unique = true)]
)
data class LayerFarmLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kandangName: String,     // e.g. "Kandang A", "Kandang B"
    val date: String,            // YYYY-MM-DD
    val eggCount: Int,           // Eggs collected (pieces / butir)
    val eggWeight: Float,        // Eggs weight in kg
    val feedAmount: Float,       // Feed consumed in kg
    val chickenDead: Int,        // Mortality count (ekor)
    val notes: String = "",
    val isSynced: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val syncId: String? = null
)
