package com.shakib.paperpass

import android.app.Application
import com.shakib.paperpass.data.local.PaperPassDatabase
import com.shakib.paperpass.data.network.ipp.IppPrintClient
import com.shakib.paperpass.data.network.nsd.NsdPrinterDiscovery
import com.shakib.paperpass.data.repository.AuthRepositoryImpl
import com.shakib.paperpass.data.repository.PrinterRepositoryImpl
import com.shakib.paperpass.data.repository.PrintJobRepositoryImpl
import com.shakib.paperpass.domain.repository.AuthRepository
import com.shakib.paperpass.domain.repository.PrinterRepository
import com.shakib.paperpass.domain.repository.PrintJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PaperPassApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: PaperPassDatabase
        private set

    lateinit var nsdDiscovery: NsdPrinterDiscovery
        private set

    lateinit var ippClient: IppPrintClient
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var printerRepository: PrinterRepository
        private set

    lateinit var printJobRepository: PrintJobRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = PaperPassDatabase.getInstance(this, applicationScope)
        nsdDiscovery = NsdPrinterDiscovery(this, applicationScope)
        ippClient = IppPrintClient()

        authRepository = AuthRepositoryImpl(database.userDao(), database.quotaDao())
        printerRepository = PrinterRepositoryImpl(database.printerDao())
        printJobRepository = PrintJobRepositoryImpl(database.printJobDao(), database.quotaDao(), ippClient)
    }
}

