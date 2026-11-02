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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
import com.example.data.model.Track
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.components.ModernMoodCard
import com.example.ui.components.TrackItemCard
import com.example.ui.theme.AccentGold
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
    onMoodSelectFromSearch: (MoodCategory) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onArtistClick: (String) -> Unit,
    onStopTrackClick: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    val lang = LocalAppLanguage.current
    val genres = listOf(
        "پاپ" to tr(lang, "پاپ", "Pop"),
        "سنتی" to tr(lang, "سنتی", "Traditional"),
        "راک" to tr(lang, "راک", "Rock"),
        "الکترونیک" to tr(lang, "الکترونیک", "Electronic"),
        "کلاسیک" to tr(lang, "کلاسیک", "Classical")
    )
    val years = listOf("2026", "2025", "2024", "2023", "2022", "2020")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding()
    ) {
        Text(
            text = tr(lang, "جستجو و کشف", "Search & Explore"),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )

        // Search Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        tr(lang, "خوانندگان، آهنگ‌ها، آلبوم‌ها و بیشتر...", "Artists, songs, albums, and more..."),
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
                shape = RoundedCornerShape(24.dp),
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
                    label = tr(lang, "همه کشورها 🌐", "All Countries 🌐"),
                    isSelected = countryFilter == null,
                    onClick = { onCountryFilterChanged(null) }
                )
            }
            items(CountriesData.allCountries) { country ->
                FilterChip(
                    label = "${country.flag} ${country.getDisplayName(lang)}",
                    isSelected = countryFilter == country.code,
                    onClick = {
                        onCountryFilterChanged(if (countryFilter == country.code) null else country.code)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Genre and Year Filter Row (including 2026 & 2025)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { (genreKey, genreLabel) ->
                FilterChip(
                    label = genreLabel,
                    isSelected = genreFilter == genreKey,
                    onClick = {
                        onGenreFilterChanged(if (genreFilter == genreKey) null else genreKey)
                    }
                )
            }
            items(years) { year ->
                FilterChip(
                    label = tr(lang, "سال $year", "Year $year"),
                    isSelected = yearFilter == year,
                    onClick = {
                        onYearFilterChanged(if (yearFilter == year) null else year)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Results / Explore Grid
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
                        text = tr(lang, "نتیجه‌ای یافت نشد", "No results found"),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr(
                            lang,
                            "لطفاً عبارت دیگری را جستجو کنید یا فیلترها را تغییر دهید",
                            "Try another search term or adjust your filters"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchQuery.isEmpty() && genreFilter == null && yearFilter == null && countryFilter == null) {
                    item {
                        Text(
                            text = tr(
                                lang,
                                "کشف سریع و دسته‌بندی‌های ${selectedCountry.nameFa} ${selectedCountry.flag}",
                                "Explore & Browse ${selectedCountry.nameEn} ${selectedCountry.flag}"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CountriesData.getMoodCategoriesForCountry(selectedCountry).chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { mood ->
                                        ModernMoodCard(
                                            mood = mood,
                                            isSelected = false,
                                            onClick = { onMoodSelectFromSearch(mood) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Top Artists Direct Access
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "آرشیو خوانندگان محبوب ${selectedCountry.nameFa} ${selectedCountry.flag}",
                                "Top Artists in ${selectedCountry.nameEn} ${selectedCountry.flag}"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(selectedCountry.topArtists) { artist ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { onArtistClick(artist) },
                            color = OledSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = tr(lang, "مشاهده آهنگ‌ها", "View Songs"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentGold
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
                            onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                            onArtistClick = onArtistClick,
                            onStopTrackClick = onStopTrackClick
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
