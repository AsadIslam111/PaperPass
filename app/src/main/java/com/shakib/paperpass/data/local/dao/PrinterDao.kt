package com.shakib.paperpass.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shakib.paperpass.data.local.entity.PrinterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterDao {
    @Query("SELECT * FROM printers ORDER BY name ASC")
    fun getAllPrintersFlow(): Flow<List<PrinterEntity>>

    @Query("SELECT * FROM printers WHERE department_restriction = 'ALL' OR department_restriction = :department ORDER BY name ASC")
    fun getPrintersForDepartmentFlow(department: String): Flow<List<PrinterEntity>>

    @Query("SELECT * FROM printers WHERE ip_address = :ipAddress LIMIT 1")
    suspend fun getPrinterByIp(ipAddress: String): PrinterEntity?

    @Query("SELECT * FROM printers WHERE id = :id LIMIT 1")
    suspend fun getPrinterById(id: Long): PrinterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrinter(printer: PrinterEntity): Long

    @Update
    suspend fun updatePrinter(printer: PrinterEntity)

    @Delete
    suspend fun deletePrinter(printer: PrinterEntity)

    @Query("UPDATE printers SET status = :status, last_seen_timestamp = :timestamp WHERE id = :id")
    suspend fun updatePrinterStatus(id: Long, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM printers WHERE status = 'ONLINE'")
    fun getOnlinePrinterCountFlow(): Flow<Int>
}

