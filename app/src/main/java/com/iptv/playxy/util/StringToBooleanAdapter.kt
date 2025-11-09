package com.iptv.playxy.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Moshi JsonAdapter to convert String values "0"/"1" to Boolean
 * Handles null values by returning false as default
 */
class StringToBooleanAdapter : JsonAdapter<Boolean>() {
    override fun toJson(writer: JsonWriter, value: Boolean?) {
        writer.value(value ?: false)
    }

    override fun fromJson(reader: JsonReader): Boolean {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                false
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                when {
                    value.equals("1", ignoreCase = true) -> true
                    value.equals("true", ignoreCase = true) -> true
                    else -> false
                }
            }
            JsonReader.Token.NUMBER -> reader.nextInt() != 0
            else -> {
                reader.skipValue()
                false
            }
        }
    }
}
