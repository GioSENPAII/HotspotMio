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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothhotspotapp.BaseActivity
import com.example.bluetoothhotspotapp.data.model.SearchHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HostActivity : BaseActivity() {

    private lateinit var binding: ActivityHostBinding
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter
    private val searchHistoryList = mutableListOf<SearchHistory>()

    private var currentClientName: String? = null
    private var currentSearchQuery: String? = null
    private var lastResultsCount: Int = 0

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                HostService.ACTION_LOG -> {
                    intent.getStringExtra(HostService.EXTRA_LOG_MESSAGE)?.let { message ->
                        addLogMessage(message)
                    }
                }
                HostService.ACTION_CLIENT_SEARCH -> {
                    val query = intent.getStringExtra(HostService.EXTRA_SEARCH_QUERY) ?: ""
                    val clientName = intent.getStringExtra(HostService.EXTRA_CLIENT_NAME) ?: "Cliente"
                    onClientSearch(query, clientName)
                }
                HostService.ACTION_SEARCH_RESULTS -> {
                    val resultsCount = intent.getIntExtra(HostService.EXTRA_RESULTS_COUNT, 0)
                    onSearchResults(resultsCount)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()

        // Pedir permisos al iniciar
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            PermissionHelper.requestBluetoothPermissions(this)
        }
    }

    private fun setupRecyclerView() {
        searchHistoryAdapter = SearchHistoryAdapter()
        binding.recyclerViewSearchHistory.apply {
            adapter = searchHistoryAdapter
            layoutManager = LinearLayoutManager(this@HostActivity)
        }
    }

    private fun setupClickListeners() {
        binding.buttonStartHost.setOnClickListener {
            val serviceIntent = Intent(this, HostService::class.java)
            startForegroundService(serviceIntent) // Usar esto para servicios en primer plano
        }

        binding.buttonStopHost.setOnClickListener {
            val serviceIntent = Intent(this, HostService::class.java)
            stopService(serviceIntent)
            resetClientMonitor()
        }
    }

    private fun onClientSearch(query: String, clientName: String) {
        currentClientName = clientName
        currentSearchQuery = query

        // Actualizar UI del monitor del cliente
        binding.textViewClientStatus.text = "Cliente conectado: $clientName"
        binding.textViewCurrentSearch.apply {
            text = "🔍 Buscando: \"$query\""
            visibility = View.VISIBLE
        }
        binding.layoutSearchStats.visibility = View.VISIBLE
        binding.textViewResultsCount.text = "Procesando..."
    }

    private fun onSearchResults(resultsCount: Int) {
        lastResultsCount = resultsCount

        // Actualizar contador de resultados
        binding.textViewResultsCount.text = resultsCount.toString()

        // Agregar al historial si tenemos toda la información
        currentSearchQuery?.let { query ->
            currentClientName?.let { clientName ->
                val searchHistory = SearchHistory(
                    query = query,
                    clientName = clientName,
                    resultsCount = resultsCount
                )

                // Agregar al inicio de la lista para mostrar las más recientes primero
                searchHistoryList.add(0, searchHistory)

                // Limitar el historial a las últimas 50 búsquedas
                if (searchHistoryList.size > 50) {
                    searchHistoryList.removeAt(searchHistoryList.size - 1)
                }

                searchHistoryAdapter.submitList(searchHistoryList.toList())
            }
        }
    }

    private fun resetClientMonitor() {
        currentClientName = null
        currentSearchQuery = null
        lastResultsCount = 0

        binding.textViewClientStatus.text = "Sin cliente conectado"
        binding.textViewCurrentSearch.visibility = View.GONE
        binding.layoutSearchStats.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Registrar el receptor para escuchar los eventos del servicio
        val filter = IntentFilter().apply {
            addAction(HostService.ACTION_LOG)
            addAction(HostService.ACTION_CLIENT_SEARCH)
            addAction(HostService.ACTION_SEARCH_RESULTS)
        }
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