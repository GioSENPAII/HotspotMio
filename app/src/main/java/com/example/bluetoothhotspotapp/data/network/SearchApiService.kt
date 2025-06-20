package com.example.bluetoothhotspotapp.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SearchApiService {
    // VOLVEMOS AL ENDPOINT /html/ PERO MANTENEMOS EL HEADER
    @GET("/html/")
    suspend fun search(
        @Query("q") query: String,
        @Header("User-Agent") userAgent: String
    ): String // Devolvemos un String simple, como al principio
}