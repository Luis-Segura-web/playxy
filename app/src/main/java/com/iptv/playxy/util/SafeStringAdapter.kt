package com.iptv.playxy.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Gson TypeAdapter to convert any value to safe String
 * Handles null values by returning empty string or specified default
 */
class SafeStringAdapter : TypeAdapter<String>() {
    override fun write(out: JsonWriter, value: String?) {
        out.value(value ?: "")
    }

    override fun read(`in`: JsonReader): String {
        return when (`in`.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                ""
            }
            JsonToken.STRING -> {
                val value = `in`.nextString()
                if (value.equals("null", ignoreCase = true)) "" else value
            }
            JsonToken.NUMBER -> `in`.nextDouble().toString()
            JsonToken.BOOLEAN -> `in`.nextBoolean().toString()
            else -> {
                `in`.skipValue()
                ""
            }
        }
    }
}
