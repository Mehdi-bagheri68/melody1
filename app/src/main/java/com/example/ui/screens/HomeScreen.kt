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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
import com.example.data.model.Track
import com.example.data.provider.EditorialBanner
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.components.HorizontalShelfTrackCard
import com.example.ui.components.ModernMoodCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackItemCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    onRetryTrending: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    editorialBanners: List<EditorialBanner> = emptyList(),
    onEditorialBannerClick: (EditorialBanner) -> Unit = {},
    homeLastUpdatedMs: Long = 0L,
    isLiveProviderBacked: Boolean = false,
    onRefreshHome: () -> Unit = onRetryTrending
) {
    val lang = LocalAppLanguage.current
    val greeting = rememberGreeting(lang)
    val heroBanner = editorialBanners.firstOrNull()
    val formattedUpdatedTime = remember(homeLastUpdatedMs) {
        if (homeLastUpdatedMs <= 0L) "--:--"
        else SimpleDateFormat("HH:mm", Locale.US).format(Date(homeLastUpdatedMs))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        // Top App Bar / Greeting + Country Switcher + Refresh + Car Mode + Profile Icon
        item(key = "home_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                        Spacer(modifier = Modifier.width(6.dp))

                        // Car Mode Shortcut
                        IconButton(
                            onClick = onDriveClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
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

                        Spacer(modifier = Modifier.width(6.dp))

                        // User Profile Shortcut
                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OledSurfaceVariant)
                                .border(1.dp, BorderHighlight, CircleShape)
                                .testTag("home_profile_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = tr(lang, "پروفایل کاربری", "User Profile"),
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stale-While-Revalidate Freshness & Source Attribution Bar (Section 2.1 & 2.3)
                Surface(
                    color = OledSurfaceVariant.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLiveProviderBacked) {
                                    tr(
                                        lang,
                                        "منبع داده: آنلاین (iTunes + آرشیو منتخب) • بروزرسانی: \u200E$formattedUpdatedTime",
                                        "Source: Live Providers (iTunes + Curated) • Updated $formattedUpdatedTime"
                                    )
                                } else {
                                    tr(
                                        lang,
                                        "منبع داده: کاتالوگ منتخب داخلی • آخرین بررسی: \u200E$formattedUpdatedTime",
                                        "Source: Curated Catalog (Offline-Ready) • Checked $formattedUpdatedTime"
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = Color(0x28FFB800),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isLoadingTrending, onClick = onRefreshHome)
                                .testTag("home_refresh_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLoadingTrending) {
                                    CircularProgressIndicator(
                                        color = AccentGold,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(13.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = tr(lang, "بروزرسانی فید", "Refresh Feed"),
                                        tint = AccentGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(lang, "بروزرسانی", "Refresh"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2.5: Validated Dynamic Editorial Hero Banner
        if (heroBanner != null) {
            item(key = "home_editorial_banner_${heroBanner.id}") {
                EditorialHeroCard(
                    banner = heroBanner,
                    lang = lang,
                    onClick = { onEditorialBannerClick(heroBanner) }
                )
            }
        }

        // 1. Modern 2-Column Explore & Moods Grid (Dynamic per Selected Country)
        item(key = "home_moods_${selectedCountry.code}") {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                SectionHeader(
                    title = tr(
                        lang,
                        "کشف و دسته‌بندی‌های ${selectedCountry.nameFa} ${selectedCountry.flag}",
                        "Explore & Moods in ${selectedCountry.nameEn} ${selectedCountry.flag}"
                    ),
                    subtitle = tr(
                        lang,
                        "انتشارهای منتخب و سبک‌های موسیقی اختصاصی ${selectedCountry.nameFa}",
                        "Curated moods & regional genres for ${selectedCountry.nameEn}"
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

        // Active Mood Tracks (if a Mood is selected)
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

        // 2. Latest Releases / Curated Catalog Releases Shelf (Section 2.2 — Honest Date Precision & Source Labels)
        if (newReleaseTracks.isNotEmpty()) {
            item(key = "home_new_releases_${selectedCountry.code}") {
                Spacer(modifier = Modifier.height(22.dp))
                val hasExactDates = newReleaseTracks.any { it.datePrecision.weight >= 2 }
                SectionHeader(
                    title = if (hasExactDates) {
                        tr(
                            lang,
                            "تازه‌ترین انتشارهای ${selectedCountry.nameFa} ${selectedCountry.flag}",
                            "Latest Releases in ${selectedCountry.nameEn} ${selectedCountry.flag}"
                        )
                    } else {
                        tr(
                            lang,
                            "انتشارهای منتخب کاتالوگ ${selectedCountry.nameFa} ${selectedCountry.flag}",
                            "Curated Catalog Releases • ${selectedCountry.nameEn} ${selectedCountry.flag}"
                        )
                    },
                    subtitle = if (hasExactDates) {
                        tr(
                            lang,
                            "مرتب‌شده بر اساس تاریخ دقیق انتشار از منابع رسمی (حداکثر ۱ اثر از هر هنرمند)",
                            "Sorted by verified release date precision (max 1 per artist)"
                        )
                    } else {
                        tr(
                            lang,
                            "آرشیو منتخب بر اساس سال انتشار (${newReleaseTracks.firstOrNull()?.releaseYear ?: "2026"}) • حداکثر ۱ اثر از هر هنرمند",
                            "Curated catalog ordered by release year • Max 1 per artist"
                        )
                    }
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
                            badgeText = track.getReleaseBadgeText(lang),
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, newReleaseTracks) },
                            onArtistClick = onArtistClick
                        )
                    }
                }
            }
        }

        // 3. Popular / Editor Picks Section (Section 2.3 — Honest Popularity Signal without Fabricated Play Counts)
        item(key = "home_trending_${selectedCountry.code}") {
            Spacer(modifier = Modifier.height(22.dp))
            SectionHeader(
                title = if (isLiveProviderBacked) {
                    tr(
                        lang,
                        "محبوب‌ترین‌ها در منابع آنلاین (${selectedCountry.nameFa}) ${selectedCountry.flag}",
                        "Popular on Live Providers (${selectedCountry.nameEn}) ${selectedCountry.flag}"
                    )
                } else {
                    tr(
                        lang,
                        "منتخب سردبیر و کاتالوگ ${selectedCountry.nameFa} ${selectedCountry.flag}",
                        "Editor & Catalog Picks in ${selectedCountry.nameEn} ${selectedCountry.flag}"
                    )
                },
                subtitle = if (isLiveProviderBacked) {
                    tr(
                        lang,
                        "رتبه‌بندی منابع آنلاین و آثار برگزیده • بدون تکرار از ردیف انتشارهای جدید",
                        "Live provider ranking & editor picks • Deduplicated across shelves"
                    )
                } else {
                    tr(
                        lang,
                        "آثار برگزیده آرشیو داخلی برای ${selectedCountry.nameFa} • روی نام خواننده بزنید",
                        "Curated catalog highlights for ${selectedCountry.nameEn} • Tap artist for discography"
                    )
                }
            )

            if (isLoadingTrending && trendingTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentGold)
                }
            } else if (trendingTracks.isEmpty() || trendingErrorMessage != null) {
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
                                "خطا در دریافت لیست آثار منتخب",
                                "Unable to load featured tracks"
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
                        val signalBadge = track.popularitySignal?.getDisplayBadge(lang)
                            ?: if (lang == AppLanguage.FA) "منتخب #${index + 1}" else "Pick #${index + 1}"
                        HorizontalShelfTrackCard(
                            track = track,
                            badgeText = signalBadge,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, trendingTracks) },
                            onArtistClick = onArtistClick
                        )
                    }
                }
            }
        }

        // 4. Recently Played Section (اخیر شنیده‌ها)
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
                            tr(lang, "پیشنهاد کاتالوگ", "CATALOG PICK"),
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
private fun EditorialHeroCard(
    banner: EditorialBanner,
    lang: AppLanguage,
    onClick: () -> Unit
) {
    val accentColor = Color(banner.accentHex)
    val secondaryColor = Color(banner.secondaryHex)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.24f),
                        secondaryColor.copy(alpha = 0.18f),
                        OledSurfaceVariant
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
            .testTag("editorial_hero_banner")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = banner.getBadge(lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = banner.getTitle(lang),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = banner.getSubtitle(lang),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = accentColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("editorial_banner_cta")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = banner.getCta(lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold
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
