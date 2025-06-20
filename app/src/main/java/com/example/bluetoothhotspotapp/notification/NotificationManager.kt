package com.example.bluetoothhotspotapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bluetoothhotspotapp.R
import com.example.bluetoothhotspotapp.ui.ClientActivity
import com.example.bluetoothhotspotapp.ui.HostActivity

class AppNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID_GENERAL = "general_notifications"
        const val CHANNEL_ID_BLUETOOTH = "bluetooth_notifications"
        const val CHANNEL_ID_SEARCH = "search_notifications"

        const val NOTIFICATION_ID_CONNECTION = 1001
        const val NOTIFICATION_ID_SEARCH_COMPLETE = 1002
        const val NOTIFICATION_ID_CLIENT_CONNECTED = 1003
        const val NOTIFICATION_ID_NEW_SEARCH = 1004
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "Notificaciones Generales",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones generales de la aplicación"
                },
                NotificationChannel(
                    CHANNEL_ID_BLUETOOTH,
                    "Estado Bluetooth",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones sobre conexiones Bluetooth"
                },
                NotificationChannel(
                    CHANNEL_ID_SEARCH,
                    "Búsquedas",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones sobre búsquedas realizadas"
                }
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    // Para el Cliente: Notificar cuando se conecta al Host
    fun notifyConnectionEstablished(hostName: String) {
        val intent = Intent(context, ClientActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BLUETOOTH)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentTitle("✅ Conectado a Host")
            .setContentText("Conectado exitosamente a $hostName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CONNECTION, notification)
    }

    // Para el Cliente: Notificar cuando se completa una búsqueda
    fun notifySearchComplete(query: String, resultsCount: Int) {
        val intent = Intent(context, ClientActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SEARCH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔍 Búsqueda completada")
            .setContentText("\"$query\" - $resultsCount resultados encontrados")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SEARCH_COMPLETE, notification)
    }

    // Para el Host: Notificar cuando se conecta un cliente
    fun notifyClientConnected(clientName: String) {
        val intent = Intent(context, HostActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BLUETOOTH)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentTitle("📱 Cliente conectado")
            .setContentText("$clientName se ha conectado al servidor")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CLIENT_CONNECTED, notification)
    }

    // Para el Host: Notificar cuando llega una nueva búsqueda
    fun notifyNewSearch(clientName: String, query: String) {
        val intent = Intent(context, HostActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SEARCH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔍 Nueva búsqueda")
            .setContentText("$clientName busca: \"$query\"")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NEW_SEARCH, notification)
    }

    // Limpiar notificaciones específicas
    fun clearConnectionNotifications() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CONNECTION)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CLIENT_CONNECTED)
    }

    fun clearSearchNotifications() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SEARCH_COMPLETE)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_NEW_SEARCH)
    }

    // Limpiar todas las notificaciones
    fun clearAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}