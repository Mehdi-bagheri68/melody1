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
}
