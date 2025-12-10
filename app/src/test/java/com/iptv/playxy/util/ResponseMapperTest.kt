package com.iptv.playxy.util

import com.iptv.playxy.data.api.*
import com.iptv.playxy.domain.Series
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ResponseMapper, specifically testing series info parsing
 * and episode handling with various provider response formats
 */
class ResponseMapperTest {

    @Test
    fun `toSeriesInfo filters out seasons without episodes`() {
        // Given: A response with 3 seasons but only 1 has episodes
        val response = SeriesInfoResponse(
            seasons = listOf(
                SeasonResponse(
                    id = "0",
                    seasonNumber = "0",
                    name = "Specials",
                    episodeCount = "3",
                    overview = null,
                    airDate = null,
                    cover = null,
                    coverBig = null
                ),
                SeasonResponse(
                    id = "1",
                    seasonNumber = "1",
                    name = "Season 1",
                    episodeCount = "12",
                    overview = null,
                    airDate = null,
                    cover = null,
                    coverBig = null
                ),
                SeasonResponse(
                    id = "2",
                    seasonNumber = "2",
                    name = "Season 2",
                    episodeCount = "9",
                    overview = null,
                    airDate = null,
                    cover = null,
                    coverBig = null
                )
            ),
            episodes = mapOf(
                "1" to listOf(
                    EpisodeResponse(
                        id = "100",
                        episodeNum = 1,
                        title = "Episode 1",
                        containerExtension = "mp4",
                        info = null,
                        customSid = null,
                        added = null,
                        season = 1,
                        directSource = null
                    )
                )
            ),
            info = SeriesInfoDetailsResponse(
                name = "Test Series",
                cover = null,
                movieImage = null,
                backdrop = null,
                plot = null,
                cast = null,
                director = null,
                genre = null,
                releaseDate = null,
                lastModified = null,
                rating = null,
                rating5Based = null,
                backdropPath = null,
                youtubeTrailer = null,
                episodeRunTime = null,
                categoryId = "1",
                tmdbId = null
            )
        )

        val originalSeries = Series(
            seriesId = "1",
            name = "Test Series",
            cover = null,
            plot = null,
            cast = null,
            director = null,
            genre = null,
            releaseDate = null,
            rating = 0f,
            rating5Based = 0f,
            backdropPath = emptyList(),
            youtubeTrailer = null,
            episodeRunTime = null,
            categoryId = "1",
            tmdbId = null,
            lastModified = null
        )

        // When: Mapping the response
        val result = ResponseMapper.toSeriesInfo(response, originalSeries)

        // Then: Only season 1 should be included (has episodes), seasons 0 and 2 filtered out
        assertEquals("Should only have 1 season with episodes", 1, result.seasons.size)
        assertEquals("Season should be number 1", 1, result.seasons[0].seasonNumber)
        assertEquals("Should have 1 episode", 1, result.episodesBySeason["1"]?.size)
        assertEquals("Total episodes should be 1", 1, result.episodesBySeason.values.flatten().size)
    }

