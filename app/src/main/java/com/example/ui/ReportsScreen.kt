package com.example.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LayerFarmLog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    logs: List<LayerFarmLog>,
    eggPrice: Float,
    feedPrice: Float,
    kandangPopulations: Map<String, Int>,
    onUpdateEggPrice: (Float) -> Unit,
    onUpdateFeedPrice: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    
    // Filter States
    var selectedKandangFilter by remember { mutableStateOf("Semua Kandang") }
    var selectedPeriodFilter by remember { mutableStateOf("Bulan Ini") } // "Hari Ini", "7 Hari Terakhir", "30 Hari Terakhir", "Bulan Ini", "Semua Waktu"
    var kandangMenuExpanded by remember { mutableStateOf(false) }
    var periodMenuExpanded by remember { mutableStateOf(false) }

    // Calculate dates
    val today = Calendar.getInstance()
    val todayStr = sdf.format(today.time)
    
    val sevenDaysAgoStr = remember(todayStr) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        sdf.format(cal.time)
    }
    
    val thirtyDaysAgoStr = remember(todayStr) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        sdf.format(cal.time)
    }

    val firstDayOfMonthStr = remember(todayStr) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        sdf.format(cal.time)
    }

    // List of available Cages
    val uniqueKandangs = remember(logs) {
        val list = logs.map { it.kandangName }.distinct().sorted()
        listOf("Semua Kandang") + list
    }

    // Filtered Logs
    val filteredLogs = remember(logs, selectedKandangFilter, selectedPeriodFilter) {
        logs.filter { log ->
            val matchKandang = selectedKandangFilter == "Semua Kandang" || log.kandangName == selectedKandangFilter
            val matchTime = when (selectedPeriodFilter) {
                "Hari Ini" -> log.date == todayStr
                "7 Hari Terakhir" -> log.date >= sevenDaysAgoStr && log.date <= todayStr
                "30 Hari Terakhir" -> log.date >= thirtyDaysAgoStr && log.date <= todayStr
                "Bulan Ini" -> log.date >= firstDayOfMonthStr && log.date <= todayStr
                else -> true
            }
            matchKandang && matchTime
        }.sortedBy { it.date }
    }

    // Metrics Calculations
    val totalEggs = filteredLogs.sumOf { it.eggCount }
    val totalEggWeight = filteredLogs.sumOf { it.eggWeight.toDouble() }.toFloat()
    val totalFeed = filteredLogs.sumOf { it.feedAmount.toDouble() }.toFloat()
    val totalDead = filteredLogs.sumOf { it.chickenDead }
    
    // FCR = totalFeed / totalEggWeight
    val fcr = if (totalEggWeight > 0) totalFeed / totalEggWeight else 0f
    
    // Average egg weight per piece in grams
    val avgEggWeightGrams = if (totalEggs > 0) (totalEggWeight * 1000f) / totalEggs else 0f

    // HDP (Hen Day Production) calculation
    // Days with records
    val uniqueDaysCount = filteredLogs.map { it.date }.distinct().size.coerceAtLeast(1)
    
    // Estimated population based on configured population of coops
    val targetCoops = if (selectedKandangFilter == "Semua Kandang") {
        kandangPopulations.keys.toList()
    } else {
        listOf(selectedKandangFilter)
    }
    val activePopulationBase = targetCoops.sumOf { kandangPopulations[it] ?: 500 }
    
    val hdp = if (activePopulationBase > 0 && uniqueDaysCount > 0) {
        val expectedTotalEggs = activePopulationBase * uniqueDaysCount
        (totalEggs.toFloat() / expectedTotalEggs.toFloat()) * 100f
    } else {
        0f
    }

    // Mortality rate based on active population
    val mortalityRate = if (activePopulationBase > 0) {
        (totalDead.toFloat() / activePopulationBase.toFloat()) * 100f
    } else {
        0f
    }

    // Financial estimations
    val estimatedRevenue = totalEggWeight * eggPrice
    val estimatedFeedCost = totalFeed * feedPrice
    val grossProfit = estimatedRevenue - estimatedFeedCost

    // Indonesian Rupiah Formatter
    val rupiahFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Laporan & Analisis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Performa produksi, pakan, dan finansial",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Share Button
            IconButton(
                onClick = {
                    val shareText = buildString {
                        appendLine("📊 *LAPORAN PERFORMA PETERNAKAN LAYER*")
                        appendLine("📅 Periode: $selectedPeriodFilter")
                        appendLine("🐓 Kandang: $selectedKandangFilter")
                        appendLine("----------------------------------------")
                        appendLine("🥚 *Produksi Telur:*")
                        appendLine("  • Total Telur: ${String.format(Locale.US, "%,d", totalEggs)} butir")
                        appendLine("  • Total Berat: ${String.format(Locale.US, "%,.2f", totalEggWeight)} kg")
                        appendLine("  • Rata-rata/butir: ${String.format(Locale.US, "%,.1f", avgEggWeightGrams)} gram")
                        appendLine("  • Hen Day Production (HDP): ${String.format(Locale.US, "%,.2f", hdp)}%")
                        appendLine()
                        appendLine("🍂 *Konsumsi Pakan:*")
                        appendLine("  • Total Pakan: ${String.format(Locale.US, "%,.2f", totalFeed)} kg")
                        appendLine("  • *FCR (Feed Conversion Ratio):* ${String.format(Locale.US, "%,.2f", fcr)}")
                        appendLine("    _Status FCR:_ ${if (fcr <= 2.2f && fcr > 0f) "🟢 Optimal" else if (fcr <= 2.4f && fcr > 0f) "🟡 Cukup" else "🔴 Buruk"}")
                        appendLine()
                        appendLine("💀 *Kematian (Mortalitas):*")
                        appendLine("  • Jumlah Mati: $totalDead ekor (${String.format(Locale.US, "%,.2f", mortalityRate)}%)")
                        appendLine()
                        appendLine("💰 *Estimasi Finansial:*")
                        appendLine("  • Omset Telur: ${rupiahFormatter.format(estimatedRevenue)}")
                        appendLine("  • Biaya Pakan: ${rupiahFormatter.format(estimatedFeedCost)}")
                        appendLine("  • *Laba Kotor (Pendapatan - Pakan):* ${rupiahFormatter.format(grossProfit)}")
                        appendLine("----------------------------------------")
                        appendLine("Generated via *Aplikasi Ternak Layer* 🐔")
                    }
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Bagikan Laporan")
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.testTag("report_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Bagikan Laporan",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Filters card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kandang Selector Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .clickable { kandangMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kandang",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = selectedKandangFilter,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Kandang",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = kandangMenuExpanded,
                        onDismissRequest = { kandangMenuExpanded = false }
                    ) {
                        uniqueKandangs.forEach { kandang ->
                            DropdownMenuItem(
                                text = { Text(kandang, fontWeight = if (selectedKandangFilter == kandang) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedKandangFilter = kandang
                                    kandangMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Period Selector Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .clickable { periodMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Periode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = when (selectedPeriodFilter) {
                                    "7 Hari Terakhir" -> "7 Hari"
                                    "30 Hari Terakhir" -> "30 Hari"
                                    "Semua Waktu" -> "Semua"
                                    else -> selectedPeriodFilter
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Periode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = periodMenuExpanded,
                        onDismissRequest = { periodMenuExpanded = false }
                    ) {
                        val timePeriods = listOf(
                            "7 Hari Terakhir" to "7 Hari",
                            "30 Hari Terakhir" to "30 Hari",
                            "Bulan Ini" to "Bulan Ini",
                            "Semua Waktu" to "Semua"
                        )
                        timePeriods.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontWeight = if (selectedPeriodFilter == value) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedPeriodFilter = value
                                    periodMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Scrollable report content
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Empty",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Data Laporan Kosong",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Belum ada rekaman data pada filter yang Anda pilih. Silakan catat data harian terlebih dahulu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Local state for expandable price adjustments
            var isPriceEditorExpanded by remember { mutableStateOf(false) }
            var localEggPriceText by remember(eggPrice) { mutableStateOf(eggPrice.toInt().toString()) }
            var localFeedPriceText by remember(feedPrice) { mutableStateOf(feedPrice.toInt().toString()) }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Interactive Price Tuning Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPriceEditorExpanded = !isPriceEditorExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Penyesuaian Harga Harian",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!isPriceEditorExpanded) {
                                        Text(
                                            text = "Telur: ${rupiahFormatter.format(eggPrice)}/kg • Pakan: ${rupiahFormatter.format(feedPrice)}/kg",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Sesuaikan harga pasar yang fluktuatif untuk analisis periode ini",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { isPriceEditorExpanded = !isPriceEditorExpanded }) {
                                Icon(
                                    imageVector = if (isPriceEditorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isPriceEditorExpanded) "Sembunyikan" else "Tampilkan"
                                )
                            }
                        }

                        AnimatedVisibility(visible = isPriceEditorExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // EGG PRICE SECTION
                                Text(
                                    text = "🥚 HARGA JUAL TELUR (Rp / Kg)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = rupiahFormatter.format(eggPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Adjust Eggs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(-1000f, -500f, +500f, +1000f).forEach { adjustment ->
                                        val label = if (adjustment > 0) "+${adjustment.toInt()}" else adjustment.toInt().toString()
                                        val isPositive = adjustment > 0
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                                .clickable {
                                                    val newPrice = (eggPrice + adjustment).coerceAtLeast(0f)
                                                    onUpdateEggPrice(newPrice)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Direct TextField Inputs
                                OutlinedTextField(
                                    value = localEggPriceText,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }
                                        localEggPriceText = filtered
                                        filtered.toFloatOrNull()?.let {
                                            onUpdateEggPrice(it)
                                        }
                                    },
                                    label = { Text("Tulis Harga Telur Manual") },
                                    prefix = { Text("Rp ") },
                                    suffix = { Text("/kg") },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("report_egg_price_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // FEED PRICE SECTION
                                Text(
                                    text = "🍂 HARGA PAKAN AYAM (Rp / Kg)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = rupiahFormatter.format(feedPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Adjust Feed
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(-500f, -100f, +100f, +500f).forEach { adjustment ->
                                        val label = if (adjustment > 0) "+${adjustment.toInt()}" else adjustment.toInt().toString()
                                        val isPositive = adjustment > 0
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                                .clickable {
                                                    val newPrice = (feedPrice + adjustment).coerceAtLeast(0f)
                                                    onUpdateFeedPrice(newPrice)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = localFeedPriceText,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }
                                        localFeedPriceText = filtered
                                        filtered.toFloatOrNull()?.let {
                                            onUpdateFeedPrice(it)
                                        }
                                    },
                                    label = { Text("Tulis Harga Pakan Manual") },
                                    prefix = { Text("Rp ") },
                                    suffix = { Text("/kg") },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("report_feed_price_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Standard Presets Reset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "*Harga terupdate otomatis ke seluruh sistem",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    TextButton(
                                        onClick = {
                                            onUpdateEggPrice(25000f)
                                            onUpdateFeedPrice(8500f)
                                        }
                                    ) {
                                        Text("Atur Ulang Default 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // FCR KPI Gauge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Feed Conversion Ratio (FCR)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (fcr > 0f) String.format(Locale.US, "%.2f", fcr) else "-",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // Rating text
                            val (fcrText, fcrColor) = when {
                                fcr == 0f -> "Belum Terhitung" to MaterialTheme.colorScheme.onSurfaceVariant
                                fcr <= 2.2f -> "SANGAT OPTIMAL (Efisien)" to Color(0xFF2E7D32)
                                fcr <= 2.4f -> "CUKUP BAIK (Standar)" to Color(0xFFE65100)
                                else -> "BOROS (Evaluasi Pakan/Kesehatan!)" to Color(0xFFC62828)
                            }
                            
                            Text(
                                text = fcrText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Black,
                                color = fcrColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        // Icon gauge based on rating
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        fcr <= 2.2f && fcr > 0f -> Color(0xFFE8F5E9)
                                        fcr <= 2.4f && fcr > 0f -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFFFEBEE)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    fcr <= 2.2f && fcr > 0f -> Icons.Default.TrendingDown
                                    fcr <= 2.4f && fcr > 0f -> Icons.Default.TrendingFlat
                                    else -> Icons.Default.TrendingUp
                                },
                                contentDescription = null,
                                tint = when {
                                    fcr <= 2.2f && fcr > 0f -> Color(0xFF2E7D32)
                                    fcr <= 2.4f && fcr > 0f -> Color(0xFFE65100)
                                    else -> Color(0xFFC62828)
                                },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // KPI Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiItemCard(
                        title = "TOTAL TELUR",
                        value = "${String.format(Locale.US, "%,d", totalEggs)} butir",
                        subText = "HDP: ${String.format(Locale.US, "%.1f", hdp)}%",
                        icon = Icons.Default.Egg,
                        color = Color(0xFF8D6E63),
                        modifier = Modifier.weight(1f)
                    )
                    KpiItemCard(
                        title = "TOTAL BERAT",
                        value = "${String.format(Locale.US, "%,.1f", totalEggWeight)} kg",
                        subText = "Rata-rata: ${String.format(Locale.US, "%.1f", avgEggWeightGrams)}g",
                        icon = Icons.Default.Scale,
                        color = Color(0xFF42A5F5),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiItemCard(
                        title = "TOTAL PAKAN",
                        value = "${String.format(Locale.US, "%,.1f", totalFeed)} kg",
                        subText = "Efisiensi: ${if (fcr > 0) String.format(Locale.US, "%.2f", fcr) else "-"}",
                        icon = Icons.Default.Co2, // Placeholder
                        color = Color(0xFFFFA726),
                        modifier = Modifier.weight(1f)
                    )
                    KpiItemCard(
                        title = "MORTALITAS",
                        value = "$totalDead ekor",
                        subText = "Rasio: ${String.format(Locale.US, "%.2f", mortalityRate)}%",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Compose Canvas Charts: Production Trend
                Text(
                    text = "GRAFIK PRODUKSI HARIAN (BUTIR)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Plot logs on a Canvas
                        val chartLogs = filteredLogs.sortedBy { it.date }.takeLast(10) // show up to 10 points
                        if (chartLogs.size < 2) {
                            Text(
                                text = "Grafik butuh minimal 2 hari rekaman data log untuk digambar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        } else {
                            val maxProduction = chartLogs.maxOf { it.eggCount }.coerceAtLeast(100)
                            val minProduction = chartLogs.minOf { it.eggCount }
                            
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val pointsCount = chartLogs.size
                                val stepX = canvasWidth / (pointsCount - 1)
                                
                                val path = Path()
                                val fillPath = Path()
                                
                                chartLogs.forEachIndexed { index, log ->
                                    val ratio = log.eggCount.toFloat() / maxProduction.toFloat()
                                    // invert y as canvas draws top-down
                                    val x = index * stepX
                                    val y = canvasHeight - (ratio * canvasHeight * 0.8f) - (canvasHeight * 0.1f)
                                    
                                    if (index == 0) {
                                        path.moveTo(x, y)
                                        fillPath.moveTo(x, canvasHeight)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }
                                    
                                    if (index == pointsCount - 1) {
                                        fillPath.lineTo(x, canvasHeight)
                                        fillPath.close()
                                    }

                                    // Draw point circle
                                    drawCircle(
                                        color = Color(0xFF8D6E63),
                                        radius = 4.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }

                                // Draw Area under curve with gradient
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF8D6E63).copy(alpha = 0.3f),
                                            Color(0xFF8D6E63).copy(alpha = 0.0f)
                                        )
                                    )
                                )

                                // Draw Curve Line
                                drawPath(
                                    path = path,
                                    color = Color(0xFF8D6E63),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                            
                            // Labels Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = chartLogs.first().date.drop(5), // MM-DD
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tren Produksi (10 Transaksi Terakhir)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = chartLogs.last().date.drop(5),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estimasi Finansial Card
                Text(
                    text = "ESTIMASI FINANSIAL MANDIRI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Omset Telur (${String.format(Locale.US, "%,.1f", totalEggWeight)} kg)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                rupiahFormatter.format(estimatedRevenue),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Biaya Pakan (${String.format(Locale.US, "%,.1f", totalFeed)} kg)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                rupiahFormatter.format(estimatedFeedCost),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "ESTIMASI KEUNTUNGAN KOTOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Pendapatan dikurangi pakan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                rupiahFormatter.format(grossProfit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (grossProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual progress bar of Cost vs Revenue ratio
                        val ratio = if (estimatedRevenue > 0) (estimatedFeedCost / estimatedRevenue).coerceIn(0f, 1f) else 0f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rasio Pengeluaran Pakan",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(ratio * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ratio > 0.75f) Color(0xFFC62828) else Color(0xFF2E7D32)
                                )
                            }
                            LinearProgressIndicator(
                                progress = ratio,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (ratio > 0.75f) Color(0xFFC62828) else Color(0xFF2E7D32),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown by Cage (Tabel Per Kandang)
                Text(
                    text = "ANALISIS PERBANDINGAN KANDANG",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kandang", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f))
                            Text("Telur (Butir)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            Text("Pakan (kg)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            Text("FCR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Compute aggregates per Kandang
                        val coopAggregates = remember(filteredLogs) {
                            filteredLogs.groupBy { it.kandangName }.mapValues { (_, cageLogs) ->
                                val cageEggs = cageLogs.sumOf { it.eggCount }
                                val cageWeight = cageLogs.sumOf { it.eggWeight.toDouble() }.toFloat()
                                val cageFeed = cageLogs.sumOf { it.feedAmount.toDouble() }.toFloat()
                                val cageFcr = if (cageWeight > 0f) cageFeed / cageWeight else 0f
                                Triple(cageEggs, cageFeed, cageFcr)
                            }.toList().sortedBy { it.first }
                        }

                        coopAggregates.forEach { (coopName, triple) ->
                            val (cageEggs, cageFeed, cageFcr) = triple
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(coopName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text(String.format(Locale.US, "%,d", cageEggs), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(String.format(Locale.US, "%,.1f", cageFeed), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(
                                    if (cageFcr > 0f) String.format(Locale.US, "%.2f", cageFcr) else "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cageFcr <= 2.2f && cageFcr > 0f) Color(0xFF2E7D32) else if (cageFcr <= 2.4f && cageFcr > 0f) Color(0xFFE65100) else Color(0xFFC62828),
                                    modifier = Modifier.weight(0.8f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun KpiItemCard(
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
