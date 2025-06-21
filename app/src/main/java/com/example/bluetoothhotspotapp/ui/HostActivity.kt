package com.example.bluetoothhotspotapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.example.bluetoothhotspotapp.HostService
import com.example.bluetoothhotspotapp.databinding.ActivityHostBinding
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothhotspotapp.BaseActivity
import com.example.bluetoothhotspotapp.data.model.SearchHistory
import com.example.bluetoothhotspotapp.data.model.SearchResult
import com.example.bluetoothhotspotapp.notification.AppNotificationManager
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HostActivity : BaseActivity() {

    private lateinit var binding: ActivityHostBinding
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter
    private lateinit var currentResultsAdapter: HostSearchResultsAdapter
    private val searchHistoryList = mutableListOf<SearchHistory>()

    private var currentClientName: String? = null
    private var currentSearchQuery: String? = null
    private var lastResultsCount: Int = 0

    // Variables para logs técnicos detallados
    private val technicalLogs = mutableListOf<String>()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAB_MONITOR = 0
        private const val TAB_HISTORY = 1
        private const val TAB_LOGS = 2
    }

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                HostService.ACTION_LOG -> {
                    intent.getStringExtra(HostService.EXTRA_LOG_MESSAGE)?.let { message ->
                        addTechnicalLog(message)
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
                HostService.ACTION_SEARCH_RESULTS_DATA -> {
                    val resultsJson = intent.getStringExtra(HostService.EXTRA_RESULTS_JSON) ?: ""
                    onSearchResultsData(resultsJson)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupRecyclerViews()
        setupClickListeners()

        // NUEVO: Verificar y solicitar permisos de notificación
        checkAndRequestNotificationPermissions()

        // Pedir permisos de Bluetooth al iniciar
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            PermissionHelper.requestBluetoothPermissions(this)
        }

        // Mostrar la primera pestaña por defecto
        showTab(TAB_MONITOR)
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("📱 Monitor"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("📋 Historial"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🔧 Logs"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    TAB_MONITOR -> showTab(TAB_MONITOR)
                    TAB_HISTORY -> showTab(TAB_HISTORY)
                    TAB_LOGS -> showTab(TAB_LOGS)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showTab(tabIndex: Int) {
        // Ocultar todas las secciones
        binding.layoutMonitor.visibility = View.GONE
        binding.layoutHistory.visibility = View.GONE
        binding.layoutLogs.visibility = View.GONE

        // Mostrar la sección seleccionada
        when (tabIndex) {
            TAB_MONITOR -> binding.layoutMonitor.visibility = View.VISIBLE
            TAB_HISTORY -> binding.layoutHistory.visibility = View.VISIBLE
            TAB_LOGS -> binding.layoutLogs.visibility = View.VISIBLE
        }
    }

    // NUEVO: Función para verificar y solicitar permisos de notificación
    private fun checkAndRequestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Solicitar el permiso
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
                addTechnicalLog("Solicitando permisos de notificación...")
            } else {
                addTechnicalLog("Permisos de notificación concedidos")
            }
        } else {
            addTechnicalLog("Permisos de notificación no requeridos en esta versión de Android")
        }
    }

    // NUEVO: Manejar la respuesta de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addTechnicalLog("✅ Permisos de notificación concedidos")
                    Toast.makeText(this, "Notificaciones habilitadas correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    addTechnicalLog("❌ Permisos de notificación denegados - Las notificaciones no funcionarán")
                    Toast.makeText(this, "Las notificaciones están deshabilitadas", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Configurar adaptador para historial de búsquedas
        searchHistoryAdapter = SearchHistoryAdapter()
        binding.recyclerViewSearchHistory.apply {
            adapter = searchHistoryAdapter
            layoutManager = LinearLayoutManager(this@HostActivity)
        }

        // Configurar adaptador para resultados actuales
        currentResultsAdapter = HostSearchResultsAdapter()
        binding.recyclerViewCurrentResults.apply {
            adapter = currentResultsAdapter
            layoutManager = LinearLayoutManager(this@HostActivity)
        }
    }

    private fun setupClickListeners() {
        binding.buttonStartHost.setOnClickListener {
            // Verificar permisos antes de iniciar el servicio
            if (!hasNotificationPermission()) {
                Toast.makeText(this, "Recomendamos habilitar las notificaciones para mejor experiencia", Toast.LENGTH_LONG).show()
            }

            val serviceIntent = Intent(this, HostService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            addTechnicalLog("🚀 Iniciando servicio Host...")
            addTechnicalLog("📡 Configurando servidor Bluetooth...")
            addTechnicalLog("🔧 UUID: 00001101-0000-1000-8000-00805F9B34FB")
            addTechnicalLog("📊 Servicio en modo foreground")
        }

        binding.buttonStopHost.setOnClickListener {
            val serviceIntent = Intent(this, HostService::class.java)
            stopService(serviceIntent)
            resetClientMonitor()
            addTechnicalLog("🛑 Deteniendo servicio Host...")
            addTechnicalLog("🔌 Cerrando sockets de conexión")
            addTechnicalLog("🧹 Limpiando recursos")
        }

        // Botón para limpiar logs
        binding.buttonClearLogs.setOnClickListener {
            technicalLogs.clear()
            updateLogsDisplay()
            addTechnicalLog("🗑️ Logs limpiados por el usuario")
        }
    }

    // NUEVO: Función para verificar permisos de notificación
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores no se necesita permiso
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
        binding.progressBarSearch.visibility = View.VISIBLE

        // Ocultar resultados anteriores mientras se procesan los nuevos
        binding.layoutCurrentResults.visibility = View.GONE

        // Logs técnicos detallados
        addTechnicalLog("📥 NUEVA BÚSQUEDA RECIBIDA")
        addTechnicalLog("👤 Cliente: $clientName")
        addTechnicalLog("🔍 Query: \"$query\"")
        addTechnicalLog("📏 Tamaño query: ${query.length} caracteres")
        addTechnicalLog("🕒 Timestamp: ${getCurrentTimestamp()}")
        addTechnicalLog("⚡ Iniciando procesamiento...")
    }

    private fun onSearchResults(resultsCount: Int) {
        lastResultsCount = resultsCount

        // Actualizar contador de resultados
        binding.textViewResultsCount.text = resultsCount.toString()
        binding.progressBarSearch.visibility = View.GONE

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

                // Limitar el historial a las últimas 100 búsquedas
                if (searchHistoryList.size > 100) {
                    searchHistoryList.removeAt(searchHistoryList.size - 1)
                }

                searchHistoryAdapter.submitList(searchHistoryList.toList())

                // Logs técnicos del procesamiento
                addTechnicalLog("✅ PROCESAMIENTO COMPLETADO")
                addTechnicalLog("📊 Resultados encontrados: $resultsCount")
                addTechnicalLog("💾 Agregado al historial (${searchHistoryList.size} total)")
            }
        }
    }

    private fun onSearchResultsData(resultsJson: String) {
        if (resultsJson.isNotEmpty()) {
            try {
                val gson = Gson()
                val listType = object : TypeToken<List<SearchResult>>() {}.type
                val results: List<SearchResult> = gson.fromJson(resultsJson, listType)

                // Mostrar los resultados en el RecyclerView
                currentResultsAdapter.submitList(results)

                // Mostrar la sección de resultados si hay resultados
                if (results.isNotEmpty()) {
                    binding.layoutCurrentResults.visibility = View.VISIBLE
                } else {
                    binding.layoutCurrentResults.visibility = View.GONE
                }

                // Logs técnicos detallados del envío
                val jsonSize = resultsJson.toByteArray().size
                addTechnicalLog("📤 ENVIANDO RESPUESTA AL CLIENTE")
                addTechnicalLog("📦 Tamaño JSON: $jsonSize bytes")
                addTechnicalLog("🔢 Número de resultados: ${results.size}")
                addTechnicalLog("📋 Formato: JSON UTF-8")

                if (results.isNotEmpty()) {
                    addTechnicalLog("🔗 Primer resultado: \"${results[0].title.take(50)}...\"")
                }

                addTechnicalLog("✈️ Transmisión completada")
                addTechnicalLog("⏰ Tiempo total: ${getCurrentTimestamp()}")
                addTechnicalLog("─".repeat(50))

            } catch (e: Exception) {
                // En caso de error al parsear el JSON, ocultar la sección
                binding.layoutCurrentResults.visibility = View.GONE
                addTechnicalLog("❌ ERROR al procesar resultados: ${e.message}")
            }
        } else {
            binding.layoutCurrentResults.visibility = View.GONE
            addTechnicalLog("⚠️ Respuesta vacía recibida")
        }
    }

    private fun resetClientMonitor() {
        currentClientName = null
        currentSearchQuery = null
        lastResultsCount = 0

        binding.textViewClientStatus.text = "Sin cliente conectado"
        binding.textViewCurrentSearch.visibility = View.GONE
        binding.layoutSearchStats.visibility = View.GONE
        binding.layoutCurrentResults.visibility = View.GONE
        binding.progressBarSearch.visibility = View.GONE

        // Limpiar resultados actuales
        currentResultsAdapter.submitList(emptyList())
    }

    override fun onResume() {
        super.onResume()
        // Registrar el receptor para escuchar los eventos del servicio
        val filter = IntentFilter().apply {
            addAction(HostService.ACTION_LOG)
            addAction(HostService.ACTION_CLIENT_SEARCH)
            addAction(HostService.ACTION_SEARCH_RESULTS)
            addAction(HostService.ACTION_SEARCH_RESULTS_DATA)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // Quitar el registro para evitar fugas de memoria
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver)
    }

    // Función mejorada para logs técnicos
    private fun addTechnicalLog(message: String) {
        val timestamp = getCurrentTimestamp()
        val formattedLog = "$timestamp - $message"

        // Agregar al principio de la lista para mostrar los más recientes arriba
        technicalLogs.add(0, formattedLog)

        // Limitar a los últimos 500 logs para evitar problemas de memoria
        if (technicalLogs.size > 500) {
            technicalLogs.removeAt(technicalLogs.size - 1)
        }

        updateLogsDisplay()
    }

    private fun updateLogsDisplay() {
        val logsText = technicalLogs.joinToString("\n")
        binding.textViewLogs.text = logsText

        // Auto-scroll al top para ver los logs más recientes
        binding.scrollViewLogs.post {
            binding.scrollViewLogs.scrollTo(0, 0)
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }
}