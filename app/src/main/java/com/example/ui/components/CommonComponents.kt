package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
import com.example.data.model.PlaybackError
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.BidiHelper

import com.example.data.model.ArtworkRef
import com.example.data.repository.ArtworkResolver

/**
 * Fixes N-01, U-01, U-02, U-07:
 * - Resolves artwork strictly via [ArtworkResolver] so no cover file owned by artist A is ever
 *   displayed on a track by artist B.
 * - Uses `Alignment.TopCenter` with `ContentScale.Crop` so portrait artist photos keep the face centered.
 * - When a track has no owned cover, renders a stylized Artist Monogram Avatar ([ArtworkRef.Initials])
 *   with a deterministic two-tone gradient and artist initials.
 */
@Composable
fun TrackArtwork(
    imageUrl: String,
    seedId: Long,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    showTextOverlay: Boolean = true
) {
    val artworkRef = remember(imageUrl, title, subtitle) {
        ArtworkResolver.resolveForUrlAndTitle(
            imageUrl = imageUrl,
            title = title,
            subtitle = subtitle
        )
    }

    var loadFailed by remember(imageUrl) { mutableStateOf(false) }
    val initialsFallback = remember(title, subtitle, seedId) {
        ArtworkResolver.resolveInitials(
            displayName = subtitle.ifBlank { title },
            seed = "${subtitle.ifBlank { title }}_$seedId"
        )
    }

    when {
        artworkRef is ArtworkRef.Image && !loadFailed -> {
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(colors = listOf(OledSurfaceVariant, OledSurfaceHigh))
                ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = artworkRef.url,
                    contentDescription = if (subtitle.isNotBlank()) "$title - $subtitle" else title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    onError = {
                        loadFailed = true
                    }
                )
            }
        }
        else -> {
            val initials = (artworkRef as? ArtworkRef.Initials) ?: initialsFallback
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(initials.primaryColorHex),
                            Color(initials.secondaryColorHex)
                        )
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                // Subtle vinyl/monogram ring + deterministic artist initials (Fixes U-02)
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.76f)
                        .clip(CircleShape)
                        .background(Color(0x28000000))
                        .border(1.dp, AccentGold.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (showTextOverlay) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = initials.text,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentGold.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = initials.text,
                            style = MaterialTheme.typography.labelLarge,
                            color = AccentGold,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernMoodCard(
    mood: MoodCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(mood.colorStartHex),
            Color(mood.colorEndHex)
        )
    )

    // Fixes N-02, UI-01 & UI-13: Dedicated mood emblem (never reuses artist cover photos)
    Box(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("mood_${mood.id}")
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.BottomEnd)
                .offset(x = if (lang.isRtl) (-10).dp else 10.dp, y = 8.dp)
                .rotate(if (lang.isRtl) -14f else 14f)
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (mood.isNewRelease) Icons.Default.Album else Icons.Default.MusicNote,
                contentDescription = mood.getPrimaryTitle(lang),
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (mood.isNewRelease) {
                Surface(
                    color = Color(0x44000000),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tr(lang, "جدید ۲۰۲۶", "NEW 2026"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = mood.getPrimaryTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = BidiHelper.formatMixed(lang.isRtl, mood.getSecondaryTitle(lang)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Unified Horizontal Shelf Card (Used identically for New Releases, Trending, and Recently Played)
 */
@Composable
fun HorizontalShelfTrackCard(
    track: Track,
    badgeText: String? = null,
    isCurrentTrack: Boolean = false,
    isPlaying: Boolean = false,
    onTrackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val borderColor = if (isCurrentTrack) AccentGold else BorderSubtle

    Card(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onTrackClick)
            .testTag("shelf_card_${track.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) OledSurfaceHigh else OledSurface
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OledSurfaceVariant)
            ) {
                TrackArtwork(
                    imageUrl = track.artworkUrl,
                    seedId = track.id,
                    title = track.title,
                    subtitle = track.artist,
                    showTextOverlay = true,
                    modifier = Modifier.fillMaxSize()
                )

                if (badgeText != null) {
                    Surface(
                        color = Color(0xCC000000),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentTrack && isPlaying) {
                            tr(lang, "مکث ${track.title}", "Pause ${track.title}")
                        } else {
                            tr(lang, "پخش ${track.title}", "Play ${track.title}")
                        },
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = BidiHelper.formatMixed(lang.isRtl, track.title),
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrentTrack) AccentGold else TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = BidiHelper.formatMixed(lang.isRtl, track.artist),
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.Underline
                ),
                color = AccentGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { onArtistClick(track.artist) }
                    .padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${BidiHelper.isolateLtr(track.releaseYear)} • ${BidiHelper.isolateLtr(track.durationFormatted)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TrackItemCard(
    track: Track,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    onTrackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    onStopTrackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    var showMenu by remember { mutableStateOf(false) }

    val cardBg = if (isCurrentTrack) {
        Brush.horizontalGradient(colors = listOf(OledSurfaceVariant, OledSurfaceHigh))
    } else {
        Brush.horizontalGradient(colors = listOf(OledSurface, OledSurface))
    }

    val borderColor = if (isCurrentTrack) AccentGold else BorderSubtle

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onTrackClick)
            .testTag("track_card_${track.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(cardBg)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OledSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtwork(
                        imageUrl = track.artworkUrl,
                        seedId = track.id,
                        title = track.title,
                        subtitle = track.artist,
                        showTextOverlay = false,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    if (isCurrentTrack) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0x77000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlaying) {
                                MiniPulsingVisualizer()
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = tr(lang, "در حال پخش", "Playing"),
                                    tint = AccentGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = BidiHelper.formatMixed(lang.isRtl, track.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrentTrack) AccentGold else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = onArtistClick != null) {
                                onArtistClick?.invoke(track.artist)
                            }
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                            .testTag("artist_link_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = BidiHelper.formatMixed(lang.isRtl, track.artist),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (onArtistClick != null) TextDecoration.Underline else TextDecoration.None
                            ),
                            color = AccentGold,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val country = CountriesData.allCountries.find { it.code.equals(track.countryCode, ignoreCase = true) }
                        if (country != null) {
                            Text(
                                text = country.flag,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = "${track.genre} • ${BidiHelper.isolateLtr(track.releaseYear)} • ${BidiHelper.isolateLtr(track.durationFormatted)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Fixes UI-12: Minimum 48dp accessible touch targets & localized contentDescriptions
                if (trailingContent != null) {
                    trailingContent()
                } else {
                    if (isCurrentTrack && onStopTrackClick != null) {
                        IconButton(
                            onClick = onStopTrackClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("stop_track_btn_${track.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = tr(lang, "توقف پخش", "Stop Playback"),
                                tint = AccentOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fav_btn_${track.id}")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (track.isFavorite) {
                                tr(lang, "حذف از علاقه‌مندی‌ها", "Remove from Favorites")
                            } else {
                                tr(lang, "افزودن به علاقه‌مندی‌ها", "Add to Favorites")
                            },
                            tint = if (track.isFavorite) AccentOrange else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("menu_btn_${track.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = tr(lang, "گزینه‌های بیشتر", "More Options"),
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(OledSurfaceHigh)
                        ) {
                            if (onArtistClick != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            tr(lang, "مشاهده آهنگ‌های ${track.artist}", "View songs by ${track.artist}"),
                                            color = AccentGold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onArtistClick(track.artist)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(tr(lang, "افزودن به پلی‌لیست", "Add to Playlist"), color = TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = AccentGold)
                                },
                                onClick = {
                                    showMenu = false
                                    onAddToPlaylistClick()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (track.isFavorite)
                                            tr(lang, "حذف از علاقه‌مندی‌ها", "Remove from Favorites")
                                        else
                                            tr(lang, "افزودن به علاقه‌مندی‌ها", "Add to Favorites"),
                                        color = TextPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = AccentOrange
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleFavorite()
                                }
                            )
                            if (onStopTrackClick != null) {
                                DropdownMenuItem(
                                    text = { Text(tr(lang, "توقف و حذف پخش", "Stop & Clear Playback"), color = AccentOrange) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = AccentOrange)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onStopTrackClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPulsingVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h1.dp)
                .clip(CircleShape)
                .background(AccentGold)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h2.dp)
                .clip(CircleShape)
                .background(AccentOrange)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h3.dp)
                .clip(CircleShape)
                .background(AccentGold)
        )
    }
}

/**
 * Fixes UI-07 & UI-12:
 * - Shows clean, human-readable status ("در حال اتصال مجدد…" or "منبع در دسترس نیست") instead of raw numbers.
 * - Provides a manual Retry button when a playback error occurs.
 * - Uses 48.dp minimum touch targets and localized contentDescriptions.
 */
@Composable
fun MiniPlayer(
    currentTrack: Track?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onStopAndDismiss: () -> Unit,
    onArtistClick: (String) -> Unit,
    onExpandClick: () -> Unit,
    isRetrying: Boolean = false,
    retryCount: Int = 0,
    playbackError: PlaybackError? = null,
    onManualRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) return
    val lang = LocalAppLanguage.current

    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, if (playbackError != null && !isRetrying) AccentOrange else BorderHighlight, RoundedCornerShape(18.dp))
            .clickable(onClick = onExpandClick)
            .testTag("mini_player"),
        color = OledSurfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (playbackError != null && !isRetrying) AccentOrange else AccentGold,
                trackColor = Color(0x33FFB800)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OledSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtwork(
                        imageUrl = currentTrack.artworkUrl,
                        seedId = currentTrack.id,
                        title = currentTrack.title,
                        subtitle = currentTrack.artist,
                        showTextOverlay = false,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = BidiHelper.formatMixed(lang.isRtl, currentTrack.title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    when {
                        isRetrying -> {
                            Text(
                                text = tr(lang, "در حال اتصال مجدد…", "Reconnecting…"),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        playbackError != null -> {
                            Text(
                                text = playbackError.getMessage(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            Text(
                                text = BidiHelper.formatMixed(lang.isRtl, currentTrack.artist),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = AccentGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    onArtistClick(currentTrack.artist)
                                }
                            )
                        }
                    }
                }

                if (playbackError != null && !isRetrying) {
                    IconButton(
                        onClick = onManualRetry,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_player_retry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = tr(lang, "تلاش مجدد", "Retry"),
                            tint = AccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else if (isBuffering) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AccentGold,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) {
                                tr(lang, "مکث پخش", "Pause")
                            } else {
                                tr(lang, "شروع پخش", "Play")
                            },
                            tint = AccentGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = tr(lang, "آهنگ بعدی", "Next Track"),
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onStopAndDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_stop_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr(lang, "توقف و بستن پلیر", "Stop and Close Player"),
                        tint = AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentGold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
        }

        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    color = AccentGold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun CountryPickerDialog(
    selectedCountry: Country,
    onSelectCountry: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val dir = if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceVariant,
        title = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                Text(
                    text = tr(lang, "انتخاب کشور موسیقی", "Select Music Country"),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CountriesData.allCountries, key = { it.code }) { country ->
                        val isSelected = country.code == selectedCountry.code
                        val isIran = country.code == "IR"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else if (isIran) 1.dp else 0.5.dp,
                                    color = if (isSelected) AccentGold else if (isIran) BorderHighlight else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectCountry(country) }
                                .testTag("country_item_${country.code}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) OledSurfaceHigh else OledSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = country.flag, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = country.getDisplayName(lang),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSelected) AccentGold else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isIran) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x33FFB800))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tr(lang, "پیشنهاد اصلی", "Featured"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentGold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (lang.isRtl) country.nameEn else country.nameFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = tr(lang, "انتخاب شده", "Selected"),
                                        tint = AccentGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                TextButton(onClick = onDismiss) {
                    Text(tr(lang, "بستن", "Close"), color = AccentGold)
                }
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val dir = if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceVariant,
        title = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                Text(
                    tr(lang, "ساخت پلی‌لیست جدید", "Create New Playlist"),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(tr(lang, "نام پلی‌لیست (مثلاً «پلی‌لیست شاد»)", "Playlist Name (e.g. Road Trip)")) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedLabelColor = AccentGold,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input")
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(tr(lang, "توضیحات اختیاری", "Optional Description")) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderSubtle,
                            focusedLabelColor = AccentGold,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                Button(
                    onClick = { onCreate(name, description) },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("save_playlist_btn")
                ) {
                    Text(tr(lang, "ایجاد پلی‌لیست", "Create"))
                }
            }
        },
        dismissButton = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                TextButton(onClick = onDismiss) {
                    Text(tr(lang, "انصراف", "Cancel"), color = TextSecondary)
                }
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val dir = if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceVariant,
        title = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                Text(
                    tr(lang, "افزودن به پلی‌لیست", "Add to Playlist"),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${track.title} - ${track.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onCreateNewPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OledSurfaceHigh)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr(lang, "ساخت پلی‌لیست جدید", "New Playlist"), color = TextPrimary)
                    }

                    if (playlists.isEmpty()) {
                        Text(
                            text = tr(lang, "هنوز پلی‌لیستی نساخته‌اید.", "No playlists created yet."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(playlists, key = { it.id }) { pl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPlaylist(pl) }
                                        .testTag("add_to_pl_${pl.id}"),
                                    colors = CardDefaults.cardColors(containerColor = OledSurface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = AccentGold
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = pl.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(
                                                text = tr(lang, "${pl.trackCount} آهنگ", "${pl.trackCount} tracks"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                TextButton(onClick = onDismiss) {
                    Text(tr(lang, "بستن", "Close"), color = AccentGold)
                }
            }
        }
    )
}
