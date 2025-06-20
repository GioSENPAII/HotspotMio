package com.example.bluetoothhotspotapp.data.network

import android.util.Log
import com.example.bluetoothhotspotapp.data.model.SearchResult
import org.jsoup.Jsoup

class HtmlParser {
    fun parseResults(html: String): List<SearchResult> {
        Log.d("HtmlParser", "HTML recibido para parsear (versión LITE)")
        val document = Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()

        // --- VOLVEMOS A LOS SELECTORES ORIGINALES PARA LA PÁGINA LITE ---
        val resultElements = document.select("div.result, div.results_links_deep")

        for (element in resultElements) {
            val titleElement = element.selectFirst("h2.result__title > a.result__a")
            val snippetElement = element.selectFirst("a.result__snippet")
            val urlElement = element.selectFirst("a.result__url")

            if (titleElement != null && snippetElement != null && urlElement != null) {
                val title = titleElement.text()
                val snippet = snippetElement.text()
                val url = urlElement.text().trim()

                if (title.isNotEmpty() && snippet.isNotEmpty() && url.isNotEmpty()) {
                    results.add(SearchResult(title, snippet, url))
                }
            }
        }
        return results
    }
}