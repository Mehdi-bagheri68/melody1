package com.example.data.model

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    FA("fa", "فارسی (Persian)", true),
    EN("en", "English", false)
}

enum class ArtistType {
    SOLO,
    BAND,
    COMPOSER
}

data class Artist(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val type: ArtistType = ArtistType.SOLO,
    val countryCode: String = "IR",
    val imageUrl: String? = null,
    val aliases: List<String> = emptyList()
) {
    fun getDisplayName(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) nameFa.ifBlank { nameEn } else nameEn.ifBlank { nameFa }
}

enum class ArtworkKind {
    TRACK_COVER,
    ARTIST_PHOTO,
    PLACEHOLDER
}

data class Artwork(
    val url: String,
    val ownerArtistId: String,
    val kind: ArtworkKind = ArtworkKind.TRACK_COVER
)

sealed interface ArtworkRef {
    data class Image(val url: String) : ArtworkRef
    data class Initials(
        val text: String,
        val seed: String,
        val primaryColorHex: Long,
        val secondaryColorHex: Long
    ) : ArtworkRef
}

enum class GroupKind {
    CURATED,
    SMART
}

enum class GroupSort {
    PLAY_COUNT_DESC,
    RELEASE_YEAR_DESC,
    ADDED_DESC
}

data class GroupRule(
    val countries: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val minYear: Int? = null,
    val sort: GroupSort = GroupSort.PLAY_COUNT_DESC,
    val limit: Int = 15,
    val perArtistCap: Int? = null
)

data class TrackGroup(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String = "",
    val subtitleEn: String = "",
    val kind: GroupKind,
    val rule: GroupRule? = null,
    val trackIds: List<Long> = emptyList(),
    val order: Int = 0,
    val enabled: Boolean = true
) {
    fun getTitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) titleFa else titleEn
    fun getSubtitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) subtitleFa else subtitleEn
}

