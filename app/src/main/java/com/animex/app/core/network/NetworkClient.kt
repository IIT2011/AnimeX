package com.animex.app.core.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClient {

    private val MIRROR_DOMAINS = listOf(
        "https://anifam.net/",
        "https://anifam.buzz/",
        "https://anifam.top/",
        "https://anifam.cfd/"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val guestHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestWithHeaders = original.newBuilder()
            .header("Accept", "application/json")
            .header("X-App-Platform", "android")
            .header("X-App-Version", "20")
            .header("User-Agent", "okhttp/4.12.0")
            .build()
        chain.proceed(requestWithHeaders)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(guestHeaderInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: AnimeXApi by lazy {
        Retrofit.Builder()
            .baseUrl(MIRROR_DOMAINS.first())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AnimeXApi::class.java)
    }

    /**
     * Dedicated OkHttpClient instance for Media3 ExoPlayer.
     * Injects the required CDN Referer to bypass anti-hotlinking.
     */
    fun createMedia3OkHttpClient(customHeaders: Map<String, String> = emptyMap()): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .header("Referer", "https://zokoanime.video/") // Default required anti-leech referer

                customHeaders.forEach { (key, value) ->
                    builder.header(key, value)
                }

                chain.proceed(builder.build())
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getFullPosterUrl(posterPath: String?): String? {
        if (posterPath == null) return null
        return if (posterPath.startsWith("http")) {
            posterPath
        } else {
            "https://anifam.net" + (if (posterPath.startsWith("/")) "" else "/") + posterPath
        }
    }

    fun resolvePosterUrl(posterPath: String?): String? = getFullPosterUrl(posterPath)
}
