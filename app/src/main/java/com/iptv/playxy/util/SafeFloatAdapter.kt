package com.iptv.playxy.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Moshi JsonAdapter to convert String/Int/null values to Float
 * Handles null values by returning 0f as default
 */
class SafeFloatAdapter : JsonAdapter<Float>() {
    override fun toJson(writer: JsonWriter, value: Float?) {
        writer.value(value?.toDouble() ?: 0.0)
    }

    override fun fromJson(reader: JsonReader): Float {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                0f
            }
            JsonReader.Token.NUMBER -> reader.nextDouble().toFloat()
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                try {
                    value.toFloatOrNull() ?: 0f
                } catch (e: Exception) {
                    0f
                }
            }
            else -> {
                reader.skipValue()
                0f
            }
        }
    }
}
