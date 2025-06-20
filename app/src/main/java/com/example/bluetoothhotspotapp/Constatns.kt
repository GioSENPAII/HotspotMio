package com.example.bluetoothhotspotapp

import java.util.UUID

object Constants {
    // Este UUID debe ser idéntico en la app Cliente y Host.
    // Puedes generar el tuyo propio si quieres, pero este funcionará.
    val BLUETOOTH_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    const val SERVICE_NAME = "BluetoothHotspotService"
}