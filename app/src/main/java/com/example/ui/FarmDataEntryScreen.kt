package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FarmDataEntryScreen(
    selectedKandang: String,
    selectedDate: String,
    eggCount: Int,
    eggWeight: Float,
    feedAmount: Float,
    chickenDead: Int,
    notes: String,
    isEditingExisting: Boolean,
    kandangPopulations: Map<String, Int>,
    onKandangSelected: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onAdjustEggCount: (Int) -> Unit,
    onAdjustEggWeight: (Float) -> Unit,
    onAdjustFeedAmount: (Float) -> Unit,
    onAdjustChickenDead: (Int) -> Unit,
    onSetEggCount: (Int) -> Unit,
    onSetEggWeight: (Float) -> Unit,
    onSetFeedAmount: (Float) -> Unit,
    onSetChickenDead: (Int) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSaveLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Local string states to allow raw user manual typing without breaking / losing focus / decimal jitter
    var eggCountStr by remember(eggCount) { mutableStateOf(eggCount.toString()) }
    var eggWeightStr by remember(eggWeight) {
        mutableStateOf(if (eggWeight == 0f) "" else String.format(Locale.US, "%.1f", eggWeight).replace(Regex("\\.0$"), ""))
    }
    var feedAmountStr by remember(feedAmount) {
        mutableStateOf(if (feedAmount == 0f) "" else String.format(Locale.US, "%.1f", feedAmount).replace(Regex("\\.0$"), ""))
    }
    var chickenDeadStr by remember(chickenDead) { mutableStateOf(chickenDead.toString()) }

    // Real-time Business Logic / Farm Input Validation Calculations (Point 1: Guardrails)
    val avgEggWeightG = if (eggCount > 0) (eggWeight * 1000f) / eggCount else 0f
    val isAvgEggWeightUnusual = eggCount > 0 && eggWeight > 0f && (avgEggWeightG < 45f || avgEggWeightG > 75f)
    val isEggCountZeroButWeightSet = eggCount == 0 && eggWeight > 0f
    val isEggWeightZeroButCountSet = eggCount > 0 && eggWeight == 0f
    val isFeedZeroButEggsSet = eggCount > 0 && feedAmount == 0f
    val isKematianHigh = chickenDead >= 10
    
    val isAllZero = eggCount == 0 && eggWeight == 0f && feedAmount == 0f

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Parse selectedDate to pre-populate the dialog
    try {
        val dateParts = selectedDate.split("-")
        if (dateParts.size == 3) {
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
        }
    } catch (e: Exception) {
        // Fallback to current date
    }

    val datePickerDialog = remember(selectedDate) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Warning if editing existing record
        if (isEditingExisting) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Edit Warning",
                        tint = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Data untuk $selectedKandang pada tanggal $selectedDate sudah ada. Menyimpan akan memperbarui data tersebut secara offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1. KANDANG SELECTOR (Segmented - Scrollable dynamic chips!)
        Text(
            text = "PILIH KANDANG",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        val sortedKandangs = remember(kandangPopulations) {
            kandangPopulations.keys.toList().sorted()
        }
        if (sortedKandangs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Belum ada kandang aktif! Silakan tambah kandang terlebih dahulu di menu Kandang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedKandangs) { kandang ->
                    val isSelected = selectedKandang == kandang
                    Box(
                        modifier = Modifier
                            .widthIn(min = 105.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onKandangSelected(kandang) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = kandang,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. DATE SELECTOR (1-Tap Today & Yesterday buttons!)
        Text(
            text = "TANGGAL PENCATATAN",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Today Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedDate == todayStr) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onDateSelected(todayStr) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HARI INI",
                    color = if (selectedDate == todayStr) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Yesterday Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedDate == yesterdayStr) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onDateSelected(yesterdayStr) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KEMARIN",
                    color = if (selectedDate == yesterdayStr) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Custom Date Input Field (DatePickerDialog trigger)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(56.dp)
            ) {
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tanggal") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date picker icon",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                // Transparent overlay to intercept click and open DatePickerDialog safely
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { datePickerDialog.show() }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. EGG COUNT STEPPER
        LargeNumericStepper(
            title = "Jumlah Telur (Butir)",
            value = eggCountStr,
            unit = "butir",
            onIncrement = { onAdjustEggCount(it.toInt()) },
            onDecrement = { onAdjustEggCount(-it.toInt()) },
            steps = listOf(1f, 10f),
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                eggCountStr = filtered
                val parsed = filtered.toIntOrNull() ?: 0
                onSetEggCount(parsed)
            },
            isError = isEggCountZeroButWeightSet,
            errorMessage = if (isEggCountZeroButWeightSet) "Berat terisi, jumlah butir tidak boleh 0" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. EGG WEIGHT STEPPER
        LargeNumericStepper(
            title = "Berat Total Telur (Kg)",
            value = eggWeightStr,
            unit = "kg",
            onIncrement = { onAdjustEggWeight(it) },
            onDecrement = { onAdjustEggWeight(-it) },
            steps = listOf(0.1f, 1.0f),
            onValueChange = { newValue ->
                val normalized = newValue.replace(',', '.')
                if (normalized.count { it == '.' } <= 1 && normalized.all { it.isDigit() || it == '.' }) {
                    eggWeightStr = normalized
                    val parsed = normalized.toFloatOrNull() ?: 0f
                    onSetEggWeight(parsed)
                }
            },
            isError = isEggWeightZeroButCountSet || isAvgEggWeightUnusual,
            errorMessage = when {
                isEggWeightZeroButCountSet -> "Jumlah terisi, berat tidak boleh 0"
                isAvgEggWeightUnusual -> "Rata-rata berat per butir (${String.format(Locale.US, "%.1f", avgEggWeightG)}g) tidak wajar (Batas: 45g - 75g)"
                else -> null
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. FEED AMOUNT STEPPER
        LargeNumericStepper(
            title = "Konsumsi Pakan (Kg)",
            value = feedAmountStr,
            unit = "kg",
            onIncrement = { onAdjustFeedAmount(it) },
            onDecrement = { onAdjustFeedAmount(-it) },
            steps = listOf(1.0f, 10.0f),
            onValueChange = { newValue ->
                val normalized = newValue.replace(',', '.')
                if (normalized.count { it == '.' } <= 1 && normalized.all { it.isDigit() || it == '.' }) {
                    feedAmountStr = normalized
                    val parsed = normalized.toFloatOrNull() ?: 0f
                    onSetFeedAmount(parsed)
                }
            },
            isError = isFeedZeroButEggsSet,
            errorMessage = if (isFeedZeroButEggsSet) "Pakan tidak boleh kosong saat ada telur" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. CHICKEN DEAD STEPPER
        LargeNumericStepper(
            title = "Kematian Ayam (Ekor)",
            value = chickenDeadStr,
            unit = "ekor",
            onIncrement = { onAdjustChickenDead(it.toInt()) },
            onDecrement = { onAdjustChickenDead(-it.toInt()) },
            steps = listOf(1f),
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                chickenDeadStr = filtered
                val parsed = filtered.toIntOrNull() ?: 0
                onSetChickenDead(parsed)
            },
            isError = isKematianHigh,
            errorMessage = if (isKematianHigh) "Peringatan: Angka kematian ayam tercatat tinggi!" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7. NOTES INPUT
        Text(
            text = "CATATAN TAMBAHAN",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            placeholder = { Text("Misal: Kondisi cuaca panas, ganti merek pakan, dll.") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Real-Time Guidance & Input Validation Panel (Point 1)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isAllZero -> MaterialTheme.colorScheme.errorContainer
                    isEggCountZeroButWeightSet || isEggWeightZeroButCountSet || isFeedZeroButEggsSet || isAvgEggWeightUnusual || isKematianHigh -> Color(0xFFFFF3E0) // Warm Amber Container
                    else -> Color(0xFFE8F5E9) // Success Green Container
                }
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = when {
                    isAllZero -> MaterialTheme.colorScheme.error
                    isEggCountZeroButWeightSet || isEggWeightZeroButCountSet || isFeedZeroButEggsSet || isAvgEggWeightUnusual || isKematianHigh -> Color(0xFFFFB74D) // Amber outline
                    else -> Color(0xFF81C784) // Green outline
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when {
                        isAllZero -> "STATUS FORMULIR: KOSONG"
                        isEggCountZeroButWeightSet || isEggWeightZeroButCountSet || isFeedZeroButEggsSet || isAvgEggWeightUnusual || isKematianHigh -> "⚠️ TINJAU PERINGATAN INPUT"
                        else -> "✅ FORMULIR VALID & SIAP"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = when {
                        isAllZero -> MaterialTheme.colorScheme.onErrorContainer
                        isEggCountZeroButWeightSet || isEggWeightZeroButCountSet || isFeedZeroButEggsSet || isAvgEggWeightUnusual || isKematianHigh -> Color(0xFFE65100)
                        else -> Color(0xFF1B5E20)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                when {
                    isAllZero -> {
                        Text(
                            text = "Semua angka bernilai 0. Harap isi data jumlah telur, berat telur, atau konsumsi pakan terlebih dahulu sebelum menyimpan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    isEggCountZeroButWeightSet || isEggWeightZeroButCountSet || isFeedZeroButEggsSet || isAvgEggWeightUnusual || isKematianHigh -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isEggCountZeroButWeightSet) {
                                Text("• Berat telur terisi ($eggWeight kg), namun jumlah butir telur masih kosong (0 butir).", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                            }
                            if (isEggWeightZeroButCountSet) {
                                Text("• Jumlah telur terisi ($eggCount butir), namun berat total telur masih kosong (0 kg).", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                            }
                            if (isAvgEggWeightUnusual) {
                                Text("• Rata-rata berat per butir telur (${String.format(Locale.US, "%.1f", avgEggWeightG)}g) di luar batas normal ayam petelur (45g - 75g).", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                            }
                            if (isFeedZeroButEggsSet) {
                                Text("• Jumlah pakan pakan tercatat kosong (0 kg), padahal terdapat produksi telur sebesar $eggCount butir.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                            }
                            if (isKematianHigh) {
                                Text("• Angka kematian ayam tercatat tinggi ($chickenDead ekor). Silakan verifikasi untuk kepastian data.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "Semua rasio input dan data rasio pakan (FCR) terlihat logis. Anda dapat menyimpan data ini dengan aman ke database lokal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 8. MASTER SAVE BUTTON - Giant, high contrast, full width!
        Button(
            onClick = onSaveLog,
            enabled = !isAllZero,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("submit_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Simpan Data",
                    tint = if (isAllZero) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "SIMPAN KE DATABASE LOKAL",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (isAllZero) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
