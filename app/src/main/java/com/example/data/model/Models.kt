package com.example.data.model

enum class AppLanguage(val code: String, val nativeName: String, val isRtl: Boolean) {
    FA("fa", "فارسی (Persian)", true),
    EN("en", "English", false)
}

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
    val isFavorite: Boolean = false,
    val playCount: Int = 1000,
    val sourceId: String = "curated"
) {
    // Fixes BUG-05 & UI-08: Return "--:--" when durationMs <= 0 (unknown duration until Player prepares)
    val durationFormatted: String
        get() {
            if (durationMs <= 0L) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val highResArtworkUrl: String
        get() = artworkUrl
            .replace("100x100bb", "600x600bb")
            .replace("60x60bb", "600x600bb")

    val hasBundledArtwork: Boolean
        get() = artworkUrl.startsWith("file:///android_asset/covers/")
}

/**
 * Structured error categorization for playback & network failures (Fixes BUG-01, BUG-02, BUG-13).
 */
enum class ErrorCategory {
    SOURCE_UNAVAILABLE, // Empty or invalid URL -> non-recoverable, never substitute a different song
    SOURCE_NOT_FOUND,   // HTTP 401, 403, 404, 410 -> non-recoverable
    RATE_LIMITED,       // HTTP 429 -> backoff with Retry-After
    NETWORK_TRANSIENT,  // Timeout, DNS, Connection drop, 5xx -> exponential backoff up to maxAttempts
    DECODER_FORMAT,     // Media decoder / container format error -> non-recoverable
    UNKNOWN
}

data class PlaybackError(
    val code: String,
    val category: ErrorCategory,
    val httpStatusCode: Int? = null,
    val sourceUrl: String = "",
    val recoverable: Boolean,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 6,
    val userMessageFa: String,
    val userMessageEn: String,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun getMessage(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) userMessageFa else userMessageEn
}

data class Country(
    val code: String,
    val nameFa: String,
    val nameEn: String,
    val flag: String,
    val defaultSearchTerm: String,
    val topArtists: List<String>
) {
    fun getDisplayName(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) nameFa else nameEn
}

data class MoodCategory(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val query: String,
    val colorStartHex: Long,
    val colorEndHex: Long,
    val coverUrl: String,
    val isNewRelease: Boolean = false
) {
    fun getPrimaryTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) titleFa else titleEn

    fun getSecondaryTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) titleEn else titleFa
}

enum class ArtistSortOption(val titleFa: String, val titleEn: String) {
    TRENDING("پرطرفدارترین (ترند)", "Trending"),
    NEWEST("جدیدترین (سال تولید)", "Newest (Release Year)");

    fun getTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) titleFa else titleEn
}

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val trackCount: Int = 0,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AudioQuality(
    val titleFa: String,
    val titleEn: String,
    val bitrate: String,
    val maxBitrateBps: Int,
    val descriptionFa: String,
    val descriptionEn: String
) {
    HIGH(
        "کیفیت بالا",
        "High Quality",
        "320 kbps",
        320_000,
        "پخش استریم ۳۲۰ کیلوبیت در صورت پشتیبانی منبع",
        "Streams 320 kbps source variant when available"
    ),
    STANDARD(
        "استاندارد",
        "Standard",
        "192 kbps",
        192_000,
        "تعادل بین کیفیت صدا و مصرف اینترنت",
        "Balanced audio quality and data consumption"
    ),
    DATA_SAVER(
        "کاهش مصرف دیتا",
        "Data Saver",
        "128 kbps",
        128_000,
        "انتخاب خودکار نسخه کم‌حجم ۱۲۸ کیلوبیت برای اینترنت ضعیف",
        "Selects 128 kbps lightweight stream for slow networks"
    );

    fun getTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) titleFa else titleEn

    fun getDescription(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) descriptionFa else descriptionEn
}

enum class PlaybackMode(val titleFa: String, val titleEn: String) {
    REPEAT_ALL("تکرار گروه/لیست", "Repeat Group"),
    REPEAT_ONE("تکرار ۱ آهنگ", "Repeat One"),
    SHUFFLE("پخش تصادفی", "Shuffle"),
    NORMAL("پخش عادی", "Normal");

    fun getTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) titleFa else titleEn
}
