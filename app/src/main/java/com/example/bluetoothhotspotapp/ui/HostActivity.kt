package com.example.bluetoothhotspotapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothhotspotapp.HostService
import com.example.bluetoothhotspotapp.databinding.ActivityHostBinding
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.bluetoothhotspotapp.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class HostActivity : BaseActivity() {

    private lateinit var binding: ActivityHostBinding
    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(HostService.EXTRA_LOG_MESSAGE)?.let { message ->
                addLogMessage(message)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Pedir permisos al iniciar
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            PermissionHelper.requestBluetoothPermissions(this)
        }
        binding.buttonStartHost.setOnClickListener {
            val serviceIntent = Intent(this, HostService::class.java)
            startForegroundService(serviceIntent) // Usar esto para servicios en primer plano
        }

        binding.buttonStopHost.setOnClickListener {
            val serviceIntent = Intent(this, HostService::class.java)
            stopService(serviceIntent)
        }
    }
    override fun onResume() {
        super.onResume()
        // Registrar el receptor para escuchar los logs del servicio
        val filter = IntentFilter(HostService.ACTION_LOG)
        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // Quitar el registro para evitar fugas de memoria
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver)
    }
    private fun addLogMessage(message: String) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentLog = binding.textViewLogs.text.toString()
        binding.textViewLogs.text = "$currentLog\n$currentTime - $message"

        // Hacemos que el ScrollView se desplace hasta el fondo para ver el último log
        binding.scrollViewLogs.post {
            binding.scrollViewLogs.fullScroll(View.FOCUS_DOWN)
        }
    }
}