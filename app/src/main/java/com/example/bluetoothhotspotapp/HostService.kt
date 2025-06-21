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
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.bluetoothhotspotapp.di.Injector
import com.example.bluetoothhotspotapp.notification.AppNotificationManager
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

    // Sistema de notificaciones
    private lateinit var notificationManager: AppNotificationManager

    companion object {
        const val CHANNEL_ID = "HostServiceChannel"
        const val ACTION_LOG = "com.example.bluetoothhotspotapp.HOST_LOG"
        const val ACTION_CLIENT_SEARCH = "com.example.bluetoothhotspotapp.CLIENT_SEARCH"
        const val ACTION_SEARCH_RESULTS = "com.example.bluetoothhotspotapp.SEARCH_RESULTS"
        const val ACTION_SEARCH_RESULTS_DATA = "com.example.bluetoothhotspotapp.SEARCH_RESULTS_DATA"
        const val EXTRA_LOG_MESSAGE = "extra_log_message"
        const val EXTRA_SEARCH_QUERY = "extra_search_query"
        const val EXTRA_RESULTS_COUNT = "extra_results_count"
        const val EXTRA_CLIENT_NAME = "extra_client_name"
        const val EXTRA_RESULTS_JSON = "extra_results_json"
    }

    override fun onCreate() {
        super.onCreate()
        // Inicializar el sistema de notificaciones ANTES de crear el canal
        notificationManager = AppNotificationManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        logToActivity("HostService creado - Sistema de notificaciones inicializado")
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

    private fun notifyClientSearch(query: String, clientName: String) {
        val intent = Intent(ACTION_CLIENT_SEARCH).apply {
            putExtra(EXTRA_SEARCH_QUERY, query)
            putExtra(EXTRA_CLIENT_NAME, clientName)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun notifySearchResults(resultsCount: Int, resultsJson: String) {
        val intent = Intent(ACTION_SEARCH_RESULTS).apply {
            putExtra(EXTRA_RESULTS_COUNT, resultsCount)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        // Enviar también los datos de los resultados en un broadcast separado
        val dataIntent = Intent(ACTION_SEARCH_RESULTS_DATA).apply {
            putExtra(EXTRA_RESULTS_JSON, resultsJson)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(dataIntent)
    }

    // Función para verificar permisos de notificación
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores no se necesita permiso
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        synchronized(lock) {
            serverThread?.cancel()
            serverThread = null
        }
        serviceJob.cancel()

        // Limpiar notificaciones al detener el servicio solo si tenemos permisos
        if (hasNotificationPermission()) {
            try {
                notificationManager.clearAllNotifications()
                logToActivity("Notificaciones limpiadas")
            } catch (e: Exception) {
                Log.e("HostService", "Error al limpiar notificaciones: ${e.message}")
            }
        }

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
        logToActivity("Canal de notificaciones creado")
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
                    val clientName = it.remoteDevice.name ?: "Cliente desconocido"
                    logToActivity("Cliente conectado: $clientName")
                    manageClientSocket(it, clientName)
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

    private fun manageClientSocket(socket: BluetoothSocket, clientName: String) {
        serviceScope.launch {
            try {
                val inputStream: InputStream = socket.inputStream
                val outputStream: OutputStream = socket.outputStream
                val buffer = ByteArray(1024)
                var numBytes: Int

                // CORREGIDO: Verificar permisos antes de mostrar notificación
                if (hasNotificationPermission()) {
                    try {
                        notificationManager.notifyClientConnected(clientName)
                        logToActivity("Notificación de cliente conectado enviada")
                    } catch (e: Exception) {
                        Log.e("HostService", "Error al enviar notificación de conexión: ${e.message}")
                        logToActivity("Error al enviar notificación de conexión: ${e.message}")
                    }
                } else {
                    logToActivity("Sin permisos para notificaciones - Cliente conectado: $clientName")
                }

                while (isActive) {
                    numBytes = inputStream.read(buffer)
                    if (numBytes == -1) break

                    val receivedMessage = String(buffer, 0, numBytes)

                    // Si el mensaje es un ping, lo ignoramos y seguimos escuchando
                    if (receivedMessage == "ping_keep_alive") {
                        logToActivity("Ping recibido de $clientName")
                        continue
                    }

                    logToActivity("Petición recibida de $clientName: '$receivedMessage'")

                    // Notificar a la Activity sobre la búsqueda del cliente
                    notifyClientSearch(receivedMessage, clientName)

                    // CORREGIDO: Verificar permisos antes de mostrar notificación
                    if (hasNotificationPermission()) {
                        try {
                            notificationManager.notifyNewSearch(clientName, receivedMessage)
                            logToActivity("Notificación de nueva búsqueda enviada")
                        } catch (e: Exception) {
                            Log.e("HostService", "Error al enviar notificación de búsqueda: ${e.message}")
                            logToActivity("Error al enviar notificación de búsqueda: ${e.message}")
                        }
                    } else {
                        logToActivity("Sin permisos para notificaciones - Nueva búsqueda: $receivedMessage")
                    }

                    val jsonResponse = searchProcessor.processSearchQuery(receivedMessage)
                    val responseBytes = jsonResponse.toByteArray()

                    // Contar resultados para mostrar en la UI
                    val resultsCount = try {
                        val gson = com.google.gson.Gson()
                        val listType = object : com.google.gson.reflect.TypeToken<List<Any>>() {}.type
                        val results: List<Any> = gson.fromJson(jsonResponse, listType)
                        results.size
                    } catch (e: Exception) {
                        0
                    }

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

                    logToActivity("Respuesta enviada a $clientName: $resultsCount resultados ($size bytes)")

                    // Notificar a la Activity sobre los resultados enviados (ahora incluye el JSON)
                    notifySearchResults(resultsCount, jsonResponse)
                }
            } catch (e: IOException) {
                logToActivity("Conexión perdida con $clientName: ${e.message}")
            } finally {
                // Este bloque se ejecutará siempre, incluso si hay un error,
                // asegurando que el socket se cierre.
                try {
                    socket.close()
                    logToActivity("Socket del cliente $clientName cerrado.")
                } catch (e: IOException) {
                    logToActivity("Error al cerrar el socket del cliente $clientName.")
                }
            }
        }
    }
}