package com.example.resqbeacon.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class IncidentLog(
    val latitude: Double,
    val longitude: Double,
    val alertType: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface EmergencyApiService {
    @POST("/post")
    suspend fun logIncident(@Body log: IncidentLog): Any
}

object RetrofitClient {
    private const val BASE_URL = "https://httpbin.org/"

    val api: EmergencyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EmergencyApiService::class.java)
    }
}