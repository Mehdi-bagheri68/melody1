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

    @Test
    fun `Section 2_2 DatePrecision never fabricates month-day and ranks full-date releases above year-only`() {
        val yearOnlyTrack = Track(
            id = 1001L,
            title = "Catalog Song",
            artist = "Mohsen Chavoshi",
            album = "Single",
            artworkUrl = "",
            previewUrl = "https://ups.musics-fa.com/tagdl/1403/Track1.mp3",
            releaseYear = "2026",
            releaseDateIso = "2026",
            datePrecision = com.example.data.model.DatePrecision.YEAR
        )
        val fullDateTrack = Track(
            id = 2002L,
            title = "Live Provider Song",
            artist = "Sirvan Khosravi",
            album = "Single",
            artworkUrl = "",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/Track2.m4a",
            releaseYear = "2026",
            releaseDateIso = "2026-03-15",
            datePrecision = com.example.data.model.DatePrecision.DAY,
            sourceId = "itunes"
        )

        assertEquals(20260000L, yearOnlyTrack.computeReleaseSortKey())
        assertEquals(20260315L, fullDateTrack.computeReleaseSortKey())
        assertTrue(fullDateTrack.computeReleaseSortKey() > yearOnlyTrack.computeReleaseSortKey())

        val group = CatalogDataSource.getDefaultNewReleasesGroup("IR")
        val ordered = GroupEngine.evaluate(group, listOf(yearOnlyTrack, fullDateTrack))
        assertEquals(2002L, ordered.first().id)
    }

    @Test
    fun `Section 2_4 GroupEngine deduplicates same artist and title across providers and prefers full stream`() {
        val fullStream = Track(
            id = 1010L,
            title = "Bi Ehsas",
            artist = "Shadmehr Aghili",
            album = "Single",
            artworkUrl = "",
            previewUrl = "https://ups.musics-fa.com/tagdl/1403/Shadmehr%20-%20Bi%20Ehsas%20(320).mp3",
            durationMs = 210_000L,
            releaseYear = "2025",
            releaseDateIso = "2025",
            datePrecision = com.example.data.model.DatePrecision.YEAR,
            sourceId = "curated",
            isPreviewOnly = false
        )
        val previewDuplicate = Track(
            id = 2020L,
            title = "Bi-Ehsas",
            artist = "Shadmehr Aghili",
            album = "Single",
            artworkUrl = "",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/BiEhsasPreview.m4a",
            durationMs = 30_000L,
            releaseYear = "2025",
            releaseDateIso = "2025-07-19",
            datePrecision = com.example.data.model.DatePrecision.DAY,
            sourceId = "itunes",
            isPreviewOnly = true
        )

        val deduped = GroupEngine.deduplicateTracks(listOf(previewDuplicate, fullStream))
        assertEquals(1, deduped.size)
        val survivor = deduped.first()
        assertFalse("Full stream must be preferred over 30s preview", survivor.isPreviewOnly)
        assertEquals("2025-07-19", survivor.releaseDateIso)
        assertEquals(com.example.data.model.DatePrecision.DAY, survivor.datePrecision)
    }

    @Test
    fun `Section 2_5 EditorialBannerValidator rejects expired, inactive, and unsafe banner targets`() {
        val nowMs = com.example.data.provider.EditorialBannerValidator.parseIsoToEpochMs("2026-06-01T00:00:00Z")!!
        val validBanner = com.example.data.provider.EditorialBannerValidator.getDefaultEditorialBanners().first()
        assertTrue(com.example.data.provider.EditorialBannerValidator.isValid(validBanner, "IR", nowMs))

        val expiredBanner = validBanner.copy(
            id = "expired_1",
            endsAtIso = "2025-01-02T00:00:00Z"
        )
        assertFalse(com.example.data.provider.EditorialBannerValidator.isValid(expiredBanner, "IR", nowMs))

        val invalidTargetBanner = validBanner.copy(
            id = "bad_target",
            actionType = com.example.data.provider.EditorialBannerActionType.MOOD,
            actionTarget = "non_existent_mood_ target"
        )
        assertFalse(com.example.data.provider.EditorialBannerValidator.isValid(invalidTargetBanner, "IR", nowMs))

        val insecureImageBanner = validBanner.copy(
            id = "http_image",
            imageUrl = "http://insecure.example.com/banner.jpg"
        )
        assertFalse(com.example.data.provider.EditorialBannerValidator.isValid(insecureImageBanner, "IR", nowMs))
    }

    @Test
    fun `Section 3_1 ProviderRegistry documents verified statuses and rejects unauthorized scrapers`() {
        assertEquals(
            com.example.data.provider.ProviderStatus.APPROVED,
            com.example.data.provider.ProviderRegistry.getStatus("curated")
        )
        assertEquals(
            com.example.data.provider.ProviderStatus.APPROVED,
            com.example.data.provider.ProviderRegistry.getStatus("itunes")
        )
        assertEquals(
            com.example.data.provider.ProviderStatus.CONDITIONAL,
            com.example.data.provider.ProviderRegistry.getStatus("internet_archive")
        )
        assertEquals(
            com.example.data.provider.ProviderStatus.METADATA_ONLY,
            com.example.data.provider.ProviderRegistry.getStatus("musicbrainz")
        )
        assertEquals(
            com.example.data.provider.ProviderStatus.REJECTED,
            com.example.data.provider.ProviderRegistry.getStatus("musics_fa_scraper")
        )
        assertEquals(
            com.example.data.provider.ProviderStatus.REJECTED,
            com.example.data.provider.ProviderRegistry.getStatus("youtube_extractor")
        )
        assertEquals(
            com.example.data.provider.ProviderStatus.BLOCKED_NO_BACKEND,
            com.example.data.provider.ProviderRegistry.getStatus("firebase_phone_auth")
        )
    }

    @Test
    fun `Section 4_1 DriveStorageService removes 123456 backdoor, never leaks OTP to client, and enforces attempt limits`() = runTest {
        val service = com.example.data.remote.DriveStorageService(userPreferences = null)
        val phone = "+989123456789"

        // 1. Requesting OTP without a backend returns BLOCKED_NO_BACKEND and never leaks a code
        val dispatch = service.requestOtp("+98", "09123456789", "user@example.com", com.example.data.model.OtpMode.SMART_FALLBACK)
        assertFalse(dispatch.success)
        assertEquals(com.example.data.model.OtpVerificationStatus.BLOCKED_NO_BACKEND, dispatch.status)
        assertEquals(null, dispatch.generatedCodeForFallbackOrTest)

        // 2. Hardcoded "123456" backdoor is rejected across all modes
        com.example.data.model.OtpMode.entries.forEach { mode ->
            assertFalse(
                "Hardcoded 123456 must never be accepted in mode $mode",
                service.verifyOtp(phone, "123456", mode)
            )
        }

        // 3. Server-issued challenge enforces 5-attempt limit and one-time use
        val now = 1_700_000_000_000L
        service.registerServerIssuedChallengeForTest(phone, "654321", issuedAtMs = now)
        repeat(4) {
            assertEquals(
                com.example.data.model.OtpVerificationStatus.EXPIRED_OR_INVALID,
                service.verifyOtpDetailed(phone, "000000", com.example.data.model.OtpMode.SMART_FALLBACK, nowMs = now + 1000L)
            )
        }
        // 5th wrong attempt triggers MAX_ATTEMPTS_EXCEEDED and invalidates the challenge
        assertEquals(
            com.example.data.model.OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED,
            service.verifyOtpDetailed(phone, "000000", com.example.data.model.OtpMode.SMART_FALLBACK, nowMs = now + 2000L)
        )
        // Even the correct code is now rejected because the challenge was invalidated
        assertFalse(service.verifyOtp(phone, "654321", com.example.data.model.OtpMode.SMART_FALLBACK, nowMs = now + 3000L))
    }
}




