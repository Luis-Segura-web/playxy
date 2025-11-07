package com.iptv.playxy.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Gson TypeAdapter to convert String/Int/null values to Int
 * Handles null values by returning 0 as default
 */
class SafeIntAdapter : TypeAdapter<Int>() {
    override fun write(out: JsonWriter, value: Int?) {
        out.value(value ?: 0)
    }

    override fun read(`in`: JsonReader): Int {
        return when (`in`.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                0
            }
            JsonToken.NUMBER -> `in`.nextInt()
            JsonToken.STRING -> {
                val value = `in`.nextString()
                try {
                    value.toIntOrNull() ?: 0
                } catch (e: Exception) {
                    0
                }
            }
            else -> {
                `in`.skipValue()
                0
            }
        }
    }
}
