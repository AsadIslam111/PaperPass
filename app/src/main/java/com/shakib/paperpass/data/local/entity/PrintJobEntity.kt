package com.shakib.paperpass.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shakib.paperpass.domain.model.PrintJob
import com.shakib.paperpass.domain.model.PrintJobStatus

@Entity(
    tableName = "print_jobs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PrinterEntity::class,
            parentColumns = ["id"],
            childColumns = ["printer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["printer_id"])
    ]
)
data class PrintJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "printer_id")
    val printerId: Long,

    @ColumnInfo(name = "user_name")
    val userName: String = "",

    @ColumnInfo(name = "printer_name")
    val printerName: String = "",

    @ColumnInfo(name = "document_name")
    val documentName: String,

    @ColumnInfo(name = "page_count")
    val pageCount: Int,

    @ColumnInfo(name = "copies")
    val copies: Int = 1,

    @ColumnInfo(name = "is_color")
    val isColor: Boolean = false,

    @ColumnInfo(name = "is_duplex")
    val isDuplex: Boolean = false,

    @ColumnInfo(name = "status")
    val status: String = "PENDING",

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "submitted_at")
    val submittedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
) {
    fun toDomain(): PrintJob = PrintJob(
        id = id,
        userId = userId,
        userName = userName,
        printerId = printerId,
        printerName = printerName,
        documentName = documentName,
        pageCount = pageCount,
        copies = copies,
        isColor = isColor,
        isDuplex = isDuplex,
        status = runCatching { PrintJobStatus.valueOf(status) }.getOrDefault(PrintJobStatus.PENDING),
        errorMessage = errorMessage,
        submittedAt = submittedAt,
        completedAt = completedAt
    )
}

