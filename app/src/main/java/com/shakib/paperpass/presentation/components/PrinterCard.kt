package com.shakib.paperpass.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.domain.model.PrinterStatus
import com.shakib.paperpass.presentation.theme.StatusGreen
import com.shakib.paperpass.presentation.theme.StatusRed
import com.shakib.paperpass.presentation.theme.StatusYellow
import com.shakib.paperpass.presentation.theme.TealAccent

@Composable
fun PrinterCard(
    printer: Printer,
    userDepartment: String,
    isAdmin: Boolean,
    onSelectPrinter: (Printer) -> Unit,
    onDeletePrinter: ((Printer) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isRestricted = printer.departmentRestriction != "ALL" && !isAdmin && printer.departmentRestriction != userDepartment

    val statusColor = when (printer.status) {
        PrinterStatus.ONLINE -> StatusGreen
        PrinterStatus.BUSY -> StatusYellow
        PrinterStatus.OFFLINE, PrinterStatus.ERROR -> StatusRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusColor)
                    )
                    Text(
                        text = printer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Department restriction badge
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = printer.departmentRestriction,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    if (onDeletePrinter != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .size(28.dp)
                                .padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove Printer",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "IP: ${printer.ipAddress}:${printer.port} • Model: ${printer.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Toner gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Black Toner: ${printer.blackTonerPct}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                    LinearProgressIndicator(
                        progress = { printer.blackTonerPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color.DarkGray,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (printer.colorTonerPct > 0) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Color Toner: ${printer.colorTonerPct}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                        LinearProgressIndicator(
                            progress = { printer.colorTonerPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = TealAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onSelectPrinter(printer) },
                enabled = !isRestricted && printer.status == PrinterStatus.ONLINE,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRestricted) "Restricted to ${printer.departmentRestriction}" else "Select & Configure Print",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Printer") },
            text = { Text("Are you sure you want to remove '${printer.name}' (${printer.ipAddress}) from your list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePrinter?.invoke(printer)
                    }
                ) {
                    Text("Remove", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
