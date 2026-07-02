package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ResolutionStrategy {
    LAST_WRITE_WINS,
    LOCAL_WINS,
    REMOTE_WINS,
    MANUAL
}

data class RemoteLog(
    val id: String,
    val kandangName: String,
    val date: String,
    val eggCount: Int,
    val eggWeight: Float,
    val feedAmount: Float,
    val chickenDead: Int,
    val notes: String = "",
    val lastUpdated: Long
)

data class SyncConflict(
    val localLog: LayerFarmLog,
    val remoteLog: RemoteLog,
    val message: String
)

class SyncManager(private val repository: FarmRepository) {

    // Simulate Internet Status
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Simulate Cloud Database
    private val _cloudDatabase = MutableStateFlow<List<RemoteLog>>(
        listOf(
            RemoteLog(
                id = "cloud-uuid-1",
                kandangName = "Kandang A",
                date = "2026-06-28",
                eggCount = 480,
                eggWeight = 30.5f,
                feedAmount = 50.0f,
                chickenDead = 1,
                notes = "Suhu kandang stabil.",
                lastUpdated = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            RemoteLog(
                id = "cloud-uuid-2",
                kandangName = "Kandang B",
                date = "2026-06-28",
                eggCount = 450,
                eggWeight = 28.2f,
                feedAmount = 48.0f,
                chickenDead = 0,
                notes = "Ayam terlihat aktif.",
                lastUpdated = System.currentTimeMillis() - 86400000 // 1 day ago
            )
        )
    )
    val cloudDatabase: StateFlow<List<RemoteLog>> = _cloudDatabase.asStateFlow()

    // Current Sync Conflicts
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()

    // Sync status message for UI feedback
    private val _syncStatusMessage = MutableStateFlow("Sistem Siap Sinkronisasi")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Resolution strategy choice
    private val _strategy = MutableStateFlow(ResolutionStrategy.LAST_WRITE_WINS)
    val strategy: StateFlow<ResolutionStrategy> = _strategy.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
        if (!online) {
            _syncStatusMessage.value = "Koneksi Terputus (Mode Offline)"
        } else {
            _syncStatusMessage.value = "Koneksi Terhubung (Mode Online)"
        }
    }

    fun setStrategy(strategy: ResolutionStrategy) {
        _strategy.value = strategy
        _syncStatusMessage.value = "Strategi konflik diubah: ${strategy.name}"
    }

