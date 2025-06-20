package com.example.bluetoothhotspotapp.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothhotspotapp.R
import com.example.bluetoothhotspotapp.databinding.DialogPairedDevicesBinding

// Le damos un nombre más descriptivo a la función lambda para mayor claridad
typealias OnDeviceSelectedListener = (BluetoothDevice) -> Unit

@SuppressLint("MissingPermission")
class PairedDevicesDialogFragment : DialogFragment() {

    private lateinit var binding: DialogPairedDevicesBinding
    private var bluetoothAdapter: BluetoothAdapter? = null

    // Interfaz para comunicar la selección a la Activity
    interface DeviceSelectionListener {
        fun onDeviceSelected(device: BluetoothDevice)
    }

    private var listener: DeviceSelectionListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Aseguramos que la Activity que nos llama implementa la interfaz
        listener = context as? DeviceSelectionListener
        if (listener == null) {
            throw ClassCastException("$context must implement DeviceSelectionListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflamos nuestro layout personalizado
        binding = DialogPairedDevicesBinding.inflate(layoutInflater)

        val bluetoothManager = requireContext().getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setupRecyclerView()
        loadPairedDevices()

        // Creamos el diálogo usando nuestro layout
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(context)
        // El adapter llamará a nuestro listener cuando un item sea seleccionado
        binding.recyclerViewDevices.adapter = PairedDevicesAdapter { device ->
            listener?.onDeviceSelected(device)
            dismiss() // Cierra el diálogo después de la selección
        }
    }

    private fun loadPairedDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(context, "Por favor, activa el Bluetooth", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        val pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

        if (pairedDevices.isEmpty()) {
            Toast.makeText(context, "No hay dispositivos emparejados.", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        // Actualizamos el adapter con la lista de dispositivos
        (binding.recyclerViewDevices.adapter as PairedDevicesAdapter).submitList(pairedDevices)
    }
}