package com.shakib.paperpass.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.domain.model.PrinterStatus

@Entity(
    tableName = "printers",
    indices = [Index(value = ["ip_address"], unique = true)]
)
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "ip_address")
    val ipAddress: String,

    @ColumnInfo(name = "port")
    val port: Int = 631,

    @ColumnInfo(name = "mac_address")
    val macAddress: String = "",

    @ColumnInfo(name = "model")
    val model: String = "Generic Network Printer",

    @ColumnInfo(name = "department_restriction")
    val departmentRestriction: String = "ALL",

    @ColumnInfo(name = "status")
    val status: String = "ONLINE",

    @ColumnInfo(name = "black_toner_pct")
    val blackTonerPct: Int = 100,

    @ColumnInfo(name = "color_toner_pct")
    val colorTonerPct: Int = 100,

    @ColumnInfo(name = "is_paper_tray_empty")
    val isPaperTrayEmpty: Boolean = false,

    @ColumnInfo(name = "is_manual_entry")
    val isManualEntry: Boolean = false,

    @ColumnInfo(name = "last_seen_timestamp")
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): Printer = Printer(
        id = id,
        name = name,
        ipAddress = ipAddress,
        port = port,
        macAddress = macAddress,
        model = model,
        departmentRestriction = departmentRestriction,
        status = runCatching { PrinterStatus.valueOf(status) }.getOrDefault(PrinterStatus.ONLINE),
        blackTonerPct = blackTonerPct,
        colorTonerPct = colorTonerPct,
        isPaperTrayEmpty = isPaperTrayEmpty,
        isManualEntry = isManualEntry,
        lastSeenTimestamp = lastSeenTimestamp
    )
}

