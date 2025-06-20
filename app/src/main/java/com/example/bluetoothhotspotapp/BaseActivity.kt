package com.example.bluetoothhotspotapp


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var themeHelper: ThemeHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializar y aplicar tema antes de super.onCreate()
        themeHelper = ThemeHelper(this)
        themeHelper.applyTheme(this)

        // Ahora llamamos a super.onCreate() con el tema ya aplicado
        super.onCreate(savedInstanceState)
    }
}
