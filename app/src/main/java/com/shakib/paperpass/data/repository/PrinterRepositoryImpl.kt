package com.shakib.paperpass.data.repository

import com.shakib.paperpass.data.local.dao.PrinterDao
import com.shakib.paperpass.data.local.entity.PrinterEntity
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.domain.repository.PrinterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrinterRepositoryImpl(
    private val printerDao: PrinterDao
) : PrinterRepository {

    override fun getAllPrintersFlow(): Flow<List<Printer>> {
        return printerDao.getAllPrintersFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getPrintersForDepartmentFlow(department: String): Flow<List<Printer>> {
        return printerDao.getPrintersForDepartmentFlow(department).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addPrinter(printer: Printer): Long {
        val entity = PrinterEntity(
            name = printer.name,
            ipAddress = printer.ipAddress,
            port = printer.port,
            macAddress = printer.macAddress,
            model = printer.model,
            departmentRestriction = printer.departmentRestriction,
            status = printer.status.name,
            blackTonerPct = printer.blackTonerPct,
            colorTonerPct = printer.colorTonerPct,
            isPaperTrayEmpty = printer.isPaperTrayEmpty,
            isManualEntry = printer.isManualEntry,
            lastSeenTimestamp = System.currentTimeMillis()
        )
        return printerDao.insertOrUpdatePrinter(entity)
    }

    override suspend fun updatePrinterStatus(printerId: Long, status: String) {
        printerDao.updatePrinterStatus(printerId, status)
    }

    override suspend fun deletePrinter(printer: Printer) {
        val entity = if (printer.id > 0) {
            printerDao.getPrinterById(printer.id)
        } else {
            printerDao.getPrinterByIp(printer.ipAddress)
        } ?: return
        printerDao.deletePrinter(entity)
    }
}

