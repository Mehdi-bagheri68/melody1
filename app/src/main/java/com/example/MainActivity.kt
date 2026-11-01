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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CountryPickerDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.ArtistDetailScreen
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Immediately set native window layout direction on frame 0 before Compose renders
        val initialRtl = viewModel.appLanguage.value.isRtl
        window.decorView.layoutDirection =
            if (initialRtl) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR

        setContent {
            val appLanguage by viewModel.appLanguage.collectAsState()
            val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            androidx.compose.runtime.SideEffect {
                window.decorView.layoutDirection =
                    if (appLanguage.isRtl) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalAppLanguage provides appLanguage
            ) {
                MeloGrooveTheme {
                    MeloGrooveMainContent(viewModel)
                }
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
    val appLanguage by viewModel.appLanguage.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val newReleaseTracks by viewModel.newReleaseTracks.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val recentHistory by viewModel.listeningHistory.collectAsState()
    val activeMood by viewModel.activeMood.collectAsState()
    val moodTracks by viewModel.moodTracks.collectAsState()
    val isLoadingMood by viewModel.isLoadingMood.collectAsState()

    val artistTracks by viewModel.artistTracks.collectAsState()
    val artistSortOption by viewModel.artistSortOption.collectAsState()
    val isLoadingArtist by viewModel.isLoadingArtist.collectAsState()

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

    val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides appLanguage
    ) {
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
                            // Floating Mini Player (visible if track is active)
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
                                    onStopAndDismiss = { viewModel.stopAndClearPlayback() },
                                    onArtistClick = { artist -> viewModel.openArtistDetail(artist) },
                                    onExpandClick = { viewModel.setFullPlayerExpanded(true) }
                                )
                            }

                            // Bottom Navigation Bar (Car / Driving mode removed from bottom bar as requested)
                            val navItems = listOf(
                                NavItem(
                                    Screen.Home,
                                    tr(appLanguage, "خانه", "Home"),
                                    Icons.Filled.Home,
                                    Icons.Outlined.Home,
                                    "nav_home"
                                ),
                                NavItem(
                                    Screen.Search,
                                    tr(appLanguage, "جستجو", "Search"),
                                    Icons.Filled.Search,
                                    Icons.Outlined.Search,
                                    "nav_search"
                                ),
                                NavItem(
                                    Screen.Library,
                                    tr(appLanguage, "کتابخانه", "My Music"),
                                    Icons.Filled.LibraryMusic,
                                    Icons.Outlined.LibraryMusic,
                                    "nav_library"
                                ),
                                NavItem(
                                    Screen.Settings,
                                    tr(appLanguage, "تنظیمات", "Settings"),
                                    Icons.Filled.Settings,
                                    Icons.Outlined.Settings,
                                    "nav_settings"
                                )
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
            ) { _ ->
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
                                newReleaseTracks = newReleaseTracks,
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
                                onArtistClick = { artist -> viewModel.openArtistDetail(artist) },
                                onStopTrackClick = { viewModel.stopAndClearPlayback() },
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
                                onMoodSelectFromSearch = { mood ->
                                    viewModel.selectMood(mood)
                                    viewModel.navigateTo(Screen.Home)
                                },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onArtistClick = { artist -> viewModel.openArtistDetail(artist) },
                                onStopTrackClick = { viewModel.stopAndClearPlayback() },
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
                                onArtistClick = { artist -> viewModel.openArtistDetail(artist) },
                                onStopTrackClick = { viewModel.stopAndClearPlayback() },
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

                        is Screen.ArtistDetail -> {
                            ArtistDetailScreen(
                                artistName = screen.artistName,
                                tracks = artistTracks,
                                sortOption = artistSortOption,
                                isLoading = isLoadingArtist,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                onBack = { viewModel.navigateTo(Screen.Home) },
                                onSortChanged = { sort -> viewModel.setArtistSortOption(sort) },
                                onPlayTrackInArtistQueue = { track, list, mode ->
                                    mode?.let { viewModel.setPlaybackMode(it) }
                                    viewModel.playTrack(track, list)
                                },
                                onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                                onAddToPlaylistClick = { track -> viewModel.openAddToPlaylist(track) },
                                onStopTrackClick = { viewModel.stopAndClearPlayback() }
                            )
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
                                appLanguage = appLanguage,
                                onCountryChangeClick = { viewModel.setCountryPickerVisible(true) },
                                onAudioQualityChanged = { quality -> viewModel.setAudioQuality(quality) },
                                onLanguageChanged = { lang -> viewModel.setAppLanguage(lang) }
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
                onStopAndClear = { viewModel.stopAndClearPlayback() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onSelectPlaybackMode = { mode -> viewModel.setPlaybackMode(mode) },
                onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
                onAddToPlaylist = { currentTrack?.let { viewModel.openAddToPlaylist(it) } },
                onArtistClick = { artist -> viewModel.openArtistDetail(artist) },
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
}