    @Test
    fun `toSeriesInfo handles all seasons with episodes`() {
        // Given: A response where all seasons have episodes
        val response = SeriesInfoResponse(
            seasons = listOf(
                SeasonResponse(id = "1", seasonNumber = "1", name = "Season 1", 
                    episodeCount = "2", overview = null, airDate = null, cover = null, coverBig = null),
                SeasonResponse(id = "2", seasonNumber = "2", name = "Season 2", 
                    episodeCount = "2", overview = null, airDate = null, cover = null, coverBig = null)
            ),
            episodes = mapOf(
                "1" to listOf(
                    EpisodeResponse(id = "101", episodeNum = 1, title = "S1E1", 
                        containerExtension = "mp4", info = null, customSid = null, 
                        added = null, season = 1, directSource = null),
                    EpisodeResponse(id = "102", episodeNum = 2, title = "S1E2", 
                        containerExtension = "mp4", info = null, customSid = null, 
                        added = null, season = 1, directSource = null)
                ),
                "2" to listOf(
                    EpisodeResponse(id = "201", episodeNum = 1, title = "S2E1", 
                        containerExtension = "mp4", info = null, customSid = null, 
                        added = null, season = 2, directSource = null),
                    EpisodeResponse(id = "202", episodeNum = 2, title = "S2E2", 
                        containerExtension = "mp4", info = null, customSid = null, 
                        added = null, season = 2, directSource = null)
                )
            ),
            info = SeriesInfoDetailsResponse(name = "Test", cover = null, movieImage = null, 
                backdrop = null, plot = null, cast = null, director = null, genre = null, 
                releaseDate = null, lastModified = null, rating = null, rating5Based = null, 
                backdropPath = null, youtubeTrailer = null, episodeRunTime = null, 
                categoryId = "1", tmdbId = null)
        )

        val originalSeries = Series(seriesId = "1", name = "Test", cover = null, plot = null, 
            cast = null, director = null, genre = null, releaseDate = null, rating = 0f, 
            rating5Based = 0f, backdropPath = emptyList(), youtubeTrailer = null, 
            episodeRunTime = null, categoryId = "1", tmdbId = null, lastModified = null)

        // When
        val result = ResponseMapper.toSeriesInfo(response, originalSeries)

        // Then: All seasons should be preserved
        assertEquals("Should have 2 seasons", 2, result.seasons.size)
        assertEquals("Should have episodes for season 1", 2, result.episodesBySeason["1"]?.size)
        assertEquals("Should have episodes for season 2", 2, result.episodesBySeason["2"]?.size)
        assertEquals("Total episodes should be 4", 4, result.episodesBySeason.values.flatten().size)
    }

    @Test
    fun `toSeriesInfo handles empty episodes map`() {
        // Given: A response with seasons but no episodes
        val response = SeriesInfoResponse(
            seasons = listOf(
                SeasonResponse(id = "1", seasonNumber = "1", name = "Season 1", 
                    episodeCount = "10", overview = null, airDate = null, cover = null, coverBig = null)
            ),
            episodes = emptyMap(),
            info = SeriesInfoDetailsResponse(name = "Test", cover = null, movieImage = null, 
                backdrop = null, plot = null, cast = null, director = null, genre = null, 
                releaseDate = null, lastModified = null, rating = null, rating5Based = null, 
                backdropPath = null, youtubeTrailer = null, episodeRunTime = null, 
                categoryId = "1", tmdbId = null)
        )

        val originalSeries = Series(seriesId = "1", name = "Test", cover = null, plot = null, 
            cast = null, director = null, genre = null, releaseDate = null, rating = 0f, 
            rating5Based = 0f, backdropPath = emptyList(), youtubeTrailer = null, 
            episodeRunTime = null, categoryId = "1", tmdbId = null, lastModified = null)

        // When
        val result = ResponseMapper.toSeriesInfo(response, originalSeries)

        // Then: No seasons should be included
        assertEquals("Should have no seasons", 0, result.seasons.size)
        assertEquals("Should have no episodes", 0, result.episodesBySeason.size)
    }

    @Test
    fun `toSeriesInfo handles null episodes`() {
        // Given: A response with seasons but null episodes
        val response = SeriesInfoResponse(
            seasons = listOf(
                SeasonResponse(id = "1", seasonNumber = "1", name = "Season 1", 
                    episodeCount = "10", overview = null, airDate = null, cover = null, coverBig = null)
            ),
            episodes = null,
            info = SeriesInfoDetailsResponse(name = "Test", cover = null, movieImage = null, 
                backdrop = null, plot = null, cast = null, director = null, genre = null, 
                releaseDate = null, lastModified = null, rating = null, rating5Based = null, 
                backdropPath = null, youtubeTrailer = null, episodeRunTime = null, 
                categoryId = "1", tmdbId = null)
        )

        val originalSeries = Series(seriesId = "1", name = "Test", cover = null, plot = null, 
            cast = null, director = null, genre = null, releaseDate = null, rating = 0f, 
            rating5Based = 0f, backdropPath = emptyList(), youtubeTrailer = null, 
            episodeRunTime = null, categoryId = "1", tmdbId = null, lastModified = null)

        // When
        val result = ResponseMapper.toSeriesInfo(response, originalSeries)

        // Then: No seasons should be included
        assertEquals("Should have no seasons", 0, result.seasons.size)
        assertEquals("Should have no episodes", 0, result.episodesBySeason.size)
    }

