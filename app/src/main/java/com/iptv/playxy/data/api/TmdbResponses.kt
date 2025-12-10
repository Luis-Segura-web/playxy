package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbMovieResponse(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "original_title") val originalTitle: String?,
    @field:Json(name = "overview") val overview: String?,
    @field:Json(name = "release_date") val releaseDate: String?,
    @field:Json(name = "runtime") val runtime: Int?,
    @field:Json(name = "poster_path") val posterPath: String?,
    @field:Json(name = "backdrop_path") val backdropPath: String?,
    @field:Json(name = "vote_average") val voteAverage: Double?,
    @field:Json(name = "vote_count") val voteCount: Int?,
    @field:Json(name = "genres") val genres: List<TmdbGenre>?,
    @field:Json(name = "videos") val videos: TmdbVideosResponse?,
    @field:Json(name = "images") val images: TmdbImagesResponse?,
    @field:Json(name = "credits") val credits: TmdbCreditsResponse?,
    @field:Json(name = "belongs_to_collection") val belongsToCollection: TmdbCollection?,
    @field:Json(name = "similar") val similar: TmdbPagedMovies?,
    @field:Json(name = "recommendations") val recommendations: TmdbPagedMovies?
)

@JsonClass(generateAdapter = true)
data class TmdbSeriesResponse(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "original_name") val originalName: String?,
    @field:Json(name = "overview") val overview: String?,
    @field:Json(name = "first_air_date") val firstAirDate: String?,
    @field:Json(name = "episode_run_time") val episodeRunTime: List<Int>?,
    @field:Json(name = "poster_path") val posterPath: String?,
    @field:Json(name = "backdrop_path") val backdropPath: String?,
    @field:Json(name = "vote_average") val voteAverage: Double?,
    @field:Json(name = "vote_count") val voteCount: Int?,
    @field:Json(name = "genres") val genres: List<TmdbGenre>?,
    @field:Json(name = "videos") val videos: TmdbVideosResponse?,
    @field:Json(name = "images") val images: TmdbImagesResponse?,
    @field:Json(name = "credits") val credits: TmdbCreditsResponse?,
    @field:Json(name = "similar") val similar: TmdbPagedSeries?,
    @field:Json(name = "recommendations") val recommendations: TmdbPagedSeries?
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class TmdbVideosResponse(
    @field:Json(name = "results") val results: List<TmdbVideo>?
)

@JsonClass(generateAdapter = true)
data class TmdbVideo(
    @field:Json(name = "key") val key: String?,
    @field:Json(name = "site") val site: String?,
    @field:Json(name = "type") val type: String?,
    @field:Json(name = "official") val official: Boolean?
)

@JsonClass(generateAdapter = true)
data class TmdbImagesResponse(
    @field:Json(name = "backdrops") val backdrops: List<TmdbImage>?,
    @field:Json(name = "posters") val posters: List<TmdbImage>?
)

@JsonClass(generateAdapter = true)
data class TmdbImage(
    @field:Json(name = "file_path") val filePath: String?
)

@JsonClass(generateAdapter = true)
data class TmdbCreditsResponse(
    @field:Json(name = "cast") val cast: List<TmdbCastMember>?,
    @field:Json(name = "crew") val crew: List<TmdbCrewMember>?
)

@JsonClass(generateAdapter = true)
data class TmdbCastMember(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "order") val order: Int?,
    @field:Json(name = "character") val character: String?,
    @field:Json(name = "profile_path") val profilePath: String?
)

@JsonClass(generateAdapter = true)
data class TmdbCrewMember(
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "job") val job: String?
)

@JsonClass(generateAdapter = true)
data class TmdbCollection(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "poster_path") val posterPath: String?
)

@JsonClass(generateAdapter = true)
data class TmdbCollectionResponse(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "poster_path") val posterPath: String?,
    @field:Json(name = "parts") val parts: List<TmdbMovieResult>?
)

@JsonClass(generateAdapter = true)
data class TmdbPagedMovies(
    @field:Json(name = "results") val results: List<TmdbMovieResult>?
)

@JsonClass(generateAdapter = true)
data class TmdbPagedSeries(
    @field:Json(name = "results") val results: List<TmdbSeriesResult>?
)

@JsonClass(generateAdapter = true)
data class TmdbSeriesResult(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "poster_path") val posterPath: String?,
    @field:Json(name = "first_air_date") val firstAirDate: String?,
    @field:Json(name = "overview") val overview: String?,
    @field:Json(name = "backdrop_path") val backdropPath: String?,
    @field:Json(name = "vote_average") val voteAverage: Double?,
    @field:Json(name = "media_type") val mediaType: String? = null,
    @field:Json(name = "character") val character: String? = null,
    @field:Json(name = "genre_ids") val genreIds: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieResult(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "poster_path") val posterPath: String?,
    @field:Json(name = "media_type") val mediaType: String? = null,
    @field:Json(name = "first_air_date") val firstAirDate: String? = null,
    @field:Json(name = "character") val character: String? = null,
    @field:Json(name = "release_date") val releaseDate: String?,
    @field:Json(name = "overview") val overview: String?,
    @field:Json(name = "backdrop_path") val backdropPath: String?,
    @field:Json(name = "vote_average") val voteAverage: Double?,
    @field:Json(name = "genre_ids") val genreIds: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbPersonResponse(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "profile_path") val profilePath: String?,
    @field:Json(name = "biography") val biography: String?,
    @field:Json(name = "birthday") val birthday: String?,
    @field:Json(name = "place_of_birth") val placeOfBirth: String?,
    @field:Json(name = "combined_credits") val combinedCredits: TmdbCombinedCredits?
)

@JsonClass(generateAdapter = true)
data class TmdbCombinedCredits(
    @field:Json(name = "cast") val cast: List<TmdbMovieResult>?
)
