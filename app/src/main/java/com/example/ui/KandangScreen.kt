package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LayerFarmLog
import com.example.data.BiosecurityCheck
import com.example.data.VaccinationSchedule
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KandangScreen(
    kandangPopulations: Map<String, Int>,
    onUpdatePopulation: (String, Int) -> Unit,
    onAddKandang: (String, Int) -> Unit,
    onDeleteKandang: (String) -> Unit,
    onRenameKandang: (String, String) -> Unit,
    logs: List<LayerFarmLog>,
    vaccinations: List<VaccinationSchedule>,
    biosecurityChecks: List<BiosecurityCheck>,
    onAddVaccination: (kandang: String, name: String, date: String, method: String, notes: String) -> Unit,
    onUpdateVaccinationStatus: (id: String, completed: Boolean, actualDate: String?) -> Unit,
    onDeleteVaccination: (id: String) -> Unit,
    onSaveBiosecurityCheck: (BiosecurityCheck) -> Unit,
    onDeleteBiosecurityCheck: (id: String) -> Unit,
    onSeedDefaultProgram: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabState by remember { mutableStateOf(0) } // 0 = Kandang, 1 = Vaksinasi, 2 = Biosekuriti

    var showAddDialog by remember { mutableStateOf(false) }
    var addNameInputText by remember { mutableStateOf("") }
    var addPopInputText by remember { mutableStateOf("500") }

    var showPopDialogForKandang by remember { mutableStateOf<String?>(null) }
    var popDialogInputText by remember { mutableStateOf("") }

    var showRenameDialogForKandang by remember { mutableStateOf<String?>(null) }
    var renameDialogInputText by remember { mutableStateOf("") }

    var showDeleteConfirmForKandang by remember { mutableStateOf<String?>(null) }

    // Map to track which cage card is expanded
    var expandedCages by remember { mutableStateOf(setOf<String>()) }

    val activeCages = kandangPopulations.keys.toList().sorted()
    val totalFlock = activeCages.sumOf { kandangPopulations[it] ?: 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTabState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTabState == 0,
                onClick = { selectedTabState = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Kandang",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kandang", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("kandang_tab_cages")
            )
            Tab(
                selected = selectedTabState == 1,
                onClick = { selectedTabState = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vaccines,
                            contentDescription = "Vaksin",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vaksinasi", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("kandang_tab_vaccines")
            )
            Tab(
                selected = selectedTabState == 2,
                onClick = { selectedTabState = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Biosekuriti",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Biosekuriti", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("kandang_tab_biosecurity")
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabState) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Upper decorative hero header
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = "Layer Coop",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Populasi Layer",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "$totalFlock ekor",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Button(
                                        onClick = { showAddDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah Kandang", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kandang", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Kandang Aktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "${activeCages.size} Unit",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Total Produksi",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "${logs.sumOf { it.eggCount }} btr",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Empty state
                        if (activeCages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Empty",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Belum Ada Kandang",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Silakan klik tombol di kanan atas untuk membuat kandang baru pertama Anda.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                    )
                                    Button(onClick = { showAddDialog = true }) {
                                        Text("Tambah Kandang Baru")
                                    }
                                }
                            }
                        } else {
                            // List of Coops
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(activeCages, key = { it }) { cage ->
                                    val currentPop = kandangPopulations[cage] ?: 0
                                    val cagePercentage = if (totalFlock > 0) (currentPop.toFloat() / totalFlock) * 100 else 0f
                                    val isExpanded = expandedCages.contains(cage)

                                    CageItemCard(
                                        cage = cage,
                                        currentPop = currentPop,
                                        cagePercentage = cagePercentage,
                                        isExpanded = isExpanded,
                                        totalFlock = totalFlock,
                                        logs = logs,
                                        onToggleExpand = {
                                            expandedCages = if (isExpanded) {
                                                expandedCages - cage
                                            } else {
                                                expandedCages + cage
                                            }
                                        },
                                        onRenameClick = {
                                            renameDialogInputText = cage
                                            showRenameDialogForKandang = cage
                                        },
                                        onDeleteClick = {
                                            showDeleteConfirmForKandang = cage
                                        },
                                        onAdjustPopulationClick = {
                                            popDialogInputText = currentPop.toString()
                                            showPopDialogForKandang = cage
                                        },
                                        onUpdatePopulation = onUpdatePopulation
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    VaccinationTab(
                        vaccinations = vaccinations,
                        kandangPopulations = kandangPopulations,
                        onAddVaccination = onAddVaccination,
                        onUpdateVaccinationStatus = onUpdateVaccinationStatus,
                        onDeleteVaccination = onDeleteVaccination,
                        onSeedDefaultProgram = onSeedDefaultProgram
                    )
                }
                2 -> {
                    BiosecurityTab(
                        checks = biosecurityChecks,
                        onSaveCheck = onSaveBiosecurityCheck,
                        onDeleteCheck = onDeleteBiosecurityCheck
                    )
                }
            }
        }

        // Add Cage Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Text(
                        text = "Tambah Kandang Baru",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = addNameInputText,
                            onValueChange = { addNameInputText = it },
                            label = { Text("Nama Kandang") },
                            placeholder = { Text("Contoh: Kandang D") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = addPopInputText,
                            onValueChange = { addPopInputText = it.filter { char -> char.isDigit() } },
                            label = { Text("Populasi Awal (Ekor)") },
                            placeholder = { Text("Contoh: 500") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cleanName = addNameInputText.trim()
                            val parsedPop = addPopInputText.toIntOrNull() ?: 500
                            if (cleanName.isNotEmpty()) {
                                onAddKandang(cleanName, parsedPop)
                                addNameInputText = ""
                                addPopInputText = "500"
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Tambah", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Population Setup Dialog
        showPopDialogForKandang?.let { cage ->
            AlertDialog(
                onDismissRequest = { showPopDialogForKandang = null },
                title = {
                    Text(
                        text = "Atur Populasi $cage",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Masukkan jumlah populasi ayam riil saat ini untuk $cage:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = popDialogInputText,
                            onValueChange = { popDialogInputText = it },
                            placeholder = { Text("Contoh: 500") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("pop_dialog_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val inputVal = popDialogInputText.toIntOrNull() ?: 500
                            onUpdatePopulation(cage, inputVal)
                            showPopDialogForKandang = null
                        },
                        modifier = Modifier.testTag("pop_dialog_save")
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPopDialogForKandang = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Rename Cage Dialog
        showRenameDialogForKandang?.let { cage ->
            AlertDialog(
                onDismissRequest = { showRenameDialogForKandang = null },
                title = {
                    Text(
                        text = "Ubah Nama Kandang",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Ubah nama dari $cage menjadi:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = renameDialogInputText,
                            onValueChange = { renameDialogInputText = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cleanNewName = renameDialogInputText.trim()
                            if (cleanNewName.isNotEmpty() && cleanNewName != cage) {
                                onRenameKandang(cage, cleanNewName)
                            }
                            showRenameDialogForKandang = null
                        }
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialogForKandang = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Delete Cage Confirmation Dialog
        showDeleteConfirmForKandang?.let { cage ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmForKandang = null },
                title = {
                    Text(
                        text = "Hapus $cage?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus $cage? Data populasi aktif akan dihapus. Laporan harian historis yang sudah tersimpan di database tidak akan terpengaruh namun tidak akan terhitung di dashboard aktif kandang.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteKandang(cage)
                            showDeleteConfirmForKandang = null
                        }
                    ) {
                        Text("Hapus", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmForKandang = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CageItemCard(
    cage: String,
    currentPop: Int,
    cagePercentage: Float,
    isExpanded: Boolean,
    totalFlock: Int,
    logs: List<LayerFarmLog>,
    onToggleExpand: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAdjustPopulationClick: () -> Unit,
    onUpdatePopulation: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate cage specific statistics
    val cageLogs = remember(logs, cage) {
        logs.filter { it.kandangName == cage }.sortedByDescending { it.date }
    }
    val totalEggs = cageLogs.sumOf { it.eggCount }
    val totalEggWeight = cageLogs.sumOf { it.eggWeight.toDouble() }.toFloat()
    val totalFeed = cageLogs.sumOf { it.feedAmount.toDouble() }.toFloat()
    val totalDead = cageLogs.sumOf { it.chickenDead }
    val activeDays = cageLogs.map { it.date }.distinct().size.coerceAtLeast(1)

    val fcr = if (totalEggWeight > 0f) totalFeed / totalEggWeight else 0f
    val hdp = if (currentPop > 0) (totalEggs.toFloat() / (currentPop * activeDays)) * 100f else 0f

    // Search and Pagination States
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(0) }

    // Reset pagination on search change
    LaunchedEffect(searchQuery) {
        currentPage = 0
    }

    val filteredLogs = remember(cageLogs, searchQuery) {
        if (searchQuery.isBlank()) {
            cageLogs
        } else {
            cageLogs.filter { log ->
                log.date.contains(searchQuery, ignoreCase = true) ||
                log.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val pageSize = 4
    val totalPages = remember(filteredLogs) {
        (filteredLogs.size + pageSize - 1) / pageSize
    }
    val currentPageClamped = remember(currentPage, totalPages) {
        if (totalPages > 0) currentPage.coerceIn(0, totalPages - 1) else 0
    }
    val paginatedLogs = remember(filteredLogs, currentPageClamped) {
        filteredLogs.drop(currentPageClamped * pageSize).take(pageSize)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("kandang_card_${cage.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row (Clickable to Expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRenameClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename cage",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Populasi: $currentPop ekor • Kontribusi: ${String.format(Locale.US, "%.1f", cagePercentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete cage",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick adjustment stepper (always visible, easy to use!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { onUpdatePopulation(cage, (currentPop - 50).coerceAtLeast(0)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("-50", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onUpdatePopulation(cage, (currentPop - 10).coerceAtLeast(0)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("-10", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onAdjustPopulationClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentPop ekor",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { onUpdatePopulation(cage, currentPop + 10) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("+10", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onUpdatePopulation(cage, currentPop + 50) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("+50", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expandable statistics details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "STATISTIK PRODUKTIVITAS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // HDP
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HDP Rata-rata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (hdp > 0f) String.format(Locale.US, "%.1f%%", hdp) else "-",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hdp >= 85f) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // FCR
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FCR Rata-rata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (fcr > 0f) String.format(Locale.US, "%.2f", fcr) else "-",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fcr > 0f && fcr <= 2.3f) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Eggs
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Kumulatif Telur", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalEggs btr", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Total Dead
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Kematian", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$totalDead ekor",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalDead > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Header for Log Section & Search
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RIWAYAT & LOG KANDANG",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (cageLogs.isNotEmpty()) {
                            Text(
                                text = "${filteredLogs.size} data",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (cageLogs.isEmpty()) {
                        Text(
                            text = "Belum ada pencatatan data log untuk kandang ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        // Search Input Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari berdasarkan tgl atau catatan...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        if (filteredLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada riwayat log yang sesuai kata kunci.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            // Render Paginated Logs
                            paginatedLogs.forEach { log ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = log.date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "🥚 ${log.eggCount} btr (${log.eggWeight}kg) • 🌾 ${log.feedAmount}kg",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (log.chickenDead > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.errorContainer)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "☠️ ${log.chickenDead} mati",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        if (log.notes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Memo: ${log.notes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Pagination Controls UI
                            if (totalPages > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (currentPageClamped > 0) currentPage = currentPageClamped - 1 },
                                        enabled = currentPageClamped > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "Halaman Sebelumnya",
                                            tint = if (currentPageClamped > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }

                                    Text(
                                        text = "Halaman ${currentPageClamped + 1} dari $totalPages",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    IconButton(
                                        onClick = { if (currentPageClamped < totalPages - 1) currentPage = currentPageClamped + 1 },
                                        enabled = currentPageClamped < totalPages - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Halaman Selanjutnya",
                                            tint = if (currentPageClamped < totalPages - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
