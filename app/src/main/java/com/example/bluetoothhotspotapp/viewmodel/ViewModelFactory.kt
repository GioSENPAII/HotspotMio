package com.example.bluetoothhotspotapp.viewmodel


import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bluetoothhotspotapp.data.repository.BluetoothClientCommunicationManager

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientViewModel::class.java)) {
            // --- EL CAMBIO ---
            // Ya no usamos el Fake, usamos la implementación real de Bluetooth
            val bluetoothManager = context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
            val communicationManager = BluetoothClientCommunicationManager(bluetoothManager.adapter)
            return ClientViewModel(communicationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}