package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AudioQuality
import com.example.data.model.CountriesData
import com.example.data.model.PlaybackMode
import com.example.data.model.Track
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GradientGoldEnd
import com.example.ui.theme.GradientGoldStart
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    track: Track?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackMode: PlaybackMode,
    audioQuality: AudioQuality,
    onDismiss: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onStopAndClear: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectPlaybackMode: (PlaybackMode) -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onArtistClick: (String) -> Unit,
    onCycleAudioQuality: () -> Unit
) {
    if (track == null) return
    val lang = LocalAppLanguage.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionRatio by remember { mutableFloatStateOf(0f) }

    val actualProgressRatio = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayRatio = if (isUserSeeking) seekPositionRatio else actualProgressRatio

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OledBackground,
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            OledSurfaceVariant,
                            OledBackground,
                            OledBackground
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("dismiss_player_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = TextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tr(lang, "درحال پخش", "Now Playing"),
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold
                        )
                        val country = CountriesData.allCountries.find { it.code.equals(track.countryCode, ignoreCase = true) }
                        if (country != null) {
                            Text(
                                text = "${country.flag} ${country.getDisplayName(lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onAddToPlaylist,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("player_add_playlist_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = onStopAndClear,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("player_stop_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = tr(lang, "توقف و حذف پخش", "Stop & Clear"),
                                tint = AccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Vinyl Disc Artwork Section
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0D14))
                        .border(4.dp, BorderHighlight, CircleShape)
                        .rotate(if (isPlaying) rotationAngle else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color(0xFF141923), radius = size.minDimension / 2.1f)
                        drawCircle(color = Color(0xFF1B2230), radius = size.minDimension / 2.3f)
                        drawCircle(color = Color(0xFF0F141E), radius = size.minDimension / 2.7f)
                    }

                    AsyncImage(
                        model = track.highResArtworkUrl,
                        contentDescription = track.title,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, AccentGold, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(OledBackground)
                            .border(2.dp, AccentOrange, CircleShape)
                    )
                }

                // Track Title and Clickable Artist Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Clickable Artist Pill to open Artist Discography
                    Surface(
                        color = OledSurfaceHigh,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                        modifier = Modifier
                            .clickable { onArtistClick(track.artist) }
                            .testTag("player_artist_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(lang, "${track.artist} (مشاهده آرشیو خواننده)", "${track.artist} (View Artist Archive)"),
                                style = MaterialTheme.typography.labelLarge,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr(lang, "${track.album} • سال ${track.releaseYear}", "${track.album} • ${track.releaseYear}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }

                // Repeat One / Repeat Group (All) / Shuffle Selector Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(PlaybackMode.REPEAT_ONE, tr(lang, "تکرار ۱ آهنگ", "Repeat 1"), Icons.Default.RepeatOne),
                        Triple(PlaybackMode.REPEAT_ALL, tr(lang, "تکرار گروه/لیست", "Repeat Queue"), Icons.Default.Repeat),
                        Triple(PlaybackMode.SHUFFLE, tr(lang, "پخش تصادفی", "Shuffle"), Icons.Default.Shuffle)
                    ).forEach { (mode, label, icon) ->
                        val isSelected = playbackMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AccentGold else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectPlaybackMode(mode) }
                                .testTag("mode_btn_${mode.name}"),
                            color = if (isSelected) OledSurfaceHigh else OledSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) AccentGold else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) AccentGold else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Interactive Progress Slider & Timestamps
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = displayRatio,
                        onValueChange = {
                            isUserSeeking = true
                            seekPositionRatio = it
                        },
                        onValueChangeFinished = {
                            val newMs = (seekPositionRatio * durationMs).toLong()
                            onSeekTo(newMs)
                            isUserSeeking = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = Color(0x33FFB800)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentMs = if (isUserSeeking) (seekPositionRatio * durationMs).toLong() else currentPositionMs
                        Text(
                            text = formatMs(currentMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = formatMs(durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop & Clear Playback button
                    IconButton(
                        onClick = onStopAndClear,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "توقف کامل",
                            tint = AccentOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("player_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Main Glowing Play / Pause Button
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GradientGoldStart, GradientGoldEnd)
                                )
                            )
                            .clickable(onClick = onPlayPauseToggle)
                            .testTag("player_main_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Skip Next
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("player_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_fav_toggle")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) AccentOrange else TextTertiary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Audio Quality & Info Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OledSurfaceHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .clickable(onClick = onCycleAudioQuality)
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = "Audio Quality",
                            tint = AccentGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${audioQuality.getTitle(lang)} (${audioQuality.bitrate})",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
