package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "token") val token: String,
    @Json(name = "user") val user: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SyncPushPayload(
    @Json(name = "logs") val logs: List<LayerFarmLog>,
    @Json(name = "vaccinations") val vaccinations: List<VaccinationSchedule>,
    @Json(name = "biosecurity") val biosecurity: List<BiosecurityCheck>
)

@JsonClass(generateAdapter = true)
data class SyncPushResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "syncedIds") val syncedIds: List<String>
)

@JsonClass(generateAdapter = true)
data class SyncPullResponse(
    @Json(name = "logs") val logs: List<LayerFarmLog>,
    @Json(name = "vaccinations") val vaccinations: List<VaccinationSchedule>,
    @Json(name = "biosecurity") val biosecurity: List<BiosecurityCheck>,
    @Json(name = "serverTimestamp") val serverTimestamp: Long
)

interface FarmApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/sync/push")
    suspend fun pushData(
        @Body payload: SyncPushPayload
    ): Response<SyncPushResponse>

    @GET("api/sync/pull")
    suspend fun pullData(
        @Query("lastSyncTimestamp") lastSyncTimestamp: Long
    ): Response<SyncPullResponse>
}

object RetrofitClient {
    private var baseUrl = "https://ais-dev-pndu4mvv2bfmazfyes5ovk-1023004766485.asia-southeast1.run.app/"
    private var authCookie = "__SECURE-aistudio_auth_token="
    private var jwtToken: String? = null

    private var retrofit: Retrofit? = null
    private var apiService: FarmApiService? = null

    fun updateConfig(newBaseUrl: String, newCookie: String) {
        val cleanUrl = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        if (cleanUrl != baseUrl || newCookie != authCookie || retrofit == null) {
            baseUrl = cleanUrl
            authCookie = newCookie
            rebuildRetrofit()
        }
    }

    fun setToken(token: String?) {
        this.jwtToken = token
    }

    fun getToken(): String? = jwtToken

    fun getBaseUrl(): String = baseUrl
    fun getAuthCookie(): String = authCookie

    private fun rebuildRetrofit() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()

                // 1. Add AI Studio Cookie if accessing a development/preview host
                val host = originalRequest.url.host
                if ((host.contains("ais-dev-") || host.contains("ais-pre-")) && authCookie.isNotEmpty()) {
                    val finalCookie = if (authCookie.contains("=")) authCookie else "__SECURE-aistudio_auth_token=$authCookie"
                    requestBuilder.addHeader("Cookie", finalCookie)
                }

                // 2. Add JWT token if logged in
                jwtToken?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                requestBuilder.addHeader("Content-Type", "application/json")
                requestBuilder.addHeader("Accept", "application/json")

                chain.proceed(requestBuilder.build())
            }
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        apiService = retrofit?.create(FarmApiService::class.java)
    }

    val service: FarmApiService
        get() {
            if (apiService == null) {
                rebuildRetrofit()
            }
            return apiService!!
        }
}
