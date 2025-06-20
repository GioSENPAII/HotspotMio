package com.example.bluetoothhotspotapp.data.model

import java.text.SimpleDateFormat
import java.util.*

data class SearchHistory(
    val query: String,
    val clientName: String,
    val resultsCount: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}