data class GroupSection(
    val group: TrackGroup,
    val tracks: List<Track>
)

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val previewUrl: String,
    val genre: String = "Pop",
    val durationMs: Long = 210000L,
    val releaseYear: String = "2026",
    val countryCode: String = "IR",
    val isFavorite: Boolean = false,
    val playCount: Int = 100000,
    val sourceId: String = "curated",
    val artistId: String = slugifyArtistId(artist),
    val featuredArtistIds: List<String> = emptyList(),
    val artwork: Artwork? = null
) {
    val durationFormatted: String
        get() {
            if (durationMs <= 0L) return "--:--"
            val totalSeconds = (durationMs / 1000).coerceAtLeast(1)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(java.util.Locale.US, minutes, seconds)
        }

    val highResArtworkUrl: String
        get() = artworkUrl

    companion object {
        fun slugifyArtistId(artistName: String): String {
            val trimmed = artistName.trim()
            if (trimmed.isBlank()) return "unknown-artist"
            val knownSlug = KNOWN_ARTIST_SLUGS[trimmed] ?: KNOWN_ARTIST_SLUGS.entries.firstOrNull {
                it.key.equals(trimmed, ignoreCase = true)
            }?.value
            if (knownSlug != null) return knownSlug

            val normalized = trimmed.lowercase(java.util.Locale.ROOT)
                .replace(Regex("[\\s_]+"), "-")
                .replace(Regex("[^a-z0-9\\u0600-\\u06FF\\-]"), "")
                .trim('-')
            return normalized.ifBlank { "artist-${trimmed.hashCode()}" }
        }

        val KNOWN_ARTIST_SLUGS: Map<String, String> = mapOf(
            "محسن چاوشی" to "mohsen-chavoshi",
            "Mohsen Chavoshi" to "mohsen-chavoshi",
            "همایون شجریان" to "homayoun-shajarian",
            "Homayoun Shajarian" to "homayoun-shajarian",
            "سیروان خسروی" to "sirvan-khosravi",
            "Sirvan Khosravi" to "sirvan-khosravi",
            "بهنام بانی" to "behnam-bani",
            "Behnam Bani" to "behnam-bani",
            "رضا بهرام" to "reza-bahram",
            "Reza Bahram" to "reza-bahram",
            "گرشا رضایی" to "garsha-rezaei",
            "Garsha Rezaei" to "garsha-rezaei",
            "حامیم" to "haamim",
            "Haamim" to "haamim",
            "مجید رضوی" to "majid-razavi",
            "Majid Razavi" to "majid-razavi",
            "فریدون آسرایی" to "fereydoun-asraei",
            "Fereydoun Asraei" to "fereydoun-asraei",
            "محمدرضا شجریان" to "mohammadreza-shajarian",
            "MohammadReza Shajarian" to "mohammadreza-shajarian",
            "عرفان طهماسبی" to "erfan-tahmasbi",
            "Erfan Tahmasbi" to "erfan-tahmasbi",
            "علیرضا قربانی" to "alireza-ghorbani",
            "Alireza Ghorbani" to "alireza-ghorbani",
            "علی لهراسبی" to "ali-lohrasbi",
            "Ali Lohrasbi" to "ali-lohrasbi",
            "محسن یگانه" to "mohsen-yeganeh",
            "Mohsen Yeganeh" to "mohsen-yeganeh",
            "احسان خواجه امیری" to "ehsan-khajeh-amiri",
            "Ehsan Khajeh Amiri" to "ehsan-khajeh-amiri",
            "شادمهر عقیلی" to "shadmehr-aghili",
            "Shadmehr Aghili" to "shadmehr-aghili",
            "Tarkan" to "tarkan",
            "İbrahim Tatlıses" to "ibrahim-tatlises",
            "Emrah" to "emrah",
            "Ramil Sedali" to "ramil-sedali",
            "Mahsun Kırmızıgül" to "mahsun-kirmizigul",
            "Turkish Club DJ" to "turkish-club-dj",
            "The Weeknd" to "the-weeknd",
            "Dr. Dre & Snoop Dogg" to "dr-dre-snoop-dogg",
            "BoyWithUke" to "boywithuke",
            "Eminem" to "eminem",
            "Rema & Selena Gomez" to "rema-selena-gomez",
            "Sean Paul" to "sean-paul",
            "Tiagz" to "tiagz",
            "Ed Sheeran" to "ed-sheeran",
            "Bring Me The Horizon" to "bring-me-the-horizon",
            "D-Block Europe" to "d-block-europe",
            "Sami Yusuf" to "sami-yusuf",
            "London Sessions" to "london-sessions",
            "Gipsy Kings" to "gipsy-kings",
            "Gipsy Kings (France)" to "gipsy-kings",
            "Cris MJ" to "cris-mj",
            "Antonio Banderas" to "antonio-banderas",
            "Koli Band" to "koli-band",
            "Spanish Guitar Masters" to "spanish-guitar-masters",
            "Adriano Celentano" to "adriano-celentano",
            "Paris Piano & Acoustic" to "paris-piano-acoustic",
            "French Touch Electro" to "french-touch-electro",
            "Modern Talking" to "modern-talking",
            "Berlin Euro Electronic" to "berlin-euro-electronic",
            "Dekamond Piano" to "dekamond-piano",
            "German Guitar Sessions" to "german-guitar-sessions"
        )
    }
}

enum class ErrorCategory {
    SOURCE_UNAVAILABLE,
    SOURCE_NOT_FOUND,
    DECODER_FORMAT,
    RATE_LIMITED,
    NETWORK_TRANSIENT,
    NETWORK_DISCONNECTED,
    TIMEOUT,
    DECODE_ERROR,
    UNKNOWN
}

data class PlaybackError(
    val code: String = "ERROR",
    val category: ErrorCategory = ErrorCategory.UNKNOWN,
    val httpStatusCode: Int? = null,
    val sourceUrl: String? = null,
    val recoverable: Boolean = true,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 6,
    val userMessageFa: String = "",
    val userMessageEn: String = "",
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun getMessage(lang: AppLanguage): String = if (lang == AppLanguage.FA) userMessageFa else userMessageEn
}

data class Country(
    val code: String,
    val nameFa: String,
    val nameEn: String,
    val flag: String,
    val defaultSearchTerm: String,
    val topArtists: List<String>
) {
    fun getDisplayName(lang: AppLanguage): String = if (lang == AppLanguage.FA) nameFa else nameEn
}

data class MoodCategory(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val query: String,
    val colorStartHex: Long,
    val colorEndHex: Long,
    val coverUrl: String = "",
    val isNewRelease: Boolean = false
) {
    fun getTitle(lang: AppLanguage): String = getPrimaryTitle(lang)
    fun getPrimaryTitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) titleFa else titleEn
    fun getSecondaryTitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) titleEn else titleFa
}

enum class ArtistSortOption(val labelFa: String, val labelEn: String) {
    TRENDING("پرطرفدارترین (ترند)", "Most Popular (Trending)"),
    NEWEST("جدیدترین (تاریخ انتشار)", "Newest Releases");

