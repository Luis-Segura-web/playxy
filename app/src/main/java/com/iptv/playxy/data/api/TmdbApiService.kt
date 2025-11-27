package com.iptv.playxy.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("movie/{id}")
    suspend fun getMovie(
        @Path("id") id: String,
        @Query("append_to_response") appendToResponse: String = "images,credits,videos,similar,recommendations",
        @Query("language") language: String = "es-ES",
        @Query("include_image_language") includeImageLanguage: String = "es,en,null"
    ): Response<TmdbMovieResponse>

    @GET("tv/{id}")
    suspend fun getSeries(
        @Path("id") id: String,
        @Query("append_to_response") appendToResponse: String = "images,credits,videos,similar,recommendations",
        @Query("language") language: String = "es-ES",
        @Query("include_image_language") includeImageLanguage: String = "es,en,null"
    ): Response<TmdbSeriesResponse>

    @GET("collection/{id}")
    suspend fun getCollection(
        @Path("id") id: String,
        @Query("language") language: String = "es-ES",
        @Query("include_image_language") includeImageLanguage: String = "es,en,null"
    ): Response<com.iptv.playxy.data.api.TmdbCollectionResponse>

    @GET("person/{id}")
    suspend fun getPerson(
        @Path("id") id: String,
        @Query("append_to_response") appendToResponse: String = "combined_credits",
        @Query("language") language: String = "es-ES",
        @Query("include_image_language") includeImageLanguage: String = "es,en,null"
    ): Response<com.iptv.playxy.data.api.TmdbPersonResponse>

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): Response<TmdbPagedMovies>

    @GET("trending/tv/week")
    suspend fun getTrendingSeries(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): Response<TmdbPagedSeries>

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): Response<TmdbPagedMovies>

    @GET("tv/popular")
    suspend fun getPopularSeries(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): Response<TmdbPagedSeries>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): Response<TmdbPagedMovies>

    @GET("tv/top_rated")
    suspend fun getTopRatedSeries(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): Response<TmdbPagedSeries>

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ES"
    ): Response<TmdbPagedMovies>

    @GET("tv/on_the_air")
    suspend fun getOnTheAirSeries(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): Response<TmdbPagedSeries>
}
