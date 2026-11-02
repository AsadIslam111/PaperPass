package com.shakib.paperpass.domain.model

enum class PrintJobStatus {
    PENDING,     // Awaiting admin approval (for guests) or queue dispatch
    APPROVED,    // Approved by admin
    PRINTING,    // Currently streaming payload via IPP
    COMPLETED,   // Successfully printed
    REJECTED,    // Declined by admin
    FAILED       // Socket / Network error
}

data class PrintJob(
    val id: Long = 0,
    val userId: Long,
    val userName: String = "",
    val printerId: Long,
    val printerName: String = "",
    val documentName: String,
    val pageCount: Int,
    val copies: Int = 1,
    val isColor: Boolean = false,
    val isDuplex: Boolean = false,
    val status: PrintJobStatus = PrintJobStatus.PENDING,
    val errorMessage: String? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

