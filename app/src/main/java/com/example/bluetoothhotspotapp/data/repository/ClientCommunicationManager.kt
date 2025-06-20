package com.example.bluetoothhotspotapp.data.repository

import com.example.bluetoothhotspotapp.data.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import android.bluetooth.BluetoothDevice

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()

}

interface ClientCommunicationManager {
    val connectionState: StateFlow<ConnectionState>
    val searchResults: Flow<List<SearchResult>>
    fun sendQuery(query: String)
    fun connectToDevice(device: BluetoothDevice)

}
fun connectToDevice(device: BluetoothDevice) { /* No hace nada en el Fake */ }
