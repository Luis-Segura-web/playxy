package com.iptv.playxy.util

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

/**
 * Custom Moshi adapter para deserializar TMDB IDs desde múltiples variantes de nombres
 * Soporta: tmdb_id, id_tmdb, tmdbId, idTmdb, tmdb, id, etc.
 * 
 * Uso: Registrar en MoshiBuilder antes de crear las instancias Response
 */
class TmdbIdDeserializer {

    /**
     * Convierte cualquier valor al formato de TMDB ID
     */
    @ToJson
    fun toJson(tmdbId: String?): String? = tmdbId

    /**
     * Deserializa desde múltiples formatos
     * Este método NO se llama automáticamente porque Moshi usa @field:Json annotations
     * Para usarlo, se debe crear un custom adapter más complejo
     */
    @FromJson
    fun fromJson(tmdbId: String?): String? {
        return tmdbId?.trim().takeIf { !it.isNullOrEmpty() && it != "0" && it.lowercase() != "null" }
    }
}

/**
 * Clase auxiliar para detectar y extraer TMDB ID desde un Map JSON
 * Busca múltiples variantes del nombre del parámetro
 */
object TmdbIdExtractor {
    
    /**
     * Extrae TMDB ID de un JSON Map buscando múltiples variantes de nombres
     * @param jsonMap Map representando el JSON
     * @return TMDB ID encontrado o null
     */
    fun extractFromMap(jsonMap: Map<String, Any>?): String? {
        if (jsonMap == null) return null
        
        // Lista de variantes posibles del parámetro TMDB ID
        val possibleKeys = listOf(
            "tmdb_id",          // Snake case estándar
            "id_tmdb",          // Snake case alternativo
            "tmdbId",           // Camel case
            "idTmdb",           // Camel case alternativo
            "tmdb",             // Corto
            "id",               // Solo ID (último recurso, muy genérico)
            "TMDB_ID",          // Upper case
            "ID_TMDB"           // Upper case alternativo
        )
        
        for (key in possibleKeys) {
            val value = jsonMap[key] as? Any ?: continue
            val tmdbId = sanitizeValue(value) ?: continue
            
            // Log para debugging (comentar en producción si es necesario)
            // Log.d("TmdbIdExtractor", "Found TMDB ID '$tmdbId' using key '$key'")
            
            return tmdbId
        }
        
        return null
    }
    
    /**
     * Convierte y valida un valor a String TMDB ID
     */
    private fun sanitizeValue(value: Any?): String? {
        return when (value) {
            is String -> value.trim().takeIf { it.isNotEmpty() && it != "0" && it.lowercase() != "null" }
            is Number -> value.toString().takeIf { value.toLong() > 0 }
            else -> null
        }
    }
    
    /**
     * Busca parámetros TMDB en un JSON, ignorando variantes de imágenes/iconos
     * @param jsonMap Map representando el JSON
     * @return Map con todos los parámetros TMDB encontrados
     */
    fun extractAllTmdbParams(jsonMap: Map<String, Any>?): Map<String, String> {
        if (jsonMap == null) return emptyMap()
        
        return jsonMap.filter { (key, _) ->
            // Busca claves que contengan "tmdb" pero NO sean de imagen/icono
            key.lowercase().contains("tmdb") && 
            !isImageOrIconKey(key)
        }.mapNotNull { (key, value) ->
            val tmdbId = sanitizeValue(value)
            if (tmdbId != null) key to tmdbId else null
        }.toMap()
    }
    
    /**
     * Verifica si una clave es de imagen o icono (para ignorarla)
     */
    private fun isImageOrIconKey(key: String): Boolean {
        val lowerKey = key.lowercase()
        return lowerKey.contains("ico") || 
               lowerKey.contains("image") || 
               lowerKey.contains("url") ||
               lowerKey.contains("path") ||
               lowerKey.contains("icon")
    }
}
