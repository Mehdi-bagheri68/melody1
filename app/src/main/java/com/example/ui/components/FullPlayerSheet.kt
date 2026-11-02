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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioQuality
import com.example.data.model.CountriesData
import com.example.data.model.PlaybackError
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
import com.example.util.BidiHelper

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
    hasQualityVariant: Boolean = true,
    effectiveBitrateLabel: String = "320 kbps",
    isRetrying: Boolean = false,
    playbackError: PlaybackError? = null,
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
    onCycleAudioQuality: () -> Unit,
    onManualRetry: () -> Unit = {}
) {
    if (track == null) return
    val lang = LocalAppLanguage.current
    val dir = if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionRatio by remember { mutableFloatStateOf(0f) }

    val hasValidDuration = durationMs > 0L
    val actualProgressRatio = if (hasValidDuration) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayRatio = if (isUserSeeking && hasValidDuration) seekPositionRatio else actualProgressRatio

    // Fixes UI-09: Only allocate and run the infinite rotation transition while actively playing!
    val rotationAngle = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        angle
    } else {
        0f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OledBackground,
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides dir) {
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
                                .minimumInteractiveComponentSize()
                                .testTag("dismiss_player_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = tr(lang, "بستن پنجره پخش", "Collapse Player"),
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
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_add_playlist_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = tr(lang, "افزودن به پلی‌لیست", "Add to playlist"),
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = onStopAndClear,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
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
                            .size(210.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0D14))
                            .border(4.dp, BorderHighlight, CircleShape)
                            .rotate(rotationAngle),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color(0xFF141923), radius = size.minDimension / 2.1f)
                            drawCircle(color = Color(0xFF1B2230), radius = size.minDimension / 2.3f)
                            drawCircle(color = Color(0xFF0F141E), radius = size.minDimension / 2.7f)
                        }

                        TrackArtwork(
                            imageUrl = track.highResArtworkUrl,
                            seedId = track.id,
                            title = track.title,
                            showTextOverlay = false,
                            modifier = Modifier
                                .size(106.dp)
                                .clip(CircleShape)
                                .border(2.dp, AccentGold, CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(OledBackground)
                                .border(2.dp, AccentOrange, CircleShape)
                        )
                    }

                    // Optional Error / Reconnecting Status Banner (Fixes BUG-02 & UI-07)
                    if (isRetrying || playbackError != null) {
                        Surface(
                            color = OledSurfaceHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isRetrying) {
                                        tr(lang, "در حال اتصال مجدد هوشمند…", "Auto-reconnecting with backoff…")
                                    } else {
                                        playbackError?.getMessage(lang).orEmpty()
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentOrange,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isRetrying) {
                                    TextButton(
                                        onClick = onManualRetry,
                                        modifier = Modifier.testTag("full_player_retry_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tr(lang, "تلاش مجدد", "Retry"),
                                            color = AccentGold,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Track Title and Clickable Artist Details
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = BidiHelper.formatMixed(lang.isRtl, track.title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))

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
                                    text = tr(
                                        lang,
                                        "${BidiHelper.formatMixed(lang.isRtl, track.artist)} (مشاهده آرشیو خواننده)",
                                        "${track.artist} (View Artist Archive)"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(
                                lang,
                                "${track.album} • سال ${BidiHelper.isolateLtr(track.releaseYear)}",
                                "${track.album} • ${track.releaseYear}"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Fixes BUG-05 & UI-08: Slider is disabled when durationMs <= 0L and displays "--:--"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = displayRatio,
                            enabled = hasValidDuration,
                            onValueChange = {
                                if (hasValidDuration) {
                                    isUserSeeking = true
                                    seekPositionRatio = it
                                }
                            },
                            onValueChangeFinished = {
                                if (hasValidDuration) {
                                    val newMs = (seekPositionRatio * durationMs).toLong()
                                    onSeekTo(newMs)
                                }
                                isUserSeeking = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_seek_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = AccentGold,
                                activeTrackColor = AccentGold,
                                inactiveTrackColor = Color(0x33FFB800),
                                disabledThumbColor = TextTertiary,
                                disabledActiveTrackColor = TextTertiary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currentMs = if (isUserSeeking && hasValidDuration) {
                                (seekPositionRatio * durationMs).toLong()
                            } else {
                                currentPositionMs
                            }
                            Text(
                                text = BidiHelper.isolateLtr(formatMs(currentMs, allowZero = true)),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = BidiHelper.isolateLtr(formatMs(durationMs, allowZero = false)),
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
                        IconButton(
                            onClick = onStopAndClear,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = tr(lang, "توقف کامل", "Stop Playback"),
                                tint = AccentOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("player_prev_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = tr(lang, "آهنگ قبلی", "Previous Track"),
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

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
                                    contentDescription = if (isPlaying) {
                                        tr(lang, "مکث", "Pause")
                                    } else {
                                        tr(lang, "پخش", "Play")
                                    },
                                    tint = Color.Black,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("player_next_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = tr(lang, "آهنگ بعدی", "Next Track"),
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_fav_toggle")
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (track.isFavorite) {
                                    tr(lang, "حذف از علاقه‌مندی‌ها", "Remove from Favorites")
                                } else {
                                    tr(lang, "افزودن به علاقه‌مندی‌ها", "Add to Favorites")
                                },
                                tint = if (track.isFavorite) AccentOrange else TextTertiary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Fixes BUG-09 & UI-10: Honest Audio Quality Pill showing whether multi-bitrate switching is supported for this track
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = OledSurfaceHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .clickable(enabled = hasQualityVariant, onClick = onCycleAudioQuality)
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = tr(lang, "کیفیت صدا", "Audio Quality"),
                                tint = if (hasQualityVariant) AccentGold else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasQualityVariant) {
                                    "${audioQuality.getTitle(lang)} (${BidiHelper.isolateLtr(effectiveBitrateLabel)})"
                                } else {
                                    tr(
                                        lang,
                                        "کیفیت اصلی منبع (${BidiHelper.isolateLtr(effectiveBitrateLabel)})",
                                        "Source Native Quality ($effectiveBitrateLabel)"
                                    )
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hasQualityVariant) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long, allowZero: Boolean): String {
    if (!allowZero && ms <= 0L) return "--:--"
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
