package com.iptv.playxy.data.api

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor

/**
 * Factory to create IptvApiService instances with dynamic base URLs
 */
@Singleton
class ApiServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {

    /**
     * Creates an IptvApiService instance with the specified base URL
     * @param baseUrl The base URL from user's profile
     * @return IptvApiService configured for the given URL
     */
    fun createService(baseUrl: String): IptvApiService {
        // Clean and ensure the base URL ends with /
        val cleanedUrl = baseUrl.trim().replace("\n", "").replace("\r", "")
        val formattedBaseUrl = if (cleanedUrl.endsWith("/")) cleanedUrl else "$cleanedUrl/"

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(IptvApiService::class.java)
    }

    fun createTmdbService(apiKey: String): TmdbApiService {
        val cleanKey = apiKey.trim()
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val originalUrl = original.url
            val urlBuilder = originalUrl.newBuilder()
            if (cleanKey.isNotEmpty()) {
                urlBuilder.addQueryParameter("api_key", cleanKey)
            }
            val url = urlBuilder.build()
            val request = original.newBuilder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json;charset=utf-8")
                .build()
            chain.proceed(request)
        }

        val client = okHttpClient.newBuilder().apply {
            // Asegurar que el auth se ejecute antes del interceptor de logging (ya presente en okHttpClient)
            interceptors().add(0, authInterceptor)
        }.build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(TmdbApiService::class.java)
    }
}
