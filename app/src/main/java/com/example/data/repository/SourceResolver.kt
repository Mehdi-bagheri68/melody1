package com.example.data.repository

import com.example.data.model.AudioQuality
import com.example.data.model.ErrorCategory
import com.example.data.model.PlaybackError
import com.example.data.model.Track
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedicated audio stream resolver & health tracker (Fixes BUG-01, BUG-09, BUG-10).
 *
 * CRITICAL GUARANTEE (BUG-01):
 * Never substitutes an invalid/empty URL with a different song's MP3!
 * Only resolves candidate stream URLs that strictly belong to the exact same Track
 * (e.g., 320kbps and 128kbps variants of the same track). If a track has no valid source URL,
 * returns [StreamResolution.Unavailable] so the player surfaces a clear, honest error.
 */
object SourceResolver {

    data class SourceHealth(
        val host: String,
        val successCount: Int = 0,
        val failureCount: Int = 0,
        val lastSuccessMs: Long = 0L,
        val lastFailureMs: Long = 0L,
        val disabledUntilMs: Long = 0L
    ) {
        val isTemporarilyDisabled: Boolean
            get() = System.currentTimeMillis() < disabledUntilMs
    }

    sealed class StreamResolution {
        data class Available(
            val primaryUrl: String,
            val fallbackSameTrackUrl: String?,
            val hasMultiBitrateVariant: Boolean,
            val effectiveBitrateLabel: String
        ) : StreamResolution()

        data class Unavailable(
            val error: PlaybackError
        ) : StreamResolution()
    }

    private val hostHealthMap = ConcurrentHashMap<String, SourceHealth>()

    fun isValidStreamUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return false
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        // Reject legacy synthetic/placeholder domains so we never play unrelated audio (Fixes BUG-01)
        if (trimmed.contains("soundhelix.com", ignoreCase = true) ||
            trimmed.contains("archive.org", ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    /**
     * Resolves the stream URL for [track] according to [quality] and [iranNoVpnMode].
     * Never returns another song's URL.
     */
    fun resolveTrackStream(
        track: Track,
        quality: AudioQuality,
        iranNoVpnMode: Boolean
    ): StreamResolution {
        val raw = track.previewUrl.trim()
        if (!isValidStreamUrl(raw)) {
            return StreamResolution.Unavailable(
                PlaybackError(
                    code = "SOURCE_UNAVAILABLE",
                    category = ErrorCategory.SOURCE_UNAVAILABLE,
                    recoverable = false,
                    userMessageFa = "منبع صوتی معتبر برای آهنگ «${track.title}» در دسترس نیست.",
                    userMessageEn = "No valid audio source available for \"${track.title}\"."
                )
            )
        }

        val normalizedBase = raw.replace(" ", "%20")

        // Check if this URL is from a blocked international preview host when Iran No-VPN mode is active
        if (iranNoVpnMode && isInternationalBlockedInIran(normalizedBase)) {
            return StreamResolution.Unavailable(
                PlaybackError(
                    code = "VPN_REQUIRED_FOR_SOURCE",
                    category = ErrorCategory.SOURCE_NOT_FOUND,
                    recoverable = false,
                    sourceUrl = normalizedBase,
                    userMessageFa = "این منبع بین‌المللی در حالت بدون فیلترشکن در دسترس نیست.",
                    userMessageEn = "This international source is unreachable in Iran No-VPN mode."
                )
            )
        }

        // Derive same-track bitrate variants if the URL uses explicit (320) or (128) suffixes
        val hasExplicit320 = normalizedBase.contains("(320).mp3", ignoreCase = true) ||
            normalizedBase.contains("%20(320).mp3", ignoreCase = true)
        val hasExplicit128 = normalizedBase.contains("(128).mp3", ignoreCase = true) ||
            normalizedBase.contains("%20(128).mp3", ignoreCase = true)

        return when {
            hasExplicit320 -> {
                val url320 = normalizedBase
                val url128 = normalizedBase
                    .replace("(320).mp3", "(128).mp3", ignoreCase = true)
                    .replace("%20(320).mp3", "%20(128).mp3", ignoreCase = true)
                if (quality == AudioQuality.DATA_SAVER) {
                    StreamResolution.Available(
                        primaryUrl = url128,
                        fallbackSameTrackUrl = url320,
                        hasMultiBitrateVariant = true,
                        effectiveBitrateLabel = "128 kbps"
                    )
                } else {
                    StreamResolution.Available(
                        primaryUrl = url320,
                        fallbackSameTrackUrl = url128,
                        hasMultiBitrateVariant = true,
                        effectiveBitrateLabel = "320 kbps"
                    )
                }
            }
            hasExplicit128 -> {
                val url128 = normalizedBase
                val url320 = normalizedBase
                    .replace("(128).mp3", "(320).mp3", ignoreCase = true)
                    .replace("%20(128).mp3", "%20(320).mp3", ignoreCase = true)
                if (quality == AudioQuality.HIGH) {
                    StreamResolution.Available(
                        primaryUrl = url320,
                        fallbackSameTrackUrl = url128,
                        hasMultiBitrateVariant = true,
                        effectiveBitrateLabel = "320 / 128 kbps"
                    )
                } else {
                    StreamResolution.Available(
                        primaryUrl = url128,
                        fallbackSameTrackUrl = url320,
                        hasMultiBitrateVariant = true,
                        effectiveBitrateLabel = "128 kbps"
                    )
                }
            }
            else -> {
                // Single native stream for this track
                StreamResolution.Available(
                    primaryUrl = normalizedBase,
                    fallbackSameTrackUrl = null,
                    hasMultiBitrateVariant = false,
                    effectiveBitrateLabel = trNativeBitrateLabel(normalizedBase)
                )
            }
        }
    }

    private fun trNativeBitrateLabel(url: String): String {
        return when {
            url.contains("320") -> "320 kbps"
            url.contains("128") -> "128 kbps"
            else -> "Auto (Original)"
        }
    }

    fun isInternationalBlockedInIran(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("itunes.apple.com") ||
            lower.contains("mzstatic.com") ||
            lower.contains("scdn.co")
    }

    fun recordSuccess(url: String) {
        val host = extractHost(url) ?: return
        val existing = hostHealthMap[host] ?: SourceHealth(host = host)
        hostHealthMap[host] = existing.copy(
            successCount = existing.successCount + 1,
            lastSuccessMs = System.currentTimeMillis(),
            disabledUntilMs = 0L
        )
    }

    fun recordFailure(url: String, isHardFailure: Boolean = false) {
        val host = extractHost(url) ?: return
        val now = System.currentTimeMillis()
        val existing = hostHealthMap[host] ?: SourceHealth(host = host)
        val newFailures = existing.failureCount + 1
        val cooldown = if (isHardFailure || newFailures >= 3) now + 60_000L else 0L
        hostHealthMap[host] = existing.copy(
            failureCount = newFailures,
            lastFailureMs = now,
            disabledUntilMs = cooldown
        )
    }

    fun isHostHealthy(host: String): Boolean {
        val health = hostHealthMap[host] ?: return true
        return !health.isTemporarilyDisabled
    }

    fun getHealthSnapshots(): List<SourceHealth> = hostHealthMap.values.toList()

    private fun extractHost(url: String): String? {
        return try {
            val withoutScheme = url.substringAfter("://", "")
            withoutScheme.substringBefore("/").lowercase().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
