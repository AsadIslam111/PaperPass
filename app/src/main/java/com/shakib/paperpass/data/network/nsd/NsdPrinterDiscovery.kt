package com.shakib.paperpass.data.network.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.shakib.paperpass.domain.model.Printer
import com.shakib.paperpass.domain.model.PrinterStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class NsdPrinterDiscovery(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "NsdPrinterDiscovery"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredPrinters = MutableStateFlow<List<Printer>>(emptyList())
    val discoveredPrinters: StateFlow<List<Printer>> = _discoveredPrinters.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val activePrintersMap = java.util.concurrent.ConcurrentHashMap<String, Printer>()
    private val hiddenIps = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun hidePrinter(ipAddress: String) {
        hiddenIps.add(ipAddress)
        _discoveredPrinters.value = activePrintersMap.values.filter { it.ipAddress !in hiddenIps }
    }

    private val IPP_SERVICE_TYPE = "_ipp._tcp."
    private val PRINTER_SERVICE_TYPE = "_printer._tcp."

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /**
     * Start mDNS / DNS-SD printer discovery on the local network.
     */
    fun startDiscovery() {
        if (_isScanning.value) return

        acquireMulticastLock()
        activePrintersMap.clear()
        _discoveredPrinters.value = emptyList()
        _isScanning.value = true

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "mDNS Service discovery started for: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}, type: ${service.serviceType}")
                // Resolve service details (IP, Port, TXT attributes)
                resolveServiceSafely(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                activePrintersMap.remove(service.serviceName)
                _discoveredPrinters.value = activePrintersMap.values.toList()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: Error code $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: Error code $errorCode")
                nsdManager?.stopServiceDiscovery(this)
            }
        }

        try {
            nsdManager?.discoverServices(IPP_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            // Auto-stop scanning after 6 seconds to restore button state
            scope.launch {
                kotlinx.coroutines.delay(6000)
                if (_isScanning.value) {
                    stopDiscovery()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating discoverServices", e)
            _isScanning.value = false
            releaseMulticastLock()
        }
    }

    /**
     * Stop mDNS service discovery and release locks.
     */
    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
        }
        discoveryListener = null
        _isScanning.value = false
        releaseMulticastLock()
    }

    private fun resolveServiceSafely(serviceInfo: NsdServiceInfo) {
        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Failed to resolve service ${service.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                val host = resolvedService.host?.hostAddress ?: return
                val port = resolvedService.port

                val printer = Printer(
                    name = resolvedService.serviceName ?: "Network Printer",
                    ipAddress = host,
                    port = if (port > 0) port else 631,
                    model = resolvedService.serviceName ?: "mDNS Printer",
                    status = PrinterStatus.ONLINE,
                    lastSeenTimestamp = System.currentTimeMillis()
                )

                scope.launch {
                    activePrintersMap[resolvedService.serviceName] = printer
                    _discoveredPrinters.value = activePrintersMap.values.toList()
                }
            }
        })
    }

    /**
     * Fallback: Probe a specific IP and port via TCP socket (for cross-VLAN or manual IP setups).
     */
    suspend fun probePrinterDirect(ip: String, port: Int = 631, timeoutMs: Int = 1500): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("PaperPassMulticastLock")?.apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
            Log.d(TAG, "MulticastLock acquired.")
        } catch (e: Exception) {
            Log.e(TAG, "Could not acquire MulticastLock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(TAG, "MulticastLock released.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not release MulticastLock", e)
        }
    }
}

