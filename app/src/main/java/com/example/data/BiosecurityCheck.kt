package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(tableName = "biosecurity_checks")
@JsonClass(generateAdapter = true)
data class BiosecurityCheck(
    @PrimaryKey @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "date") val date: String,                      // YYYY-MM-DD
    @Json(name = "inspectorName") val inspectorName: String = "Petugas",
    @Json(name = "footBathActive") val footBathActive: Boolean = false,   // 1. Sanitasi alas kaki (Footbath) aktif & bersih
    @Json(name = "vehicleSpray") val vehicleSpray: Boolean = false,     // 2. Penyemprotan desinfektan kendaraan masuk
    @Json(name = "feedWarehouseClean") val feedWarehouseClean: Boolean = false, // 3. Gudang pakan bersih & bebas hama/tikus
    @Json(name = "cageWalkwayClean") val cageWalkwayClean: Boolean = false,  // 4. Area sela kandang & jalan dibersihkan
    @Json(name = "safeMortalityDisposal") val safeMortalityDisposal: Boolean = false, // 5. Pembuangan bangkai ayam aman (dibakar/dikubur)
    @Json(name = "eggTrayDisinfected") val eggTrayDisinfected: Boolean = false, // 6. Tray telur disinfeksi sebelum masuk/keluar
    @Json(name = "waterSanitization") val waterSanitization: Boolean = false, // 7. Klorinasi/sanitasi air minum ayam berjalan
    @Json(name = "wildBirdControl") val wildBirdControl: Boolean = false,   // 8. Kawat pelindung dari burung liar utuh
    @Json(name = "score") val score: Int = 0,                    // Percentage of compliance (0 to 100)
    @Json(name = "notes") val notes: String = "",
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
