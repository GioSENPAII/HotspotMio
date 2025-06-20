package com.example.bluetoothhotspotapp.util

import com.google.gson.Gson

class JsonSerializer {
    private val gson = Gson()

    fun toJson(data: Any): String {
        return gson.toJson(data)
    }
}