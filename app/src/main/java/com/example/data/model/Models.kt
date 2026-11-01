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
    val playCount: Int = 1000
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
    val descriptionFa: String,
    val descriptionEn: String
) {
    HIGH(
        "کیفیت بالا",
        "High Quality",
        "320 kbps",
        "بهترین وضوح صدا با بالاترین پهنای باند",
        "Best audio clarity with highest bandwidth"
    ),
    STANDARD(
        "استاندارد",
        "Standard",
        "192 kbps",
        "تعادل مناسب بین کیفیت و مصرف دیتا",
        "Balanced audio quality and data usage"
    ),
    DATA_SAVER(
        "کاهش مصرف دیتا",
        "Data Saver",
        "96 kbps",
        "بهینه‌سازی حداکثری برای اتصال ضعیف اینترنت",
        "Optimized for slow network connections"
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
