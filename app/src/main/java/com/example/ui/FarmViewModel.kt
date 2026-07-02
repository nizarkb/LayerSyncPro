package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FarmDatabase
import com.example.data.FarmRepository
import com.example.data.LayerFarmLog
import com.example.data.VaccinationSchedule
import com.example.data.BiosecurityCheck
import com.example.data.ResolutionStrategy
import com.example.data.SyncConflict
import com.example.data.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FarmDatabase.getDatabase(application)
    private val repository = FarmRepository(database.farmDao())
    val syncManager = SyncManager(repository)

    // Persistent Theme selection: true = dark theme, false = light theme. Default is false (Light mode).
    private val sharedPrefs = application.getSharedPreferences("farm_prefs", Context.MODE_PRIVATE)
    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme", false))
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newVal = !_isDarkTheme.value
        _isDarkTheme.value = newVal
        sharedPrefs.edit().putBoolean("is_dark_theme", newVal).apply()
    }

    // Dynamic Financial Estimations Settings (Persistent in SharedPreferences)
    private val _eggPrice = MutableStateFlow(sharedPrefs.getFloat("egg_price", 26000f))
    val eggPrice = _eggPrice.asStateFlow()

    private val _feedPrice = MutableStateFlow(sharedPrefs.getFloat("feed_price", 8500f))
    val feedPrice = _feedPrice.asStateFlow()

    fun updateEggPrice(price: Float) {
        _eggPrice.value = price
        sharedPrefs.edit().putFloat("egg_price", price).apply()
    }

    fun updateFeedPrice(price: Float) {
        _feedPrice.value = price
        sharedPrefs.edit().putFloat("feed_price", price).apply()
    }

    // Dynamic Population Configuration (Persistent in SharedPreferences)
    private val _kandangPopulations = MutableStateFlow<Map<String, Int>>(emptyMap())
    val kandangPopulations = _kandangPopulations.asStateFlow()

    fun updatePopulation(kandang: String, population: Int) {
        val cleanPop = population.coerceAtLeast(0)
        sharedPrefs.edit().putInt("population_$kandang", cleanPop).apply()
        _kandangPopulations.value = _kandangPopulations.value.toMutableMap().apply {
            put(kandang, cleanPop)
        }
    }

    fun addKandang(name: String, population: Int) {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty() && !_kandangPopulations.value.containsKey(cleanName)) {
            updatePopulation(cleanName, population)
        }
    }

    fun deleteKandang(name: String) {
        sharedPrefs.edit().remove("population_$name").apply()
        _kandangPopulations.value = _kandangPopulations.value.toMutableMap().apply {
            remove(name)
        }
        // If the selected coop was the deleted one, switch to another available one if exists
        if (_selectedKandang.value == name) {
            val remaining = _kandangPopulations.value.keys.firstOrNull() ?: ""
            _selectedKandang.value = remaining
        }
    }

    fun renameKandang(oldName: String, newName: String) {
        val cleanNewName = newName.trim()
        if (cleanNewName.isNotEmpty() && oldName != cleanNewName && !_kandangPopulations.value.containsKey(cleanNewName)) {
            val population = _kandangPopulations.value[oldName] ?: 500
            sharedPrefs.edit().remove("population_$oldName").apply()
            sharedPrefs.edit().putInt("population_$cleanNewName", population).apply()
            _kandangPopulations.value = _kandangPopulations.value.toMutableMap().apply {
                remove(oldName)
                put(cleanNewName, population)
            }
            if (_selectedKandang.value == oldName) {
                _selectedKandang.value = cleanNewName
            }
        }
    }

    // UI state of all logs from local database
    val allLogs: StateFlow<List<LayerFarmLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Vaccination and Biosecurity StateFlows
    val allVaccinations: StateFlow<List<VaccinationSchedule>> = repository.allVaccinations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allBiosecurityChecks: StateFlow<List<BiosecurityCheck>> = repository.allBiosecurityChecks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addVaccination(kandangName: String, vaccineName: String, plannedDate: String, method: String, notes: String) {
        viewModelScope.launch {
            val v = VaccinationSchedule(
                kandangName = kandangName,
                vaccineName = vaccineName,
                plannedDate = plannedDate,
                method = method,
                notes = notes,
                status = "Pending",
                actualDate = null,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveVaccination(v)
        }
    }

    fun updateVaccinationStatus(id: String, completed: Boolean, actualDate: String? = null) {
        viewModelScope.launch {
            val list = allVaccinations.value
            val match = list.find { it.id == id }
            if (match != null) {
                val updated = match.copy(
                    status = if (completed) "Completed" else "Pending",
                    actualDate = if (completed) (actualDate ?: getTodayDateString()) else null,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.updateVaccination(updated)
            }
        }
    }

    fun deleteVaccination(id: String) {
        viewModelScope.launch {
            repository.deleteVaccination(id)
        }
    }

    fun saveBiosecurityCheck(check: BiosecurityCheck) {
        viewModelScope.launch {
            repository.saveBiosecurityCheck(check)
        }
    }

    fun deleteBiosecurityCheck(id: String) {
        viewModelScope.launch {
            repository.deleteBiosecurityCheck(id)
        }
    }

    fun seedDefaultVaccinationProgram() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val baseTime = System.currentTimeMillis()
            val c1 = baseTime - 4 * 86400000L // 4 days ago
            val c2 = baseTime + 10 * 86400000L // 10 days in future
            val c3 = baseTime + 17 * 86400000L // 17 days in future
            val c4 = baseTime + 30 * 86400000L
            val c5 = baseTime + 45 * 86400000L
            val c6 = baseTime + 60 * 86400000L

            val presets = listOf(
                VaccinationSchedule(
                    kandangName = "Semua Kandang",
                    vaccineName = "ND-IB Live (Newcastle Disease + Inf. Bronchitis)",
                    plannedDate = sdf.format(Date(c1)),
                    actualDate = sdf.format(Date(c1)),
                    method = "Tetes Mata",
                    status = "Completed",
                    ageWeeks = 1,
                    notes = "Vaksinasi awal penting untuk kekebalan pernapasan ayam."
                ),
                VaccinationSchedule(
                    kandangName = "Semua Kandang",
                    vaccineName = "Gumboro (Infectious Bursal Disease - IBD)",
                    plannedDate = sdf.format(Date(c2)),
                    method = "Air Minum",
                    status = "Pending",
                    ageWeeks = 2,
                    notes = "Mencegah penyakit Gumboro yang menyerang kekebalan tubuh ayam."
                ),
                VaccinationSchedule(
                    kandangName = "Semua Kandang",
                    vaccineName = "ND-IB Live Clone",
                    plannedDate = sdf.format(Date(c3)),
                    method = "Air Minum",
                    status = "Pending",
                    ageWeeks = 3,
                    notes = "Vaksinasi booster ND-IB via air minum."
                ),
                VaccinationSchedule(
                    kandangName = "Semua Kandang",
                    vaccineName = "Coryza I (Snot - Haemophilus)",
                    plannedDate = sdf.format(Date(c4)),
                    method = "Suntik (Intramuscular)",
                    status = "Pending",
                    ageWeeks = 6,
                    notes = "Mencegah pilek ayam (snot) yang menurunkan produksi telur."
                ),
                VaccinationSchedule(
                    kandangName = "Semua Kandang",
                    vaccineName = "Fowl Pox (Cacar Ayam)",
                    plannedDate = sdf.format(Date(c5)),
                    method = "Tusuk Sayap (Wing Web)",
                    status = "Pending",
                    ageWeeks = 10,
                    notes = "Melindungi ayam dari cacar melalui tusukan di selaput sayap."
                ),
                VaccinationSchedule(
                    kandangName = "Semua Kandang",
                    vaccineName = "ND+IB+EDS Kills (Kekebalan Produksi)",
                    plannedDate = sdf.format(Date(c6)),
                    method = "Suntik (Intramuscular)",
                    status = "Pending",
                    ageWeeks = 16,
                    notes = "Vaksin penting sebelum puncak masa bertelur ayam (Egg Drop Syndrome)."
                )
            )

            for (p in presets) {
                repository.saveVaccination(p)
            }
        }
    }

    // Current online status
    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    
    // Active conflicts
    val conflicts: StateFlow<List<SyncConflict>> = syncManager.conflicts

    // Sync progress
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val syncStatusMessage: StateFlow<String> = syncManager.syncStatusMessage
    val currentStrategy: StateFlow<ResolutionStrategy> = syncManager.strategy
    val cloudDatabase = syncManager.cloudDatabase

    // State for Data Entry Form
    private val _selectedKandang = MutableStateFlow("Kandang A")
    val selectedKandang = _selectedKandang.asStateFlow()

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate = _selectedDate.asStateFlow()

    private val _eggCount = MutableStateFlow(400) // Default starting point for easy editing
    val eggCount = _eggCount.asStateFlow()

    private val _eggWeight = MutableStateFlow(25.0f) // Default starting in kg
    val eggWeight = _eggWeight.asStateFlow()

    private val _feedAmount = MutableStateFlow(45.0f) // Default in kg
    val feedAmount = _feedAmount.asStateFlow()

    private val _chickenDead = MutableStateFlow(0)
    val chickenDead = _chickenDead.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _isEditingExisting = MutableStateFlow(false)
    val isEditingExisting = _isEditingExisting.asStateFlow()

    // Active log ID being edited (if any)
    private var editingLogId: String? = null

    init {
        // Load existing populations
        val initialPopulations = mutableMapOf<String, Int>()
        val keys = sharedPrefs.all.keys.filter { it.startsWith("population_") }
        if (keys.isEmpty()) {
            // First run, populate defaults
            sharedPrefs.edit()
                .putInt("population_Kandang A", 500)
                .putInt("population_Kandang B", 500)
                .putInt("population_Kandang C", 500)
                .apply()
            initialPopulations["Kandang A"] = 500
            initialPopulations["Kandang B"] = 500
            initialPopulations["Kandang C"] = 500
        } else {
            sharedPrefs.all.forEach { (key, value) ->
                if (key.startsWith("population_") && value is Int) {
                    val kandangName = key.removePrefix("population_")
                    initialPopulations[kandangName] = value
                }
            }
        }
        _kandangPopulations.value = initialPopulations

        // Automatically fetch and load if today's record already exists
        viewModelScope.launch {
            loadExistingRecordForSelected()
        }
    }

    // Load existing local record if exists for selected kandang & date
    suspend fun loadExistingRecordForSelected() {
        val existing = repository.getLogByKandangAndDate(_selectedKandang.value, _selectedDate.value)
        if (existing != null) {
            editingLogId = existing.id
            _eggCount.value = existing.eggCount
            _eggWeight.value = existing.eggWeight
            _feedAmount.value = existing.feedAmount
            _chickenDead.value = existing.chickenDead
            _notes.value = existing.notes
            _isEditingExisting.value = true
        } else {
            editingLogId = null
            // Reset to realistic standard defaults
            _eggCount.value = 400
            _eggWeight.value = 25.0f
            _feedAmount.value = 45.0f
            _chickenDead.value = 0
            _notes.value = ""
            _isEditingExisting.value = false
        }
    }

    fun selectKandang(kandang: String) {
        _selectedKandang.value = kandang
        viewModelScope.launch { loadExistingRecordForSelected() }
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
        viewModelScope.launch { loadExistingRecordForSelected() }
    }

    // High speed data adjustments
    fun adjustEggCount(amount: Int) {
        _eggCount.value = (_eggCount.value + amount).coerceAtLeast(0)
    }

    fun setEggCount(value: Int) {
        _eggCount.value = value.coerceAtLeast(0)
    }

    fun adjustEggWeight(amount: Float) {
        _eggWeight.value = String.format(Locale.US, "%.1f", (_eggWeight.value + amount).coerceAtLeast(0f)).toFloat()
    }

    fun setEggWeight(value: Float) {
        _eggWeight.value = String.format(Locale.US, "%.1f", value.coerceAtLeast(0f)).toFloat()
    }

    fun adjustFeedAmount(amount: Float) {
        _feedAmount.value = String.format(Locale.US, "%.1f", (_feedAmount.value + amount).coerceAtLeast(0f)).toFloat()
    }

    fun setFeedAmount(value: Float) {
        _feedAmount.value = String.format(Locale.US, "%.1f", value.coerceAtLeast(0f)).toFloat()
    }

    fun adjustChickenDead(amount: Int) {
        _chickenDead.value = (_chickenDead.value + amount).coerceAtLeast(0)
    }

    fun setChickenDead(value: Int) {
        _chickenDead.value = value.coerceAtLeast(0)
    }

    fun updateNotes(newNotes: String) {
        _notes.value = newNotes
    }

    // Save Data - Local Storage First!
    fun saveCurrentLog() {
        viewModelScope.launch {
            val log = LayerFarmLog(
                id = editingLogId ?: java.util.UUID.randomUUID().toString(),
                kandangName = _selectedKandang.value,
                date = _selectedDate.value,
                eggCount = _eggCount.value,
                eggWeight = _eggWeight.value,
                feedAmount = _feedAmount.value,
                chickenDead = _chickenDead.value,
                notes = _notes.value,
                isSynced = false, // Always false on save/update locally
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveLog(log)
            loadExistingRecordForSelected()
        }
    }

    fun deleteLog(id: String) {
        viewModelScope.launch {
            repository.deleteLog(id)
            loadExistingRecordForSelected()
        }
    }

    // Sync operations
    fun triggerSync() {
        viewModelScope.launch {
            syncManager.performSync()
        }
    }

    fun resolveConflict(conflict: SyncConflict, keepLocal: Boolean) {
        viewModelScope.launch {
            syncManager.resolveConflict(conflict, keepLocal)
        }
    }

    fun toggleNetwork() {
        syncManager.setOnline(!isOnline.value)
    }

    fun changeSyncStrategy(strategy: ResolutionStrategy) {
        syncManager.setStrategy(strategy)
    }

    // Help users easily test conflict resolutions by injecting cloud records
    fun injectSimulatedCloudConflict() {
        // We inject a record for Kandang A / Today with different values and a newer timestamp
        val today = getTodayDateString()
        syncManager.injectCloudUpdate(
            id = editingLogId ?: "simulated-conflict-id-123",
            kandangName = _selectedKandang.value,
            date = today,
            eggCount = 475, // Different egg count
            eggWeight = 29.3f,
            feedAmount = 49.0f,
            chickenDead = 2, // Different mortality
            notes = "Died from heat (Modifikasi Cloud)"
        )
    }

    // Seed dummy data for first-launch experience
    fun seedSampleData() {
        viewModelScope.launch {
            // Seed a few days ago
            val days = listOf(1, 2, 3)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val kandangs = _kandangPopulations.value.keys.toList().ifEmpty { listOf("Kandang A", "Kandang B", "Kandang C") }
            
            for (day in days) {
                val dateStr = sdf.format(Date(System.currentTimeMillis() - day * 86400000))
                for (kandang in kandangs) {
                    val log = LayerFarmLog(
                        kandangName = kandang,
                        date = dateStr,
                        eggCount = (380..440).random(),
                        eggWeight = (23..27).random().toFloat() + (0..9).random() / 10f,
                        feedAmount = (42..47).random().toFloat(),
                        chickenDead = (0..2).random(),
                        notes = "Kondisi pakan lancar.",
                        isSynced = true, // mark synced so we start clean
                        createdAt = System.currentTimeMillis() - day * 86400000,
                        lastUpdated = System.currentTimeMillis() - day * 86400000
                    )
                    repository.saveLog(log)
                }
            }
            loadExistingRecordForSelected()
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
