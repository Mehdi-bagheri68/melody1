package com.example.util

import android.text.BidiFormatter
import android.util.Log
import com.example.BuildConfig

/**
 * Structured, privacy-safe logger (Fixes BUG-12 & BUG-13).
 * Only outputs debug/warning logs in debug builds and masks sensitive query tokens.
 */
object AppLogger {
    private const val ROOT_TAG = "MeloGroove"

    fun d(operation: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$ROOT_TAG:$operation", maskSensitive(message))
        }
    }

    fun w(operation: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val errType = throwable?.javaClass?.simpleName ?: "Warning"
            Log.w("$ROOT_TAG:$operation", "[$errType] ${maskSensitive(message)}", throwable)
        }
    }

    fun e(operation: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val errType = throwable?.javaClass?.simpleName ?: "Error"
            Log.e("$ROOT_TAG:$operation", "[$errType] ${maskSensitive(message)}", throwable)
        }
    }

    fun maskSensitive(input: String): String {
        return input
            .replace(Regex("([?&](token|key|auth|sig|signature)=)[^&\\s]+", RegexOption.IGNORE_CASE), "$1***")
            .replace(Regex("(Bearer\\s+)[A-Za-z0-9._\\-]+", RegexOption.IGNORE_CASE), "$1***")
    }
}

/**
 * Bidirectional text helper (Fixes UI-11) for isolating Latin words/numbers inside Persian RTL text.
 */
object BidiHelper {
    fun isolateLtr(text: String): String {
        if (text.isBlank()) return text
        return try {
            BidiFormatter.getInstance(false).unicodeWrap(text)
        } catch (_: Throwable) {
            "\u2066$text\u2069"
        }
    }

    fun formatMixed(isRtl: Boolean, text: String): String {
        if (text.isBlank()) return text
        return try {
            BidiFormatter.getInstance(isRtl).unicodeWrap(text)
        } catch (_: Throwable) {
            text
        }
    }
}
