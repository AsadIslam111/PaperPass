package com.shakib.paperpass.presentation.screens.print

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.domain.model.Quota
import com.shakib.paperpass.domain.model.User
import com.shakib.paperpass.domain.model.UserRole
import com.shakib.paperpass.presentation.components.PinEntryDialog
import com.shakib.paperpass.presentation.theme.StatusGreen
import com.shakib.paperpass.presentation.theme.StatusRed
import com.shakib.paperpass.presentation.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintConfigScreen(
    user: User,
    printer: Printer,
    quota: Quota?,
    onBack: () -> Unit,
    onSubmitPrintJob: (
        documentName: String,
        pageCount: Int,
        copies: Int,
        isColor: Boolean,
        isDuplex: Boolean,
        enteredPin: String,
        documentUri: Uri?
    ) -> Unit
) {
    val context = LocalContext.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileSize by remember { mutableStateOf<String?>(null) }
    var documentName by remember { mutableStateOf("Invoice-5893201.pdf") }
    var pageCount by remember { mutableIntStateOf(1) }
    var copies by remember { mutableIntStateOf(1) }
    var isColor by remember { mutableStateOf(false) }
    var isDuplex by remember { mutableStateOf(false) }

    // New Orientation, Paper Size, and Quality features
    var orientation by remember { mutableStateOf("PORTRAIT") } // "PORTRAIT" or "LANDSCAPE"
    var paperSize by remember { mutableStateOf("A4") }         // "A4", "Letter", "Legal"
    var quality by remember { mutableStateOf("Standard") }      // "Draft", "Standard", "High"

    var showPinDialog by remember { mutableStateOf(false) }

    // Native System File Chooser (PDF, Images, Text)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri) ?: "document.pdf"
            documentName = name
            selectedFileSize = getFileSize(context, uri)

            val detectedPages = getPdfPageCount(context, uri)
            if (detectedPages > 0) {
                pageCount = detectedPages
            }
            Toast.makeText(context, "Loaded: $name ($pageCount pages)", Toast.LENGTH_SHORT).show()
        }
    }

    val totalPagesRequired = pageCount * copies
    val remainingAfter = quota?.let { it.remainingDaily - totalPagesRequired } ?: 999
    val isQuotaExceeded = quota != null && user.role != UserRole.ADMIN && remainingAfter < 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Print Configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Real-time Quota Impact Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isQuotaExceeded) StatusRed else StatusGreen)
                            )
                            Text(
                                text = "Total: $totalPagesRequired page(s) ($pageCount pgs × $copies copy)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (quota != null && user.role != UserRole.ADMIN) {
                            Text(
                                text = "Remaining: $remainingAfter",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isQuotaExceeded) StatusRed else TealAccent
                            )
                        } else {
                            Text(
                                text = "Admin Unlimited",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Pinned Release Button: Always accessible without scrolling
                    Button(
                        onClick = {
                            if (isQuotaExceeded) {
                                Toast.makeText(
                                    context,
                                    "Cannot release: Daily quota exceeded! Needed: $totalPagesRequired, Remaining: ${quota?.remainingDaily ?: 0}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (documentName.isBlank()) {
                                Toast.makeText(context, "Please enter or select a document first.", Toast.LENGTH_SHORT).show()
                            } else {
                                showPinDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isQuotaExceeded) StatusRed else TealAccent
                        )
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isQuotaExceeded -> "Quota Exceeded ($totalPagesRequired pages needed)"
                                user.role == UserRole.GUEST -> "Submit for Admin Approval"
                                else -> "Verify PIN & Release Print"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Selected Printer Header Banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TealAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = TealAccent, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = printer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${printer.ipAddress}:${printer.port} • Dept: ${printer.departmentRestriction}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Document Picker Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Document Selection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    if (selectedUri != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TealAccent.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(text = documentName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(
                                            text = "${selectedFileSize ?: "Document"} • $pageCount page(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        selectedUri = null
                                        selectedFileSize = null
                                        documentName = "Sample_Report.pdf"
                                        pageCount = 1
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "text/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = TealAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedUri != null) "Choose Different File" else "📁 Browse Device Storage (PDF / Image)")
                    }

                    OutlinedTextField(
                        value = documentName,
                        onValueChange = { documentName = it },
                        label = { Text("File / Job Display Name") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Document Pages Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Document Pages:", fontWeight = FontWeight.Medium)
                            if (selectedUri != null) {
                                Text("(Auto-detected from PDF)", fontSize = 11.sp, color = TealAccent)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { if (pageCount > 1) pageCount-- },
                                enabled = pageCount > 1
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "$pageCount", modifier = Modifier.padding(horizontal = 14.dp), fontWeight = FontWeight.Bold)
                            FilledTonalIconButton(onClick = { pageCount++ }) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Print Copies Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Print Copies:", fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { if (copies > 1) copies-- },
                                enabled = copies > 1
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "$copies", modifier = Modifier.padding(horizontal = 14.dp), fontWeight = FontWeight.Bold)
                            FilledTonalIconButton(onClick = { copies++ }) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Print Layout & Options Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Page Orientation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = orientation == "PORTRAIT",
                            onClick = { orientation = "PORTRAIT" },
                            label = { Text("Portrait", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = orientation == "LANDSCAPE",
                            onClick = { orientation = "LANDSCAPE" },
                            label = { Text("Landscape", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Paper Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("A4", "Letter", "Legal").forEach { size ->
                            FilterChip(
                                selected = paperSize == size,
                                onClick = { paperSize = size },
                                label = { Text(size, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Color & Sides", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isColor,
                            onClick = { isColor = false },
                            label = { Text("Mono (B&W)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isColor,
                            onClick = { isColor = true },
                            label = { Text("Color CMYK", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isDuplex,
                            onClick = { isDuplex = false },
                            label = { Text("1-Sided", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isDuplex,
                            onClick = { isDuplex = true },
                            label = { Text("2-Sided (Duplex)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Print Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Draft", "Standard", "High").forEach { q ->
                            FilterChip(
                                selected = quality == q,
                                onClick = { quality = q },
                                label = { Text(q, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showPinDialog) {
        PinEntryDialog(
            onDismiss = { showPinDialog = false },
            onPinSubmitted = { pin ->
                showPinDialog = false
                val jobDisplay = if (!documentName.contains("[")) {
                    val base = documentName.substringBeforeLast(".")
                    val ext = documentName.substringAfterLast(".", "pdf")
                    "$base [${orientation.lowercase().replaceFirstChar { it.uppercase() }}, $paperSize].$ext"
                } else {
                    documentName
                }
                onSubmitPrintJob(jobDisplay, pageCount, copies, isColor, isDuplex, pin, selectedUri)
            }
        )
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name ?: uri.lastPathSegment
}

private fun getFileSize(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                val sizeBytes = cursor.getLong(sizeIndex)
                return if (sizeBytes >= 1024 * 1024) {
                    "%.1f MB".format(sizeBytes / (1024f * 1024f))
                } else {
                    "${(sizeBytes / 1024).coerceAtLeast(1)} KB"
                }
            }
        }
    }
    return null
}

private fun getPdfPageCount(context: Context, uri: Uri): Int {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                renderer.pageCount
            }
        } ?: 1
    } catch (e: Exception) {
        1
    }
}
