package com.iptv.playxy.util

/**
 * Utility object for validating and processing TMDB IDs
 * Handles null, empty, zero, and invalid values
 * Also supports multiple variations of parameter names (tmdb_id, id_tmdb, tmdbId, etc.)
 */
object TmdbIdValidator {
    
    /**
     * Lista de variantes de nombres de parámetro TMDB que la app soporta
     */
    val SUPPORTED_TMDB_ID_VARIANTS = listOf(
        "tmdb_id",      // Snake case estándar
        "id_tmdb",      // Snake case alternativo
        "tmdbId",       // Camel case
        "idTmdb",       // Camel case alternativo
        "tmdb",         // Corto
        "TMDB_ID",      // Upper case
        "ID_TMDB"       // Upper case alternativo
    )
    
    /**
     * Valida si una clave de JSON corresponde a un TMDB ID
     * Evita confundirse con parámetros como tmdb_ico, tmdb_image, etc.
     * @param key La clave del parámetro JSON
     * @return true si es un parámetro TMDB ID válido
     */
    fun isTmdbIdKey(key: String?): Boolean {
        if (key == null) return false
        return SUPPORTED_TMDB_ID_VARIANTS.any { it.equals(key, ignoreCase = true) } &&
               !isImageOrIconKey(key)
    }
    
    /**
     * Verifica si una clave corresponde a imagen o icono
     */
    private fun isImageOrIconKey(key: String): Boolean {
        val lowerKey = key.lowercase()
        return lowerKey.contains("ico") ||
               lowerKey.contains("image") ||
               lowerKey.contains("url") ||
               lowerKey.contains("path") ||
               lowerKey.contains("icon") ||
               lowerKey.contains("img")
    }
    
    /**
     * Valida si un TMDB ID es válido (not null, not empty, not "0")
     * @param tmdbId El TMDB ID a validar (puede ser String o Int)
     * @return true si TMDB ID es válido, false otherwise
     */
    fun isValidTmdbId(tmdbId: Any?): Boolean {
        return when (tmdbId) {
            null -> false
            is String -> {
                val trimmed = tmdbId.trim()
                trimmed.isNotEmpty() && trimmed != "0" && trimmed.lowercase() != "null"
            }
            is Int -> tmdbId > 0
            is Long -> tmdbId > 0
            is Number -> tmdbId.toLong() > 0
            else -> false
        }
    }
    
    /**
     * Convierte TMDB ID a un String válido, o null si inválido
     * @param tmdbId El TMDB ID a convertir
     * @return Valid TMDB ID string, o null si inválido
     */
    fun sanitizeTmdbId(tmdbId: Any?): String? {
        return if (isValidTmdbId(tmdbId)) {
            when (tmdbId) {
                is String -> tmdbId.trim()
                // Handle Double/Float to remove unnecessary decimals (e.g., 12345.0 -> "12345")
                is Double -> if (tmdbId == tmdbId.toLong().toDouble()) tmdbId.toLong().toString() else tmdbId.toString()
                is Float -> if (tmdbId == tmdbId.toLong().toFloat()) tmdbId.toLong().toString() else tmdbId.toString()
                is Number -> tmdbId.toLong().toString()
                else -> tmdbId.toString()
            }
        } else {
            null
        }
    }
    
    /**
     * Extrae TMDB ID de una URL (si la URL contiene TMDB ID)
     * Ejemplos: 
     * - "https://www.themoviedb.org/movie/11778" -> "11778"
     * - "https://image.tmdb.org/t/p/w600_and_h900_bestv2/wT3DeCZ3Ax5VYhKu6ajyEvA1hXG.jpg" -> null
     * @param url La URL a procesar
     * @return TMDB ID string, o null si no se encuentra
     */
    fun extractTmdbIdFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        
        // Pattern: /movie/{id} o /tv/{id}
        val moviePattern = Regex("""(?:themoviedb\.org/(?:movie|tv)/(\d+))""")
        val match = moviePattern.find(url)
        return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
    
    /**
     * Intenta extraer TMDB ID desde múltiples fuentes con prioridad
     * @param directId TMDB ID directo (prioridad más alta)
     * @param url URL que podría contener TMDB ID
     * @param fallback Valor fallback si otros fallan
     * @return Valid TMDB ID string, o null si todas las fuentes fallan
     */
    fun resolveTmdbId(
        directId: String? = null,
        url: String? = null,
        fallback: String? = null
    ): String? {
        // Intenta ID directo primero
        sanitizeTmdbId(directId)?.let { return it }
        
        // Intenta extraer desde URL
        extractTmdbIdFromUrl(url)?.let { 
            if (isValidTmdbId(it)) return it 
        }
        
        // Intenta fallback
        return sanitizeTmdbId(fallback)
    }
    
    /**
     * Extrae TMDB ID desde un Map JSON buscando múltiples variantes
     * @param jsonMap Map representando el JSON
     * @return TMDB ID encontrado o null
     */
    fun extractFromJsonMap(jsonMap: Map<String, Any>?): String? {
        if (jsonMap == null) return null
        
        // Busca en orden de preferencia
        for (key in SUPPORTED_TMDB_ID_VARIANTS) {
            val value = jsonMap[key] ?: continue
            val tmdbId = sanitizeTmdbId(value) ?: continue
            return tmdbId
        }
        
        return null
    }
    
    /**
     * Busca todas las variantes de parámetros TMDB en un JSON
     * Útil para debugging y validación
     * @param jsonMap Map representando el JSON
     * @return Map con todos los parámetros TMDB encontrados
     */
    fun findAllTmdbParamsInMap(jsonMap: Map<String, Any>?): Map<String, String> {
        if (jsonMap == null) return emptyMap()
        
        return jsonMap.filter { (key, _) ->
            isTmdbIdKey(key)
        }.mapNotNull { (key, value) ->
            val tmdbId = sanitizeTmdbId(value)
            if (tmdbId != null) key to tmdbId else null
        }.toMap()
    }
}
