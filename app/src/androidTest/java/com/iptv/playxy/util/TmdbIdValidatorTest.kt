package com.iptv.playxy.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for TmdbIdValidator
 * Tests all edge cases and validation scenarios
 */
class TmdbIdValidatorTest {

    @Test
    fun testIsValidTmdbId_WithNull() {
        assertFalse(TmdbIdValidator.isValidTmdbId(null))
    }

    @Test
    fun testIsValidTmdbId_WithEmptyString() {
        assertFalse(TmdbIdValidator.isValidTmdbId(""))
    }

    @Test
    fun testIsValidTmdbId_WithBlankString() {
        assertFalse(TmdbIdValidator.isValidTmdbId("   "))
    }

    @Test
    fun testIsValidTmdbId_WithZeroString() {
        assertFalse(TmdbIdValidator.isValidTmdbId("0"))
    }

    @Test
    fun testIsValidTmdbId_WithNullString() {
        assertFalse(TmdbIdValidator.isValidTmdbId("null"))
    }

    @Test
    fun testIsValidTmdbId_WithNullStringCapitalized() {
        assertFalse(TmdbIdValidator.isValidTmdbId("NULL"))
    }

    @Test
    fun testIsValidTmdbId_WithValidString() {
        assertTrue(TmdbIdValidator.isValidTmdbId("123"))
    }

    @Test
    fun testIsValidTmdbId_WithValidStringWithSpaces() {
        assertTrue(TmdbIdValidator.isValidTmdbId("  456  "))
    }

    @Test
    fun testIsValidTmdbId_WithZeroInt() {
        assertFalse(TmdbIdValidator.isValidTmdbId(0))
    }

    @Test
    fun testIsValidTmdbId_WithNegativeInt() {
        assertFalse(TmdbIdValidator.isValidTmdbId(-1))
    }

    @Test
    fun testIsValidTmdbId_WithPositiveInt() {
        assertTrue(TmdbIdValidator.isValidTmdbId(123))
    }

    @Test
    fun testIsValidTmdbId_WithZeroLong() {
        assertFalse(TmdbIdValidator.isValidTmdbId(0L))
    }

    @Test
    fun testIsValidTmdbId_WithNegativeLong() {
        assertFalse(TmdbIdValidator.isValidTmdbId(-1L))
    }

    @Test
    fun testIsValidTmdbId_WithPositiveLong() {
        assertTrue(TmdbIdValidator.isValidTmdbId(456L))
    }

    // ===== sanitizeTmdbId Tests =====

    @Test
    fun testSanitizeTmdbId_WithNull() {
        assertNull(TmdbIdValidator.sanitizeTmdbId(null))
    }

    @Test
    fun testSanitizeTmdbId_WithEmptyString() {
        assertNull(TmdbIdValidator.sanitizeTmdbId(""))
    }

    @Test
    fun testSanitizeTmdbId_WithZeroString() {
        assertNull(TmdbIdValidator.sanitizeTmdbId("0"))
    }

    @Test
    fun testSanitizeTmdbId_WithValidString() {
        assertEquals("123", TmdbIdValidator.sanitizeTmdbId("123"))
    }

    @Test
    fun testSanitizeTmdbId_WithStringWithSpaces() {
        assertEquals("456", TmdbIdValidator.sanitizeTmdbId("  456  "))
    }

    @Test
    fun testSanitizeTmdbId_WithPositiveInt() {
        assertEquals("789", TmdbIdValidator.sanitizeTmdbId(789))
    }

    @Test
    fun testSanitizeTmdbId_WithPositiveLong() {
        assertEquals("999", TmdbIdValidator.sanitizeTmdbId(999L))
    }

    @Test
    fun testSanitizeTmdbId_WithZeroInt() {
        assertNull(TmdbIdValidator.sanitizeTmdbId(0))
    }

    // ===== extractTmdbIdFromUrl Tests =====

    @Test
    fun testExtractTmdbIdFromUrl_WithNull() {
        assertNull(TmdbIdValidator.extractTmdbIdFromUrl(null))
    }

    @Test
    fun testExtractTmdbIdFromUrl_WithEmptyString() {
        assertNull(TmdbIdValidator.extractTmdbIdFromUrl(""))
    }

    @Test
    fun testExtractTmdbIdFromUrl_WithMovieUrl() {
        val url = "https://www.themoviedb.org/movie/11778"
        assertEquals("11778", TmdbIdValidator.extractTmdbIdFromUrl(url))
    }

    @Test
    fun testExtractTmdbIdFromUrl_WithTvUrl() {
        val url = "https://www.themoviedb.org/tv/95829"
        assertEquals("95829", TmdbIdValidator.extractTmdbIdFromUrl(url))
    }