    fun getLabel(lang: AppLanguage): String = if (lang == AppLanguage.FA) labelFa else labelEn
    fun getTitle(lang: AppLanguage): String = getLabel(lang)
}

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PlaybackMode(val labelFa: String, val labelEn: String) {
    REPEAT_ALL("پخش پیوسته و تکرار لیست", "Continuous & Repeat Group"),
    REPEAT_ONE("تکرار همین یک آهنگ", "Repeat Single Track"),
    SHUFFLE("پخش تصادفی گروه", "Shuffle Group"),
    NORMAL("پخش ترتیبی (تا انتهای لیست)", "Sequential Play");

    fun getLabel(lang: AppLanguage): String = if (lang == AppLanguage.FA) labelFa else labelEn
    fun getTitle(lang: AppLanguage): String = getLabel(lang)
}

enum class AudioQuality(
    val labelFa: String,
    val labelEn: String,
    val bitrate: String,
    val maxBitrateBps: Int
) {
    HIGH("کیفیت بالا (320kbps)", "High Quality (320kbps)", "320 kbps", 320_000),
    STANDARD("کیفیت استاندارد (192kbps)", "Standard (192kbps)", "192 kbps", 192_000),
    DATA_SAVER("صرفه‌جویی در اینترنت (128kbps)", "Data Saver (128kbps)", "128 kbps", 128_000);

    fun getLabel(lang: AppLanguage): String = if (lang == AppLanguage.FA) labelFa else labelEn
    fun getTitle(lang: AppLanguage): String = getLabel(lang)
}

object CountriesData {
    val iran = Country(
        code = "IR",
        nameFa = "ایران",
        nameEn = "Iran",
        flag = "🇮🇷",
        defaultSearchTerm = "آهنگ جدید ایرانی",
        topArtists = listOf(
            "محسن چاوشی",
            "همایون شجریان",
            "سیروان خسروی",
            "بهنام بانی",
            "رضا بهرام",
            "گرشا رضایی",
            "حامیم",
            "مجید رضوی",
            "عرفان طهماسبی",
            "علیرضا قربانی",
            "محسن یگانه",
            "محمدرضا شجریان",
            "احسان خواجه امیری",
            "فریدون آسرایی",
            "علی لهراسبی"
        )
    )

    val turkey = Country(
        code = "TR",
        nameFa = "ترکیه",
        nameEn = "Turkey",
        flag = "🇹🇷",
        defaultSearchTerm = "Turkish Pop",
        topArtists = listOf("Tarkan", "İbrahim Tatlıses", "Mahsun Kırmızıgül", "Emrah", "Ramil Sedali")
    )

    val usa = Country(
        code = "US",
        nameFa = "آمریکا",
        nameEn = "United States",
        flag = "🇺🇸",
        defaultSearchTerm = "Top Pop Hits",
        topArtists = listOf("The Weeknd", "Eminem", "Dr. Dre & Snoop Dogg", "Rema & Selena Gomez", "Sean Paul", "BoyWithUke")
    )

    val uk = Country(
        code = "GB",
        nameFa = "بریتانیا",
        nameEn = "United Kingdom",
        flag = "🇬🇧",
        defaultSearchTerm = "UK Top Hits",
        topArtists = listOf("Ed Sheeran", "Bring Me The Horizon", "D-Block Europe", "Sami Yusuf")
    )

    val spain = Country(
        code = "ES",
        nameFa = "اسپانیا",
        nameEn = "Spain",
        flag = "🇪🇸",
        defaultSearchTerm = "Top Espana",
        topArtists = listOf("Gipsy Kings", "Cris MJ", "Antonio Banderas", "Koli Band")
    )

    val france = Country(
        code = "FR",
        nameFa = "فرانسه",
        nameEn = "France",
        flag = "🇫🇷",
        defaultSearchTerm = "Chanson Francaise",
        topArtists = listOf("Adriano Celentano", "Gipsy Kings (France)", "Paris Piano & Acoustic", "French Touch Electro")
    )

    val germany = Country(
        code = "DE",
        nameFa = "آلمان",
        nameEn = "Germany",
        flag = "🇩🇪",
        defaultSearchTerm = "Deutsche Hits",
        topArtists = listOf("Modern Talking", "Berlin Euro Electronic", "Dekamond Piano", "German Guitar Sessions")
    )

    val allCountries = listOf(iran, turkey, usa, uk, spain, france, germany)

