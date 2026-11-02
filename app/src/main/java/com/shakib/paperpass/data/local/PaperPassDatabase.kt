package com.shakib.paperpass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shakib.paperpass.core.security.SecurityUtils
import com.shakib.paperpass.data.local.dao.PrintJobDao
import com.shakib.paperpass.data.local.dao.PrinterDao
import com.shakib.paperpass.data.local.dao.QuotaDao
import com.shakib.paperpass.data.local.dao.UserDao
import com.shakib.paperpass.data.local.entity.PrintJobEntity
import com.shakib.paperpass.data.local.entity.PrinterEntity
import com.shakib.paperpass.data.local.entity.QuotaEntity
import com.shakib.paperpass.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        PrinterEntity::class,
        QuotaEntity::class,
        PrintJobEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PaperPassDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun printerDao(): PrinterDao
    abstract fun quotaDao(): QuotaDao
    abstract fun printJobDao(): PrintJobDao

    companion object {
        const val DATABASE_NAME = "paperpass_secure.db"

        @Volatile
        private var INSTANCE: PaperPassDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): PaperPassDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaperPassDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope.launch(Dispatchers.IO) {
                                // Wait briefly so INSTANCE is assigned after .build() returns
                                delay(200)
                                INSTANCE?.let { seedInitialData(it) }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: PaperPassDatabase) {
            val userDao = database.userDao()
            val quotaDao = database.quotaDao()
            val printerDao = database.printerDao()

            // 1. Seed Admin User
            val adminId = userDao.insertUser(
                UserEntity(
                    employeeId = "ADMIN01",
                    fullName = "System IT Administrator",
                    email = "admin@corp.local",
                    passwordHash = SecurityUtils.hashPassword("admin123"),
                    securityPin = "1234",
                    role = "ADMIN",
                    department = "IT"
                )
            )
            quotaDao.insertOrUpdateQuota(
                QuotaEntity(
                    userId = adminId,
                    dailyLimit = 9999,
                    monthlyLimit = 99999
                )
            )

            // 2. Seed Employee User
            val empId = userDao.insertUser(
                UserEntity(
                    employeeId = "EMP101",
                    fullName = "Shakib (Network Ops)",
                    email = "shakib@corp.local",
                    passwordHash = SecurityUtils.hashPassword("pass123"),
                    securityPin = "4321",
                    role = "USER",
                    department = "ENGINEERING"
                )
            )
            quotaDao.insertOrUpdateQuota(
                QuotaEntity(
                    userId = empId,
                    dailyLimit = 50,
                    monthlyLimit = 500,
                    pagesUsedToday = 12,
                    pagesUsedMonth = 86
                )
            )

            // 3. Seed Guest User
            val guestId = userDao.insertUser(
                UserEntity(
                    employeeId = "GUEST01",
                    fullName = "Visiting Client (Guest)",
                    email = "guest@external.com",
                    passwordHash = SecurityUtils.hashPassword("guest123"),
                    securityPin = "0000",
                    role = "GUEST",
                    department = "VISITOR"
                )
            )
            quotaDao.insertOrUpdateQuota(
                QuotaEntity(
                    userId = guestId,
                    dailyLimit = 10,
                    monthlyLimit = 20,
                    pagesUsedToday = 2,
                    pagesUsedMonth = 2
                )
            )

            // 4. Seed Default Corporate Printers
            printerDao.insertOrUpdatePrinter(
                PrinterEntity(
                    name = "HP LaserJet Enterprise M608",
                    ipAddress = "192.168.1.100",
                    port = 631,
                    macAddress = "00:1E:C9:4A:88:12",
                    model = "HP LaserJet M608dn",
                    departmentRestriction = "ALL",
                    status = "ONLINE",
                    blackTonerPct = 84,
                    colorTonerPct = 0
                )
            )

            printerDao.insertOrUpdatePrinter(
                PrinterEntity(
                    name = "Canon imageRUNNER ADVANCE DX",
                    ipAddress = "192.168.1.105",
                    port = 631,
                    macAddress = "70:85:C2:5B:33:91",
                    model = "Canon C3530i",
                    departmentRestriction = "ENGINEERING",
                    status = "ONLINE",
                    blackTonerPct = 62,
                    colorTonerPct = 78
                )
            )

            printerDao.insertOrUpdatePrinter(
                PrinterEntity(
                    name = "Epson WorkForce Pro (HR)",
                    ipAddress = "192.168.1.110",
                    port = 631,
                    macAddress = "AC:18:26:FA:9C:04",
                    model = "Epson WF-C579R",
                    departmentRestriction = "HR",
                    status = "ONLINE",
                    blackTonerPct = 34,
                    colorTonerPct = 45
                )
            )
        }
    }
}