    @Test
    fun testExtractTmdbIdFromUrl_WithImageUrl() {
        val url = "https://image.tmdb.org/t/p/w600_and_h900_bestv2/wT3DeCZ3Ax5VYhKu6ajyEvA1hXG.jpg"
        assertNull(TmdbIdValidator.extractTmdbIdFromUrl(url))
    }

    @Test
    fun testExtractTmdbIdFromUrl_WithThemoviedbImageUrl() {
        val url = "https://www.themoviedb.org/t/p/w600_and_h900_bestv2/wT3DeCZ3Ax5VYhKu6ajyEvA1hXG.jpg"
        assertNull(TmdbIdValidator.extractTmdbIdFromUrl(url))
    }

    @Test
    fun testExtractTmdbIdFromUrl_WithInvalidUrl() {
        val url = "https://example.com/some/path/123"
        assertNull(TmdbIdValidator.extractTmdbIdFromUrl(url))
    }

    // ===== resolveTmdbId Tests =====

    @Test
    fun testResolveTmdbId_WithValidDirectId() {
        val result = TmdbIdValidator.resolveTmdbId(directId = "123", url = null, fallback = null)
        assertEquals("123", result)
    }

    @Test
    fun testResolveTmdbId_WithValidDirectIdAndUrl() {
        // Direct ID takes priority
        val result = TmdbIdValidator.resolveTmdbId(
            directId = "123",
            url = "https://www.themoviedb.org/movie/456",
            fallback = null
        )
        assertEquals("123", result)
    }

    @Test
    fun testResolveTmdbId_WithInvalidDirectIdValidUrl() {
        // Should use URL when direct ID is invalid
        val result = TmdbIdValidator.resolveTmdbId(
            directId = "",
            url = "https://www.themoviedb.org/movie/789",
            fallback = null
        )
        assertEquals("789", result)
    }

    @Test
    fun testResolveTmdbId_WithInvalidDirectIdUrlFallback() {
        // Should use fallback when others fail
        val result = TmdbIdValidator.resolveTmdbId(
            directId = "",
            url = "https://example.com/invalid",
            fallback = "999"
        )
        assertEquals("999", result)
    }

    @Test
    fun testResolveTmdbId_AllInvalid() {
        // Should return null when all are invalid
        val result = TmdbIdValidator.resolveTmdbId(
            directId = "",
            url = "https://example.com/invalid",
            fallback = ""
        )
        assertNull(result)
    }

    @Test
    fun testResolveTmdbId_WithNull() {
        // Should return null when all are null
        val result = TmdbIdValidator.resolveTmdbId(
            directId = null,
            url = null,
            fallback = null
        )
        assertNull(result)
    }

    // ===== Integration Tests =====

    @Test
    fun testRealWorldScenario_VodStreamWithoutTmdbId() {
        // Simulates: get_vod_streams.json element without tmdb_id
        val tmdbId: String? = null
        val sanitized = TmdbIdValidator.sanitizeTmdbId(tmdbId)
        assertNull(sanitized)
    }

    @Test
    fun testRealWorldScenario_VodStreamWithEmptyTmdbId() {
        // Simulates: get_vod_streams.json element with tmdb_id = ""
        val tmdbId: String? = ""
        val sanitized = TmdbIdValidator.sanitizeTmdbId(tmdbId)
        assertNull(sanitized)
    }

    @Test
    fun testRealWorldScenario_SeriesWithValidTmdbId() {
        // Simulates: get_series.json element with valid tmdb_id
        val tmdbId: String? = "95829"
        val sanitized = TmdbIdValidator.sanitizeTmdbId(tmdbId)
        assertEquals("95829", sanitized)
    }

    @Test
    fun testRealWorldScenario_EpisodeWithoutTmdbId() {
        // Simulates: get_series_info.json S01E01 without tmdb_id
        val tmdbId: String? = null
        val sanitized = TmdbIdValidator.sanitizeTmdbId(tmdbId)
        assertNull(sanitized)
    }

    @Test
    fun testRealWorldScenario_FilteringNullsTmdbIds() {
        // Simulates filtering streams before groupBy
        val streams = listOf(
            "null",  // Invalid
            "",      // Invalid
            "0",     // Invalid
            "123",   // Valid
            null,    // Invalid
            "456"    // Valid
        )

        val validStreams = streams.filter { TmdbIdValidator.isValidTmdbId(it) }
        assertEquals(2, validStreams.size)
        assertTrue(validStreams.contains("123"))
        assertTrue(validStreams.contains("456"))
    }
}
