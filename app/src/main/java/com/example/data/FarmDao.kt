package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    @Query("SELECT * FROM layer_farm_logs ORDER BY date DESC, kandangName ASC")
    fun getAllLogs(): Flow<List<LayerFarmLog>>

    @Query("SELECT * FROM layer_farm_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<LayerFarmLog>

    @Query("SELECT * FROM layer_farm_logs WHERE kandangName = :kandangName AND date = :date LIMIT 1")
    suspend fun getLogByKandangAndDate(kandangName: String, date: String): LayerFarmLog?

    @Query("SELECT * FROM layer_farm_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: String): LayerFarmLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LayerFarmLog)

    @Update
    suspend fun updateLog(log: LayerFarmLog)

    @Query("DELETE FROM layer_farm_logs WHERE id = :id")
    suspend fun deleteLogById(id: String)

    @Query("UPDATE layer_farm_logs SET isSynced = 1, syncId = :syncId, lastUpdated = :syncTime WHERE id = :id")
    suspend fun markAsSynced(id: String, syncId: String, syncTime: Long)

    @Query("UPDATE layer_farm_logs SET isSynced = 0, lastUpdated = :updateTime WHERE id = :id")
    suspend fun markAsUnsynced(id: String, updateTime: Long)

    // Vaccination Schedule Queries
    @Query("SELECT * FROM vaccination_schedules ORDER BY plannedDate ASC")
    fun getAllVaccinations(): Flow<List<VaccinationSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccination(schedule: VaccinationSchedule)

    @Update
    suspend fun updateVaccination(schedule: VaccinationSchedule)

    @Query("DELETE FROM vaccination_schedules WHERE id = :id")
    suspend fun deleteVaccinationById(id: String)

    // Biosecurity Check Queries
    @Query("SELECT * FROM biosecurity_checks ORDER BY date DESC")
    fun getAllBiosecurityChecks(): Flow<List<BiosecurityCheck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBiosecurityCheck(check: BiosecurityCheck)

    @Query("SELECT * FROM biosecurity_checks WHERE date = :date LIMIT 1")
    suspend fun getBiosecurityCheckByDate(date: String): BiosecurityCheck?

    @Query("DELETE FROM biosecurity_checks WHERE id = :id")
    suspend fun deleteBiosecurityCheckById(id: String)

    // Sync Helper Queries
    @Query("SELECT * FROM layer_farm_logs WHERE lastUpdated > :timestamp")
    suspend fun getLogsUpdatedAfter(timestamp: Long): List<LayerFarmLog>

    @Query("SELECT * FROM vaccination_schedules WHERE lastUpdated > :timestamp")
    suspend fun getVaccinationsUpdatedAfter(timestamp: Long): List<VaccinationSchedule>

    @Query("SELECT * FROM biosecurity_checks WHERE timestamp > :timestamp")
    suspend fun getBiosecurityChecksUpdatedAfter(timestamp: Long): List<BiosecurityCheck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LayerFarmLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccinations(vaccinations: List<VaccinationSchedule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBiosecurityChecks(checks: List<BiosecurityCheck>)
}
