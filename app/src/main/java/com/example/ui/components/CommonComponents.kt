package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.example.data.model.Playlist
import com.example.data.model.Track
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

@Composable
fun TrackItemCard(
    track: Track,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    onTrackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    val cardBg = if (isCurrentTrack) {
        Brush.horizontalGradient(
            colors = listOf(
                OledSurfaceVariant,
                OledSurfaceHigh
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                OledSurface,
                OledSurface
            )
        )
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
                // Album artwork with overlay play state
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OledSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
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

                // Title, Artist, Genre & Country flag
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
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val country = CountriesData.allCountries.find { it.code.equals(track.countryCode, ignoreCase = true) }
                        if (country != null) {
                            Text(
                                text = country.flag,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = "${track.genre} • ${track.durationFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1
                        )
                    }
                }

                if (trailingContent != null) {
                    trailingContent()
                } else {
                    // Favorite button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("fav_btn_${track.id}")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) AccentOrange else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3-dots popup menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(44.dp)
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
                            DropdownMenuItem(
                                text = { Text("افزودن به پلی‌لیست", color = TextPrimary) },
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
                                        if (track.isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
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
            // Track playback progress strip
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OledSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentTrack.artworkUrl,
                        contentDescription = currentTrack.title,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentTrack.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Buffering indicator or Play/Pause
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
                            .size(44.dp)
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceVariant,
        title = {
            Text(
                text = "انتخاب کشور موسیقی",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
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
                                        text = country.nameFa,
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
                                                text = "پیشنهاد اصلی",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AccentGold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = country.nameEn,
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = AccentGold)
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceVariant,
        title = {
            Text("ساخت پلی‌لیست جدید", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام پلی‌لیست (مثلاً «پلی‌لیست شاد»)") },
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
                    label = { Text("توضیحات اختیاری (مثلاً «مخصوص رانندگی»)") },
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
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("save_playlist_btn")
            ) {
                Text("ایجاد پلی‌لیست")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = TextSecondary)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceVariant,
        title = {
            Text("افزودن به پلی‌لیست", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "آهنگ: ${track.title} - ${track.artist}",
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
                    Text("ساخت پلی‌لیست جدید", color = TextPrimary)
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "هنوز پلی‌لیستی نساخته‌اید.",
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
                                            text = "${pl.trackCount} آهنگ",
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = AccentGold)
            }
        }
    )
}
