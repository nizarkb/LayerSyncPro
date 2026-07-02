package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "biosecurity_checks")
data class BiosecurityCheck(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,                      // YYYY-MM-DD (unique check per day is good, or multiple)
    val inspectorName: String = "Petugas",
    val footBathActive: Boolean = false,   // 1. Sanitasi alas kaki (Footbath) aktif & bersih
    val vehicleSpray: Boolean = false,     // 2. Penyemprotan desinfektan kendaraan masuk
    val feedWarehouseClean: Boolean = false, // 3. Gudang pakan bersih & bebas hama/tikus
    val cageWalkwayClean: Boolean = false,  // 4. Area sela kandang & jalan dibersihkan
    val safeMortalityDisposal: Boolean = false, // 5. Pembuangan bangkai ayam aman (dibakar/dikubur)
    val eggTrayDisinfected: Boolean = false, // 6. Tray telur disinfeksi sebelum masuk/keluar
    val waterSanitization: Boolean = false, // 7. Klorinasi/sanitasi air minum ayam berjalan
    val wildBirdControl: Boolean = false,   // 8. Kawat pelindung dari burung liar utuh
    val score: Int = 0,                    // Percentage of compliance (0 to 100)
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
