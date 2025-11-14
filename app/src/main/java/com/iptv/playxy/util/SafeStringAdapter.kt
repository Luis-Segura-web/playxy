package com.iptv.playxy.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Moshi JsonAdapter to convert any value to safe String
 * Handles null values by returning empty string or specified default
 */
class SafeStringAdapter : JsonAdapter<String>() {
    override fun toJson(writer: JsonWriter, value: String?) {
        writer.value(value ?: "")
    }

    override fun fromJson(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                ""
            }
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                if (value.equals("null", ignoreCase = true)) "" else value
            }
            JsonReader.Token.NUMBER -> reader.nextDouble().toString()
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            else -> {
                reader.skipValue()
                ""
            }
        }
    }
}
