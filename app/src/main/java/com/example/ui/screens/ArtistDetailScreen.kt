package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ArtistSortOption
import com.example.data.model.PlaybackMode
import com.example.data.model.Track
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.components.TrackItemCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ArtistDetailScreen(
    artistName: String,
    tracks: List<Track>,
    sortOption: ArtistSortOption,
    isLoading: Boolean,
    currentTrack: Track?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSortChanged: (ArtistSortOption) -> Unit,
    onPlayTrackInArtistQueue: (Track, List<Track>, PlaybackMode?) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onStopTrackClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    BackHandler { onBack() }

    val heroImage = tracks.firstOrNull()?.highResArtworkUrl
        ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model = heroImage,
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x66090C10),
                                    Color(0xCC090C10),
                                    OledBackground
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x99111622))
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        color = Color(0x44FFB800),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tr(lang, "آرشیو کامل خواننده", "Artist Complete Discography"),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = tr(
                            lang,
                            "${tracks.size} آهنگ کامل • پخش خودکار پشت سر هم",
                            "${tracks.size} full tracks • Auto-advancing queue"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        if (tracks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onPlayTrackInArtistQueue(tracks.first(), tracks, PlaybackMode.REPEAT_ALL)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("artist_play_all_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(lang, "پخش پشت سر هم همه", "Play All"), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val randomTrack = tracks.random()
                            onPlayTrackInArtistQueue(randomTrack, tracks, PlaybackMode.SHUFFLE)
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("artist_shuffle_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OledSurfaceHigh,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(lang, "تصادفی", "Shuffle"))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tr(lang, "مرتب‌سازی بر اساس:", "Sort by:"),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArtistSortOption.values().forEach { option ->
                        val isSelected = sortOption == option
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AccentGold else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSortChanged(option) }
                                .testTag("sort_artist_${option.name}"),
                            color = if (isSelected) OledSurfaceHigh else OledSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (option == ArtistSortOption.TRENDING)
                                        Icons.Default.TrendingUp else Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isSelected) AccentGold else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = option.getTitle(lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) AccentGold else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isLoading) {
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
            itemsIndexed(tracks) { _, track ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    TrackItemCard(
                        track = track,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        isCurrentTrack = currentTrack?.id == track.id,
                        onTrackClick = {
                            onPlayTrackInArtistQueue(track, tracks, null)
                        },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                        onArtistClick = null,
                        onStopTrackClick = onStopTrackClick
                    )
                }
            }
        }
    }
}
