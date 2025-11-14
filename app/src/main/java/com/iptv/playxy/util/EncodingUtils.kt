package com.iptv.playxy.util

import android.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Utility object for encoding/decoding operations
 */
object EncodingUtils {
    
    /**
     * Decode a Base64 encoded string
     * @param encoded The Base64 encoded string
     * @return Decoded string, or original if decoding fails
     */
    fun decodeBase64(encoded: String?): String {
        if (encoded.isNullOrEmpty()) return ""
        
        return try {
            val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encoded
        }
    }
    
    /**
     * Encode a string to Base64
     * @param text The string to encode
     * @return Base64 encoded string
     */
    fun encodeBase64(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        
        return try {
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            text
        }
    }
}
