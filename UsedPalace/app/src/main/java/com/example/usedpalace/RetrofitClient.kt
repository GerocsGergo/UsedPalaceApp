package com.example.usedpalace

import android.content.Context
import android.content.SharedPreferences
import network.ApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    //private const val BASE_URL = "http://10.0.2.2:3000/"
    //84.225.136.123
    private const val BASE_URL = "http://10.224.83.75:3000/"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    }

    private val authInterceptor = Interceptor { chain ->
        val token = prefs.getString("token", null)
        val requestBuilder = chain.request().newBuilder()

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val retrofit: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

object RetrofitClientNoAuth {
    //private const val BASE_URL = "http://10.0.2.2:3000/"

    private const val BASE_URL = "http://10.224.83.75:3000/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}