package com.example.bluetoothhotspotapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.bluetoothhotspotapp.di.Injector
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// Necesitamos este permiso aquí, lo pediremos en la Activity
@SuppressLint("MissingPermission")
class HostService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val searchProcessor = Injector.provideSearchProcessor()
    private var serverThread: BluetoothServerThread? = null
    private val lock = Any() // Un objeto para sincronización

    companion object {
        const val CHANNEL_ID = "HostServiceChannel"
        const val ACTION_LOG = "com.example.bluetoothhotspotapp.HOST_LOG"
        const val EXTRA_LOG_MESSAGE = "extra_log_message"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Servidor Host esperando...")
        startForeground(1, notification)

        // Usamos 'synchronized' para evitar que se creen dos hilos a la vez
        synchronized(lock) {
            if (serverThread == null) {
                serverThread = BluetoothServerThread()
                serverThread?.start()
                logToActivity("Servidor Bluetooth iniciado. Esperando conexiones.")
            } else {
                logToActivity("El servidor ya está en ejecución.")
            }
        }

        return START_NOT_STICKY
    }


    private fun logToActivity(message: String) {
        // Usamos LocalBroadcastManager para una comunicación segura y eficiente con la Activity
        val intent = Intent(ACTION_LOG).apply {
            putExtra(EXTRA_LOG_MESSAGE, message)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d("HostService", message)
    }
    override fun onDestroy() {
        super.onDestroy()
        synchronized(lock) {
            serverThread?.cancel()
            serverThread = null
        }
        serviceJob.cancel()
        logToActivity("Servidor detenido.")
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Host Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Servidor Bluetooth Host")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con tu ícono
            .build()
    }
    // --- HILO PARA EL SERVIDOR BLUETOOTH ---
    private inner class BluetoothServerThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            // Creamos un "puerto" Bluetooth usando el nombre y UUID que definimos
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(Constants.SERVICE_NAME, Constants.BLUETOOTH_UUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    // accept() es una llamada bloqueante. Se detiene aquí hasta que un cliente se conecta.
                    serverSocket?.accept()
                } catch (e: IOException) {
                    logToActivity("Error al aceptar conexión: ${e.message}")
                    shouldLoop = false
                    null
                }
                socket?.also {
                    logToActivity("Cliente conectado: ${it.remoteDevice.name}")
                    manageClientSocket(it)
                    // Para este ejemplo, cerramos el socket del servidor después de una conexión.
                    // Si quisieras múltiples clientes simultáneos, la lógica sería más compleja.
                    // serverSocket?.close()
                    // shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                logToActivity("No se pudo cerrar el socket del servidor: ${e.message}")
            }
        }
    }

    private fun manageClientSocket(socket: BluetoothSocket) {
        serviceScope.launch {
            try {
                val inputStream: InputStream = socket.inputStream
                val outputStream: OutputStream = socket.outputStream
                val buffer = ByteArray(1024)
                var numBytes: Int

                while (isActive) {
                    numBytes = inputStream.read(buffer)
                    if (numBytes == -1) break

                    val receivedMessage = String(buffer, 0, numBytes)

                    // Si el mensaje es un ping, lo ignoramos y seguimos escuchando
                    if (receivedMessage == "ping_keep_alive") {
                        logToActivity("Ping recibido.")
                        continue
                    }

                    logToActivity("Petición recibida: '$receivedMessage'")

                    val jsonResponse = searchProcessor.processSearchQuery(receivedMessage)
                    val responseBytes = jsonResponse.toByteArray()

                    // --- LÓGICA DE ENVÍO MEJORADA ---
                    // 1. Convertir el tamaño del JSON a bytes
                    val size = responseBytes.size
                    val sizeBytes = size.toString().toByteArray()

                    // 2. Enviar el tamaño, seguido de un delimitador (ej. un salto de línea)
                    outputStream.write(sizeBytes)
                    outputStream.write('\n'.code) // Delimitador

                    // 3. Enviar el JSON
                    outputStream.write(responseBytes)
                    outputStream.flush()
                    logToActivity("Respuesta enviada ($size bytes).")
                }
            } catch (e: IOException) {
                logToActivity("Conexión perdida: ${e.message}")
            } finally {
                // Este bloque se ejecutará siempre, incluso si hay un error,
                // asegurando que el socket se cierre.
                try {
                    socket.close()
                    logToActivity("Socket del cliente cerrado.")
                } catch (e: IOException) {
                    logToActivity("Error al cerrar el socket del cliente.")
                }
            }
        }

    }
}
