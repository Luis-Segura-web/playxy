package com.iptv.playxy.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for IPTV provider API
 */
interface IptvApiService {
    
    /**
     * Get live TV streams
     * @param username User's username
     * @param password User's password
     */
    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): Response<List<LiveStreamResponse>>
    
    /**
     * Get VOD (Video on Demand) streams
     * @param username User's username
     * @param password User's password
     */
    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): Response<List<VodStreamResponse>>
    
    /**
     * Get series
     * @param username User's username
     * @param password User's password
     */
    @GET("player_api.php")
    suspend fun getSeries(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): Response<List<SeriesResponse>>
    
    /**
     * Get live stream categories
     * @param username User's username
     * @param password User's password
     */
    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): Response<List<CategoryResponse>>
    
    /**
     * Get VOD categories
     * @param username User's username
     * @param password User's password
     */
    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): Response<List<CategoryResponse>>
    
    /**
     * Get series categories
     * @param username User's username
     * @param password User's password
     */
    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): Response<List<CategoryResponse>>
    
    /**
     * Get series info with seasons and episodes
     * @param username User's username
     * @param password User's password
     * @param seriesId Series ID to fetch info for
     */
    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String
    ): Response<SeriesInfoResponse>

    /**
     * Get detailed VOD information
     * @param username User's username
     * @param password User's password
     * @param vodId VOD stream ID
     */
    @GET("player_api.php")
    suspend fun getVodInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: String
    ): Response<VodInfoResponse>

    /**
     * Validate user credentials
     * @param username User's username
     * @param password User's password
     */
    @GET("player_api.php")
    suspend fun validateCredentials(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<Any>
}
