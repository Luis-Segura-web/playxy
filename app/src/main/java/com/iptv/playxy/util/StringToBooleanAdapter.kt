package com.iptv.playxy.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Gson TypeAdapter to convert String values "0"/"1" to Boolean
 * Handles null values by returning false as default
 */
class StringToBooleanAdapter : TypeAdapter<Boolean>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        out.value(value ?: false)
    }

    override fun read(`in`: JsonReader): Boolean {
        return when (`in`.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                false
            }
            JsonToken.BOOLEAN -> `in`.nextBoolean()
            JsonToken.STRING -> {
                val value = `in`.nextString()
                when {
                    value.equals("1", ignoreCase = true) -> true
                    value.equals("true", ignoreCase = true) -> true
                    else -> false
                }
            }
            JsonToken.NUMBER -> `in`.nextInt() != 0
            else -> {
                `in`.skipValue()
                false
            }
        }
    }
}
