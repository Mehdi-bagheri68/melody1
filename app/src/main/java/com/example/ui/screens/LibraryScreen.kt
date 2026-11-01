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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.TrackItemCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    favorites: List<Track>,
    playlists: List<Playlist>,
    listeningHistory: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onClearHistory: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("علاقه‌مندی‌ها", "پلی‌لیست‌ها", "تاریخچه گوش دادن")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کتابخانه من",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (selectedTab == 1) {
                Button(
                    onClick = onCreatePlaylistClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("create_playlist_tab_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("پلی‌لیست جدید", style = MaterialTheme.typography.labelMedium)
                }
            } else if (selectedTab == 2 && listeningHistory.isNotEmpty()) {
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = AccentOrange
                    )
                }
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = OledBackground,
            contentColor = AccentGold,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentGold
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selectedTab == index) AccentGold else TextSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> FavoritesTabContent(
                favorites = favorites,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onTrackClick = onTrackClick,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylistClick = onAddToPlaylistClick
            )
            1 -> PlaylistsTabContent(
                playlists = playlists,
                onCreatePlaylistClick = onCreatePlaylistClick,
                onOpenPlaylist = onOpenPlaylist
            )
            2 -> HistoryTabContent(
                listeningHistory = listeningHistory,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onTrackClick = onTrackClick,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylistClick = onAddToPlaylistClick
            )
        }
    }
}

@Composable
private fun FavoritesTabContent(
    favorites: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "هنوز آهنگی را مورد علاقه نکرده‌اید",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "برای دسترسی سریع، آیکون قلب را روی هر آهنگ لمس کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Action button to play all favorites
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${favorites.size} آهنگ ذخیره شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { favorites.firstOrNull()?.let { onTrackClick(it, favorites) } },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OledSurfaceVariant)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پخش همه", color = TextPrimary)
                    }
                }
            }

            items(favorites) { track ->
                TrackItemCard(
                    track = track,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isCurrentTrack = currentTrack?.id == track.id,
                    onTrackClick = { onTrackClick(track, favorites) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(track) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<Playlist>,
    onCreatePlaylistClick: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "پلی‌لیستی ساخته نشده است",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "می‌توانید برای رانندگی، مهمانی یا ورزش پوشه‌های سفارشی بسازید",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreatePlaylistClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color.Black
                    )
                ) {
                    Text("ساخت اولین پلی‌لیست")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(playlists) { pl ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .clickable { onOpenPlaylist(pl) }
                        .testTag("playlist_item_${pl.id}"),
                    colors = CardDefaults.cardColors(containerColor = OledSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OledSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (pl.description.isNotEmpty()) {
                                Text(
                                    text = pl.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${pl.trackCount} آهنگ",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTabContent(
    listeningHistory: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    if (listeningHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "تاریخچه‌ای ثبت نشده است",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "هر آهنگی که پخش کنید در این بخش ثبت خواهد شد",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listeningHistory) { track ->
                TrackItemCard(
                    track = track,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isCurrentTrack = currentTrack?.id == track.id,
                    onTrackClick = { onTrackClick(track, listeningHistory) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(track) }
                )
            }
        }
    }
}
