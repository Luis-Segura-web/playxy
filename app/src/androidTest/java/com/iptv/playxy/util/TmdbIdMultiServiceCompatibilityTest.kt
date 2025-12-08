package com.iptv.playxy.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests para validar compatibilidad multi-servicio con TMDB IDs
 * Verifica que la app reconoce múltiples variantes de nombres de parámetro
 */
class TmdbIdMultiServiceCompatibilityTest {

    // ===== Tests de Reconocimiento de Claves =====

    @Test
    fun testRecognizesTmdbIdKey() {
        assertTrue(TmdbIdValidator.isTmdbIdKey("tmdb_id"))
    }

    @Test
    fun testRecognizesIdTmdbKey() {
        assertTrue(TmdbIdValidator.isTmdbIdKey("id_tmdb"))
    }

    @Test
    fun testRecognizesTmdbIdCamelCase() {
        assertTrue(TmdbIdValidator.isTmdbIdKey("tmdbId"))
    }

    @Test
    fun testRecognizesIdTmdbCamelCase() {
        assertTrue(TmdbIdValidator.isTmdbIdKey("idTmdb"))
    }

    @Test
    fun testRecognizesTmdbShort() {
        assertTrue(TmdbIdValidator.isTmdbIdKey("tmdb"))
    }

    @Test
    fun testRecognizesTmdbIdUpperCase() {
        assertTrue(TmdbIdValidator.isTmdbIdKey("TMDB_ID"))
    }

    @Test
    fun testRecognizesIdTmdbUpperCase() {
        assertTrue(TmdbIdValidator.isTmdbIdKey("ID_TMDB"))
    }

    @Test
    fun testIgnoresTmdbIcoKey() {
        assertFalse(TmdbIdValidator.isTmdbIdKey("tmdb_ico"))
    }

    @Test
    fun testIgnoresTmdbImageKey() {
        assertFalse(TmdbIdValidator.isTmdbIdKey("tmdb_image"))
    }

    @Test
    fun testIgnoresTmdbPathKey() {
        assertFalse(TmdbIdValidator.isTmdbIdKey("tmdb_path"))
    }

    @Test
    fun testIgnoresTmdbUrlKey() {
        assertFalse(TmdbIdValidator.isTmdbIdKey("tmdb_url"))
    }

    // ===== Tests de Extracción desde Map =====

    @Test
    fun testExtractsFromMapWithTmdbIdKey() {
        val map = mapOf("name" to "Movie", "tmdb_id" to "123")
        assertEquals("123", TmdbIdValidator.extractFromJsonMap(map))
    }

    @Test
    fun testExtractsFromMapWithIdTmdbKey() {
        val map = mapOf("name" to "Movie", "id_tmdb" to "456")
        assertEquals("456", TmdbIdValidator.extractFromJsonMap(map))
    }

    @Test
    fun testExtractsFromMapWithTmdbIdCamelCase() {
        val map = mapOf("name" to "Movie", "tmdbId" to "789")
        assertEquals("789", TmdbIdValidator.extractFromJsonMap(map))
    }

    @Test
    fun testExtractsFromMapWithIdTmdbCamelCase() {
        val map = mapOf("name" to "Movie", "idTmdb" to "999")
        assertEquals("999", TmdbIdValidator.extractFromJsonMap(map))
    }

    @Test
    fun testExtractsFromMapWithNumericValue() {
        val map = mapOf("name" to "Movie", "tmdb_id" to 555)
        assertEquals("555", TmdbIdValidator.extractFromJsonMap(map))
    }

    @Test
    fun testIgnoresInvalidValuesInMap() {
        val map = mapOf("name" to "Movie", "tmdb_id" to "0")
        assertNull(TmdbIdValidator.extractFromJsonMap(map))
    }

    @Test
    fun testIgnoresNullValuesInMap() {
        val map = mapOf<String, Any>("name" to "Movie")
        assertNull(TmdbIdValidator.extractFromJsonMap(map))
    }

    @Test
    fun testIgnoresImageKeysInMap() {
        val map = mapOf("name" to "Movie", "tmdb_ico" to "http://...")
        assertNull(TmdbIdValidator.extractFromJsonMap(map))
    }

    // ===== Tests de Búsqueda de Todas las Variantes =====

    @Test
    fun testFindsAllTmdbParamsInMap() {
        val map = mapOf(
            "name" to "Movie",
            "tmdb_id" to "111",
            "id_tmdb" to "222",
            "tmdbId" to "333"
        )
        val result = TmdbIdValidator.findAllTmdbParamsInMap(map)
        assertEquals(3, result.size)
    }

    @Test
    fun testFindsAllValidTmdbParams() {
        val map = mapOf(
            "name" to "Movie",
            "tmdb_id" to "111",
            "tmdb_ico" to "http://...",
            "id_tmdb" to "222",
            "tmdb_image" to "http://..."
        )
        val result = TmdbIdValidator.findAllTmdbParamsInMap(map)
        assertEquals(2, result.size)  // Solo tmdb_id e id_tmdb
        assertTrue(result.containsKey("tmdb_id"))
        assertTrue(result.containsKey("id_tmdb"))
    }

