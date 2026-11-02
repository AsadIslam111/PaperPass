package com.shakib.paperpass.domain.repository

import com.shakib.paperpass.core.util.Resource
import com.shakib.paperpass.domain.model.PrintJob
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.domain.model.Quota
import com.shakib.paperpass.domain.model.User
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface AuthRepository {
    val currentUserFlow: Flow<User?>
    suspend fun login(employeeId: String, passwordRaw: String): Resource<User>
    suspend fun logout()
    suspend fun verifyPin(userId: Long, enteredPin: String): Boolean
    fun getQuotaFlow(userId: Long): Flow<Quota?>
}

interface PrinterRepository {
    fun getAllPrintersFlow(): Flow<List<Printer>>
    fun getPrintersForDepartmentFlow(department: String): Flow<List<Printer>>
    suspend fun addPrinter(printer: Printer): Long
    suspend fun updatePrinterStatus(printerId: Long, status: String)
    suspend fun deletePrinter(printer: Printer)
}

interface PrintJobRepository {
    fun getAllJobsFlow(): Flow<List<PrintJob>>
    fun getJobsForUserFlow(userId: Long): Flow<List<PrintJob>>
    fun getPendingRequestsFlow(): Flow<List<PrintJob>>
    suspend fun submitJob(
        user: User,
        printer: Printer,
        documentName: String,
        pageCount: Int,
        copies: Int,
        isColor: Boolean,
        isDuplex: Boolean,
        documentStream: InputStream?
    ): Resource<PrintJob>
    suspend fun approveJob(jobId: Long): Resource<Unit>
    suspend fun rejectJob(jobId: Long, reason: String): Resource<Unit>
}

