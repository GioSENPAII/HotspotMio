package com.example.bluetoothhotspotapp.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import com.example.bluetoothhotspotapp.data.model.SearchResult
import com.example.bluetoothhotspotapp.data.repository.BluetoothClientCommunicationManager
import com.example.bluetoothhotspotapp.data.repository.ClientCommunicationManager
import com.example.bluetoothhotspotapp.data.repository.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ClientViewModel(
    private val communicationManager: ClientCommunicationManager
) : ViewModel() {

    // SIMPLEMENTE EXPONEMOS LOS FLOWS DEL MANAGER DIRECTAMENTE
    val searchResults: Flow<List<SearchResult>> = communicationManager.searchResults
    val connectionState: StateFlow<ConnectionState> = communicationManager.connectionState

    fun connectToDevice(device: BluetoothDevice) {
        if (communicationManager is BluetoothClientCommunicationManager) {
            communicationManager.connectToDevice(device)
        }
    }

    fun onSearchClicked(query: String) {
        communicationManager.sendQuery(query)
    }
}