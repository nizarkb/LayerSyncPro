package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BiosecurityCheck
import com.example.data.VaccinationSchedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    vaccinations: List<VaccinationSchedule>,
    biosecurityChecks: List<BiosecurityCheck>,
    kandangPopulations: Map<String, Int>,
    onAddVaccination: (kandang: String, name: String, date: String, method: String, notes: String) -> Unit,
    onUpdateVaccinationStatus: (id: String, completed: Boolean, actualDate: String?) -> Unit,
    onDeleteVaccination: (id: String) -> Unit,
    onSaveBiosecurityCheck: (BiosecurityCheck) -> Unit,
    onDeleteBiosecurityCheck: (id: String) -> Unit,
    onSeedDefaultProgram: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabState by remember { mutableStateOf(0) } // 0 = Vaksinasi, 1 = Biosekuriti

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
                            imageVector = Icons.Default.Vaccines,
                            contentDescription = "Vaksin",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vaksinasi", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("health_tab_vaccines")
            )
            Tab(
                selected = selectedTabState == 1,
                onClick = { selectedTabState = 1 },
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
                modifier = Modifier.testTag("health_tab_biosecurity")
            )
        }

        if (selectedTabState == 0) {
            VaccinationTab(
                vaccinations = vaccinations,
                kandangPopulations = kandangPopulations,
                onAddVaccination = onAddVaccination,
                onUpdateVaccinationStatus = onUpdateVaccinationStatus,
                onDeleteVaccination = onDeleteVaccination,
                onSeedDefaultProgram = onSeedDefaultProgram
            )
        } else {
            BiosecurityTab(
                checks = biosecurityChecks,
                onSaveCheck = onSaveBiosecurityCheck,
                onDeleteCheck = onDeleteBiosecurityCheck
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationTab(
    vaccinations: List<VaccinationSchedule>,
    kandangPopulations: Map<String, Int>,
    onAddVaccination: (kandang: String, name: String, date: String, method: String, notes: String) -> Unit,
    onUpdateVaccinationStatus: (id: String, completed: Boolean, actualDate: String?) -> Unit,
    onDeleteVaccination: (id: String) -> Unit,
    onSeedDefaultProgram: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedKandang by remember { mutableStateOf("Semua Kandang") }
    var vaccineNameInput by remember { mutableStateOf("") }
    var plannedDateInput by remember { mutableStateOf(getTodayString()) }
    var methodInput by remember { mutableStateOf("Air Minum") }
    var notesInput by remember { mutableStateOf("") }

    var filterState by remember { mutableStateOf("Semua") } // "Semua", "Belum", "Selesai"

    // Filtered vaccinations
    val filteredVaccines = remember(vaccinations, filterState) {
        when (filterState) {
            "Belum" -> vaccinations.filter { it.status == "Pending" }
            "Selesai" -> vaccinations.filter { it.status == "Completed" }
            else -> vaccinations
        }
    }

    val totalCount = vaccinations.size
    val pendingCount = vaccinations.count { it.status == "Pending" }
    val completedCount = vaccinations.count { it.status == "Completed" }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Upper Summary KPI Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status Program Vaksinasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Ayam layer sehat dengan perlindungan optimal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    if (totalCount == 0) {
                        Button(
                            onClick = onSeedDefaultProgram,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Seed", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rekomendasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                        Text("$totalCount Jadwal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Selesai", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20))
                        Text("$completedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Menunggu", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                        Text("$pendingCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                    }
                }
            }

            // Quick Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Semua", "Belum", "Selesai").forEach { option ->
                    val isSelected = filterState == option
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { filterState = option },
                        label = { Text(option, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // List of schedules
            if (filteredVaccines.isEmpty()) {
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
                            imageVector = Icons.Default.MedicalInformation,
                            contentDescription = "Empty vaccine",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Jadwal Vaksin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (totalCount == 0) "Belum ada rencana program vaksinasi ayam Anda. Silakan tambah manual atau pakai program rekomendasi standar."
                            else "Tidak ada jadwal yang sesuai filter ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        if (totalCount == 0) {
                            Button(onClick = onSeedDefaultProgram) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gunakan Program Standar Layer")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredVaccines, key = { it.id }) { schedule ->
                        VaccineItemCard(
                            schedule = schedule,
                            onToggleStatus = { isCompleted ->
                                onUpdateVaccinationStatus(schedule.id, isCompleted, if (isCompleted) getTodayString() else null)
                            },
                            onDelete = { onDeleteVaccination(schedule.id) }
                        )
                    }
                }
            }
        }

        // Add Vaccine Floating Action Button (FAB)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_vaccine_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Vaksinasi")
        }
    }

    // Add Vaccine Schedule Dialog
    if (showAddDialog) {
        val cageOptions = remember(kandangPopulations) {
            listOf("Semua Kandang") + kandangPopulations.keys.toList().sorted()
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Tambah Jadwal Vaksinasi",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kandang Selector Dialog Dropdown/Choice
                    Text("Pilih Kandang Sasaran:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cageOptions.take(3).forEach { option ->
                            val isSel = selectedKandang == option
                            OutlinedCard(
                                onClick = { selectedKandang = option },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (cageOptions.size > 3) {
                        TextButton(
                            onClick = {
                                val currentIdx = cageOptions.indexOf(selectedKandang)
                                val nextIdx = (currentIdx + 1) % cageOptions.size
                                selectedKandang = cageOptions[nextIdx]
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Opsi lain: $selectedKandang 🔄", fontSize = 11.sp)
                        }
                    }

                    // Vaccine Name Input
                    OutlinedTextField(
                        value = vaccineNameInput,
                        onValueChange = { vaccineNameInput = it },
                        label = { Text("Nama Vaksin") },
                        placeholder = { Text("Contoh: ND-IB Live, AI") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("vaccine_dialog_name")
                    )

                    // Planned Date Input
                    OutlinedTextField(
                        value = plannedDateInput,
                        onValueChange = { plannedDateInput = it },
                        label = { Text("Tanggal Rencana (YYYY-MM-DD)") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Method Selector Options (Choice pills)
                    Text("Metode Pemberian:", style = MaterialTheme.typography.labelMedium)
                    val adminMethods = listOf("Air Minum", "Suntik", "Tetes Mata", "Tetes Mulut", "Spray", "Tusuk Sayap")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            adminMethods.take(3).forEach { m ->
                                val isSelected = methodInput == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { methodInput = m }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = m,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            adminMethods.drop(3).forEach { m ->
                                val isSelected = methodInput == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { methodInput = m }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = m,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Catatan / Keterangan") },
                        placeholder = { Text("Contoh: Aturan 2 jam puasa minum") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanName = vaccineNameInput.trim()
                        if (cleanName.isNotEmpty()) {
                            onAddVaccination(
                                selectedKandang,
                                cleanName,
                                plannedDateInput.ifEmpty { getTodayString() },
                                methodInput,
                                notesInput
                            )
                            vaccineNameInput = ""
                            notesInput = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("vaccine_dialog_confirm")
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun VaccineItemCard(
    schedule: VaccinationSchedule,
    onToggleStatus: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = schedule.status == "Completed"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Status Badge & Delete Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PendingActions,
                                contentDescription = null,
                                tint = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCompleted) "Selesai" else "Menunggu",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Cage Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = schedule.kandangName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus jadwal",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Info
            Text(
                text = schedule.vaccineName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Admin Method & Expected Age
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${schedule.method} • Tanggal Rencana: ${schedule.plannedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actual Date details
            if (isCompleted && schedule.actualDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tanggal Selesai: ${schedule.actualDate}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            if (schedule.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 ${schedule.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checkbox Action Row (48dp Touch target)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                    .clickable { onToggleStatus(!isCompleted) }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Undo else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompleted) "Batalkan Selesai (Set Pending)" else "Tandai Selesai Vaksinasi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Jadwal?") },
            text = { Text("Apakah Anda yakin ingin menghapus jadwal vaksinasi '${schedule.vaccineName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --------------------------------------------------------------------------
// BIOSECURITY TAB
// --------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiosecurityTab(
    checks: List<BiosecurityCheck>,
    onSaveCheck: (BiosecurityCheck) -> Unit,
    onDeleteCheck: (id: String) -> Unit
) {
    // Current Checklist State
    var inspectorName by remember { mutableStateOf("Petugas Kandang") }
    var notes by remember { mutableStateOf("") }
    var footBath by remember { mutableStateOf(false) }
    var vehicleSpray by remember { mutableStateOf(false) }
    var warehouseClean by remember { mutableStateOf(false) }
    var walkwayClean by remember { mutableStateOf(false) }
    var mortalityDisposal by remember { mutableStateOf(false) }
    var trayDisinfection by remember { mutableStateOf(false) }
    var waterSanitization by remember { mutableStateOf(false) }
    var wildBirdControl by remember { mutableStateOf(false) }

    // Calculate live score
    val checklistItems = listOf(
        footBath, vehicleSpray, warehouseClean, walkwayClean,
        mortalityDisposal, trayDisinfection, waterSanitization, wildBirdControl
    )
    val checkedCount = checklistItems.count { it }
    val liveScore = (checkedCount.toFloat() / checklistItems.size * 100).toInt()

    // Status Styling based on compliance score
    val complianceColor = when {
        liveScore >= 80 -> Color(0xFF2E7D32) // Green
        liveScore >= 50 -> Color(0xFFF57C00) // Orange
        else -> Color(0xFFD32F2F)            // Red
    }
    val complianceText = when {
        liveScore >= 80 -> "Biosekuriti Sangat Aman (Optimal)"
        liveScore >= 50 -> "Biosekuriti Cukup Aman (Butuh Pembenahan)"
        else -> "Biosekuriti Rawan Infeksi! (Tingkatkan Disiplin)"
    }

    var historyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Compliance gauge card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, complianceColor.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Checklist Biosekuriti Harian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hari ini: ${getTodayString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(complianceColor.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$liveScore%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = complianceColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = complianceText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = complianceColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                LinearProgressIndicator(
                    progress = liveScore / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .height(8.dp)
                        .clip(CircleShape),
                    color = complianceColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Checklist Form Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "STANDAR OPERASIONAL PROSEDUR (SOP)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 8 checklist components with robust touch targets (48dp+)
        BiosecurityCheckItem(
            title = "1. Sanitasi Alas Kaki (Foot Bath)",
            desc = "Bak disinfeksi alas kaki terisi air pembunuh kuman segar di setiap pintu masuk kandang.",
            checked = footBath,
            onCheckedChange = { footBath = it },
            tag = "bio_footbath"
        )
        BiosecurityCheckItem(
            title = "2. Semprot Kendaraan Masuk",
            desc = "Kendaraan tamu, pakan, atau agen telur disemprot cairan desinfektan sebelum masuk area farm.",
            checked = vehicleSpray,
            onCheckedChange = { vehicleSpray = it },
            tag = "bio_vehiclespray"
        )
        BiosecurityCheckItem(
            title = "3. Kebersihan Gudang Pakan",
            desc = "Gudang pakan bersih kering, tertutup rapat, bebas dari tumpahan pakan, tikus, atau serangga liar.",
            checked = warehouseClean,
            onCheckedChange = { warehouseClean = it },
            tag = "bio_warehouse"
        )
        BiosecurityCheckItem(
            title = "4. Sapu Area Sela Kandang",
            desc = "Selokan parit bersih lancar, tidak ada tumpukan bulu basah atau feses berserakan di jalan.",
            checked = walkwayClean,
            onCheckedChange = { walkwayClean = it },
            tag = "bio_walkway"
        )
        BiosecurityCheckItem(
            title = "5. Pembuangan Bangkai Aman",
            desc = "Bangkai ayam mati dikubur dalam dengan kapur atau dibakar di tungku insinerator khusus secara rutin.",
            checked = mortalityDisposal,
            onCheckedChange = { mortalityDisposal = it },
            tag = "bio_mortality"
        )
        BiosecurityCheckItem(
            title = "6. Sterilisasi Tray Telur",
            desc = "Tray telur plastik dicuci/disemprot disinfektan sebelum diletakkan kembali ke dalam gudang.",
            checked = trayDisinfection,
            onCheckedChange = { trayDisinfection = it },
            tag = "bio_tray"
        )
        BiosecurityCheckItem(
            title = "7. Klorinasi Air Minum",
            desc = "Sistem sanitasi pipa douching berjalan, air minum steril mengandung dosis klorin aman.",
            checked = waterSanitization,
            onCheckedChange = { waterSanitization = it },
            tag = "bio_water"
        )
        BiosecurityCheckItem(
            title = "8. Kawat Penghalau Burung Liar",
            desc = "Kawat pelindung jaring kandang utuh sempurna tanpa celah berlubang untuk pencegahan flu burung.",
            checked = wildBirdControl,
            onCheckedChange = { wildBirdControl = it },
            tag = "bio_birds"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Inspector & notes
        OutlinedTextField(
            value = inspectorName,
            onValueChange = { inspectorName = it },
            label = { Text("Pemeriksa / Inspector") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Catatan Temuan Tambahan") },
            placeholder = { Text("Contoh: Kawat jaring kandang B ada robekan tipis") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Checklist Button
        Button(
            onClick = {
                val check = BiosecurityCheck(
                    date = getTodayString(),
                    inspectorName = inspectorName.ifEmpty { "Petugas" },
                    footBathActive = footBath,
                    vehicleSpray = vehicleSpray,
                    feedWarehouseClean = warehouseClean,
                    cageWalkwayClean = walkwayClean,
                    safeMortalityDisposal = mortalityDisposal,
                    eggTrayDisinfected = trayDisinfection,
                    waterSanitization = waterSanitization,
                    wildBirdControl = wildBirdControl,
                    score = liveScore,
                    notes = notes
                )
                onSaveCheck(check)
                notes = "" // reset
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_biosecurity_button"),
            colors = ButtonDefaults.buttonColors(containerColor = complianceColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Checklist Hari Ini", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Historic compliance checklist logs toggler
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { historyExpanded = !historyExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histori Kepatuhan Biosekuriti (${checks.size})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = if (historyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand history"
                    )
                }

                AnimatedVisibility(visible = historyExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (checks.isEmpty()) {
                            Text(
                                text = "Belum ada riwayat tercatat di database.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            checks.forEach { historic ->
                                val scoreColor = when {
                                    historic.score >= 80 -> Color(0xFF2E7D32)
                                    historic.score >= 50 -> Color(0xFFE65100)
                                    else -> Color(0xFFC62828)
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = historic.date,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Pemeriksa: ${historic.inspectorName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (historic.notes.isNotEmpty()) {
                                                Text(
                                                    text = "Temuan: ${historic.notes}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(scoreColor.copy(alpha = 0.12f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Kepatuhan: ${historic.score}%",
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = scoreColor
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteCheck(historic.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus log",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
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

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun BiosecurityCheckItem(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    val cardColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onCheckedChange(!checked) }
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(36.dp) // Generous sizing for easy touch
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Global date helper
private fun getTodayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
