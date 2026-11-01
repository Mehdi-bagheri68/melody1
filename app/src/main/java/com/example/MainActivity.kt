package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Country
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CountryPickerDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.DrivingModeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.MeloGrooveTheme
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MeloGrooveTheme {
                MeloGrooveMainContent(viewModel)
            }
        }
    }
}

data class NavItem(
    val screen: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun MeloGrooveMainContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val recentHistory by viewModel.listeningHistory.collectAsState()
    val activeMood by viewModel.activeMood.collectAsState()
    val moodTracks by viewModel.moodTracks.collectAsState()
    val isLoadingMood by viewModel.isLoadingMood.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchGenreFilter by viewModel.searchGenreFilter.collectAsState()
    val searchYearFilter by viewModel.searchYearFilter.collectAsState()
    val searchCountryFilter by viewModel.searchCountryFilter.collectAsState()

    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsState()

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()

    val isFullPlayerExpanded by viewModel.isFullPlayerExpanded.collectAsState()
    val isCountryPickerVisible by viewModel.isCountryPickerVisible.collectAsState()
    val isCreatePlaylistVisible by viewModel.isCreatePlaylistVisible.collectAsState()
    val trackForPlaylist by viewModel.trackForPlaylist.collectAsState()

    // Back handler for sub-screens
    if (currentScreen !is Screen.Home && currentScreen !is Screen.Onboarding) {
        BackHandler {
            viewModel.navigateTo(Screen.Home)
        }
    }

    if (currentScreen is Screen.Onboarding) {
        OnboardingScreen(
            onSelectAndProceed = { country ->
                viewModel.completeOnboarding(country)
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                if (currentScreen !is Screen.Driving) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Floating Mini Player (visible if track is selected)
                        AnimatedVisibility(
                            visible = currentTrack != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            MiniPlayer(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                onPlayPauseToggle = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onExpandClick = { viewModel.setFullPlayerExpanded(true) }
                            )
                        }

                        // Bottom Navigation Bar
                        val navItems = listOf(
                            NavItem(Screen.Home, "خانه", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
                            NavItem(Screen.Search, "جستجو", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
                            NavItem(Screen.Library, "کتابخانه", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
                            NavItem(Screen.Driving, "خودرو", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar, "nav_drive"),
                            NavItem(Screen.Settings, "تنظیمات", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
                        )

                        NavigationBar(
                            containerColor = OledBackground,
                            contentColor = TextPrimary,
                            tonalElevation = 8.dp
                        ) {
                            navItems.forEach { item ->
                                val isSelected = currentScreen::class == item.screen::class

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(item.screen) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    },
                                    label = {
                                        Text(text = item.title)
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = AccentGold,
                                        indicatorColor = AccentGold,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = OledBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OledBackground)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            selectedCountry = selectedCountry,
                            trendingTracks = trendingTracks,
                            isLoadingTrending = isLoadingTrending,
                            recentHistory = recentHistory,
                            activeMood = activeMood,
                            moodTracks = moodTracks,
                            isLoadingMood = isLoadingMood,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onCountryClick = { viewModel.setCountryPickerVisible(true) },
                            onSearchClick = { viewModel.navigateTo(Screen.Search) },
                            onDriveClick = { viewModel.navigateTo(Screen.Driving) },
                            onMoodClick = { mood -> viewModel.selectMood(if (activeMood?.id == mood.id) null else mood) },
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                            onAddToPlaylistClick = { track -> viewModel.openAddToPlaylist(track) }
                        )
                    }

                    is Screen.Search -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            isSearching = isSearching,
                            selectedCountry = selectedCountry,
                            genreFilter = searchGenreFilter,
                            yearFilter = searchYearFilter,
                            countryFilter = searchCountryFilter,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            onGenreFilterChanged = { viewModel.setGenreFilter(it) },
                            onYearFilterChanged = { viewModel.setYearFilter(it) },
                            onCountryFilterChanged = { viewModel.setSearchCountryFilter(it) },
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                            onAddToPlaylistClick = { track -> viewModel.openAddToPlaylist(track) }
                        )
                    }

                    is Screen.Library -> {
                        LibraryScreen(
                            favorites = favorites,
                            playlists = playlists,
                            listeningHistory = recentHistory,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                            onAddToPlaylistClick = { track -> viewModel.openAddToPlaylist(track) },
                            onCreatePlaylistClick = { viewModel.setCreatePlaylistVisible(true) },
                            onOpenPlaylist = { playlist -> viewModel.openPlaylist(playlist) },
                            onClearHistory = { viewModel.clearHistory() }
                        )
                    }

                    is Screen.PlaylistDetail -> {
                        selectedPlaylist?.let { pl ->
                            PlaylistDetailScreen(
                                playlist = pl,
                                tracks = selectedPlaylistTracks,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                onBack = { viewModel.navigateTo(Screen.Library) },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                                onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(pl.id, trackId) },
                                onDeletePlaylist = { viewModel.deletePlaylist(pl) }
                            )
                        }
                    }

                    is Screen.Driving -> {
                        DrivingModeScreen(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            onBack = { viewModel.navigateTo(Screen.Home) },
                            onPlayPauseToggle = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onSkipPrevious = { viewModel.skipPrevious() },
                            onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                            onSelectDriveMood = { query ->
                                viewModel.onSearchQueryChanged(query)
                                viewModel.navigateTo(Screen.Search)
                            }
                        )
                    }

                    is Screen.Settings -> {
                        SettingsScreen(
                            selectedCountry = selectedCountry,
                            audioQuality = audioQuality,
                            onCountryChangeClick = { viewModel.setCountryPickerVisible(true) },
                            onAudioQualityChanged = { quality -> viewModel.setAudioQuality(quality) }
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    // Full Screen Player Sheet
    if (isFullPlayerExpanded && currentTrack != null) {
        FullPlayerSheet(
            track = currentTrack,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            playbackMode = playbackMode,
            audioQuality = audioQuality,
            onDismiss = { viewModel.setFullPlayerExpanded(false) },
            onPlayPauseToggle = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeekTo = { viewModel.seekTo(it) },
            onTogglePlaybackMode = { viewModel.togglePlaybackMode() },
            onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
            onAddToPlaylist = { currentTrack?.let { viewModel.openAddToPlaylist(it) } },
            onCycleAudioQuality = {
                val next = when (audioQuality) {
                    com.example.data.model.AudioQuality.HIGH -> com.example.data.model.AudioQuality.STANDARD
                    com.example.data.model.AudioQuality.STANDARD -> com.example.data.model.AudioQuality.DATA_SAVER
                    com.example.data.model.AudioQuality.DATA_SAVER -> com.example.data.model.AudioQuality.HIGH
                }
                viewModel.setAudioQuality(next)
            }
        )
    }

    // Country Picker Dialog
    if (isCountryPickerVisible) {
        CountryPickerDialog(
            selectedCountry = selectedCountry,
            onSelectCountry = { country -> viewModel.selectCountry(country) },
            onDismiss = { viewModel.setCountryPickerVisible(false) }
        )
    }

    // Create Playlist Dialog
    if (isCreatePlaylistVisible) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.setCreatePlaylistVisible(false) },
            onCreate = { name, desc -> viewModel.createPlaylist(name, desc) }
        )
    }

    // Add To Playlist Dialog
    trackForPlaylist?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onSelectPlaylist = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, track)
            },
            onCreateNewPlaylist = {
                viewModel.closeAddToPlaylist()
                viewModel.setCreatePlaylistVisible(true)
            },
            onDismiss = { viewModel.closeAddToPlaylist() }
        )
    }
}
