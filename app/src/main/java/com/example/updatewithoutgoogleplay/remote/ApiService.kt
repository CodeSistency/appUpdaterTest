package com.example.updatewithoutgoogleplay.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface UpdateService {
    @GET("version-check.json")
    suspend fun getVersionCheck(): VersionCheck
}

object RetrofitClient {
    private const val BASE_URL = "https://github.com/CodeSistency/appUpdaterTest/releases/download/version-check/"
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: UpdateService = retrofit.create(UpdateService::class.java)
}