    @Test
    fun `toSeriesInfo handles season with empty episode list`() {
        // Given: A response where a season key exists but has empty episode list
        val response = SeriesInfoResponse(
            seasons = listOf(
                SeasonResponse(id = "1", seasonNumber = "1", name = "Season 1", 
                    episodeCount = "10", overview = null, airDate = null, cover = null, coverBig = null)
            ),
            episodes = mapOf("1" to emptyList()),
            info = SeriesInfoDetailsResponse(name = "Test", cover = null, movieImage = null, 
                backdrop = null, plot = null, cast = null, director = null, genre = null, 
                releaseDate = null, lastModified = null, rating = null, rating5Based = null, 
                backdropPath = null, youtubeTrailer = null, episodeRunTime = null, 
                categoryId = "1", tmdbId = null)
        )

        val originalSeries = Series(seriesId = "1", name = "Test", cover = null, plot = null, 
            cast = null, director = null, genre = null, releaseDate = null, rating = 0f, 
            rating5Based = 0f, backdropPath = emptyList(), youtubeTrailer = null, 
            episodeRunTime = null, categoryId = "1", tmdbId = null, lastModified = null)

        // When
        val result = ResponseMapper.toSeriesInfo(response, originalSeries)

        // Then: Season should be filtered out since it has no actual episodes
        assertEquals("Should have no seasons with episodes", 0, result.seasons.size)
        assertEquals("Should have no episodes in map", 0, result.episodesBySeason.size)
    }

    @Test
    fun `toSeriesInfo sorts episodes by episode number`() {
        // Given: Episodes in random order
        val response = SeriesInfoResponse(
            seasons = listOf(
                SeasonResponse(id = "1", seasonNumber = "1", name = "Season 1", 
                    episodeCount = "3", overview = null, airDate = null, cover = null, coverBig = null)
            ),
            episodes = mapOf(
                "1" to listOf(
                    EpisodeResponse(id = "103", episodeNum = 3, title = "Episode 3", 
                        containerExtension = "mp4", info = null, customSid = null, 
                        added = null, season = 1, directSource = null),
                    EpisodeResponse(id = "101", episodeNum = 1, title = "Episode 1", 
                        containerExtension = "mp4", info = null, customSid = null, 
                        added = null, season = 1, directSource = null),
                    EpisodeResponse(id = "102", episodeNum = 2, title = "Episode 2", 
                        containerExtension = "mp4", info = null, customSid = null, 
                        added = null, season = 1, directSource = null)
                )
            ),
            info = SeriesInfoDetailsResponse(name = "Test", cover = null, movieImage = null, 
                backdrop = null, plot = null, cast = null, director = null, genre = null, 
                releaseDate = null, lastModified = null, rating = null, rating5Based = null, 
                backdropPath = null, youtubeTrailer = null, episodeRunTime = null, 
                categoryId = "1", tmdbId = null)
        )

        val originalSeries = Series(seriesId = "1", name = "Test", cover = null, plot = null, 
            cast = null, director = null, genre = null, releaseDate = null, rating = 0f, 
            rating5Based = 0f, backdropPath = emptyList(), youtubeTrailer = null, 
            episodeRunTime = null, categoryId = "1", tmdbId = null, lastModified = null)

        // When
        val result = ResponseMapper.toSeriesInfo(response, originalSeries)

        // Then: Episodes should be sorted by episode number
        val episodes = result.episodesBySeason["1"]!!
        assertEquals("Should have 3 episodes", 3, episodes.size)
        assertEquals("First episode should be 1", 1, episodes[0].episodeNum)
        assertEquals("Second episode should be 2", 2, episodes[1].episodeNum)
        assertEquals("Third episode should be 3", 3, episodes[2].episodeNum)
    }
}