    // Add mock item to cloud to trigger a conflict or new download
    fun injectCloudUpdate(
        id: String,
        kandangName: String,
        date: String,
        eggCount: Int,
        eggWeight: Float,
        feedAmount: Float,
        chickenDead: Int,
        notes: String
    ) {
        val existing = _cloudDatabase.value.find { it.id == id || (it.kandangName == kandangName && it.date == date) }
        val updatedList = if (existing != null) {
            _cloudDatabase.value.map {
                if (it.id == existing.id) {
                    it.copy(
                        eggCount = eggCount,
                        eggWeight = eggWeight,
                        feedAmount = feedAmount,
                        chickenDead = chickenDead,
                        notes = notes,
                        lastUpdated = System.currentTimeMillis() // simulated newer modification
                    )
                } else it
            }
        } else {
            _cloudDatabase.value + RemoteLog(
                id = id,
                kandangName = kandangName,
                date = date,
                eggCount = eggCount,
                eggWeight = eggWeight,
                feedAmount = feedAmount,
                chickenDead = chickenDead,
                notes = notes,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _cloudDatabase.value = updatedList
        _syncStatusMessage.value = "Simulasi: Data baru dimodifikasi di Cloud! Siap disinkronkan."
    }

    /**
     * Core Sync logic.
     * We simulate background/foreground synchronization.
     */
    suspend fun performSync() {
        if (!_isOnline.value) {
            _syncStatusMessage.value = "Gagal: Tidak ada koneksi internet!"
            return
        }

        _isSyncing.value = true
        _syncStatusMessage.value = "Sinkronisasi sedang berlangsung..."
        _conflicts.value = emptyList()

        // Simulate network latency (very realistic!)
        kotlinx.coroutines.delay(1200)

        val localUnsynced = repository.getUnsyncedLogs()
        val currentCloud = _cloudDatabase.value.toMutableList()
        val unresolvedConflicts = mutableListOf<SyncConflict>()

        // 1. PUSH local changes to Cloud & resolve conflicts
        for (localLog in localUnsynced) {
            // Find match by id OR by kandangName + date
            val remoteMatch = currentCloud.find { it.id == localLog.id || (it.kandangName == localLog.kandangName && it.date == localLog.date) }

            if (remoteMatch != null) {
                // Collision / overlapping update!
                if (localLog.isIdenticalTo(remoteMatch)) {
                    // Exactly identical, just mark as synced
                    repository.markAsSynced(localLog.id, remoteMatch.id)
                } else {
                    when (_strategy.value) {
                        ResolutionStrategy.LAST_WRITE_WINS -> {
                            if (localLog.lastUpdated > remoteMatch.lastUpdated) {
                                // Local is newer, overwrite cloud
                                val index = currentCloud.indexOf(remoteMatch)
                                currentCloud[index] = remoteMatch.copyFromLocal(localLog)
                                repository.markAsSynced(localLog.id, remoteMatch.id)
                            } else {
                                // Cloud is newer, overwrite local
                                repository.insertOrUpdateFromSync(localLog.copyFromRemote(remoteMatch, isSynced = true))
                            }
                        }
                        ResolutionStrategy.LOCAL_WINS -> {
                            // Local wins, update cloud
                            val index = currentCloud.indexOf(remoteMatch)
                            currentCloud[index] = remoteMatch.copyFromLocal(localLog)
                            repository.markAsSynced(localLog.id, remoteMatch.id)
                        }
                        ResolutionStrategy.REMOTE_WINS -> {
                            // Remote wins, update local
                            repository.insertOrUpdateFromSync(localLog.copyFromRemote(remoteMatch, isSynced = true))
                        }
                        ResolutionStrategy.MANUAL -> {
                            // Let the user choose manually
                            unresolvedConflicts.add(
                                SyncConflict(
                                    localLog = localLog,
                                    remoteLog = remoteMatch,
                                    message = "Perbedaan data ${localLog.kandangName} (${localLog.date})"
                                )
                            )
                        }
                    }
                }
            } else {
                // No cloud matching record: Simply upload!
                val remoteId = localLog.syncId ?: UUID.randomUUID().toString()
                currentCloud.add(
                    RemoteLog(
                        id = remoteId,
                        kandangName = localLog.kandangName,
                        date = localLog.date,
                        eggCount = localLog.eggCount,
                        eggWeight = localLog.eggWeight,
                        feedAmount = localLog.feedAmount,
                        chickenDead = localLog.chickenDead,
                        notes = localLog.notes,
                        lastUpdated = localLog.lastUpdated
                    )
                )
                repository.markAsSynced(localLog.id, remoteId)
            }
        }

        // Update Simulated Cloud
        _cloudDatabase.value = currentCloud

        // 2. PULL new/updated data from cloud to local
        val updatedCloud = _cloudDatabase.value
        for (remoteLog in updatedCloud) {
            val localLog = repository.getLogByKandangAndDate(remoteLog.kandangName, remoteLog.date)
            if (localLog == null) {
                // New record on the server, insert locally
                repository.insertOrUpdateFromSync(
                    LayerFarmLog(
                        id = remoteLog.id,
                        kandangName = remoteLog.kandangName,
                        date = remoteLog.date,
                        eggCount = remoteLog.eggCount,
                        eggWeight = remoteLog.eggWeight,
                        feedAmount = remoteLog.feedAmount,
                        chickenDead = remoteLog.chickenDead,
                        notes = remoteLog.notes,
                        isSynced = true,
                        lastUpdated = remoteLog.lastUpdated,
                        createdAt = remoteLog.lastUpdated,
                        syncId = remoteLog.id
                    )
                )
            } else {
                // Record exists. If local is already synced, and remote is newer, update local.
                if (localLog.isSynced && remoteLog.lastUpdated > localLog.lastUpdated) {
                    repository.insertOrUpdateFromSync(
                        localLog.copy(
                            eggCount = remoteLog.eggCount,
                            eggWeight = remoteLog.eggWeight,
                            feedAmount = remoteLog.feedAmount,
                            chickenDead = remoteLog.chickenDead,
                            notes = remoteLog.notes,
                            isSynced = true,
                            lastUpdated = remoteLog.lastUpdated,
                            syncId = remoteLog.id
                        )
                    )
                }
            }
        }

        _isSyncing.value = false
        if (unresolvedConflicts.isNotEmpty()) {
            _conflicts.value = unresolvedConflicts
            _syncStatusMessage.value = "Menunggu penyelesaian ${unresolvedConflicts.size} konflik manual!"
        } else {
            _syncStatusMessage.value = "Sinkronisasi Sukses! Semua data ter-update."
        }
    }

    suspend fun resolveConflict(conflict: SyncConflict, keepLocal: Boolean) {
        val currentCloud = _cloudDatabase.value.toMutableList()
        val localLog = conflict.localLog
        val remoteLog = conflict.remoteLog

        if (keepLocal) {
            // Keep Local version: Update the Cloud database and mark local as synced
            val remoteMatch = currentCloud.find { it.id == remoteLog.id }
            if (remoteMatch != null) {
                val index = currentCloud.indexOf(remoteMatch)
                currentCloud[index] = remoteMatch.copyFromLocal(localLog)
            } else {
                currentCloud.add(
                    RemoteLog(
                        id = remoteLog.id,
                        kandangName = localLog.kandangName,
                        date = localLog.date,
                        eggCount = localLog.eggCount,
                        eggWeight = localLog.eggWeight,
                        feedAmount = localLog.feedAmount,
                        chickenDead = localLog.chickenDead,
                        notes = localLog.notes,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            repository.markAsSynced(localLog.id, remoteLog.id)
            _syncStatusMessage.value = "Konflik selesai: Mempertahankan Data Lokal."
        } else {
            // Keep Cloud version: Update Local database from Cloud and mark as synced
            repository.insertOrUpdateFromSync(
                localLog.copy(
                    eggCount = remoteLog.eggCount,
                    eggWeight = remoteLog.eggWeight,
                    feedAmount = remoteLog.feedAmount,
                    chickenDead = remoteLog.chickenDead,
                    notes = remoteLog.notes,
                    isSynced = true,
                    lastUpdated = remoteLog.lastUpdated,
                    syncId = remoteLog.id
                )
            )
            _syncStatusMessage.value = "Konflik selesai: Mempertahankan Data Cloud."
        }

        // Update list of conflicts
        _conflicts.value = _conflicts.value.filterNot { it.localLog.id == localLog.id }
        _cloudDatabase.value = currentCloud

        if (_conflicts.value.isEmpty()) {
            _syncStatusMessage.value = "Semua konflik manual berhasil diselesaikan!"
        }
    }

    private fun RemoteLog.copyFromLocal(local: LayerFarmLog): RemoteLog {
        return this.copy(
            eggCount = local.eggCount,
            eggWeight = local.eggWeight,
            feedAmount = local.feedAmount,
            chickenDead = local.chickenDead,
            notes = local.notes,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun LayerFarmLog.copyFromRemote(remote: RemoteLog, isSynced: Boolean): LayerFarmLog {
        return this.copy(
            eggCount = remote.eggCount,
            eggWeight = remote.eggWeight,
            feedAmount = remote.feedAmount,
            chickenDead = remote.chickenDead,
            notes = remote.notes,
            isSynced = isSynced,
            lastUpdated = remote.lastUpdated,
            syncId = remote.id
        )
    }

    private fun LayerFarmLog.isIdenticalTo(remote: RemoteLog): Boolean {
        return this.eggCount == remote.eggCount &&
                Math.abs(this.eggWeight - remote.eggWeight) < 0.01 &&
                Math.abs(this.feedAmount - remote.feedAmount) < 0.01 &&
                this.chickenDead == remote.chickenDead &&
                this.notes == remote.notes
    }
}
