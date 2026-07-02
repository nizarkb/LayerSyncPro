package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RemoteLog
import com.example.data.ResolutionStrategy
import com.example.data.SyncConflict
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(
    isOnline: Boolean,
    isSyncing: Boolean,
    syncStatusMessage: String,
    currentStrategy: ResolutionStrategy,
    conflicts: List<SyncConflict>,
    cloudDatabase: List<RemoteLog>,
    onToggleNetwork: () -> Unit,
    onChangeStrategy: (ResolutionStrategy) -> Unit,
    onTriggerSync: () -> Unit,
    onResolveConflict: (SyncConflict, Boolean) -> Unit,
    onInjectConflict: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. NETWORK SIMULATOR & CONTROL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.NetworkCheck else Icons.Default.OfflineBolt,
                            contentDescription = "Network",
                            tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SIMULATOR KONEKSI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (isOnline) "Status: Online (Daring)" else "Status: Offline (Luring)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Connection Switch
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { onToggleNetwork() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.error,
                            uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.testTag("network_switch")
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isOnline) 
                        "Aplikasi dapat berkomunikasi dengan server cloud. Semua data baru akan di-sync saat Anda menekan tombol 'Sinkronisasi'." 
                        else "Semua input data akan disimpan langsung ke database lokal SQLite (Room) tanpa hambatan. Koneksi ke server cloud ditutup sementara.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. CONFLICT RESOLUTION POLICY
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STRATEGI RESOLUSI KONFLIK",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ResolutionStrategy.values().forEach { strategy ->
                    val isSelected = currentStrategy == strategy
                    val desc = when (strategy) {
                        ResolutionStrategy.LAST_WRITE_WINS -> "Last-Write-Wins (LWW): Memilih data dengan timestamp perubahan terbaru."
                        ResolutionStrategy.LOCAL_WINS -> "Local Wins: Data perangkat offline selalu menimpa cloud."
                        ResolutionStrategy.REMOTE_WINS -> "Remote Wins: Data cloud selalu menimpa perangkat lokal."
                        ResolutionStrategy.MANUAL -> "Manual: Menampilkan konflik dan membiarkan pengguna memilih data."
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable { onChangeStrategy(strategy) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = strategy.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
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
        }

        // 3. MANUAL CONFLICT RESOLUTION HUB
        AnimatedVisibility(visible = conflicts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE65100))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SyncProblem,
                            contentDescription = "Conflict Alert",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KONFLIK DATA TERDETEKSI (${conflicts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Silakan selesaikan perbedaan data berikut ini:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )

                    conflicts.forEach { conflict ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = conflict.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Local Version Panel
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "VERSI LOKAL (OFFLINE)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🥚 Telur: ${conflict.localLog.eggCount} btr (${conflict.localLog.eggWeight} kg)", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                    Text("🌾 Pakan: ${conflict.localLog.feedAmount} kg", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                    Text("☠️ Mati: ${conflict.localLog.chickenDead}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                    Text("📝 Note: ${conflict.localLog.notes}", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Black)
                                    Text("🕒 Update: ${timeFormatter.format(Date(conflict.localLog.lastUpdated))}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onResolveConflict(conflict, true) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Pilih Lokal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Cloud Version Panel
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "VERSI CLOUD (ONLINE)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🥚 Telur: ${conflict.remoteLog.eggCount} btr (${conflict.remoteLog.eggWeight} kg)", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                    Text("🌾 Pakan: ${conflict.remoteLog.feedAmount} kg", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                    Text("☠️ Mati: ${conflict.remoteLog.chickenDead}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                    Text("📝 Note: ${conflict.remoteLog.notes}", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Black)
                                    Text("🕒 Update: ${timeFormatter.format(Date(conflict.remoteLog.lastUpdated))}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onResolveConflict(conflict, false) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Pilih Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. ACTION SYNC BUTTON
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sync status message banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = syncStatusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Trigger sync button
                    Button(
                        onClick = onTriggerSync,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(56.dp)
                            .testTag("sync_now_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SINKRONISASI SEKARANG", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Simulated Conflict Generator Button
                    Button(
                        onClick = onInjectConflict,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("inject_conflict_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = "Inject conflict")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suntik Konflik", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. CLOUD DATABASE VIEWER (SIMULATION LOGS)
        Column {
            Text(
                text = "DATABASE SERVER CLOUD (SIMULASI)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (cloudDatabase.isEmpty()) {
                Text(
                    text = "Cloud Database masih kosong.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                cloudDatabase.forEach { remote ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${remote.kandangName} - ${remote.date}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "🥚 ${remote.eggCount} butir (${remote.eggWeight} kg)  |  🌾 ${remote.feedAmount} kg |  ☠️ ${remote.chickenDead} mati",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (remote.notes.isNotEmpty()) {
                                    Text(
                                        text = "Note: ${remote.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "ID: ${remote.id.take(6)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
