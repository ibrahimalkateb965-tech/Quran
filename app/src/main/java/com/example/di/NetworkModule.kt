package com.example.di

import com.example.data.network.QuranApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val retryInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        var response: okhttp3.Response? = null
        var exception: java.io.IOException? = null
        var tryCount = 0
        val maxLimit = 3

        while (tryCount < maxLimit) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful || response.code < 500) {
                    return@Interceptor response
                }
                response.close()
                exception = java.io.IOException("Server error: ${response.code}")
            } catch (e: java.io.IOException) {
                exception = e
            }
            
            tryCount++
            android.util.Log.w("NetworkModule", "Network failure. Retrying... (Attempt $tryCount/$maxLimit)")
            if (tryCount < maxLimit) {
                Thread.sleep((1000 * tryCount).toLong())
            }
        }
        throw exception ?: java.io.IOException("Max retries reached")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(retryInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(com.example.BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val quranApiService: QuranApiService by lazy {
        retrofit.create(QuranApiService::class.java)
    }
}
