package com.shakib.paperpass.domain.model

enum class PrinterStatus {
    ONLINE,
    OFFLINE,
    BUSY,
    ERROR
}

data class Printer(
    val id: Long = 0,
    val name: String,
    val ipAddress: String,
    val port: Int = 631,
    val macAddress: String = "",
    val model: String = "Generic Network Printer",
    val departmentRestriction: String = "ALL", // "ALL", "HR", "IT", "ACCOUNTS", etc.
    val status: PrinterStatus = PrinterStatus.ONLINE,
    val blackTonerPct: Int = 100,
    val colorTonerPct: Int = 100,
    val isPaperTrayEmpty: Boolean = false,
    val isManualEntry: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

