package com.example.data

import android.content.Context
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

class SyncManager(
    private val repository: FarmRepository,
    private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    // Sync Mode configuration
    private val _useRealServer = MutableStateFlow(sharedPrefs.getBoolean("use_real_server", false))
    val useRealServer: StateFlow<Boolean> = _useRealServer.asStateFlow()

    private val _serverUrl = MutableStateFlow(
        sharedPrefs.getString("server_url", "https://ais-dev-pndu4mvv2bfmazfyes5ovk-1023004766485.asia-southeast1.run.app/") ?: ""
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _authCookie = MutableStateFlow(
        sharedPrefs.getString("auth_cookie", "") ?: ""
    )
    val authCookie: StateFlow<String> = _authCookie.asStateFlow()

    private val _username = MutableStateFlow(
        sharedPrefs.getString("username", "admin") ?: ""
    )
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow(
        sharedPrefs.getString("password", "adminpeternakan") ?: ""
    )
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

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

    init {
        // Initialize Retrofit configuration with stored values
        RetrofitClient.updateConfig(_serverUrl.value, _authCookie.value)
    }

    fun setUseRealServer(value: Boolean) {
        _useRealServer.value = value
        sharedPrefs.edit().putBoolean("use_real_server", value).apply()
        _syncStatusMessage.value = if (value) "Mode Sinkronisasi Server AKTIF" else "Mode Simulasi AKTIF"
        RetrofitClient.updateConfig(_serverUrl.value, _authCookie.value)
    }

    fun setServerUrl(value: String) {
        _serverUrl.value = value
        sharedPrefs.edit().putString("server_url", value).apply()
        RetrofitClient.updateConfig(value, _authCookie.value)
    }

    fun setAuthCookie(value: String) {
        _authCookie.value = value
        sharedPrefs.edit().putString("auth_cookie", value).apply()
        RetrofitClient.updateConfig(_serverUrl.value, value)
    }

    fun setCredentials(user: String, pass: String) {
        _username.value = user
        _password.value = pass
        sharedPrefs.edit().putString("username", user).putString("password", pass).apply()
    }

    fun logout() {
        RetrofitClient.setToken(null)
        _isLoggedIn.value = false
        _syncStatusMessage.value = "Logged out from server."
    }

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

    private fun getLastSyncTimestamp(): Long {
        return sharedPrefs.getLong("last_sync_timestamp", 0L)
    }

    private fun setLastSyncTimestamp(timestamp: Long) {
        sharedPrefs.edit().putLong("last_sync_timestamp", timestamp).apply()
    }

    // Attempt to authenticate with backend server
    suspend fun attemptLogin(): Boolean {
        if (RetrofitClient.getToken() != null && _isLoggedIn.value) {
            return true
        }

        val request = LoginRequest(_username.value, _password.value)
        val response = RetrofitClient.service.login(request)
        return if (response.isSuccessful && response.body() != null) {
            val token = response.body()!!.token
            RetrofitClient.setToken(token)
            _isLoggedIn.value = true
            true
        } else {
            _isLoggedIn.value = false
            false
        }
    }

    suspend fun testConnectionAndLogin(): Boolean {
        _isSyncing.value = true
        _syncStatusMessage.value = "Menghubungkan & memverifikasi kredensial..."
        try {
            // Reset existing token first to test fresh
            RetrofitClient.setToken(null)
            _isLoggedIn.value = false

            val success = attemptLogin()
            if (success) {
                _syncStatusMessage.value = "Login Sukses! Sesi terverifikasi."
            } else {
                _syncStatusMessage.value = "Login Gagal: Username atau Password salah!"
            }
            return success
        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatusMessage.value = "Gagal Terhubung ke Server: ${e.localizedMessage ?: "Timeout/Koneksi Bermasalah"}"
            _isLoggedIn.value = false
            return false
        } finally {
            _isSyncing.value = false
        }
    }

    // Add mock item to cloud to trigger a conflict or new download (Simulation only)
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
                        lastUpdated = System.currentTimeMillis()
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
     * We support both the Real Backend API sync and Simulated Local Sync.
     */
    suspend fun performSync() {
        if (!_isOnline.value) {
            _syncStatusMessage.value = "Gagal: Tidak ada koneksi internet!"
            return
        }

        _isSyncing.value = true
        _syncStatusMessage.value = "Sinkronisasi sedang berlangsung..."
        _conflicts.value = emptyList()

        if (_useRealServer.value) {
            try {
                // 1. Authenticate
                _syncStatusMessage.value = "Melakukan login ke backend server..."
                val loginSuccess = attemptLogin()
                if (!loginSuccess) {
                    _syncStatusMessage.value = "Error: Autentikasi server gagal! Periksa username & password."
                    _isSyncing.value = false
                    return
                }

                _syncStatusMessage.value = "Autentikasi berhasil. Memulai sinkronisasi..."
                val lastSync = getLastSyncTimestamp()

                // --- LANGKAH A: PUSH (Upload Local Changes) ---
                _syncStatusMessage.value = "Mengunggah data lokal baru/diubah..."
                val unsyncedLogs = repository.getLogsUpdatedAfter(lastSync)
                val unsyncedVac = repository.getVaccinationsUpdatedAfter(lastSync)
                val unsyncedBio = repository.getBiosecurityChecksUpdatedAfter(lastSync)

                var pushMessage = "Tidak ada data lokal baru untuk diunggah."
                if (unsyncedLogs.isNotEmpty() || unsyncedVac.isNotEmpty() || unsyncedBio.isNotEmpty()) {
                    val payload = SyncPushPayload(unsyncedLogs, unsyncedVac, unsyncedBio)
                    val pushResponse = RetrofitClient.service.pushData(payload)
                    
                    if (pushResponse.isSuccessful && pushResponse.body()?.success == true) {
                        val body = pushResponse.body()!!
                        // Update local logs to isSynced = true
                        val syncedIds = body.syncedIds
                        if (!syncedIds.isNullOrEmpty()) {
                            for (id in syncedIds) {
                                repository.markAsSynced(id, id)
                            }
                        } else {
                            // Fallback: mark all pushed logs as synced
                            for (log in unsyncedLogs) {
                                repository.markAsSynced(log.id, log.id)
                            }
                        }
                        pushMessage = "Unggah sukses: ${body.message ?: "Selesai"} (${unsyncedLogs.size} log, ${unsyncedVac.size} vak, ${unsyncedBio.size} bio)"
                    } else {
                        val errBody = pushResponse.errorBody()?.string()
                        val code = pushResponse.code()
                        _syncStatusMessage.value = "Gagal push ke server (HTTP $code): ${errBody ?: pushResponse.message()}"
                        _isSyncing.value = false
                        return
                    }
                }

                // --- LANGKAH B: PULL (Download Server Changes) ---
                _syncStatusMessage.value = "Mengunduh data baru dari server..."
                val pullResponse = RetrofitClient.service.pullData(lastSync)
                if (pullResponse.isSuccessful && pullResponse.body() != null) {
                    val serverData = pullResponse.body()!!

                    // Update local Room database
                    if (serverData.logs.isNotEmpty()) {
                        // Mark downloaded logs as synced
                        val syncedServerLogs = serverData.logs.map { it.copy(isSynced = true) }
                        repository.insertLogs(syncedServerLogs)
                    }
                    if (serverData.vaccinations.isNotEmpty()) {
                        repository.insertVaccinations(serverData.vaccinations)
                    }
                    if (serverData.biosecurity.isNotEmpty()) {
                        repository.insertBiosecurityChecks(serverData.biosecurity)
                    }

                    // Save server timestamp as the last synced benchmark
                    setLastSyncTimestamp(serverData.serverTimestamp)
                    _syncStatusMessage.value = "Sinkronisasi Sukses! $pushMessage | Diunduh: ${serverData.logs.size} log, ${serverData.vaccinations.size} vak, ${serverData.biosecurity.size} bio."
                } else {
                    val errBody = pullResponse.errorBody()?.string()
                    val code = pullResponse.code()
                    _syncStatusMessage.value = "Gagal pull dari server (HTTP $code): ${errBody ?: pullResponse.message()}"
                }

            } catch (e: com.squareup.moshi.JsonDataException) {
                e.printStackTrace()
                _syncStatusMessage.value = "Gagal Sinkronisasi: Format JSON tidak sesuai (JsonDataException: ${e.localizedMessage})"
            } catch (e: java.io.IOException) {
                e.printStackTrace()
                _syncStatusMessage.value = "Gagal Sinkronisasi: Masalah Koneksi/Timeout (${e.javaClass.simpleName}: ${e.localizedMessage})"
            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatusMessage.value = "Gagal Sinkronisasi: ${e.javaClass.simpleName} - ${e.localizedMessage ?: "Unknown Error"}"
            } finally {
                _isSyncing.value = false
            }
        } else {
            // --- SIMULATED SYNC ---
            kotlinx.coroutines.delay(1200)

            val localUnsynced = repository.getUnsyncedLogs()
            val currentCloud = _cloudDatabase.value.toMutableList()
            val unresolvedConflicts = mutableListOf<SyncConflict>()

            for (localLog in localUnsynced) {
                val remoteMatch = currentCloud.find { it.id == localLog.id || (it.kandangName == localLog.kandangName && it.date == localLog.date) }

                if (remoteMatch != null) {
                    if (localLog.isIdenticalTo(remoteMatch)) {
                        repository.markAsSynced(localLog.id, remoteMatch.id)
                    } else {
                        when (_strategy.value) {
                            ResolutionStrategy.LAST_WRITE_WINS -> {
                                if (localLog.lastUpdated > remoteMatch.lastUpdated) {
                                    val index = currentCloud.indexOf(remoteMatch)
                                    currentCloud[index] = remoteMatch.copyFromLocal(localLog)
                                    repository.markAsSynced(localLog.id, remoteMatch.id)
                                } else {
                                    repository.insertOrUpdateFromSync(localLog.copyFromRemote(remoteMatch, isSynced = true))
                                }
                            }
                            ResolutionStrategy.LOCAL_WINS -> {
                                val index = currentCloud.indexOf(remoteMatch)
                                currentCloud[index] = remoteMatch.copyFromLocal(localLog)
                                repository.markAsSynced(localLog.id, remoteMatch.id)
                            }
                            ResolutionStrategy.REMOTE_WINS -> {
                                repository.insertOrUpdateFromSync(localLog.copyFromRemote(remoteMatch, isSynced = true))
                            }
                            ResolutionStrategy.MANUAL -> {
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

            _cloudDatabase.value = currentCloud

            val updatedCloud = _cloudDatabase.value
            for (remoteLog in updatedCloud) {
                val localLog = repository.getLogByKandangAndDate(remoteLog.kandangName, remoteLog.date)
                if (localLog == null) {
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
    }

    suspend fun resolveConflict(conflict: SyncConflict, keepLocal: Boolean) {
        val currentCloud = _cloudDatabase.value.toMutableList()
        val localLog = conflict.localLog
        val remoteLog = conflict.remoteLog

        if (keepLocal) {
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
