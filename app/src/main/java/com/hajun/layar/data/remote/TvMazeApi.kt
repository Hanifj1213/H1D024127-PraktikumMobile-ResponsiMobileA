package com.hajun.layar.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

internal val tvMazeJson = Json { ignoreUnknownKeys = true }

interface TvMazeApi {
    // Retrofit reflects Java generic types, which erase nullable list elements.
    // Decode array elements explicitly in the repository to retain that nullability.
    @GET("shows")
    suspend fun browseShows(@Query("page") page: Int = 0): JsonArray

    @GET("shows/{id}")
    suspend fun getShow(@Path("id") id: Int): ShowDto

    @GET("search/shows")
    suspend fun searchShows(@Query("q") query: String): JsonArray

    companion object {
        fun create(baseUrl: HttpUrl = "https://api.tvmaze.com/".toHttpUrl()): TvMazeApi {
            // Plain HTTP is reserved for deterministic loopback tests, never remote servers.
            require(baseUrl.isHttps || baseUrl.host in setOf("localhost", "127.0.0.1", "::1")) {
                "Remote API endpoints must use HTTPS"
            }
            val client = OkHttpClient.Builder()
                .followSslRedirects(false)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(tvMazeJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TvMazeApi::class.java)
        }
    }
}
