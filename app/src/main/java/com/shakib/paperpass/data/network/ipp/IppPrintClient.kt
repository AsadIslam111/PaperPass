package com.shakib.paperpass.data.network.ipp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class IppPrintClient {
    private val TAG = "IppPrintClient"

    sealed class PrintResult {
        data class Success(val message: String) : PrintResult()
        data class Failure(val error: String) : PrintResult()
    }

    /**
     * Send print job via standard IPP over HTTP POST (Port 631).
     */
    suspend fun sendIppJob(
        printerIp: String,
        port: Int = 631,
        jobName: String,
        userName: String,
        documentStream: InputStream,
        documentMimeType: String = "application/pdf"
    ): PrintResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val printerUrl = "http://$printerIp:$port/ipp/print"
            val url = URL(printerUrl)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 5000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/ipp")
                setRequestProperty("User-Agent", "PaperPass-Android-Client/1.0")
            }

            // Build binary IPP frame header
            val ippHeader = buildIppHeader(printerUrl, jobName, userName, documentMimeType)

            connection.outputStream.use { output ->
                output.write(ippHeader)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (documentStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "IPP Server HTTP response code: $responseCode")

            if (responseCode in 200..299) {
                PrintResult.Success("Job successfully spooled to printer $printerIp")
            } else {
                PrintResult.Failure("Printer responded with HTTP $responseCode: ${connection.responseMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during IPP transmission", e)
            PrintResult.Failure("Network transmission failed: ${e.localizedMessage ?: "Unknown socket error"}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fallback: Send raw bytes directly via TCP socket (JetDirect / RAW port 9100).
     */
    suspend fun sendRawSocketJob(
        printerIp: String,
        port: Int = 9100,
        documentStream: InputStream
    ): PrintResult = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(printerIp, port), 5000)
                socket.soTimeout = 15000
                val outputStream: OutputStream = socket.getOutputStream()

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (documentStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
            PrintResult.Success("Raw job dispatched to port $port on $printerIp")
        } catch (e: Exception) {
            Log.e(TAG, "Error in raw socket print", e)
            PrintResult.Failure("Socket error: ${e.localizedMessage ?: "Connection refused"}")
        }
    }

    /**
     * Construct RFC 8011 binary IPP header (Print-Job operation).
     */
    private fun buildIppHeader(
        printerUri: String,
        jobName: String,
        userName: String,
        mimeType: String
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Version: IPP 2.0 (0x02, 0x00)
        dos.writeByte(0x02)
        dos.writeByte(0x00)

        // Operation-ID: Print-Job (0x0002)
        dos.writeShort(0x0002)

        // Request-ID: 1
        dos.writeInt(1)

        // Operation Attributes Tag (0x01)
        dos.writeByte(0x01)

        // 1. attributes-charset = utf-8
        writeAttribute(dos, 0x47, "attributes-charset", "utf-8")

        // 2. attributes-natural-language = en-us
        writeAttribute(dos, 0x48, "attributes-natural-language", "en-us")

        // 3. printer-uri
        writeAttribute(dos, 0x45, "printer-uri", printerUri)

        // 4. requesting-user-name
        writeAttribute(dos, 0x42, "requesting-user-name", userName)

        // 5. job-name
        writeAttribute(dos, 0x42, "job-name", jobName)

        // 6. document-format
        writeAttribute(dos, 0x49, "document-format", mimeType)

        // End of Attributes Tag (0x03)
        dos.writeByte(0x03)
        dos.flush()

        return baos.toByteArray()
    }

    private fun writeAttribute(dos: DataOutputStream, tag: Int, name: String, value: String) {
        dos.writeByte(tag)
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        dos.writeShort(nameBytes.size)
        dos.write(nameBytes)

        val valBytes = value.toByteArray(Charsets.UTF_8)
        dos.writeShort(valBytes.size)
        dos.write(valBytes)
    }
}

