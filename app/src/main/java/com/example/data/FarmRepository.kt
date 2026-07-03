package com.example.data

import kotlinx.coroutines.flow.Flow

class FarmRepository(private val farmDao: FarmDao) {

    val allLogs: Flow<List<LayerFarmLog>> = farmDao.getAllLogs()

    suspend fun getLogByKandangAndDate(kandangName: String, date: String): LayerFarmLog? {
        return farmDao.getLogByKandangAndDate(kandangName, date)
    }

    suspend fun getLogById(id: String): LayerFarmLog? {
        return farmDao.getLogById(id)
    }

    /**
     * Local Storage-First approach: Always save to local database first.
     * Set [isSynced] to false and update the [lastUpdated] timestamp.
     */
    suspend fun saveLog(log: LayerFarmLog) {
        val updatedLog = log.copy(
            isSynced = false,
            lastUpdated = System.currentTimeMillis()
        )
        farmDao.insertLog(updatedLog)
    }

    suspend fun deleteLog(id: String) {
        farmDao.deleteLogById(id)
    }

    suspend fun getUnsyncedLogs(): List<LayerFarmLog> {
        return farmDao.getUnsyncedLogs()
    }

    suspend fun markAsSynced(id: String, syncId: String) {
        farmDao.markAsSynced(id, syncId, System.currentTimeMillis())
    }

    suspend fun insertOrUpdateFromSync(log: LayerFarmLog) {
        farmDao.insertLog(log)
    }

    // Health and Sanitation Planner Data Access
    val allVaccinations: Flow<List<VaccinationSchedule>> = farmDao.getAllVaccinations()
    val allBiosecurityChecks: Flow<List<BiosecurityCheck>> = farmDao.getAllBiosecurityChecks()

    suspend fun saveVaccination(schedule: VaccinationSchedule) {
        val updated = schedule.copy(lastUpdated = System.currentTimeMillis())
        farmDao.insertVaccination(updated)
    }

    suspend fun updateVaccination(schedule: VaccinationSchedule) {
        val updated = schedule.copy(lastUpdated = System.currentTimeMillis())
        farmDao.updateVaccination(updated)
    }

    suspend fun deleteVaccination(id: String) {
        farmDao.deleteVaccinationById(id)
    }

    suspend fun saveBiosecurityCheck(check: BiosecurityCheck) {
        val updated = check.copy(timestamp = System.currentTimeMillis())
        farmDao.insertBiosecurityCheck(updated)
    }

    suspend fun getBiosecurityCheckByDate(date: String): BiosecurityCheck? {
        return farmDao.getBiosecurityCheckByDate(date)
    }

    suspend fun deleteBiosecurityCheck(id: String) {
        farmDao.deleteBiosecurityCheckById(id)
    }

    // Sync helpers
    suspend fun getLogsUpdatedAfter(timestamp: Long): List<LayerFarmLog> =
        farmDao.getLogsUpdatedAfter(timestamp)

    suspend fun getVaccinationsUpdatedAfter(timestamp: Long): List<VaccinationSchedule> =
        farmDao.getVaccinationsUpdatedAfter(timestamp)

    suspend fun getBiosecurityChecksUpdatedAfter(timestamp: Long): List<BiosecurityCheck> =
        farmDao.getBiosecurityChecksUpdatedAfter(timestamp)

    suspend fun insertLogs(logs: List<LayerFarmLog>) =
        farmDao.insertLogs(logs)

    suspend fun insertVaccinations(vaccinations: List<VaccinationSchedule>) =
        farmDao.insertVaccinations(vaccinations)

    suspend fun insertBiosecurityChecks(checks: List<BiosecurityCheck>) =
        farmDao.insertBiosecurityChecks(checks)
}
