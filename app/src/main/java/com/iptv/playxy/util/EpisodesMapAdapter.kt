package com.iptv.playxy.util

import com.iptv.playxy.data.api.EpisodeResponse
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Custom Moshi adapter para deserializar el campo "episodes" en SeriesInfoResponse.
 * 
 * El problema: Los proveedores IPTV devuelven "episodes" en diferentes formatos:
 * 
 * 1. Objeto/Map (formato estándar): {"1": [...], "2": [...]}
 * 2. Array de arrays: [[episodios_temp_0], [episodios_temp_1]]
 * 3. Array vacío: []
 * 4. null
 * 
 * Este adapter convierte todos los formatos a Map<String, List<EpisodeResponse>>
 * donde la clave es el número de temporada extraído del campo "season" de cada episodio.
 */
class EpisodesMapAdapter {
    
    @FromJson
    fun fromJson(
        reader: JsonReader,
        episodeListAdapter: com.squareup.moshi.JsonAdapter<List<EpisodeResponse>>
    ): Map<String, List<EpisodeResponse>>? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                // Es un array - puede ser vacío o array de arrays de episodios
                parseArrayFormat(reader, episodeListAdapter)
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                // Es un objeto/Map, lo leemos normalmente
                parseObjectFormat(reader, episodeListAdapter)
            }
            else -> {
                // Cualquier otro tipo, lo ignoramos y devolvemos null
                reader.skipValue()
                null
            }
        }
    }
    
    /**
     * Parsea el formato array: [[episodios_temp_0], [episodios_temp_1], ...]
     * Cada sub-array contiene los episodios de una temporada.
     * Extrae el número de temporada del campo "season" de cada episodio.
     */
    private fun parseArrayFormat(
        reader: JsonReader,
        episodeListAdapter: com.squareup.moshi.JsonAdapter<List<EpisodeResponse>>
    ): Map<String, List<EpisodeResponse>> {
        val result = mutableMapOf<String, MutableList<EpisodeResponse>>()
        
        reader.beginArray()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.BEGIN_ARRAY -> {
                    // Sub-array de episodios
                    val episodes = episodeListAdapter.fromJson(reader) ?: emptyList()
                    for (episode in episodes) {
                        // Extraer temporada del episodio
                        val seasonKey = extractSeasonKey(episode)
                        result.getOrPut(seasonKey) { mutableListOf() }.add(episode)
                    }
                }
                JsonReader.Token.BEGIN_OBJECT -> {
                    // Episodio individual directamente en el array (formato inusual)
                    reader.skipValue()
                }
                else -> {
                    reader.skipValue()
                }
            }
        }
        reader.endArray()
        
        return result
    }
    
    /**
     * Parsea el formato objeto: {"1": [...], "2": [...]}
     */
    private fun parseObjectFormat(
        reader: JsonReader,
        episodeListAdapter: com.squareup.moshi.JsonAdapter<List<EpisodeResponse>>
    ): Map<String, List<EpisodeResponse>> {
        val result = mutableMapOf<String, List<EpisodeResponse>>()
        
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val episodes = when (reader.peek()) {
                JsonReader.Token.NULL -> {
                    reader.nextNull<Unit>()
                    emptyList()
                }
                JsonReader.Token.BEGIN_ARRAY -> {
                    episodeListAdapter.fromJson(reader) ?: emptyList()
                }
                else -> {
                    reader.skipValue()
                    emptyList()
                }
            }
            result[key] = episodes
        }
        reader.endObject()
        
        return result
    }
    
    /**
     * Extrae la clave de temporada de un episodio.
     * Prioriza el campo "season" del episodio, luego "info.season".
     * Si no hay, usa "0" como default.
     */
    private fun extractSeasonKey(episode: EpisodeResponse): String {
        // Intentar obtener del campo season del episodio
        val seasonFromEpisode = episode.season?.toString()?.trim()
        if (!seasonFromEpisode.isNullOrEmpty() && seasonFromEpisode != "null") {
            return seasonFromEpisode
        }
        
        // Intentar obtener del info.season
        val seasonFromInfo = episode.info?.season?.toString()?.trim()
        if (!seasonFromInfo.isNullOrEmpty() && seasonFromInfo != "null") {
            return seasonFromInfo
        }
        
        // Default: temporada 0 (especiales)
        return "0"
    }
    
    @ToJson
    fun toJson(
        writer: JsonWriter,
        value: Map<String, List<EpisodeResponse>>?,
        episodeListAdapter: com.squareup.moshi.JsonAdapter<List<EpisodeResponse>>
    ) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.beginObject()
            for ((key, episodes) in value) {
                writer.name(key)
                episodeListAdapter.toJson(writer, episodes)
            }
            writer.endObject()
        }
    }
}
