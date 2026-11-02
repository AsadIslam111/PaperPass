package com.shakib.paperpass.presentation.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.domain.model.Quota
import com.shakib.paperpass.domain.model.User
import com.shakib.paperpass.domain.model.UserRole
import com.shakib.paperpass.presentation.components.PrinterCard
import com.shakib.paperpass.presentation.components.QuotaCard
import com.shakib.paperpass.presentation.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    quota: Quota?,
    printers: List<Printer>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onSelectPrinter: (Printer) -> Unit,
    onAddManualPrinter: (String, String, String) -> Unit,
    onDeletePrinter: ((Printer) -> Unit)? = null,
    onNavigateToAdmin: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit
) {
    var showAddPrinterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PaperPass", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${user.fullName} (${user.role.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    if (user.role == UserRole.ADMIN) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Panel", tint = TealAccent)
                        }
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Print History")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartScan,
                containerColor = TealAccent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
                text = { Text(if (isScanning) "Discovering LAN..." else "Scan LAN Printers") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                QuotaCard(
                    quota = quota,
                    userName = user.fullName,
                    department = user.department
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Network Printers (${printers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FilledTonalButton(
                        onClick = { showAddPrinterDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add IP", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }

            if (printers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrintDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No printers discovered yet",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap 'Scan LAN Printers' or enter a manual IP address.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(printers) { printer ->
                    PrinterCard(
                        printer = printer,
                        userDepartment = user.department,
                        isAdmin = user.role == UserRole.ADMIN,
                        onSelectPrinter = onSelectPrinter,
                        onDeletePrinter = onDeletePrinter
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(120.dp)) // Generous padding so FAB never covers card buttons
            }
        }
    }

    if (showAddPrinterDialog) {
        AddPrinterDialog(
            onDismiss = { showAddPrinterDialog = false },
            onConfirm = { name, ip, dept ->
                onAddManualPrinter(name, ip, dept)
                showAddPrinterDialog = false
            }
        )
    }
}

@Composable
fun AddPrinterDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, ip: String, department: String) -> Unit
) {
    var name by remember { mutableStateOf("PC Virtual Printer") }
    var ip by remember { mutableStateOf("192.168.0.226") }
    var department by remember { mutableStateOf("ALL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Printer by IP") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Printer Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IPv4 Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Restricted Dept (or ALL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, ip, department) },
                enabled = name.isNotBlank() && ip.isNotBlank()
            ) {
                Text("Save Printer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

