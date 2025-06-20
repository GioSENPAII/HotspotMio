package com.example.bluetoothhotspotapp.viewmodel

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothhotspotapp.data.model.SearchResult
import com.example.bluetoothhotspotapp.data.repository.BluetoothClientCommunicationManager
import com.example.bluetoothhotspotapp.data.repository.ClientCommunicationManager
import com.example.bluetoothhotspotapp.data.repository.ConnectionState
import com.example.bluetoothhotspotapp.notification.AppNotificationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientViewModel(
    private val communicationManager: ClientCommunicationManager,
    private val context: Context
) : ViewModel() {

    private val notificationManager = AppNotificationManager(context)

    // SIMPLEMENTE EXPONEMOS LOS FLOWS DEL MANAGER DIRECTAMENTE
    val searchResults: Flow<List<SearchResult>> = communicationManager.searchResults
    val connectionState: StateFlow<ConnectionState> = communicationManager.connectionState

    private var lastSearchQuery: String = ""

    init {
        // Observar cambios en el estado de conexión
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        // Notificar conexión exitosa
                        notificationManager.notifyConnectionEstablished("Servidor Host")
                    }
                    is ConnectionState.Disconnected, is ConnectionState.Error -> {
                        // Limpiar notificaciones de conexión
                        notificationManager.clearConnectionNotifications()
                    }
                    else -> { /* No hacer nada para Connecting */ }
                }
            }
        }

        // Observar resultados de búsqueda
        viewModelScope.launch {
            searchResults.collect { results ->
                if (results.isNotEmpty() && lastSearchQuery.isNotEmpty()) {
                    // Notificar que se completó la búsqueda
                    notificationManager.notifySearchComplete(lastSearchQuery, results.size)
                }
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (communicationManager is BluetoothClientCommunicationManager) {
            communicationManager.connectToDevice(device)
        }
    }

    fun onSearchClicked(query: String) {
        lastSearchQuery = query
        communicationManager.sendQuery(query)
    }

    override fun onCleared() {
        super.onCleared()
        // Limpiar notificaciones al cerrar el ViewModel
        notificationManager.clearAllNotifications()
    }
}