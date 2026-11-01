package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val previewUrl: String,
    val genre: String,
    val durationMs: Long,
    val releaseYear: String,
    val countryCode: String,
    val isFavorite: Boolean = false
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val highResArtworkUrl: String
        get() = artworkUrl
            .replace("100x100bb", "600x600bb")
            .replace("60x60bb", "600x600bb")
}

data class Country(
    val code: String,
    val nameFa: String,
    val nameEn: String,
    val flag: String,
    val defaultSearchTerm: String,
    val topArtists: List<String>
)

data class MoodCategory(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val query: String,
    val iconEmoji: String
)

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val trackCount: Int = 0,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AudioQuality(val titleFa: String, val bitrate: String, val descriptionFa: String) {
    HIGH("کیفیت بالا", "320 kbps", "بهترین وضوح صدا با بالاترین پهنای باند"),
    STANDARD("استاندارد", "192 kbps", "تعادل مناسب بین کیفیت و مصرف دیتا"),
    DATA_SAVER("کاهش مصرف دیتا", "96 kbps", "بهینه‌سازی حداکثری برای اتصال ضعیف اینترنت")
}

enum class PlaybackMode {
    NORMAL,
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE
}
