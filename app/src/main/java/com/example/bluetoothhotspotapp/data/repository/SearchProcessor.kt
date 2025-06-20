package com.example.bluetoothhotspotapp.data.repository

import android.util.Log
import com.example.bluetoothhotspotapp.data.model.SearchResult
import com.example.bluetoothhotspotapp.data.network.HtmlParser
import com.example.bluetoothhotspotapp.data.network.SearchApiService
import com.example.bluetoothhotspotapp.util.JsonSerializer

class SearchProcessor(
    private val searchService: SearchApiService,
    private val htmlParser: HtmlParser,
    private val jsonSerializer: JsonSerializer
) {
    suspend fun processSearchQuery(query: String): String {
        return try {
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

            // VOLVEMOS A LA LLAMADA SIMPLE
            val html = searchService.search(query, userAgent)

            val searchResults = htmlParser.parseResults(html)
            jsonSerializer.toJson(searchResults)
        } catch (e: Exception) {
            Log.e("SearchProcessor", "Error al procesar la búsqueda", e)
            jsonSerializer.toJson(emptyList<SearchResult>())
        }
    }
}