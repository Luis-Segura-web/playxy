package com.iptv.playxy.data.api

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

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
        // Ensure the base URL ends with /
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(IptvApiService::class.java)
    }
}

