package com.example

import com.example.data.model.AudioQuality
import com.example.data.model.ErrorCategory
import com.example.data.model.Track
import com.example.data.repository.MusicsFaHtmlParser
import com.example.data.repository.SourceResolver
import com.example.util.BidiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            artworkUrl = "file:///android_asset/covers/ir_1.jpg",
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
}
