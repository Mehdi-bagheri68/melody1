package com.example.data.repository

import android.content.Context
import com.example.data.model.Artist
import com.example.data.model.ArtistType
import com.example.data.model.Artwork
import com.example.data.model.ArtworkKind
import com.example.data.model.GroupKind
import com.example.data.model.GroupRule
import com.example.data.model.GroupSort
import com.example.data.model.Track
import com.example.data.model.TrackGroup
import com.example.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fixes N-01 & N-03 (Section 7.5):
 * Loads externalized `artists.json`, `tracks.json`, and `groups.json` from `assets/catalog/`
 * and guarantees strict 1-to-1 artwork ownership (`Artwork.ownerArtistId == Track.artistId`).
 */
object CatalogDataSource {

    @Volatile
    private var cachedArtists: List<Artist>? = null

    @Volatile
    private var cachedTracks: List<Track>? = null

    @Volatile
    private var cachedGroups: List<TrackGroup>? = null

    fun initialize(context: Context) {
        try {
            val artistsJson = context.assets.open("catalog/artists.json").bufferedReader().use { it.readText() }
            val tracksJson = context.assets.open("catalog/tracks.json").bufferedReader().use { it.readText() }
            val groupsJson = context.assets.open("catalog/groups.json").bufferedReader().use { it.readText() }

            cachedArtists = parseArtistsJson(artistsJson)
            cachedTracks = parseTracksJson(tracksJson)
            cachedGroups = parseGroupsJson(groupsJson)
        } catch (e: Exception) {
            AppLogger.w("CatalogDataSource", "Falling back to built-in catalog definitions", e)
        }
    }

    fun getArtists(): List<Artist> = cachedArtists ?: defaultArtists()

    fun getArtistsById(): Map<String, Artist> = getArtists().associateBy { it.id }

    fun getTracks(): List<Track> = cachedTracks ?: defaultTracks()

    fun getGroups(): List<TrackGroup> = cachedGroups ?: defaultGroups()

    fun getDefaultNewReleasesGroup(countryCode: String? = null): TrackGroup {
        val base = getGroups().find { it.id == "new-releases" } ?: defaultGroups().first()
        val countries = if (countryCode.isNullOrBlank()) emptySet() else setOf(countryCode.uppercase())
        return base.copy(
            rule = (base.rule ?: GroupRule(sort = GroupSort.RELEASE_YEAR_DESC, limit = 12, perArtistCap = 1)).copy(
                countries = countries,
                perArtistCap = 1
            )
        )
    }

    fun getDefaultTrendingGroup(countryCode: String? = null): TrackGroup {
        val base = getGroups().find { it.id == "trending" } ?: defaultGroups()[1]
        val countries = if (countryCode.isNullOrBlank()) emptySet() else setOf(countryCode.uppercase())
        return base.copy(
            rule = (base.rule ?: GroupRule(sort = GroupSort.PLAY_COUNT_DESC, limit = 15, perArtistCap = 2)).copy(
                countries = countries
            )
        )
    }

