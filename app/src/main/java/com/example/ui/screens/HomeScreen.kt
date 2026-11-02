package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
import com.example.data.model.Track
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.components.HorizontalShelfTrackCard
import com.example.ui.components.ModernMoodCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackItemCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun HomeScreen(
    selectedCountry: Country,
    trendingTracks: List<Track>,
    newReleaseTracks: List<Track>,
    isLoadingTrending: Boolean,
    recentHistory: List<Track>,
    activeMood: MoodCategory?,
    moodTracks: List<Track>,
    isLoadingMood: Boolean,
    currentTrack: Track?,
    isPlaying: Boolean,
    onCountryClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDriveClick: () -> Unit,
    onMoodClick: (MoodCategory) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onArtistClick: (String) -> Unit,
    onStopTrackClick: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    trendingErrorMessage: String? = null,
    onRetryTrending: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val greeting = rememberGreeting(lang)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        // Top App Bar / Greeting + Country Switcher + Car Mode Icon
        item(key = "home_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MeloGroove",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0x33FFB800),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "v${com.example.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Country Switcher Pill
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, BorderHighlight, RoundedCornerShape(20.dp))
                            .clickable(onClick = onCountryClick)
                            .testTag("country_switcher_pill"),
                        color = OledSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedCountry.flag, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCountry.getDisplayName(lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Car Mode Shortcut (Top-Bar next to Country Selector)
                    IconButton(
                        onClick = onDriveClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(OledSurfaceVariant)
                            .testTag("car_mode_shortcut")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = tr(lang, "حالت رانندگی", "Driving Mode"),
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 1. Modern 2-Column Explore & Moods Grid (Dynamic per Selected Country)
        item(key = "home_moods_${selectedCountry.code}") {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                SectionHeader(
                    title = tr(
                        lang,
                        "کشف و دسته‌بندی‌های ${selectedCountry.nameFa} ${selectedCountry.flag}",
                        "Explore & Moods in ${selectedCountry.nameEn} ${selectedCountry.flag}"
                    ),
                    subtitle = tr(
                        lang,
                        "بروزترین‌ها و سبک‌های موسیقی اختصاصی ${selectedCountry.nameFa}",
                        "New releases & curated genres for ${selectedCountry.nameEn}"
                    )
                )

                val moods = CountriesData.getMoodCategoriesForCountry(selectedCountry)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    moods.chunked(2).forEach { rowMoods ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowMoods.forEach { mood ->
                                ModernMoodCard(
                                    mood = mood,
                                    isSelected = activeMood?.id == mood.id,
                                    onClick = { onMoodClick(mood) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowMoods.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Active Mood Tracks (if a Mood or "بروزترین‌ها" is selected)
        if (activeMood != null) {
            item(key = "active_mood_header_${activeMood.id}") {
                Spacer(modifier = Modifier.height(18.dp))
                SectionHeader(
                    title = tr(
                        lang,
                        "لیست ${activeMood.titleFa} (${selectedCountry.nameFa})",
                        "${activeMood.titleEn} (${selectedCountry.nameEn})"
                    ),
                    subtitle = tr(
                        lang,
                        "پخش خودکار پشت سر هم تمام آهنگ‌های این بخش",
                        "Continuous auto-play queue for this category"
                    )
                )
            }

            if (isLoadingMood) {
                item(key = "mood_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                items(
                    items = moodTracks,
                    key = { track -> "mood_${activeMood.id}_${track.id}" }
                ) { track ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TrackItemCard(
                            track = track,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isCurrentTrack = currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, moodTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                            onArtistClick = onArtistClick,
                            onStopTrackClick = onStopTrackClick
                        )
                    }
                }
            }
        }

        // 2. Just Released / Newest Tracks Shelf (بروزترین انتشارها - Horizontal Carousel)
        if (newReleaseTracks.isNotEmpty()) {
            item(key = "home_new_releases_${selectedCountry.code}") {
                Spacer(modifier = Modifier.height(22.dp))
                SectionHeader(
                    title = tr(
                        lang,
                        "بروزترین انتشارهای ${selectedCountry.nameFa} ${selectedCountry.flag}",
                        "Just Released in ${selectedCountry.nameEn} ${selectedCountry.flag}"
                    ),
                    subtitle = tr(
                        lang,
                        "جدیدترین آهنگ‌های منتشر شده (${newReleaseTracks.firstOrNull()?.releaseYear ?: "2026"})",
                        "Latest songs sorted by release year"
                    )
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = newReleaseTracks,
                        key = { track -> "new_${track.id}" }
                    ) { track ->
                        HorizontalShelfTrackCard(
                            track = track,
                            badgeText = "NEW ${track.releaseYear}",
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, newReleaseTracks) },
                            onArtistClick = onArtistClick
                        )
                    }
                }
            }
        }

        // 3. Trending & Popular Section directly under New Releases in the EXACT SAME Horizontal Carousel Format!
        item(key = "home_trending_${selectedCountry.code}") {
            Spacer(modifier = Modifier.height(22.dp))
            SectionHeader(
                title = tr(
                    lang,
                    "پرطرفدارها و ترند ${selectedCountry.nameFa} ${selectedCountry.flag}",
                    "Trending in ${selectedCountry.nameEn} ${selectedCountry.flag}"
                ),
                subtitle = tr(
                    lang,
                    "محبوب‌ترین آهنگ‌های روز • برای آرشیو خواننده روی نام او بزنید",
                    "Most popular tracks • Tap artist name for full discography"
                )
            )

            if (isLoadingTrending) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentGold)
                }
            } else if (trendingTracks.isEmpty() || trendingErrorMessage != null) {
                // UI-02: Explicit error / empty state with Retry button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = trendingErrorMessage ?: tr(
                                lang,
                                "خطا در دریافت لیست ترندها",
                                "Unable to load trending tracks"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRetryTrending,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("retry_trending_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(lang, "تلاش مجدد", "Retry"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        items = trendingTracks,
                        key = { _, track -> "trending_${track.id}" }
                    ) { index, track ->
                        HorizontalShelfTrackCard(
                            track = track,
                            badgeText = "🔥 #${index + 1}",
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, trendingTracks) },
                            onArtistClick = onArtistClick
                        )
                    }
                }
            }
        }

        // 4. Recently Played Section (اخیر شنیده‌ها) right below Trending in the exact same Horizontal Shelf Format!
        item(key = "home_recent_played") {
            Spacer(modifier = Modifier.height(22.dp))
            SectionHeader(
                title = tr(lang, "اخیر شنیده‌ها", "Recently Played"),
                subtitle = tr(
                    lang,
                    "ادامه پخش آخرین آهنگ‌هایی که گوش داده‌اید",
                    "Pick up right where you left off"
                )
            )

            val displayRecent = if (recentHistory.isNotEmpty()) recentHistory else trendingTracks.take(5)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    items = displayRecent,
                    key = { track -> "recent_${track.id}" }
                ) { track ->
                    HorizontalShelfTrackCard(
                        track = track,
                        badgeText = if (recentHistory.isNotEmpty())
                            tr(lang, "پخش اخیر", "RECENT")
                        else
                            tr(lang, "پیشنهادی", "FOR YOU"),
                        isCurrentTrack = currentTrack?.id == track.id,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        onTrackClick = { onTrackClick(track, displayRecent) },
                        onArtistClick = onArtistClick
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberGreeting(lang: AppLanguage): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> tr(lang, "صبح بخیر ☀️", "Good Morning ☀️")
        in 12..16 -> tr(lang, "عصر بخیر 🌤️", "Good Afternoon 🌤️")
        in 17..22 -> tr(lang, "غروب بخیر 🌆", "Good Evening 🌆")
        else -> tr(lang, "شب بخیر 🌙", "Good Night 🌙")
    }
}
