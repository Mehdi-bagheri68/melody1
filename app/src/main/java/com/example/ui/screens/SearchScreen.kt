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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.Track
import com.example.ui.components.TrackItemCard
import com.example.ui.theme.AccentGold
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
fun SearchScreen(
    searchQuery: String,
    searchResults: List<Track>,
    isSearching: Boolean,
    selectedCountry: Country,
    genreFilter: String?,
    yearFilter: String?,
    countryFilter: String?,
    currentTrack: Track?,
    isPlaying: Boolean,
    onQueryChanged: (String) -> Unit,
    onGenreFilterChanged: (String?) -> Unit,
    onYearFilterChanged: (String?) -> Unit,
    onCountryFilterChanged: (String?) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    val genres = listOf("پاپ", "سنتی", "راک", "الکترونیک", "هیپ‌هاپ", "کلاسیک")
    val years = listOf("2024", "2023", "2022", "2020", "2015", "2010")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding()
    ) {
        // Search Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        "جستجوی آهنگ، هنرمند، آلبوم...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AccentGold
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChanged("") },
                            modifier = Modifier.testTag("clear_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = OledSurfaceVariant,
                    unfocusedContainerColor = OledSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_input")
            )
        }

        // Multi-Criteria Filter Bar (Country Filter Chips)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    label = "همه کشورها 🌐",
                    isSelected = countryFilter == null,
                    onClick = { onCountryFilterChanged(null) }
                )
            }
            items(CountriesData.allCountries) { country ->
                FilterChip(
                    label = "${country.flag} ${country.nameFa}",
                    isSelected = countryFilter == country.code,
                    onClick = {
                        onCountryFilterChanged(if (countryFilter == country.code) null else country.code)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Genre and Year Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                FilterChip(
                    label = genre,
                    isSelected = genreFilter == genre,
                    onClick = {
                        onGenreFilterChanged(if (genreFilter == genre) null else genre)
                    }
                )
            }
            items(years) { year ->
                FilterChip(
                    label = year,
                    isSelected = yearFilter == year,
                    onClick = {
                        onYearFilterChanged(if (yearFilter == year) null else year)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Results / Suggested Content
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "نتیجه‌ای یافت نشد",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "لطفاً عبارت دیگری را جستجو کنید یا فیلترها را حذف نمایید",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    item {
                        Text(
                            text = "پیشنهادهای برتر برای ${selectedCountry.nameFa} ${selectedCountry.flag}",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentGold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(selectedCountry.topArtists) { artist ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { onQueryChanged(artist) },
                            color = OledSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                } else {
                    items(searchResults) { track ->
                        TrackItemCard(
                            track = track,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isCurrentTrack = currentTrack?.id == track.id,
                            onTrackClick = { onTrackClick(track, searchResults) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToPlaylistClick = { onAddToPlaylistClick(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AccentGold else BorderSubtle,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        color = if (isSelected) OledSurfaceHigh else OledSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) AccentGold else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
