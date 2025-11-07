package com.iptv.playxy.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Gson TypeAdapter to convert String/Int/null values to Float
 * Handles null values by returning 0f as default
 */
class SafeFloatAdapter : TypeAdapter<Float>() {
    override fun write(out: JsonWriter, value: Float?) {
        out.value(value ?: 0f)
    }

    override fun read(`in`: JsonReader): Float {
        return when (`in`.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                0f
            }
            JsonToken.NUMBER -> `in`.nextDouble().toFloat()
            JsonToken.STRING -> {
                val value = `in`.nextString()
                try {
                    value.toFloatOrNull() ?: 0f
                } catch (e: Exception) {
                    0f
                }
            }
            else -> {
                `in`.skipValue()
                0f
            }
        }
    }
}
