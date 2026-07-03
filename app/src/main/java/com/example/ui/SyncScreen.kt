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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    // Real backend additionals
    useRealServer: Boolean,
    serverUrl: String,
    authCookie: String,
    usernameStr: String,
    passwordStr: String,
    isLoggedIn: Boolean,
    onToggleUseRealServer: (Boolean) -> Unit,
    onConnectAndLogin: (String, String, String, String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    // Form states
    var inputServerUrl by remember { mutableStateOf(serverUrl) }
    var inputAuthCookie by remember { mutableStateOf(authCookie) }
    var inputUsername by remember { mutableStateOf(usernameStr) }
    var inputPassword by remember { mutableStateOf(passwordStr) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Sync form values with external updates
    LaunchedEffect(serverUrl, authCookie, usernameStr, passwordStr) {
        inputServerUrl = serverUrl
        inputAuthCookie = authCookie
        inputUsername = usernameStr
        inputPassword = passwordStr
    }

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
                containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
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
                        "Aplikasi dapat berkomunikasi dengan server. Semua data baru akan di-sync saat Anda menekan tombol 'Sinkronisasi'." 
                        else "Semua input data akan disimpan langsung ke database lokal SQLite (Room) tanpa hambatan. Koneksi ke server ditutup sementara.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1.5. BACKEND SERVER CONFIGURATION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INTEGRASI BACKEND SERVER",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (useRealServer) "Mode: Real Server (API)" else "Mode: Simulasi Offline-First",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (useRealServer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = useRealServer,
                        onCheckedChange = { onToggleUseRealServer(it) },
                        modifier = Modifier.testTag("use_real_server_switch")
                    )
                }

                AnimatedVisibility(visible = useRealServer) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Divider()

                        // Server URL
                        OutlinedTextField(
                            value = inputServerUrl,
                            onValueChange = { inputServerUrl = it },
                            label = { Text("Server Base URL (Express/NodeJS)") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://example-backend.com/") }
                        )

                        // AI Studio Cookie
                        OutlinedTextField(
                            value = inputAuthCookie,
                            onValueChange = { inputAuthCookie = it },
                            label = { Text("AI Studio Auth Cookie (Opsional untuk Dev)") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("__SECURE-aistudio_auth_token=...") }
                        )
                        Text(
                            text = "Dibutuhkan hanya jika melakukan hit ke URL development 'ais-dev-...'.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Credentials Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputUsername,
                                onValueChange = { inputUsername = it },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = inputPassword,
                                onValueChange = { inputPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Save Configurations Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status login
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isLoggedIn) Color(0xFF4CAF50) else Color(0xFFFF5722))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLoggedIn) "Otorisasi OK (JWT)" else "Sesi Belum Login",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLoggedIn) Color(0xFF4CAF50) else Color(0xFFFF5722)
                                )
                            }

                            Row {
                                if (isLoggedIn) {
                                    TextButton(onClick = { onLogout() }) {
                                        Text("Keluar (Logout)", color = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Button(
                                    onClick = {
                                        onConnectAndLogin(inputServerUrl, inputAuthCookie, inputUsername, inputPassword)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Simpan & Connect", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. CONFLICT RESOLUTION POLICY (Only relevant for simulation)
        AnimatedVisibility(visible = !useRealServer) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STRATEGI RESOLUSI KONFLIK (SIMULASI)",
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
        }

        // 3. TRIGGER SYNC BUTTON & STATUS BAR
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(
                            imageVector = if (conflicts.isNotEmpty()) Icons.Default.SyncProblem else Icons.Default.CloudSync,
                            contentDescription = "Sync Info",
                            tint = if (conflicts.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = syncStatusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (conflicts.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = { onTriggerSync() },
                    enabled = isOnline && !isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_trigger_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (conflicts.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSyncing) "Mensinkronkan..." else "Mulai Sinkronisasi Data",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // 4. CONFLICT RESOLUTION VIEWER (Manual conflict UI)
        AnimatedVisibility(visible = conflicts.isNotEmpty() && !useRealServer) {
            Column {
                Text(
                    text = "KONFLIK DATA TERDETEKSI (MEMBUTUHKAN RESOLUSI MANUAL)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                conflicts.forEach { conflict ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = conflict.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Local Version Column
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Versi HP (Lokal)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                                        Text("🥚 ${conflict.localLog.eggCount} butir", style = MaterialTheme.typography.bodySmall)
                                        Text("⚖️ ${conflict.localLog.eggWeight} kg", style = MaterialTheme.typography.bodySmall)
                                        Text("🌾 ${conflict.localLog.feedAmount} kg", style = MaterialTheme.typography.bodySmall)
                                        Text("☠️ ${conflict.localLog.chickenDead} mati", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "Diubah: ${timeFormatter.format(Date(conflict.localLog.lastUpdated))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { onResolveConflict(conflict, true) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Pilih HP", fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Cloud Version Column
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Versi Cloud (Remote)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                                        Text("🥚 ${conflict.remoteLog.eggCount} butir", style = MaterialTheme.typography.bodySmall)
                                        Text("⚖️ ${conflict.remoteLog.eggWeight} kg", style = MaterialTheme.typography.bodySmall)
                                        Text("🌾 ${conflict.remoteLog.feedAmount} kg", style = MaterialTheme.typography.bodySmall)
                                        Text("☠️ ${conflict.remoteLog.chickenDead} mati", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "Diubah: ${timeFormatter.format(Date(conflict.remoteLog.lastUpdated))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { onResolveConflict(conflict, false) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Pilih Cloud", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. CLOUD DATABASE VIEWER (SIMULATION LOGS)
        AnimatedVisibility(visible = !useRealServer) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATABASE SERVER CLOUD (SIMULASI)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { onInjectConflict() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = "Inject conflict")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suntik Konflik", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
}