    @Test
    fun testIgnoresImageUrlsInParamSearch() {
        val map = mapOf(
            "name" to "Movie",
            "tmdb_ico" to "http://image.url",
            "tmdb_image" to "http://image.url",
            "tmdb_path" to "http://path"
        )
        val result = TmdbIdValidator.findAllTmdbParamsInMap(map)
        assertEquals(0, result.size)
    }

    // ===== Tests de Variantes de Servicio =====

    @Test
    fun testServiceAWithStandardFormat() {
        // Servicio A: tmdb_id estándar
        val serviceAResponse = mapOf(
            "name" to "El Francotirador",
            "tmdb_id" to "11778",
            "stream_icon" to "https://..."
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(serviceAResponse)
        assertEquals("11778", tmdbId)
    }

    @Test
    fun testServiceBWithAlternativeFormat() {
        // Servicio B: id_tmdb (formato alternativo)
        val serviceBResponse = mapOf(
            "name" to "The Walking Dead",
            "id_tmdb" to "95829",
            "poster" to "https://..."
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(serviceBResponse)
        assertEquals("95829", tmdbId)
    }

    @Test
    fun testServiceCWithCamelCase() {
        // Servicio C: tmdbId (camelCase)
        val serviceCResponse = mapOf(
            "name" to "Game of Thrones",
            "tmdbId" to "1399",
            "rating" to 8.5
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(serviceCResponse)
        assertEquals("1399", tmdbId)
    }

    @Test
    fun testServiceDWithNumericValue() {
        // Servicio D: tmdb_id como número
        val serviceDResponse = mapOf(
            "name" to "Breaking Bad",
            "tmdb_id" to 1396,
            "episodes" to 62
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(serviceDResponse)
        assertEquals("1396", tmdbId)
    }

    @Test
    fun testServiceEWithMissingTmdbId() {
        // Servicio E: sin parámetro TMDB ID
        val serviceEResponse = mapOf(
            "name" to "Unknown Series",
            "series_id" to "123",
            "rating" to 7.0
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(serviceEResponse)
        assertNull(tmdbId)
    }

    @Test
    fun testServiceFWithEmptyTmdbId() {
        // Servicio F: tmdb_id vacío
        val serviceFResponse = mapOf(
            "name" to "Another Movie",
            "tmdb_id" to "",
            "stream_id" to "456"
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(serviceFResponse)
        assertNull(tmdbId)  // Se valida y rechaza
    }

    @Test
    fun testServiceGWithZeroTmdbId() {
        // Servicio G: tmdb_id = "0"
        val serviceGResponse = mapOf(
            "name" to "Yet Another Film",
            "tmdb_id" to "0",
            "duration" to 120
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(serviceGResponse)
        assertNull(tmdbId)  // Se valida y rechaza
    }

    @Test
    fun testServiceWithMultipleTmdbIdVariants() {
        // Servicio envía múltiples variantes (prioridad al primero encontrado)
        val map = mapOf(
            "name" to "Movie",
            "tmdb_id" to "111",      // Encontrará este primero
            "id_tmdb" to "222",
            "tmdbId" to "333"
        )
        val tmdbId = TmdbIdValidator.extractFromJsonMap(map)
        assertEquals("111", tmdbId)  // Usa el primero
    }

    @Test
    fun testCaseInsensitiveKeyMatching() {
        // Prueba que isTmdbIdKey no es sensible a mayúsculas
        assertTrue(TmdbIdValidator.isTmdbIdKey("TMDB_ID"))
        assertTrue(TmdbIdValidator.isTmdbIdKey("Tmdb_Id"))
        assertTrue(TmdbIdValidator.isTmdbIdKey("tmDb_Id"))
    }

    // ===== Tests de Robustez =====

    @Test
    fun testHandlesNullMap() {
        assertNull(TmdbIdValidator.extractFromJsonMap(null))
    }

    @Test
    fun testHandlesEmptyMap() {
        assertNull(TmdbIdValidator.extractFromJsonMap(emptyMap()))
    }

    @Test
    fun testHandlesNullKey() {
        assertFalse(TmdbIdValidator.isTmdbIdKey(null))
    }

    @Test
    fun testHandlesEmptyKey() {
        assertFalse(TmdbIdValidator.isTmdbIdKey(""))
    }

    @Test
    fun testDistinguishesBetweenValidAndInvalidServices() {
        val validService = mapOf("tmdb_id" to "12345")
        val invalidService = mapOf("tmdb_id" to "")
        
        assertNotNull(TmdbIdValidator.extractFromJsonMap(validService))
        assertNull(TmdbIdValidator.extractFromJsonMap(invalidService))
    }

    @Test
    fun testSupportedVariantsListIsComplete() {
        val variants = TmdbIdValidator.SUPPORTED_TMDB_ID_VARIANTS
        assertEquals(7, variants.size)
        assertTrue(variants.contains("tmdb_id"))
        assertTrue(variants.contains("id_tmdb"))
        assertTrue(variants.contains("tmdbId"))
        assertTrue(variants.contains("idTmdb"))
        assertTrue(variants.contains("tmdb"))
        assertTrue(variants.contains("TMDB_ID"))
        assertTrue(variants.contains("ID_TMDB"))
    }
}
