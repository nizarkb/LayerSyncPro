package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FarmDashboardScreen
import com.example.ui.FarmDataEntryScreen
import com.example.ui.FeedPlannerScreen
import com.example.ui.KandangScreen
import com.example.ui.HealthScreen
import com.example.ui.ReportsScreen
import com.example.ui.SyncScreen
import com.example.ui.FarmViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FarmViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainLayout(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: FarmViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSyncScreen by remember { mutableStateOf(false) }

    // Observe flows from ViewModel
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val currentStrategy by viewModel.currentStrategy.collectAsStateWithLifecycle()
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()
    val cloudDatabase by viewModel.cloudDatabase.collectAsStateWithLifecycle()

    val useRealServer by viewModel.useRealServer.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val authCookie by viewModel.authCookie.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    // Form inputs state
    val selectedKandang by viewModel.selectedKandang.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val eggCount by viewModel.eggCount.collectAsStateWithLifecycle()
    val eggWeight by viewModel.eggWeight.collectAsStateWithLifecycle()
    val feedAmount by viewModel.feedAmount.collectAsStateWithLifecycle()
    val chickenDead by viewModel.chickenDead.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isEditingExisting by viewModel.isEditingExisting.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val eggPrice by viewModel.eggPrice.collectAsStateWithLifecycle()
    val feedPrice by viewModel.feedPrice.collectAsStateWithLifecycle()
    val kandangPopulations by viewModel.kandangPopulations.collectAsStateWithLifecycle()
    val vaccinations by viewModel.allVaccinations.collectAsStateWithLifecycle()
    val biosecurityChecks by viewModel.allBiosecurityChecks.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = "LayerSync",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Pro",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        )
                    }
                },
                navigationIcon = {
                    if (showSyncScreen) {
                        IconButton(onClick = { showSyncScreen = false }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali ke Operasional"
                            )
                        }
                    }
                },
                actions = {
                    // Option Bar Item: Sinkronisasi
                    FilterChip(
                        selected = showSyncScreen,
                        onClick = { showSyncScreen = !showSyncScreen },
                        label = { Text("Sinkronisasi") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = "Sinkronisasi",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Tukar Tema" else "Tukar Tema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (!showSyncScreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.AddCard, contentDescription = "Catat Data") },
                        label = { Text("Catat Data") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Kandang") },
                        label = { Text("Kandang") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = "Simulasi") },
                        label = { Text("Simulasi") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Laporan") },
                        label = { Text("Laporan") },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSyncScreen) {
                SyncScreen(
                    isOnline = isOnline,
                    isSyncing = isSyncing,
                    syncStatusMessage = syncStatusMessage,
                    currentStrategy = currentStrategy,
                    conflicts = conflicts,
                    cloudDatabase = cloudDatabase,
                    onToggleNetwork = { viewModel.toggleNetwork() },
                    onChangeStrategy = { viewModel.changeSyncStrategy(it) },
                    onTriggerSync = { viewModel.triggerSync() },
                    onResolveConflict = { conflict, keepLocal -> viewModel.resolveConflict(conflict, keepLocal) },
                    onInjectConflict = { viewModel.injectSimulatedCloudConflict() },
                    useRealServer = useRealServer,
                    serverUrl = serverUrl,
                    authCookie = authCookie,
                    usernameStr = username,
                    passwordStr = password,
                    isLoggedIn = isLoggedIn,
                    onToggleUseRealServer = { viewModel.setUseRealServer(it) },
                    onConnectAndLogin = { url, cookie, user, pass ->
                        viewModel.connectAndLogin(url, cookie, user, pass)
                    },
                    onLogout = { viewModel.logout() }
                )
            } else {
                when (selectedTab) {
                    0 -> FarmDashboardScreen(
                        logs = logs,
                        isOnline = isOnline,
                        eggPrice = eggPrice,
                        feedPrice = feedPrice,
                        kandangPopulations = kandangPopulations,
                        onNavigateToEntry = { selectedTab = 1 },
                        onSeedData = { viewModel.seedSampleData() },
                        onDeleteLog = { viewModel.deleteLog(it) }
                    )
                    1 -> FarmDataEntryScreen(
                        selectedKandang = selectedKandang,
                        selectedDate = selectedDate,
                        eggCount = eggCount,
                        eggWeight = eggWeight,
                        feedAmount = feedAmount,
                        chickenDead = chickenDead,
                        notes = notes,
                        isEditingExisting = isEditingExisting,
                        kandangPopulations = kandangPopulations,
                        onKandangSelected = { viewModel.selectKandang(it) },
                        onDateSelected = { viewModel.selectDate(it) },
                        onAdjustEggCount = { viewModel.adjustEggCount(it) },
                        onAdjustEggWeight = { viewModel.adjustEggWeight(it) },
                        onAdjustFeedAmount = { viewModel.adjustFeedAmount(it) },
                        onAdjustChickenDead = { viewModel.adjustChickenDead(it) },
                        onSetEggCount = { viewModel.setEggCount(it) },
                        onSetEggWeight = { viewModel.setEggWeight(it) },
                        onSetFeedAmount = { viewModel.setFeedAmount(it) },
                        onSetChickenDead = { viewModel.setChickenDead(it) },
                        onNotesChanged = { viewModel.updateNotes(it) },
                        onSaveLog = {
                            viewModel.saveCurrentLog()
                            // Automatically switch back to Dashboard after saving so they see their log immediately!
                            selectedTab = 0
                        }
                    )
                    2 -> KandangScreen(
                        kandangPopulations = kandangPopulations,
                        onUpdatePopulation = { kandang, pop -> viewModel.updatePopulation(kandang, pop) },
                        onAddKandang = { name, pop -> viewModel.addKandang(name, pop) },
                        onDeleteKandang = { name -> viewModel.deleteKandang(name) },
                        onRenameKandang = { old, new -> viewModel.renameKandang(old, new) },
                        logs = logs,
                        vaccinations = vaccinations,
                        biosecurityChecks = biosecurityChecks,
                        onAddVaccination = { kandang, name, date, method, notes ->
                            viewModel.addVaccination(kandang, name, date, method, notes)
                        },
                        onUpdateVaccinationStatus = { id, completed, actualDate ->
                            viewModel.updateVaccinationStatus(id, completed, actualDate)
                        },
                        onDeleteVaccination = { id -> viewModel.deleteVaccination(id) },
                        onSaveBiosecurityCheck = { check -> viewModel.saveBiosecurityCheck(check) },
                        onDeleteBiosecurityCheck = { id -> viewModel.deleteBiosecurityCheck(id) },
                        onSeedDefaultProgram = { viewModel.seedDefaultVaccinationProgram() }
                    )
                    3 -> FeedPlannerScreen(
                        eggPrice = eggPrice,
                        feedPrice = feedPrice,
                        onUpdateEggPrice = { viewModel.updateEggPrice(it) },
                        onUpdateFeedPrice = { viewModel.updateFeedPrice(it) }
                    )
                    4 -> ReportsScreen(
                        logs = logs,
                        eggPrice = eggPrice,
                        feedPrice = feedPrice,
                        kandangPopulations = kandangPopulations,
                        onUpdateEggPrice = { viewModel.updateEggPrice(it) },
                        onUpdateFeedPrice = { viewModel.updateFeedPrice(it) }
                    )
                }
            }
        }
    }
}
