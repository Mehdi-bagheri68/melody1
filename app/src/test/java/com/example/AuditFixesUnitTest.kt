package com.example

import com.example.data.auth.AuthError
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthState
import com.example.data.auth.LocalEmailAuthRepository
import com.example.data.model.ArtworkRef
import com.example.data.model.AudioQuality
import com.example.data.model.CountriesData
import com.example.data.model.ErrorCategory
import com.example.data.model.Track
import com.example.data.repository.ArtworkResolver
import com.example.data.repository.CatalogDataSource
import com.example.data.repository.GroupEngine
import com.example.data.repository.MusicsFaHtmlParser
import com.example.data.repository.SourceResolver
import com.example.util.BidiHelper
import com.example.util.IdGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditFixesUnitTest {

    @Test
    fun `BUG-01 SourceResolver never substitutes invalid or empty URL with another song`() {
        val emptyTrack = Track(
            id = 999L,
            title = "Missing Song",
            artist = "Unknown Artist",
            album = "Single",
            artworkUrl = "",
            previewUrl = "",
            durationMs = 0L,
            genre = "Pop",
            countryCode = "IR",
            releaseYear = "2026"
        )
        val resolution = SourceResolver.resolveTrackStream(
            track = emptyTrack,
            quality = AudioQuality.HIGH,
            iranNoVpnMode = true
        )
        assertTrue(
            "Expected Unavailable resolution instead of substituting another song",
            resolution is SourceResolver.StreamResolution.Unavailable
        )
        val unavailable = resolution as SourceResolver.StreamResolution.Unavailable
        assertEquals(ErrorCategory.SOURCE_UNAVAILABLE, unavailable.error.category)
    }

    @Test
    fun `BUG-01 SourceResolver rejects soundhelix placeholder URLs`() {
        assertFalse(SourceResolver.isValidStreamUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"))
        assertFalse(SourceResolver.isValidStreamUrl(""))
        assertTrue(SourceResolver.isValidStreamUrl("https://ups.musics-fa.com/tagdl/1403/Artist%20-%20Song%20(320).mp3"))
    }

    @Test
    fun `BUG-04 and BUG-05 MusicsFaHtmlParser parses valid HTML and sets unknown duration to 0L`() {
        val sampleHtml = """
            <html>
            <body>
            <article class="mf_pst">
                <h2><a href="https://musics-fa.com/song/">دانلود آهنگ شادمهر عقیلی تماشاچی</a></h2>
                <img src="https://musics-fa.com/wp-content/uploads/2026/02/Shadmehr-Cover.jpg" />
                <a href="https://dls.musics-fa.com/tagdl/2026/Shadmehr%20Aghili%20-%20Tamashachi%20(320).mp3">Download 320</a>
            </article>
            </body>
            </html>
        """.trimIndent()

        val outcome = MusicsFaHtmlParser.parseHtml(
            html = sampleHtml,
            query = "شادمهر عقیلی",
            favoriteIds = setOf(),
            defaultArtist = "شادمهر عقیلی",
            countryCode = "IR"
        )

        assertTrue("Expected ParseOutcome.Success", outcome is MusicsFaHtmlParser.ParseOutcome.Success)
        val tracks = (outcome as MusicsFaHtmlParser.ParseOutcome.Success).tracks
        assertEquals(1, tracks.size)
        val track = tracks.first()
        assertEquals("شادمهر عقیلی", track.artist)
        assertEquals("تماشاچی", track.title)
        assertEquals("2026", track.releaseYear)
        assertEquals("BUG-05: Scraped tracks must have durationMs = 0L until ExoPlayer resolves real duration", 0L, track.durationMs)
        assertEquals("--:--", track.durationFormatted)
    }

    @Test
    fun `BUG-04 MusicsFaHtmlParser detects Cloudflare or CAPTCHA anti-bot challenge`() {
        val challengeHtml = """
            <html>
            <head><title>Just a moment... Cloudflare</title></head>
            <body>Attention Required! | Cloudflare</body>
            </html>
        """.trimIndent()

        val outcome = MusicsFaHtmlParser.parseHtml(
            html = challengeHtml,
            query = "Artist",
            favoriteIds = emptySet(),
            countryCode = "IR"
        )
        assertTrue("Expected anti-bot challenge detection", outcome is MusicsFaHtmlParser.ParseOutcome.CaptchaOrBlocked)
    }

    @Test
    fun `BUG-09 SourceResolver switches between 320kbps and 128kbps variants of the same track`() {
        val track320 = Track(
            id = 1001L,
            title = "Ghalbe Man",
            artist = "Behnam Bani",
            album = "Single",
            artworkUrl = "file:///android_asset/covers/cover_1017.jpg",
            previewUrl = "https://ups.musics-fa.com/tagdl/99/Behnam%20Bani%20-%20Ghalbe%20Man%20(320).mp3",
            durationMs = 205000L,
            genre = "پاپ",
            countryCode = "IR",
            releaseYear = "2024"
        )

        val highRes = SourceResolver.resolveTrackStream(
            track = track320,
            quality = AudioQuality.HIGH,
            iranNoVpnMode = true
        ) as SourceResolver.StreamResolution.Available

        val saverRes = SourceResolver.resolveTrackStream(
            track = track320,
            quality = AudioQuality.DATA_SAVER,
            iranNoVpnMode = true
        ) as SourceResolver.StreamResolution.Available

        assertTrue(highRes.primaryUrl.contains("(320).mp3"))
        assertTrue(saverRes.primaryUrl.contains("(128).mp3"))
        assertTrue(highRes.hasMultiBitrateVariant)
    }

    @Test
    fun `UI-11 BidiHelper wraps mixed Persian and Latin strings safely`() {
        val isolated = BidiHelper.isolateLtr("Pop 2026")
        assertTrue(isolated.contains("Pop 2026"))
    }

    @Test
    fun `N-01 every artwork belongs to the track's artist`() {
        val tracks = CatalogDataSource.getTracks()
        assertTrue(tracks.isNotEmpty())
        tracks.forEach { track ->
            track.artwork?.let { art ->
                assertTrue(
                    "Track ${track.id} (${track.title}) uses artwork owned by ${art.ownerArtistId}, expected ${track.artistId}",
                    art.ownerArtistId == track.artistId || art.ownerArtistId in track.featuredArtistIds
                )
            }
        }
    }

    @Test
    fun `N-01 no artwork file is shared between different owners`() {
        val tracks = CatalogDataSource.getTracks()
        val byUrl = tracks.mapNotNull { it.artwork }.groupBy { it.url }
        byUrl.forEach { (url, list) ->
            val owners = list.map { it.ownerArtistId }.toSet()
            assertEquals("URL $url has multiple owners: $owners", 1, owners.size)
        }
    }

    @Test
    fun `N-01 ArtworkResolver rejects foreign artist cover and returns deterministic Initials`() {
        val mismatchedTrack = Track(
            id = 5004L,
            title = "Gipsy Flamenco Medley",
            artist = "Koli Band",
            artistId = "koli-band",
            album = "Barcelona Fiesta",
            artworkUrl = "file:///android_asset/covers/cover_1017.jpg", // Owned by behnam-bani!
            previewUrl = "https://dls.musics-fa.com/song/alibz/dlswm/Koli%20Band%20-%20Gipsy%20Medley%20(320).mp3"
        )
        val resolved = ArtworkResolver.resolve(mismatchedTrack)
        assertTrue(
            "Expected ArtworkRef.Initials when cover belongs to another artist",
            resolved is ArtworkRef.Initials
        )
    }

    @Test
    fun `N-02 MoodCategories never reuse artist cover files`() {
        CountriesData.allCountries.forEach { country ->
            val moods = CountriesData.getMoodCategoriesForCountry(country)
            moods.forEach { mood ->
                assertTrue(
                    "Mood ${mood.id} in ${country.code} must not use artist cover file (${mood.coverUrl})",
                    mood.coverUrl.isBlank()
                )
            }
        }
    }

    @Test
    fun `N-03 new-releases group respects perArtistCap`() {
        val tracks = CatalogDataSource.getTracks().filter { it.countryCode == "IR" }
        val group = CatalogDataSource.getDefaultNewReleasesGroup("IR")
        val resolved = GroupEngine.evaluate(group, tracks, CatalogDataSource.getArtistsById())
        val countsByArtist = resolved.groupingBy { it.artistId }.eachCount()
        val cap = group.rule?.perArtistCap ?: Int.MAX_VALUE
        assertEquals(1, cap)
        countsByArtist.forEach { (artistId, count) ->
            assertTrue("Artist $artistId appears $count times, cap is $cap", count <= cap)
        }
    }

    @Test
    fun `N-07 IdGenerator produces deterministic 64-bit IDs without collision`() {
        val id1 = IdGenerator.stable64BitId("musics_fa", "https://dls.musics-fa.com/a.mp3")
        val id2 = IdGenerator.stable64BitId("musics_fa", "https://dls.musics-fa.com/b.mp3")
        val id1Repeat = IdGenerator.stable64BitId("musics_fa", "https://dls.musics-fa.com/a.mp3")
        assertEquals(id1, id1Repeat)
        assertNotEquals(id1, id2)
        assertTrue(id1 > 1_000_000L)
    }

    @Test
    fun `Section 6 LocalEmailAuthRepository sign-up, sign-in, password reset, and sign-out`() = runTest {
        val authRepo = LocalEmailAuthRepository(musicDao = null, userPreferences = null)

        // 1. Invalid email rejected
        val invalidEmailRes = authRepo.signUp("not-an-email", "password123")
        assertTrue(invalidEmailRes is AuthResult.Failure && invalidEmailRes.reason == AuthError.INVALID_EMAIL)

        // 2. Weak password (< 8 chars) rejected
        val weakPassRes = authRepo.signUp("user@example.com", "1234567")
        assertTrue(weakPassRes is AuthResult.Failure && weakPassRes.reason == AuthError.WEAK_PASSWORD)

        // 3. Valid sign-up succeeds
        val signUpRes = authRepo.signUp("user@example.com", "StrongPass123", "Ali Reza")
        assertTrue(signUpRes is AuthResult.Success)
        assertTrue(authRepo.state.value is AuthState.SignedIn)

        // 4. Duplicate sign-up rejected
        val duplicateRes = authRepo.signUp("user@example.com", "StrongPass123")
        assertTrue(duplicateRes is AuthResult.Failure && duplicateRes.reason == AuthError.EMAIL_IN_USE)

        // 5. Sign-out transitions to SignedOut
        authRepo.signOut()
        assertTrue(authRepo.state.value is AuthState.SignedOut)

        // 6. Wrong password rejected
        val wrongPass = authRepo.signIn("user@example.com", "WrongPass999")
        assertTrue(wrongPass is AuthResult.Failure && wrongPass.reason == AuthError.WRONG_CREDENTIALS)

        // 7. Password reset updates password and allows sign-in
        val resetRes = authRepo.sendPasswordReset("user@example.com", "NewSecretPass88")
        assertTrue(resetRes is AuthResult.Success)
        val signInNew = authRepo.signIn("user@example.com", "NewSecretPass88")
        assertTrue(signInNew is AuthResult.Success)
    }

    @Test
    fun `v1_8_0 AppConfig validates and normalizes Iran and international mobile numbers`() {
        // Valid Iran numbers (+98)
        assertTrue(com.example.data.model.AppConfig.isValidPhoneNumber("+98", "09123456789"))
        assertTrue(com.example.data.model.AppConfig.isValidPhoneNumber("+98", "9123456789"))
        assertTrue(com.example.data.model.AppConfig.isValidPhoneNumber("+98", "۰۹۱۲۳۴۵۶۷۸۹"))
        assertEquals(
            "+989123456789",
            com.example.data.model.AppConfig.normalizePhoneNumber("+98", "09123456789")
        )
        assertEquals(
            "+989123456789",
            com.example.data.model.AppConfig.normalizePhoneNumber("+98", "۰۹۱۲۳۴۵۶۷۸۹")
        )

        // Invalid Iran numbers
        assertFalse(com.example.data.model.AppConfig.isValidPhoneNumber("+98", "08123456789"))
        assertFalse(com.example.data.model.AppConfig.isValidPhoneNumber("+98", "912345"))

        // Valid International numbers
        assertTrue(com.example.data.model.AppConfig.isValidPhoneNumber("+90", "5321234567"))
        assertEquals(
            "+905321234567",
            com.example.data.model.AppConfig.normalizePhoneNumber("+90", "05321234567")
        )
    }

    @Test
    fun `v1_8_0 UserProfile JSON schema round-trip and Strict Onboarding Guard predicates`() {
        val initialProfile = com.example.data.model.UserProfile(
            uid = "google_12345",
            email = "listener@gmail.com",
            displayName = "Sara",
            photoURL = "https://example.com/avatar.jpg",
            phoneNumber = "",
            phoneVerified = false,
            selectedCountry = "",
            favoriteGenres = listOf("Pop", "Traditional"),
            bio = "Loves Persian classical music",
            createdAt = "2026-11-08T00:00:00Z",
            lastLoginAt = "2026-11-08T00:05:00Z",
            deviceInfo = "Android 14"
        )

        // Step 1 passed (authenticated), Step 3 not yet passed (phone unverified), Step 4 not yet passed
        assertTrue(initialProfile.isAuthenticated)
        assertFalse(initialProfile.isPhoneVerifiedStrict)
        assertFalse(initialProfile.isCountrySelectedStrict)

        // After OTP verification & country selection
        val completedProfile = initialProfile.copy(
            phoneNumber = "+989123456789",
            phoneVerified = true,
            selectedCountry = "IR"
        )
        assertTrue(completedProfile.isPhoneVerifiedStrict)
        assertTrue(completedProfile.isCountrySelectedStrict)

        val jsonStr = completedProfile.toJsonString()
        val parsed = com.example.data.model.UserProfile.fromJsonString(jsonStr)!!
        assertEquals("google_12345", parsed.uid)
        assertEquals("listener@gmail.com", parsed.email)
        assertEquals("+989123456789", parsed.phoneNumber)
        assertTrue(parsed.phoneVerified)
        assertEquals("IR", parsed.selectedCountry)
        assertEquals(listOf("Pop", "Traditional"), parsed.favoriteGenres)
        assertEquals("Loves Persian classical music", parsed.bio)
    }

    @Test
    fun `v1_9_0 Three-Day Free Trial duration, validity, and expiration check`() {
        val startMs = 1_000_000_000L
        // Immediately after starting trial -> full 3 days remaining & valid
        assertEquals(
            com.example.data.model.AppConfig.TRIAL_DURATION_MS,
            com.example.data.model.AppConfig.computeTrialRemainingMs(startMs, startMs)
        )
        assertTrue(com.example.data.model.AppConfig.isTrialValid(startMs, startMs))

        // After 2 days -> 1 day remaining & still valid
        val twoDaysLater = startMs + 2L * 24L * 60L * 60L * 1000L
        assertTrue(com.example.data.model.AppConfig.isTrialValid(startMs, twoDaysLater))
        val remAfterTwoDays = com.example.data.model.AppConfig.computeTrialRemainingMs(startMs, twoDaysLater)
        assertEquals(24L * 60L * 60L * 1000L, remAfterTwoDays)
        assertTrue(
            com.example.data.model.AppConfig.formatTrialRemaining(
                remAfterTwoDays,
                com.example.data.model.AppLanguage.FA
            ).contains("1 روز")
        )

        // Exactly after 3 days (72 hours) -> expired (0 ms remaining)
        val threeDaysLater = startMs + com.example.data.model.AppConfig.TRIAL_DURATION_MS
        assertFalse(com.example.data.model.AppConfig.isTrialValid(startMs, threeDaysLater))
        assertEquals(0L, com.example.data.model.AppConfig.computeTrialRemainingMs(startMs, threeDaysLater))
    }

    @Test
    fun `v2_0_0 UserLoginLog and DeviceTelemetryInfo JSON serialization and stealth compliance`() {
        val logEntry = com.example.data.model.UserLoginLog(
            log_id = "log-uuid-001",
            user_id = "google_998877",
            account_username = "bagheri.cmd@gmail.com",
            phone_number = "+989123456789",
            ip_address = "185.143.232.10",
            device_info = com.example.data.model.DeviceTelemetryInfo(
                brand = "Samsung",
                model = "SM-S928B",
                os_version = "Android 15 (SDK 35)",
                device_id = "a1b2c3d4e5f60011",
                app_version = "2.0.0 (Build 10)",
                network_type = "Wi-Fi (Irancell)"
            ),
            login_timestamp = "2026-11-08T16:30:00Z",
            local_time_display = "2026-11-08 20:00:00",
            status = "Active",
            synced_to_server = true
        )

        val jsonList = com.example.data.model.UserLoginLog.toJsonList(listOf(logEntry))
        assertTrue(jsonList.contains("185.143.232.10"))
        assertTrue(jsonList.contains("SM-S928B"))
        assertTrue(jsonList.contains("+989123456789"))

        val parsedList = com.example.data.model.UserLoginLog.fromJsonList(jsonList)
        assertEquals(1, parsedList.size)
        val restored = parsedList.first()
        assertEquals("log-uuid-001", restored.logId)
        assertEquals("google_998877", restored.userId)
        assertEquals("+989123456789", restored.phoneNumber)
        assertEquals("185.143.232.10", restored.ipAddress)
        assertEquals("Samsung", restored.deviceInfo.brand)
        assertEquals("SM-S928B", restored.deviceInfo.model)
        assertEquals("Android 15 (SDK 35)", restored.deviceInfo.osVersion)
        assertEquals("a1b2c3d4e5f60011", restored.deviceInfo.deviceId)
        assertEquals("Active", restored.status)

        // Verify OtpMode titles/subtitles have zero mention of Google Drive or Apps Script (Stealth Rule)
        com.example.data.model.OtpMode.entries.forEach { mode ->
            val titleFa = mode.getTitle(com.example.data.model.AppLanguage.FA)
            val titleEn = mode.getTitle(com.example.data.model.AppLanguage.EN)
            val subFa = mode.getSubtitle(com.example.data.model.AppLanguage.FA)
            val subEn = mode.getSubtitle(com.example.data.model.AppLanguage.EN)
            assertFalse(titleFa.contains("Drive", ignoreCase = true) || titleFa.contains("درایو"))
            assertFalse(titleEn.contains("Drive", ignoreCase = true))
            assertFalse(subFa.contains("Drive", ignoreCase = true) || subFa.contains("درایو"))
            assertFalse(subEn.contains("Drive", ignoreCase = true))
        }
    }
}