    /**
     * Fixes N-02: Mood categories NEVER use artist/song cover images (`cover_10XX.jpg`).
     * `coverUrl` is empty so `ModernMoodCard` renders a clean, dedicated mood emblem.
     */
    fun getMoodCategoriesForCountry(country: Country): List<MoodCategory> {
        val code = country.code.uppercase()
        return listOf(
            MoodCategory(
                id = "newest",
                titleFa = "بروزترین‌های ${country.nameFa}",
                titleEn = "New in ${country.nameEn}",
                query = "new_releases",
                colorStartHex = 0xFF7CB342,
                colorEndHex = 0xFF33691E,
                coverUrl = "",
                isNewRelease = true
            ),
            MoodCategory(
                id = "pop",
                titleFa = when (code) {
                    "TR" -> "پاپ ترکی"
                    "US" -> "پاپ و هیت آمریکا"
                    "GB" -> "پاپ بریتانیا"
                    "ES" -> "پاپ لاتین و اسپانیا"
                    "FR" -> "پاپ و شانسون فرانسه"
                    "DE" -> "یوروپاپ آلمان"
                    else -> "پاپ ایرانی"
                },
                titleEn = "${country.nameEn} Pop",
                query = "pop",
                colorStartHex = 0xFFE53935,
                colorEndHex = 0xFF8E0000,
                coverUrl = ""
            ),
            MoodCategory(
                id = "party",
                titleFa = "پلی‌لیست شاد و پارتی",
                titleEn = "Party & Dance",
                query = "party",
                colorStartHex = 0xFFD81B60,
                colorEndHex = 0xFF6A0080,
                coverUrl = ""
            ),
            MoodCategory(
                id = "drive",
                titleFa = "مخصوص رانندگی و جاده",
                titleEn = "Night Drive",
                query = "drive",
                colorStartHex = 0xFFFBC02D,
                colorEndHex = 0xFFF57F17,
                coverUrl = ""
            ),
            MoodCategory(
                id = "chill",
                titleFa = "آرامش و تمرکز",
                titleEn = "Chill & Focus",
                query = "relax",
                colorStartHex = 0xFF1E88E5,
                colorEndHex = 0xFF0D47A1,
                coverUrl = ""
            ),
            MoodCategory(
                id = "traditional",
                titleFa = when (code) {
                    "TR" -> "آرابسک و سنتی ترکی"
                    "US" -> "هیپ‌هاپ و آر اند بی"
                    "GB" -> "راک و آلترناتیو بریتانیا"
                    "ES" -> "گیتار و فلامنکو اسپانیا"
                    "FR" -> "کلاسیک و آکوستیک پاریس"
                    "DE" -> "کلاسیک و گیتار آلمان"
                    else -> "سنتی و اصیل ایرانی"
                },
                titleEn = when (code) {
                    "TR" -> "Arabesk & Folk"
                    "US" -> "Hip-Hop & R&B"
                    "GB" -> "UK Rock & Indie"
                    "ES" -> "Flamenco & Guitar"
                    "FR" -> "French Acoustic"
                    "DE" -> "Classical & Guitar"
                    else -> "Persian Traditional"
                },
                query = "traditional",
                colorStartHex = 0xFFFB8C00,
                colorEndHex = 0xFFBF360C,
                coverUrl = ""
            ),
            MoodCategory(
                id = "workout",
                titleFa = "ورزش و انرژی بالا",
                titleEn = "Workout & Gym",
                query = "workout",
                colorStartHex = 0xFF8E24AA,
                colorEndHex = 0xFF4A148C,
                coverUrl = ""
            ),
            MoodCategory(
                id = "romance",
                titleFa = "عاشقانه و احساسی",
                titleEn = "Romance & Mood",
                query = "romance",
                colorStartHex = 0xFF00ACC1,
                colorEndHex = 0xFF006064,
                coverUrl = ""
            )
        )
    }

    val moodCategories: List<MoodCategory> = getMoodCategoriesForCountry(allCountries.first())

    val genres = listOf(
        "همه" to "",
        "پاپ" to "پاپ",
        "سنتی و اصیل" to "سنتی",
        "شاد و پارتی" to "شاد",
        "راک و هیپ‌هاپ" to "راک",
        "عاشقانه" to "عاشقانه",
        "آرامش و پیانو" to "آرامش"
    )

    val releaseYears = listOf("همه", "2026", "2025", "2024", "2023", "2022")

    fun getCountryByCode(code: String): Country {
        return allCountries.find { it.code.equals(code, ignoreCase = true) } ?: allCountries.first()
    }
}
