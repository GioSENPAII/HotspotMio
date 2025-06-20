package com.example.bluetoothhotspotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothhotspotapp.databinding.ActivityMainBinding
import com.example.bluetoothhotspotapp.ui.ClientActivity
import com.example.bluetoothhotspotapp.ui.HostActivity

class MainActivity : BaseActivity()  {

    private lateinit var binding: ActivityMainBinding
    private var pendingThemeChange = false
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el listener para el botón del Cliente
        binding.buttonOpenClient.setOnClickListener {
            // Crear un Intent para lanzar ClientActivity
            val intent = Intent(this, ClientActivity::class.java)
            startActivity(intent)
        }

        // Habilitar y configurar el botón del Host
        binding.buttonOpenHost.isEnabled = true // Habilitarlo
        binding.buttonOpenHost.setOnClickListener {
            val intent = Intent(this, HostActivity::class.java)
            startActivity(intent)
        }
        val btnToggleTheme = findViewById<Button>(R.id.btnToggleTheme)
        updateThemeButtonText(btnToggleTheme)

        btnToggleTheme.setOnClickListener {

            // Cambiar tema
            val newTheme = themeHelper.toggleTheme(this)
            pendingThemeChange = true
            updateThemeButtonText(btnToggleTheme)
            recreate()

        }
        // El botón del host está deshabilitado en el XML, así que no necesitamos un listener por ahora.
    }
    private fun updateThemeButtonText(button: Button) {
        val currentTheme = themeHelper.getCurrentTheme()
        val buttonText = if (currentTheme == "ipn") {
            "tema ESCOM"
        } else {
            "tema IPN"
        }
        button.text = buttonText
    }
}