package com.example.util

import java.security.MessageDigest
import java.nio.ByteBuffer

/**
 * Fixes N-07: Replaces 32-bit `String.hashCode()` with deterministic 64-bit SHA-256 truncation
 * to prevent ID collisions across scraped and remote tracks.
 */
object IdGenerator {

    fun stable64BitId(namespace: String, rawKey: String, offset: Long = 10_000_000L): Long {
        val canonical = "${namespace.trim().lowercase()}:${rawKey.trim()}"
        return try {
            val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            val rawLong = ByteBuffer.wrap(digest, 0, 8).long
            val positive = (rawLong and Long.MAX_VALUE) % 9_000_000_000_000L
            offset + positive
        } catch (_: Exception) {
            // FNV-1a 64-bit fallback
            var hash = -0x340d631b7bdddcdbL
            for (ch in canonical) {
                hash = hash xor ch.code.toLong()
                hash *= 0x100000001b3L
            }
            offset + ((hash and Long.MAX_VALUE) % 9_000_000_000_000L)
        }
    }
}
