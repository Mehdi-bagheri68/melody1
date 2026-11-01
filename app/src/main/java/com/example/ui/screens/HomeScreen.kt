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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
import com.example.data.model.Track
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackItemCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Calendar

@Composable
fun HomeScreen(
    selectedCountry: Country,
    trendingTracks: List<Track>,
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
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top App Bar / Greeting
        item {
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
                    Text(
                        text = "موزیک پلیر MeloGroove",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Country Switcher Pill
                    Surface(
                        modifier = Modifier
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
                                text = selectedCountry.nameFa,
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Car Mode Shortcut
                    IconButton(
                        onClick = onDriveClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(OledSurfaceVariant)
                            .testTag("car_mode_shortcut")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Driving Mode",
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Moods & Activities Carousel
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                SectionHeader(
                    title = "مودها و فعالیت‌ها",
                    subtitle = "پلی‌لیست‌های هوشمند متناسب با حس و حال شما"
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(CountriesData.moodCategories) { mood ->
                        val isSelected = activeMood?.id == mood.id

                        Card(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AccentGold else BorderSubtle,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onMoodClick(mood) }
                                .testTag("mood_${mood.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) OledSurfaceHigh else OledSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = mood.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mood.titleFa,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) AccentGold else TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Mood Tracks (if mood selected)
        if (activeMood != null) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(
                    title = "موزیک‌های ${activeMood.titleFa} ${activeMood.iconEmoji}",
                    subtitle = "منتخب بر اساس سلیقه شما"
                )
            }

            if (isLoadingMood) {
                item {
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
                items(moodTracks.take(8)) { track ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TrackItemCard(
                            track = track,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isCurrentTrack = currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, moodTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToPlaylistClick = { onAddToPlaylistClick(track) }
                        )
                    }
                }
            }
        }

        // Recently Played Horizontal Shelf (if exists)
        if (recentHistory.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                SectionHeader(
                    title = "اخیراً گوش داده‌اید",
                    subtitle = "ادامه پخش آخرین آهنگ‌ها"
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentHistory.take(10)) { track ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { onTrackClick(track, recentHistory) },
                            colors = CardDefaults.cardColors(containerColor = OledSurface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(OledSurfaceVariant),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(AccentGold),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Smart Recommendations & Trending Section for Selected Country
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title = "ترندترین‌های ${selectedCountry.nameFa} ${selectedCountry.flag}",
                subtitle = "محبوب‌ترین آهنگ‌های درحال پخش در این کشور"
            )
        }

        if (isLoadingTrending) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentGold)
                }
            }
        } else {
            itemsIndexed(trendingTracks) { index, track ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    TrackItemCard(
                        track = track,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        isCurrentTrack = currentTrack?.id == track.id,
                        onTrackClick = { onTrackClick(track, trendingTracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                        trailingContent = {
                            // Rank number badge (#1, #2, #3...)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (index < 3) Color(0x33FFB800) else OledSurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (index < 3) AccentGold else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "صبح بخیر ☀️"
        in 12..16 -> "عصر بخیر 🌤️"
        in 17..22 -> "غروب بخیر 🌆"
        else -> "شب بخیر 🌙"
    }
}
