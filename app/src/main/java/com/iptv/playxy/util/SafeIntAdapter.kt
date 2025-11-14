package com.iptv.playxy.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Moshi JsonAdapter to convert String/Int/null values to Int
 * Handles null values by returning 0 as default
 */
class SafeIntAdapter : JsonAdapter<Int>() {
    override fun toJson(writer: JsonWriter, value: Int?) {
        writer.value(value ?: 0)
    }

    override fun fromJson(reader: JsonReader): Int {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                0
            }
            JsonReader.Token.NUMBER -> reader.nextInt()
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                try {
                    value.toIntOrNull() ?: 0
                } catch (e: Exception) {
                    0
                }
            }
            else -> {
                reader.skipValue()
                0
            }
        }
    }
}
