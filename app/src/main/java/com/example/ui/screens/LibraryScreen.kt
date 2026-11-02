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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
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
    onArtistClick: (String) -> Unit,
    onStopTrackClick: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onClearHistory: () -> Unit
) {
    val lang = LocalAppLanguage.current
    // UI-05: Preserve selectedTab across navigation and orientation changes
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        tr(lang, "علاقه‌مندی‌ها (${favorites.size})", "Liked (${favorites.size})"),
        tr(lang, "پلی‌لیست‌ها (${playlists.size})", "Playlists (${playlists.size})"),
        tr(lang, "تاریخچه (${listeningHistory.size})", "History (${listeningHistory.size})")
    )

    // UI-06: Confirmation Dialog before clearing listening history
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = OledSurfaceHigh,
            title = {
                Text(
                    text = tr(lang, "پاکسازی تاریخچه پخش", "Clear Listening History"),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = tr(
                        lang,
                        "آیا مطمئن هستید که می‌خواهید تمام ${listeningHistory.size} آهنگ موجود در تاریخچه پخش را حذف کنید؟",
                        "Are you sure you want to delete all ${listeningHistory.size} tracks from your listening history?"
                    ),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_clear_history_btn")
                ) {
                    Text(tr(lang, "حذف تاریخچه", "Delete All"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(tr(lang, "انصراف", "Cancel"), color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(lang, "کتابخانه من", "My Music"),
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
                    Text(tr(lang, "پلی‌لیست جدید", "New Playlist"), style = MaterialTheme.typography.labelMedium)
                }
            } else if (selectedTab == 2 && listeningHistory.isNotEmpty()) {
                IconButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = AccentOrange
                    )
                }
            }
        }

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

        when (selectedTab) {
            0 -> FavoritesTabContent(
                favorites = favorites,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onTrackClick = onTrackClick,
                onArtistClick = onArtistClick,
                onStopTrackClick = onStopTrackClick,
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
                onArtistClick = onArtistClick,
                onStopTrackClick = onStopTrackClick,
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
    onArtistClick: (String) -> Unit,
    onStopTrackClick: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    val lang = LocalAppLanguage.current
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
                    text = tr(lang, "هنوز آهنگی را مورد علاقه نکرده‌اید", "No liked songs yet"),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(
                        lang,
                        "برای دسترسی سریع، آیکون قلب را روی هر آهنگ لمس کنید",
                        "Tap the heart icon on any track to save it here"
                    ),
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
            item(key = "fav_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(lang, "${favorites.size} آهنگ ذخیره شده", "${favorites.size} saved tracks"),
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
                        Text(tr(lang, "پخش پشت سر هم همه", "Play All"), color = TextPrimary)
                    }
                }
            }

            items(
                items = favorites,
                key = { track -> "fav_${track.id}" }
            ) { track ->
                TrackItemCard(
                    track = track,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isCurrentTrack = currentTrack?.id == track.id,
                    onTrackClick = { onTrackClick(track, favorites) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                    onArtistClick = onArtistClick,
                    onStopTrackClick = onStopTrackClick
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
    val lang = LocalAppLanguage.current
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
                    text = tr(lang, "پلی‌لیستی ساخته نشده است", "No playlists created yet"),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tr(
                        lang,
                        "می‌توانید برای رانندگی، مهمانی یا ورزش پوشه‌های سفارشی بسازید",
                        "Create custom playlists for driving, workouts, or parties"
                    ),
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
                    Text(tr(lang, "ساخت اولین پلی‌لیست", "Create First Playlist"))
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = playlists,
                key = { pl -> "playlist_${pl.id}" }
            ) { pl ->
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
                                text = tr(lang, "${pl.trackCount} آهنگ", "${pl.trackCount} tracks"),
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
    onArtistClick: (String) -> Unit,
    onStopTrackClick: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    val lang = LocalAppLanguage.current
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
                    text = tr(lang, "تاریخچه‌ای ثبت نشده است", "No listening history yet"),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(
                        lang,
                        "هر آهنگی که پخش کنید در این بخش ثبت خواهد شد",
                        "Tracks you play will automatically appear here"
                    ),
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
            items(
                items = listeningHistory,
                key = { track -> "history_${track.id}" }
            ) { track ->
                TrackItemCard(
                    track = track,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isCurrentTrack = currentTrack?.id == track.id,
                    onTrackClick = { onTrackClick(track, listeningHistory) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                    onArtistClick = onArtistClick,
                    onStopTrackClick = onStopTrackClick
                )
            }
        }
    }
}