    internal fun parseArtistsJson(json: String): List<Artist> {
        val arr = JSONArray(json)
        val list = ArrayList<Artist>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optString("id")
            val rawImageUrl = if (obj.isNull("imageUrl")) null else obj.optString("imageUrl").takeIf { it.isNotBlank() }
            val verifiedImageUrl = rawImageUrl?.takeIf { url ->
                val owner = ArtworkResolver.BUNDLED_COVER_OWNERS[url]
                owner == null || owner == id
            }
            val aliasesArr = obj.optJSONArray("aliases")
            val aliases = mutableListOf<String>()
            if (aliasesArr != null) {
                for (j in 0 until aliasesArr.length()) {
                    aliases.add(aliasesArr.getString(j))
                }
            }
            val typeStr = obj.optString("type", "SOLO")
            val type = try {
                ArtistType.valueOf(typeStr)
            } catch (_: Exception) {
                ArtistType.SOLO
            }
            list.add(
                Artist(
                    id = id,
                    nameFa = obj.optString("nameFa"),
                    nameEn = obj.optString("nameEn"),
                    type = type,
                    countryCode = obj.optString("countryCode", "IR"),
                    imageUrl = verifiedImageUrl,
                    aliases = aliases
                )
            )
        }
        return list
    }

    internal fun parseTracksJson(json: String): List<Track> {
        val arr = JSONArray(json)
        val list = ArrayList<Track>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val artistName = obj.optString("artist")
            val artistId = obj.optString("artistId").ifBlank { Track.slugifyArtistId(artistName) }
            val rawArtworkUrl = obj.optString("artworkUrl", "").trim()
            val rawOwnerId = obj.optString("ownerArtistId", "").trim()

            // Strict ownership check: only attach artwork if owned by this track's artistId
            val ownedArtwork = if (rawArtworkUrl.isNotBlank() && rawOwnerId == artistId) {
                ArtworkResolver.buildOwnedArtworkOrNull(rawArtworkUrl, artistId)
            } else {
                null
            }

            list.add(
                Track(
                    id = obj.getLong("id"),
                    title = obj.optString("title"),
                    artist = artistName,
                    album = obj.optString("album", "Single"),
                    artworkUrl = ownedArtwork?.url ?: "",
                    previewUrl = obj.optString("previewUrl"),
                    genre = obj.optString("genre", "Pop"),
                    durationMs = obj.optLong("durationMs", 210000L),
                    releaseYear = obj.optString("releaseYear", "2026"),
                    countryCode = obj.optString("countryCode", "IR"),
                    playCount = obj.optInt("playCount", 100000),
                    sourceId = "curated",
                    artistId = artistId,
                    artwork = ownedArtwork
                )
            )
        }
        return list
    }

    internal fun parseGroupsJson(json: String): List<TrackGroup> {
        val arr = JSONArray(json)
        val list = ArrayList<TrackGroup>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val kind = try {
                GroupKind.valueOf(obj.optString("kind", "SMART"))
            } catch (_: Exception) {
                GroupKind.SMART
            }
            val ruleObj: JSONObject? = obj.optJSONObject("rule")
            val rule = if (ruleObj != null) {
                val countriesArr = ruleObj.optJSONArray("countries")
                val countries = mutableSetOf<String>()
                if (countriesArr != null) {
                    for (j in 0 until countriesArr.length()) countries.add(countriesArr.getString(j))
                }
                val genresArr = ruleObj.optJSONArray("genres")
                val genres = mutableSetOf<String>()
                if (genresArr != null) {
                    for (j in 0 until genresArr.length()) genres.add(genresArr.getString(j))
                }
                val minYear = if (ruleObj.isNull("minYear")) null else ruleObj.optInt("minYear")
                val perArtistCap = if (ruleObj.isNull("perArtistCap")) null else ruleObj.optInt("perArtistCap")
                val sort = try {
                    GroupSort.valueOf(ruleObj.optString("sort", "PLAY_COUNT_DESC"))
                } catch (_: Exception) {
                    GroupSort.PLAY_COUNT_DESC
                }
                GroupRule(
                    countries = countries,
                    genres = genres,
                    minYear = minYear,
                    sort = sort,
                    limit = ruleObj.optInt("limit", 15),
                    perArtistCap = perArtistCap
                )
            } else null

            val trackIdsArr = obj.optJSONArray("trackIds")
            val trackIds = mutableListOf<Long>()
            if (trackIdsArr != null) {
                for (j in 0 until trackIdsArr.length()) {
                    trackIds.add(trackIdsArr.getLong(j))
                }
            }

            list.add(
                TrackGroup(
                    id = obj.optString("id"),
                    titleFa = obj.optString("titleFa"),
                    titleEn = obj.optString("titleEn"),
                    subtitleFa = obj.optString("subtitleFa", ""),
                    subtitleEn = obj.optString("subtitleEn", ""),
                    kind = kind,
                    rule = rule,
                    trackIds = trackIds,
                    order = obj.optInt("order", i),
                    enabled = obj.optBoolean("enabled", true)
                )
            )
        }
        return list
    }

    fun defaultGroups(): List<TrackGroup> = listOf(
        TrackGroup(
            id = "new-releases",
            titleFa = "بروزترین انتشارها",
            titleEn = "Just Released",
            subtitleFa = "جدیدترین آهنگ‌های منتشر شده (حداکثر ۱ آهنگ از هر خواننده)",
            subtitleEn = "Latest songs sorted by release year (1 per artist)",
            kind = GroupKind.SMART,
            rule = GroupRule(
                minYear = 2024,
                sort = GroupSort.RELEASE_YEAR_DESC,
                limit = 12,
                perArtistCap = 1
            ),
            order = 1,
            enabled = true
        ),
        TrackGroup(
            id = "trending",
            titleFa = "پرطرفدارها و ترند",
            titleEn = "Trending & Popular",
            subtitleFa = "محبوب‌ترین آهنگ‌های روز • برای آرشیو خواننده روی نام او بزنید",
            subtitleEn = "Most popular tracks • Tap artist name for full discography",
            kind = GroupKind.SMART,
            rule = GroupRule(
                sort = GroupSort.PLAY_COUNT_DESC,
                limit = 15,
                perArtistCap = 2
            ),
            order = 2,
            enabled = true
        ),
        TrackGroup(
            id = "persian-traditional-gems",
            titleFa = "گنجینه سنتی و اصیل",
            titleEn = "Persian Traditional Gems",
            subtitleFa = "آثار ماندگار شجریان، همایون و علیرضا قربانی",
            subtitleEn = "Timeless classical and traditional masterpieces",
            kind = GroupKind.CURATED,
            trackIds = listOf(1015L, 1006L, 1003L, 1005L, 1007L, 1008L, 1012L),
            order = 3,
            enabled = true
        )
    )

    fun defaultArtists(): List<Artist> = listOf(
        Artist("mohsen-chavoshi", "محسن چاوشی", "Mohsen Chavoshi", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1013.jpg", listOf("محسن چاوشی", "Mohsen Chavoshi")),
        Artist("homayoun-shajarian", "همایون شجریان", "Homayoun Shajarian", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1015.jpg", listOf("همایون شجریان", "Homayoun Shajarian")),
        Artist("sirvan-khosravi", "سیروان خسروی", "Sirvan Khosravi", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1016.jpg", listOf("سیروان خسروی", "Sirvan Khosravi")),
        Artist("behnam-bani", "بهنام بانی", "Behnam Bani", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1017.jpg", listOf("بهنام بانی", "Behnam Bani")),
        Artist("reza-bahram", "رضا بهرام", "Reza Bahram", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1023.jpg", listOf("رضا بهرام", "Reza Bahram")),
        Artist("garsha-rezaei", "گرشا رضایی", "Garsha Rezaei", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1024.jpg", listOf("گرشا رضایی", "Garsha Rezaei")),
        Artist("haamim", "حامیم", "Haamim", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1018.jpg", listOf("حامیم", "Haamim")),
        Artist("majid-razavi", "مجید رضوی", "Majid Razavi", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1019.jpg", listOf("مجید رضوی", "Majid Razavi")),
        Artist("fereydoun-asraei", "فریدون آسرایی", "Fereydoun Asraei", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1025.jpg", listOf("فریدون آسرایی", "Fereydoun Asraei")),
        Artist("mohammadreza-shajarian", "محمدرضا شجریان", "MohammadReza Shajarian", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1006.jpg", listOf("محمدرضا شجریان", "MohammadReza Shajarian")),
        Artist("erfan-tahmasbi", "عرفان طهماسبی", "Erfan Tahmasbi", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1014.jpg", listOf("عرفان طهماسبی", "Erfan Tahmasbi")),
        Artist("alireza-ghorbani", "علیرضا قربانی", "Alireza Ghorbani", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1005.jpg", listOf("علیرضا قربانی", "Alireza Ghorbani")),
        Artist("ali-lohrasbi", "علی لهراسبی", "Ali Lohrasbi", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1021.jpg", listOf("علی لهراسبی", "Ali Lohrasbi")),
        Artist("mohsen-yeganeh", "محسن یگانه", "Mohsen Yeganeh", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1001.jpg", listOf("محسن یگانه", "Mohsen Yeganeh")),
        Artist("ehsan-khajeh-amiri", "احسان خواجه امیری", "Ehsan Khajeh Amiri", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1020.jpg", listOf("احسان خواجه امیری", "Ehsan Khajeh Amiri")),
        Artist("shadmehr-aghili", "شادمهر عقیلی", "Shadmehr Aghili", ArtistType.SOLO, "IR", "file:///android_asset/covers/cover_1022.jpg", listOf("شادمهر عقیلی", "Shadmehr Aghili")),
        Artist("tarkan", "تارکان", "Tarkan", ArtistType.SOLO, "TR", null, listOf("Tarkan")),
        Artist("ibrahim-tatlises", "ابراهیم تاتلیسس", "İbrahim Tatlıses", ArtistType.SOLO, "TR", null, listOf("İbrahim Tatlıses")),
        Artist("emrah", "امراه", "Emrah", ArtistType.SOLO, "TR", null, listOf("Emrah")),
        Artist("ramil-sedali", "رامیل سدالی", "Ramil Sedali", ArtistType.SOLO, "TR", null, listOf("Ramil Sedali")),
        Artist("mahsun-kirmizigul", "ماهسون کیرمیزی‌گول", "Mahsun Kırmızıgül", ArtistType.SOLO, "TR", null, listOf("Mahsun Kırmızıgül")),
        Artist("turkish-club-dj", "کلاب ترکی", "Turkish Club DJ", ArtistType.BAND, "TR", null, listOf("Turkish Club DJ")),
        Artist("the-weeknd", "د ویکند", "The Weeknd", ArtistType.SOLO, "US", null, listOf("The Weeknd")),
        Artist("dr-dre-snoop-dogg", "دکتر دری و اسنوپ داگ", "Dr. Dre & Snoop Dogg", ArtistType.BAND, "US", null, listOf("Dr. Dre & Snoop Dogg")),
        Artist("boywithuke", "بوی‌ویت‌یوک", "BoyWithUke", ArtistType.SOLO, "US", null, listOf("BoyWithUke")),
        Artist("eminem", "امینم", "Eminem", ArtistType.SOLO, "US", null, listOf("Eminem")),
        Artist("rema-selena-gomez", "رما و سلنا گومز", "Rema & Selena Gomez", ArtistType.BAND, "US", null, listOf("Rema & Selena Gomez")),
        Artist("sean-paul", "شان پال", "Sean Paul", ArtistType.SOLO, "US", null, listOf("Sean Paul")),
        Artist("tiagz", "تیاگز", "Tiagz", ArtistType.SOLO, "US", null, listOf("Tiagz")),
        Artist("ed-sheeran", "اد شیرن", "Ed Sheeran", ArtistType.SOLO, "GB", null, listOf("Ed Sheeran")),
        Artist("bring-me-the-horizon", "برینگ می د هورایزن", "Bring Me The Horizon", ArtistType.BAND, "GB", null, listOf("Bring Me The Horizon")),
        Artist("d-block-europe", "دی‌بلاک یوروپ", "D-Block Europe", ArtistType.BAND, "GB", null, listOf("D-Block Europe")),
        Artist("sami-yusuf", "سامی یوسف", "Sami Yusuf", ArtistType.SOLO, "GB", null, listOf("Sami Yusuf")),
        Artist("london-sessions", "لندن سشنز", "London Sessions", ArtistType.COMPOSER, "GB", null, listOf("London Sessions")),
        Artist("gipsy-kings", "جیپسی کینگز", "Gipsy Kings", ArtistType.BAND, "ES", null, listOf("Gipsy Kings", "Gipsy Kings (France)")),
        Artist("cris-mj", "کریس ام‌جی", "Cris MJ", ArtistType.SOLO, "ES", null, listOf("Cris MJ")),
        Artist("antonio-banderas", "آنتونیو باندراس", "Antonio Banderas", ArtistType.SOLO, "ES", null, listOf("Antonio Banderas")),
        Artist("koli-band", "گروه کولی", "Koli Band", ArtistType.BAND, "ES", null, listOf("Koli Band")),
        Artist("spanish-guitar-masters", "اساتید گیتار اسپانیا", "Spanish Guitar Masters", ArtistType.COMPOSER, "ES", null, listOf("Spanish Guitar Masters")),
        Artist("adriano-celentano", "آدریانو چلنتانو", "Adriano Celentano", ArtistType.SOLO, "FR", null, listOf("Adriano Celentano")),
        Artist("paris-piano-acoustic", "پیانو و آکوستیک پاریس", "Paris Piano & Acoustic", ArtistType.COMPOSER, "FR", null, listOf("Paris Piano & Acoustic")),
        Artist("french-touch-electro", "فرنچ تاچ الکترو", "French Touch Electro", ArtistType.BAND, "FR", null, listOf("French Touch Electro")),
        Artist("modern-talking", "مدرن تاکینگ", "Modern Talking", ArtistType.BAND, "DE", null, listOf("Modern Talking")),
        Artist("berlin-euro-electronic", "برلین یورو الکترونیک", "Berlin Euro Electronic", ArtistType.BAND, "DE", null, listOf("Berlin Euro Electronic")),
        Artist("dekamond-piano", "دکاموند پیانو", "Dekamond Piano", ArtistType.COMPOSER, "DE", null, listOf("Dekamond Piano")),
        Artist("german-guitar-sessions", "گیتار سشنز آلمان", "German Guitar Sessions", ArtistType.COMPOSER, "DE", null, listOf("German Guitar Sessions"))
    )

    private fun ownedTrack(
        id: Long,
        title: String,
        artist: String,
        artistId: String,
        album: String,
        ownedCoverFile: String?,
        previewUrl: String,
        genre: String,
        durationMs: Long,
        releaseYear: String,
        countryCode: String,
        playCount: Int
    ): Track {
        val artwork = if (!ownedCoverFile.isNullOrBlank()) {
            Artwork(url = ownedCoverFile, ownerArtistId = artistId, kind = ArtworkKind.TRACK_COVER)
        } else null
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artwork?.url ?: "",
            previewUrl = previewUrl,
            genre = genre,
            durationMs = durationMs,
            releaseYear = releaseYear,
            countryCode = countryCode,
            playCount = playCount,
            sourceId = "curated",
            artistId = artistId,
            artwork = artwork
        )
    }

    fun defaultTracks(): List<Track> = listOf(
        // IRAN (Each cover_10XX.jpg is strictly owned by that exact artistId)
        ownedTrack(1013L, "رود سوخته", "محسن چاوشی", "mohsen-chavoshi", "تک‌آهنگ جدید ۲۰۲۶", "file:///android_asset/covers/cover_1013.jpg", "https://dls.musics-fa.com/song/alibz/2026/08/Mohsen%20Chavoshi%20-%20Roode%20Sookhte%20Musics-Fa.mp3", "پاپ راک ایرانی", 231000L, "2026", "IR", 148000),
        ownedTrack(1015L, "می‌تراود مهتاب", "همایون شجریان", "homayoun-shajarian", "تک‌آهنگ جدید ۲۰۲۶", "file:///android_asset/covers/cover_1015.jpg", "https://dls.musics-fa.com/song/alibz/2026/10/Homayoun%20Shajarian%20-%20Mitaravad%20Mahtab%20Musics-Fa.mp3", "سنتی و اصیل", 264000L, "2026", "IR", 142000),
        ownedTrack(1016L, "میدونم اشتباس", "سیروان خسروی", "sirvan-khosravi", "تک‌آهنگ جدید ۲۰۲۶", "file:///android_asset/covers/cover_1016.jpg", "https://dls.musics-fa.com/song/alibz/2026/10/Sirvan%20Khosravi%20-%20Midoonam%20Eshtebas%20Musics-Fa.mp3", "پاپ مدرن", 228000L, "2026", "IR", 136500),
        ownedTrack(1017L, "چشات میگه", "بهنام بانی", "behnam-bani", "تک‌آهنگ جدید ۲۰۲۶", "file:///android_asset/covers/cover_1017.jpg", "https://dls.musics-fa.com/song/alibz/2026/10/Behnam%20Bani%20-%20Cheshat%20Mige%20Musics-Fa.mp3", "پاپ و شاد", 198000L, "2026", "IR", 132000),
        ownedTrack(1023L, "تو آمدی (بی تو بی معنایم)", "رضا بهرام", "reza-bahram", "تک‌آهنگ جدید ۲۰۲۶", "file:///android_asset/covers/cover_1023.jpg", "https://dls.musics-fa.com/song/alibz/2026/08/Reza%20Bahram%20-%20To%20Amadi%20Musics-Fa.mp3", "پاپ عاشقانه", 246000L, "2026", "IR", 129000),
        ownedTrack(1024L, "بگردم", "گرشا رضایی", "garsha-rezaei", "تک‌آهنگ جدید ۲۰۲۶", "file:///android_asset/covers/cover_1024.jpg", "https://dls.musics-fa.com/song/alibz/2026/09/Garsha%20Rezaei%20-%20Begardam%20Musics-Fa.mp3", "پاپ و شاد", 192000L, "2026", "IR", 125500),
        ownedTrack(1002L, "مریض تخت آخری", "محسن چاوشی", "mohsen-chavoshi", "تک‌آهنگ ۲۰۲۶", "file:///android_asset/covers/cover_1002.jpg", "https://dls.musics-fa.com/song/alibz/2026/06/Mohsen%20Chavoshi%20-%20Marize%20Takhte%20Akhari%20Musics-Fa.mp3", "پاپ راک", 252000L, "2026", "IR", 139000),
        ownedTrack(1004L, "هنوزم یه تار موتو به دنیا نمیدم", "محسن چاوشی", "mohsen-chavoshi", "تک‌آهنگ مشترک ۲۰۲۶", "file:///android_asset/covers/cover_1004.jpg", "https://dls.musics-fa.com/song/alibz/2026/02/Mohsen%20Chavoshi%20And%20Mehdi%20Ahmadvand%20-%20Hanoozam%20Ye%20Tare%20Mooto%20Be%20Donya%20Nemidam%20Musics-Fa.mp3", "پاپ ایرانی", 238000L, "2026", "IR", 127000),
        ownedTrack(1018L, "با تو", "حامیم", "haamim", "تک‌آهنگ ۲۰۲۶", "file:///android_asset/covers/cover_1018.jpg", "https://dls.musics-fa.com/song/alibz/2026/08/Haamim%20-%20Ba%20To%20Musics-Fa.mp3", "پاپ احساسی", 220000L, "2026", "IR", 119000),
        ownedTrack(1019L, "تغییرم بده", "مجید رضوی", "majid-razavi", "تک‌آهنگ ۲۰۲۶", "file:///android_asset/covers/cover_1019.jpg", "https://dls.musics-fa.com/song/alibz/2026/06/Majid%20Razavi%20-%20Taghiram%20Bede%20Musics-Fa.mp3", "پاپ و شاد", 204000L, "2026", "IR", 116000),
        ownedTrack(1025L, "ما بهار می‌شویم", "فریدون آسرایی", "fereydoun-asraei", "تک‌آهنگ ۲۰۲۶", "file:///android_asset/covers/cover_1025.jpg", "https://dls.musics-fa.com/song/alibz/2026/02/Fereydoun%20Asraei%20-%20Ma%20Bahar%20Mishavim%20Musics-Fa.mp3", "پاپ ایرانی", 212000L, "2026", "IR", 111000),
        ownedTrack(1006L, "دلا خون شو (بارون)", "محمدرضا شجریان", "mohammadreza-shajarian", "گنجینه سنتی ۲۰۲۶", "file:///android_asset/covers/cover_1006.jpg", "https://dls.musics-fa.com/tagdl/99/MohammadReza%20Shajaryan%20-%20Baroon%20(128).mp3", "سنتی اصیل", 195000L, "2026", "IR", 122000),
        ownedTrack(1014L, "امشب", "عرفان طهماسبی", "erfan-tahmasbi", "تک‌آهنگ ۲۰۲۵", "file:///android_asset/covers/cover_1014.jpg", "https://dls.musics-fa.com/song/alibz/dlswm/Erfan%20Tahmasbi%20-%20Emshab%20(320).mp3", "پاپ سنتی", 205000L, "2025", "IR", 134000),
        ownedTrack(1003L, "ای عاشقان", "محمدرضا شجریان", "mohammadreza-shajarian", "تصنیف‌های ماندگار ۲۰۲۵", "file:///android_asset/covers/cover_1003.jpg", "https://dls.musics-fa.com/song/alibz/dlswm/MohammadReza%20Shajaryan%20-%20Ey%20Asheghan%20(320).mp3", "سنتی اصیل", 224000L, "2025", "IR", 128000),
        ownedTrack(1005L, "گیسوی باران", "علیرضا قربانی", "alireza-ghorbani", "تک‌آهنگ ۲۰۲۵", "file:///android_asset/covers/cover_1005.jpg", "https://dls.musics-fa.com/song/alibz/dlswm/Alireza%20Ghorbani%20-%20Gisooye%20Baran%20(320).mp3", "سنتی و عاشقانه", 218000L, "2025", "IR", 124000),
        ownedTrack(1007L, "ماه من", "علیرضا قربانی", "alireza-ghorbani", "تک‌آهنگ ۲۰۲۵", "file:///android_asset/covers/cover_1007.jpg", "https://dls.musics-fa.com/song/alibz/dlswm/Alireza%20Ghorbani%20-%20Mahe%20Man%20(320).mp3", "سنتی و عاشقانه", 185000L, "2025", "IR", 117000),
        ownedTrack(1008L, "ایران روشن", "همایون شجریان", "homayoun-shajarian", "تک‌آهنگ ۲۰۲۵", "file:///android_asset/covers/cover_1008.jpg", "https://dls.musics-fa.com/song/alibz/dlswm/Homayoun%20Shajarian%20-%20Irane%20Roshan%20(320).mp3", "سنتی", 282000L, "2025", "IR", 121000),
        ownedTrack(1021L, "رویا", "علی لهراسبی", "ali-lohrasbi", "تک‌آهنگ ۲۰۲۵", "file:///android_asset/covers/cover_1021.jpg", "https://dl.rozmusic.com/Music/1404/01/17/Ali%20Lohrasbi%20-%20Roya%20%28128%29.mp3", "پاپ ایرانی", 216000L, "2025", "IR", 106000),
        ownedTrack(1011L, "برگای نارنجی", "سیروان خسروی", "sirvan-khosravi", "پاییز ۲۰۲۴", "file:///android_asset/covers/cover_1011.jpg", "https://dls.musics-fa.com/song/alibz/dlswm/Sirvan%20Khosravi%20-%20Bargaye%20Narenji%20(320).mp3", "پاپ آرامش", 232000L, "2024", "IR", 115000),
        ownedTrack(1012L, "خودم تنها", "همایون شجریان", "homayoun-shajarian", "تک‌آهنگ ۲۰۲۴", "file:///android_asset/covers/cover_1012.jpg", "https://dls.musics-fa.com/song/alibz/1403/Homayoun%20Shajarian%20-%20Khodam%20Tanha%20(320).mp3", "سنتی", 278000L, "2024", "IR", 113000),
        ownedTrack(1009L, "در سکوت شب (خداحافظ)", "عرفان طهماسبی", "erfan-tahmasbi", "تک‌آهنگ منتخب", "file:///android_asset/covers/cover_1009.jpg", "https://dls.musics-fa.com/tagdl/1402/Erfan%20Tahmasbi%20-%20Khodahafez%20(320).mp3", "پاپ سنتی", 215000L, "2024", "IR", 118500),
        ownedTrack(1001L, "بهت قول میدم", "محسن یگانه", "mohsen-yeganeh", "نگاه (نسخه کامل)", "file:///android_asset/covers/cover_1001.jpg", "https://dls.musics-fa.com/tagdl/downloads/Mohsen%20Yeganeh%20-%20Behet%20Ghol%20Midam%20(128).mp3", "پاپ ایرانی", 221000L, "2023", "IR", 145000),
        ownedTrack(1010L, "ندارمت", "محسن یگانه", "mohsen-yeganeh", "تک‌آهنگ منتخب", "file:///android_asset/covers/cover_1010.jpg", "https://dls.musics-fa.com/tagdl/1401/Mohsen%20Yegane%20-%20Nadaramet%20(128).mp3", "پاپ عاشقانه", 235000L, "2023", "IR", 114000),
        ownedTrack(1020L, "سی سالگی", "احسان خواجه امیری", "ehsan-khajeh-amiri", "سی سالگی (نسخه کامل)", "file:///android_asset/covers/cover_1020.jpg", "https://dl.rozmusic.com/Music/1396/06/04/Ehsan%20Khajeh%20Amiri%20-%2030%20Salegi%20(128).mp3", "پاپ ایرانی", 226000L, "2022", "IR", 109000),
        ownedTrack(1026L, "ماهی", "گرشا رضایی", "garsha-rezaei", "تک‌آهنگ پرطرفدار", "file:///android_asset/covers/cover_1026.jpg", "https://dls.musics-fa.com/tagdl/ali/Garsha%20Rezaei%20-%20Mahi%20(128).mp3", "پاپ و شاد", 219000L, "2022", "IR", 126000),
        ownedTrack(1027L, "عاشقم (هوای دل)", "رضا بهرام", "reza-bahram", "تک‌آهنگ ماندگار", "file:///android_asset/covers/cover_1027.jpg", "https://dls.musics-fa.com/tagdl/ati/Reza%20Bahram%20-%20Havaye%20Del%20(128).mp3", "پاپ عاشقانه", 228000L, "2022", "IR", 123000),

        // TURKEY (Never reuses Persian artists' covers — ArtworkResolver renders each artist's own monogram)
        ownedTrack(2001L, "Ben Senin (Turkish Hit)", "Tarkan", "tarkan", "Istanbul Pop 2026", null, "https://dls.musics-fa.com/song/alibz/dlswm/Tarkan%20-%20Ben%20Senin%20(320).mp3", "پاپ و شاد ترکی (Turkish Pop)", 230000L, "2026", "TR", 152000),
        ownedTrack(2002L, "Tabib (طبیب)", "İbrahim Tatlıses", "ibrahim-tatlises", "İmparator 2026", null, "https://dls.musics-fa.com/song/alibz/2026/02/Ibrahim%20Tatlis%20-%20Tabib%20Musics-Fa.mp3", "آرابسک و سنتی ترکی (Arabesk)", 248000L, "2026", "TR", 149000),
        ownedTrack(2003L, "Sensiz (بی تو)", "Emrah", "emrah", "Turkish Romance 2026", null, "https://dls.musics-fa.com/song/alibz/2026/02/Emrah%20-%20Sen%20Siz%20Musics-Fa.mp3", "عاشقانه و دامار ترکی (Damar)", 236000L, "2026", "TR", 138000),
        ownedTrack(2004L, "Ayrılığın Adı", "Ramil Sedali", "ramil-sedali", "Bosphorus Nights 2026", null, "https://dls.musics-fa.com/song/alibz/2026/09/Ramil%20Sedali%20-%20Ayriligin%20Adi%20Musics-Fa.mp3", "پاپ احساسی ترکی", 222000L, "2026", "TR", 131000),
        ownedTrack(2005L, "Bebeğim (عزیزم)", "İbrahim Tatlıses", "ibrahim-tatlises", "Klasikler 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Ibrahim%20Tatlises%20-%20Bebegim%20(320).mp3", "آرابسک و عاشقانه (Arabesk)", 265000L, "2025", "TR", 146000),
        ownedTrack(2006L, "Belalım (بلای جانم)", "Mahsun Kırmızıgül", "mahsun-kirmizigul", "Istanbul Memories 2025", null, "https://dls.musics-fa.com/song/alibz/1403/Mahsun%20-%20Belalim%20(320).mp3", "آرابسک و آرامش (Turkish Chill)", 254000L, "2025", "TR", 141000),
        ownedTrack(2007L, "Melekler Ağlar", "Mahsun Kırmızıgül", "mahsun-kirmizigul", "Turkish Hits 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Mahsun%20-%20Melekler%20Aglar%20(320).mp3", "عاشقانه و پاپ ترکی", 240000L, "2025", "TR", 133000),
        ownedTrack(2008L, "Yallah Şoför", "İbrahim Tatlıses", "ibrahim-tatlises", "Turkish Party & Drive", null, "https://dls.musics-fa.com/song/alibz/dlswm/Ibrahim%20Tatlises%20-%20Yallah%20Sofor%20(320).mp3", "پارتی و شاد ترکی (Party & Drive)", 218000L, "2024", "TR", 139000),
        ownedTrack(2009L, "Turkish Dance Club", "Turkish Club DJ", "turkish-club-dj", "Antalya Club 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Dj%20Ali%20Chitsaz%20-%20Turkish%20Dance%20(320).mp3", "کلاب، ورزش و شاد (Club & Workout)", 210000L, "2025", "TR", 128000),
        ownedTrack(2010L, "Vefasız (بی‌وفا)", "Mahsun Kırmızıgül", "mahsun-kirmizigul", "Damar Gold 2024", null, "https://dls.musics-fa.com/song/alibz/dlswm/Mahsun%20-%20Vefasiz%20(320).mp3", "آرابسک و احساسی", 245000L, "2024", "TR", 125000),
        ownedTrack(2011L, "Derbeder (دربه‌در)", "Emrah", "emrah", "Klasik 2024", null, "https://dls.musics-fa.com/song/alibz/dlswm/Emrah%20-%20Darbedar%20(320).mp3", "سنتی و عاشقانه ترکی", 238000L, "2024", "TR", 121000),

        // USA
        ownedTrack(3001L, "Save Your Tears", "The Weeknd", "the-weeknd", "After Hours Deluxe 2026", null, "https://dls.musics-fa.com/song/alibz/2026/10/The%20Weeknd%20-%20Save%20Your%20Tears%20Musics-Fa.mp3", "پاپ و سینث‌پاپ (US Pop Hits)", 215000L, "2026", "US", 165000),
        ownedTrack(3002L, "Timeless", "The Weeknd", "the-weeknd", "Hurry Up Tomorrow 2026", null, "https://dls.musics-fa.com/song/alibz/dlswm/The%20Weeknd%20-%20Timeless%20(320).mp3", "آر اند بی و هیپ‌هاپ (R&B / Hip-Hop)", 256000L, "2026", "US", 158000),
        ownedTrack(3003L, "Still D.R.E. (West Coast)", "Dr. Dre & Snoop Dogg", "dr-dre-snoop-dogg", "California Highway 2026", null, "https://dls.musics-fa.com/song/alibz/2026/09/Dr%20Dre%20Snoop%20Dogg%20-%20Still%20DRE%20Musics-Fa.mp3", "هیپ‌هاپ و رانندگی (US Hip-Hop)", 270000L, "2026", "US", 154000),
        ownedTrack(3004L, "Toxic (Alternative Hit)", "BoyWithUke", "boywithuke", "US Viral Hits 2026", null, "https://dls.musics-fa.com/song/alibz/2026/09/BoyWithUke%20-%20Toxic%20Musics-Fa.mp3", "پاپ آلترناتیو و آرامش (Indie Pop)", 168000L, "2026", "US", 142000),
        ownedTrack(3005L, "Starboy", "The Weeknd", "the-weeknd", "Starboy Platinum 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/The%20Weeknd%20-%20StarBoy%20(320).mp3", "پاپ، کلاب و رانندگی (Pop & Club)", 230000L, "2025", "US", 161000),
        ownedTrack(3006L, "Lose Yourself", "Eminem", "eminem", "8 Mile Workout 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Eminem%20-%20lose%20yourself%20(320).mp3", "هیپ‌هاپ و ورزش (Hip-Hop Workout)", 320000L, "2025", "US", 159000),
        ownedTrack(3007L, "Calm Down (US Remix)", "Rema & Selena Gomez", "rema-selena-gomez", "Billboard Hot 100 (2025)", null, "https://dls.musics-fa.com/tagdl/8e401/Rema%20-%20Calm%20Down%20(320).mp3", "پاپ، پارتی و شاد (Pop & Party)", 239000L, "2025", "US", 151000),
        ownedTrack(3008L, "Rap God", "Eminem", "eminem", "Marshall Mathers LP", null, "https://dls.musics-fa.com/song/alibz/dlswm/Eminem%20-%20Rap%20God%20(320).mp3", "هیپ‌هاپ و انرژی (US Rap)", 363000L, "2024", "US", 147000),
        ownedTrack(3009L, "Beautiful", "Eminem", "eminem", "Relapse Emotional", null, "https://dls.musics-fa.com/song/alibz/dlswm/Eminem%20-%20Beautiful%20(320).mp3", "احساسی و راک رپ (Emotional)", 392000L, "2024", "US", 136000),
        ownedTrack(3010L, "Go Down Deh", "Sean Paul", "sean-paul", "Miami Club Nights 2024", null, "https://dls.musics-fa.com/song/alibz/1403/Sean%20Paul%20-%20Go%20Down%20Deh%20(320).mp3", "پارتی و کلاب (Dancehall & Club)", 195000L, "2024", "US", 132000),
        ownedTrack(3011L, "Tacata (US Club Mix)", "Tiagz", "tiagz", "Viral Club 2024", null, "https://dls.musics-fa.com/song/alibz/dlswm/Tiagz%20-%20Tacata%20(320).mp3", "دنس و ورزش (Club & Workout)", 182000L, "2024", "US", 128000),

        // UK
        ownedTrack(4001L, "Azizam (UK Chart Single)", "Ed Sheeran", "ed-sheeran", "London Sessions 2026", null, "https://dls.musics-fa.com/song/alibz/dlswm/Ed%20Sheeran%E2%80%8C%20-%20Azizam%20(320).mp3", "پاپ و هیت بریتانیا (UK Pop)", 212000L, "2026", "GB", 156000),
        ownedTrack(4002L, "Can You Feel My Heart", "Bring Me The Horizon", "bring-me-the-horizon", "Sheffield Rock Anthem 2026", null, "https://dls.musics-fa.com/song/alibz/2026/09/Bring%20Me%20The%20Horizon%20-%20Can%20You%20Feel%20My%20Heart%20Musics-Fa.mp3", "راک، آلترناتیو و ورزش (UK Rock)", 227000L, "2026", "GB", 149000),
        ownedTrack(4003L, "Darling (London Street Hit)", "D-Block Europe", "d-block-europe", "UK Official Chart 2025", null, "https://dls.musics-fa.com/tagdl/1402/D-block%20Europe%20-%20Darling%20(320).mp3", "پاپ، کلاب و هیپ‌هاپ بریتانیا", 204000L, "2025", "GB", 141000),
        ownedTrack(4004L, "London Duet (Ed Sheeran Mix)", "Ed Sheeran", "ed-sheeran", "Acoustic & Pop 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Mohammadreza%20Golzar%20And%20Ed%20Sheeran%20-%20Ahmagh%20(320).mp3", "پاپ و عاشقانه (UK Pop Duet)", 218000L, "2025", "GB", 135000),
        ownedTrack(4005L, "Spiritual Peace (London Orchestra)", "Sami Yusuf", "sami-yusuf", "Royal Albert Hall Live", null, "https://dls.musics-fa.com/song/alibz/dlswm/Sami%20Yousuf%20-%20Mahe%20Ramazan%20(320).mp3", "آرامش و ارکسترال (Acoustic & Chill)", 260000L, "2024", "GB", 132000),
        ownedTrack(4006L, "Rainy London Piano", "London Sessions", "london-sessions", "Thames Acoustic 2026", null, "https://dls.musics-fa.com/song/alibz/2026/09/Dekamond%20-%20Bezan%20Baroon%20Piano%20Version%20Musics-Fa.mp3", "پیانو، آرامش و بارانی (Piano Chill)", 198000L, "2026", "GB", 126000),

        // SPAIN
        ownedTrack(5001L, "Amor Mio (Flamenco Classic)", "Gipsy Kings", "gipsy-kings", "Guitarra Española 2026", null, "https://dls.musics-fa.com/tagdl/downloads/Gipsy%20Kings%20-%20Amor%20Mio%20(128).mp3", "فلامنکو و عاشقانه اسپانیایی (Flamenco)", 242000L, "2026", "ES", 153000),
        ownedTrack(5002L, "Gata Only (Ibiza Fiesta Hit)", "Cris MJ", "cris-mj", "Top 50 España 2026", null, "https://dls.musics-fa.com/song/alibz/dlswm/Cris%20MJ%20-%20Gata%20Only%20(320).mp3", "لاتین پاپ، پارتی و شاد (Reggaeton)", 222000L, "2026", "ES", 158000),
        ownedTrack(5003L, "Canción del Mariachi (Desperado)", "Antonio Banderas", "antonio-banderas", "Spanish Cinema Classics", null, "https://dls.musics-fa.com/tagdl/downloads/Antonio%20Banderas%20-%20Desperado%20(128).mp3", "گیتار اسپانیایی و فلامنکو (Spanish Guitar)", 175000L, "2025", "ES", 149000),
        ownedTrack(5004L, "Gipsy Flamenco Medley", "Koli Band", "koli-band", "Barcelona Fiesta 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Koli%20Band%20-%20Gipsy%20Medley%20(320).mp3", "فلامنکو، پارتی و شاد (Fiesta)", 248000L, "2025", "ES", 141000),
        ownedTrack(5005L, "Noche de Guitarra (Madrid Remix)", "Gipsy Kings", "gipsy-kings", "Spanish Fusion 2025", null, "https://dls.musics-fa.com/song/alibz/1403/Dj%20Kia%20-%20Remix%20Reza%20Baharam%20And%20Gipsykings%20(320).mp3", "پاپ لاتین و رانندگی (Spanish Drive)", 216000L, "2025", "ES", 134000),
        ownedTrack(5006L, "Guitarra del Sol", "Spanish Guitar Masters", "spanish-guitar-masters", "Mediterranean Sunset 2026", null, "https://dls.musics-fa.com/song/alibz/dlswm/Omid%20Radmehr%20-%20Sokoot%20Guitar%20Version%20(320).mp3", "گیتار آرامش‌بخش (Spanish Chill)", 205000L, "2026", "ES", 129000),

        // FRANCE
        ownedTrack(6001L, "Amour & Chanson (Euro Hit)", "Adriano Celentano", "adriano-celentano", "Paris Soirée 2026", null, "https://dls.musics-fa.com/song/alibz/2026/10/Adriano%20Celentano%20-%20Ja%20Tebia%20Liubliu%20Musics-Fa.mp3", "پاپ و شانسون اروپایی (Chanson & Pop)", 238000L, "2026", "FR", 147000),
        ownedTrack(6002L, "Amor Mio (Arles Sessions)", "Gipsy Kings (France)", "gipsy-kings", "Sud de la France 2026", null, "https://dls.musics-fa.com/tagdl/downloads/Gipsy%20Kings%20-%20Amor%20Mio%20(128).mp3", "کلاسیک، گیتار و عاشقانه فرانسه", 242000L, "2026", "FR", 143000),
        ownedTrack(6003L, "Café de Paris Piano", "Paris Piano & Acoustic", "paris-piano-acoustic", "Montmartre Chill 2026", null, "https://dls.musics-fa.com/song/alibz/2026/08/Ehsan%20Yasin%20-%20Khodae%20Dige%20Piano%20Version%20Musics-Fa.mp3", "پیانو و آرامش پاریسی (Paris Chill)", 210000L, "2026", "FR", 135000),
        ownedTrack(6004L, "Champs-Élysées Club Mix", "French Touch Electro", "french-touch-electro", "Paris Night Club 2025", null, "https://dls.musics-fa.com/song/alibz/2026/11/Sina%20Shabankhani%20-%20Jadooye%20Khas%20Remix%20Musics-Fa.mp3", "الکترو، پارتی و شاد (French Club)", 218000L, "2025", "FR", 131000),
        ownedTrack(6005L, "Voyage Nocturne (Piano)", "Paris Piano & Acoustic", "paris-piano-acoustic", "Seine River 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Amir%20Arsalan%20-%20Mosafer%20Piano%20Version%20(320).mp3", "عاشقانه و آرامش (Romantique)", 204000L, "2025", "FR", 126000),

        // GERMANY
        ownedTrack(7001L, "Brother Louie (320 Remaster)", "Modern Talking", "modern-talking", "Berlin Euro-Pop Legends 2026", null, "https://dls.musics-fa.com/tagdl/1402/Modern%20Talking%20-%20Brother%20Louie%20(320).mp3", "یوروپاپ، شاد و رانندگی (Euro-Pop)", 223000L, "2026", "DE", 162000),
        ownedTrack(7002L, "Euro Stadium Anthem", "Berlin Euro Electronic", "berlin-euro-electronic", "Autobahn Beats 2026", null, "https://dls.musics-fa.com/song/alibz/dlswm/Unknown%20Artist%20-%20Uefa%20Euro%202008%20(320).mp3", "الکترونیک، ورزش و کلاب (Berlin Club)", 215000L, "2026", "DE", 148000),
        ownedTrack(7003L, "Berlin Nocturne Piano", "Dekamond Piano", "dekamond-piano", "Neoclassical Berlin 2026", null, "https://dls.musics-fa.com/song/alibz/2026/09/Dekamond%20-%20Bezan%20Baroon%20Piano%20Version%20Musics-Fa.mp3", "پیانو، کلاسیک و آرامش (Classical Chill)", 198000L, "2026", "DE", 139000),
        ownedTrack(7004L, "Acoustic Autobahn Guitar", "German Guitar Sessions", "german-guitar-sessions", "Munich Acoustic 2025", null, "https://dls.musics-fa.com/song/alibz/2026/10/Ebi%20Ali%20-%20Remix%20Guitari%20Masti%20Musics-Fa.mp3", "گیتار، راک و رانندگی (Drive & Guitar)", 224000L, "2025", "DE", 133000),
        ownedTrack(7005L, "Silent Night Guitar", "German Guitar Sessions", "german-guitar-sessions", "Hamburg Chill 2025", null, "https://dls.musics-fa.com/song/alibz/dlswm/Omid%20Radmehr%20-%20Sokoot%20Guitar%20Version%20(320).mp3", "آرامش و بیکلام (Instrumental Focus)", 205000L, "2025", "DE", 127000)
    )
}
