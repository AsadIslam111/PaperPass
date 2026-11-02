package com.shakib.paperpass

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.shakib.paperpass.core.util.Resource
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.presentation.screens.admin.AdminPanelScreen
import com.shakib.paperpass.presentation.screens.auth.LoginScreen
import com.shakib.paperpass.presentation.screens.dashboard.DashboardScreen
import com.shakib.paperpass.presentation.screens.history.PrintHistoryScreen
import com.shakib.paperpass.presentation.screens.print.PrintConfigScreen
import com.shakib.paperpass.presentation.theme.PaperPassTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class AppDestination {
    LOGIN,
    DASHBOARD,
    PRINT_CONFIG,
    ADMIN,
    HISTORY
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PaperPassApp
        val authRepo = app.authRepository
        val printerRepo = app.printerRepository
        val jobRepo = app.printJobRepository
        val nsd = app.nsdDiscovery

        setContent {
            PaperPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppDestination.LOGIN) }
                    var selectedPrinter by remember { mutableStateOf<Printer?>(null) }
                    var loginError by remember { mutableStateOf<String?>(null) }
                    var isLoggingIn by remember { mutableStateOf(false) }

                    val currentUser by authRepo.currentUserFlow.collectAsState(initial = null)
                    val dbPrinters by printerRepo.getAllPrintersFlow().collectAsState(initial = emptyList())
                    val discoveredPrinters by nsd.discoveredPrinters.collectAsState()
                    val isScanning by nsd.isScanning.collectAsState()

                    // Combine static DB printers with real-time mDNS discovered printers
                    val allPrinters = remember(dbPrinters, discoveredPrinters) {
                        val map = dbPrinters.associateBy { it.ipAddress }.toMutableMap()
                        discoveredPrinters.forEach { p -> map[p.ipAddress] = p }
                        map.values.toList()
                    }

                    val userQuota by (currentUser?.let { authRepo.getQuotaFlow(it.id) }
                        ?: remember { kotlinx.coroutines.flow.flowOf(null) }).collectAsState(initial = null)

                    val pendingJobs by jobRepo.getPendingRequestsFlow().collectAsState(initial = emptyList())
                    val allJobs by jobRepo.getAllJobsFlow().collectAsState(initial = emptyList())
                    val userJobs = remember(allJobs, currentUser) {
                        currentUser?.let { u -> allJobs.filter { it.userId == u.id } } ?: emptyList()
                    }

                    when (currentScreen) {
                        AppDestination.LOGIN -> {
                            LoginScreen(
                                onLoginClick = { empId, pwd ->
                                    isLoggingIn = true
                                    loginError = null
                                    lifecycleScope.launch {
                                        when (val res = authRepo.login(empId, pwd)) {
                                            is Resource.Success -> {
                                                isLoggingIn = false
                                                currentScreen = AppDestination.DASHBOARD
                                                nsd.startDiscovery()
                                            }
                                            is Resource.Error -> {
                                                isLoggingIn = false
                                                loginError = res.message
                                            }
                                            is Resource.Loading -> {}
                                        }
                                    }
                                },
                                errorMessage = loginError,
                                isLoading = isLoggingIn
                            )
                        }

                        AppDestination.DASHBOARD -> {
                            currentUser?.let { user ->
                                DashboardScreen(
                                    user = user,
                                    quota = userQuota,
                                    printers = allPrinters,
                                    isScanning = isScanning,
                                    onStartScan = { nsd.startDiscovery() },
                                    onSelectPrinter = { printer ->
                                        selectedPrinter = printer
                                        currentScreen = AppDestination.PRINT_CONFIG
                                    },
                                    onAddManualPrinter = { name, ipInput, dept ->
                                        lifecycleScope.launch {
                                            val parts = ipInput.split(":")
                                            val cleanIp = parts[0].trim()
                                            val customPort = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 631 else 631

                                            printerRepo.addPrinter(
                                                Printer(
                                                    name = name,
                                                    ipAddress = cleanIp,
                                                    port = customPort,
                                                    departmentRestriction = dept,
                                                    isManualEntry = true
                                                )
                                            )
                                            Toast.makeText(this@MainActivity, "Printer '$name' added", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeletePrinter = { printer ->
                                        lifecycleScope.launch {
                                            printerRepo.deletePrinter(printer)
                                            nsd.hidePrinter(printer.ipAddress)
                                            Toast.makeText(this@MainActivity, "Printer '${printer.name}' removed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onNavigateToAdmin = { currentScreen = AppDestination.ADMIN },
                                    onNavigateToHistory = { currentScreen = AppDestination.HISTORY },
                                    onLogout = {
                                        lifecycleScope.launch {
                                            authRepo.logout()
                                            nsd.stopDiscovery()
                                            currentScreen = AppDestination.LOGIN
                                        }
                                    }
                                )
                            }
                        }

                        AppDestination.PRINT_CONFIG -> {
                            val user = currentUser
                            val printer = selectedPrinter
                            if (user != null && printer != null) {
                                PrintConfigScreen(
                                    user = user,
                                    printer = printer,
                                    quota = userQuota,
                                    onBack = { currentScreen = AppDestination.DASHBOARD },
                                    onSubmitPrintJob = { docName, pages, copies, isColor, isDuplex, pin, docUri ->
                                        lifecycleScope.launch {
                                            // 1. Verify PIN
                                            val isPinValid = authRepo.verifyPin(user.id, pin)
                                            if (!isPinValid) {
                                                Toast.makeText(this@MainActivity, "❌ Security PIN Incorrect!", Toast.LENGTH_LONG).show()
                                                return@launch
                                            }

                                            // 2. Open stream from selected file URI
                                            val stream = docUri?.let { uri ->
                                                try {
                                                    contentResolver.openInputStream(uri)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }

                                            // 3. Submit print job
                                            val res = jobRepo.submitJob(
                                                user = user,
                                                printer = printer,
                                                documentName = docName,
                                                pageCount = pages,
                                                copies = copies,
                                                isColor = isColor,
                                                isDuplex = isDuplex,
                                                documentStream = stream
                                            )

                                            when (res) {
                                                is Resource.Success -> {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "✅ ${res.data?.status?.name ?: "Success"}: Print Job Dispatched!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    currentScreen = AppDestination.DASHBOARD
                                                }
                                                is Resource.Error -> {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "❌ ${res.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                is Resource.Loading -> {}
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        AppDestination.ADMIN -> {
                            AdminPanelScreen(
                                pendingJobs = pendingJobs,
                                allJobs = allJobs,
                                onApproveJob = { jobId ->
                                    lifecycleScope.launch {
                                        jobRepo.approveJob(jobId)
                                        Toast.makeText(this@MainActivity, "Job Approved!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRejectJob = { jobId ->
                                    lifecycleScope.launch {
                                        jobRepo.rejectJob(jobId, "Declined by IT Administrator")
                                        Toast.makeText(this@MainActivity, "Job Rejected", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onBack = { currentScreen = AppDestination.DASHBOARD }
                            )
                        }

                        AppDestination.HISTORY -> {
                            PrintHistoryScreen(
                                userJobs = userJobs,
                                onBack = { currentScreen = AppDestination.DASHBOARD }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? PaperPassApp)?.nsdDiscovery?.stopDiscovery()
    }
}

