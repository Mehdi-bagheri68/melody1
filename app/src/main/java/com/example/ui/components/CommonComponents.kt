package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
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
import kotlin.math.abs

/**
 * Guaranteed No-VPN Album Cover Artwork!
 * Renders a rich, studio-designed procedural cover art on frame 0 (works 100% offline / without VPN in Iran)
 * and layers AsyncImage on top if an unblocked CDN image is reachable.
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
    val palettes = listOf(
        listOf(Color(0xFF8E24AA), Color(0xFF311B92), Color(0xFFFFB800)),
        listOf(Color(0xFFE53935), Color(0xFF880E4F), Color(0xFFFF8F00)),
        listOf(Color(0xFF00897B), Color(0xFF004D40), Color(0xFF64FFDA)),
        listOf(Color(0xFF1E88E5), Color(0xFF0D47A1), Color(0xFFFFD54F)),
        listOf(Color(0xFFF4511E), Color(0xFFBF360C), Color(0xFFFFEA00)),
        listOf(Color(0xFF43A047), Color(0xFF1B5E20), Color(0xFFB9F6CA)),
        listOf(Color(0xFF6D4C41), Color(0xFF263238), Color(0xFFFFB300)),
        listOf(Color(0xFFD81B60), Color(0xFF4A148C), Color(0xFFFF80AB))
    )
    val palette = palettes[abs(seedId.toInt()) % palettes.size]
    val icons = listOf(
        Icons.Default.MusicNote,
        Icons.Default.GraphicEq,
        Icons.Default.Headphones,
        Icons.Default.Album
    )
    val chosenIcon = icons[abs(seedId.toInt()) % icons.size]

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(palette[0], palette[1])
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Studio Vinyl & Soundwave Rings (0ms, 100% No-VPN)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = r * 0.44f,
                center = Offset(size.width * 0.8f, size.height * 0.2f)
            )
            drawCircle(
                color = palette[2].copy(alpha = 0.20f),
                radius = r * 0.36f,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = r * 0.24f,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Icon(
                imageVector = chosenIcon,
                contentDescription = null,
                tint = palette[2],
                modifier = Modifier.size(26.dp)
            )
            if (showTextOverlay && subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp
                )
            }
        }

        // Optional network image overlay (if reachable without VPN)
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
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

    Box(
        modifier = modifier
            .height(92.dp)
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
        TrackArtwork(
            imageUrl = mood.coverUrl,
            seedId = mood.id.hashCode().toLong(),
            title = mood.getPrimaryTitle(lang),
            subtitle = "",
            showTextOverlay = false,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.BottomEnd)
                .offset(x = if (lang.isRtl) (-10).dp else 10.dp, y = 8.dp)
                .rotate(if (lang.isRtl) -14f else 14f)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                Spacer(modifier = Modifier.height(2.dp))
            }

            Column {
                Text(
                    text = mood.getPrimaryTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mood.getSecondaryTitle(lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                color = if (isCurrentTrack) AccentGold else TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.Underline
                ),
                color = AccentGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onArtistClick(track.artist) }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${track.releaseYear} • ${track.durationFormatted}",
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
                        subtitle = "",
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
                                    contentDescription = "Playing",
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
                        text = track.title,
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
                            text = track.artist,
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
                            text = "${track.genre} • ${track.releaseYear} • ${track.durationFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1
                        )
                    }
                }

                if (trailingContent != null) {
                    trailingContent()
                } else {
                    if (isCurrentTrack && onStopTrackClick != null) {
                        IconButton(
                            onClick = onStopTrackClick,
                            modifier = Modifier
                                .size(40.dp)
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
                            .size(40.dp)
                            .testTag("fav_btn_${track.id}")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) AccentOrange else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("menu_btn_${track.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
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
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) return

    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, BorderHighlight, RoundedCornerShape(18.dp))
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
                color = AccentGold,
                trackColor = Color(0x33FFB800)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
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
                        showTextOverlay = false,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack.artist,
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

                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp),
                        color = AccentGold,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = AccentGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onStopAndDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_stop_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
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
        Column {
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
                    items(CountriesData.allCountries) { country ->
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
                                        contentDescription = "Selected",
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
                            items(playlists) { pl ->
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
