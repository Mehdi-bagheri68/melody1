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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CountryPickerDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.GlassyBottomNavBar
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.DrivingModeScreen
import com.example.ui.screens.GoogleLoginScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhoneVerificationScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MeloGrooveTheme
import com.example.ui.theme.OledBackground

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Immediately set native window layout direction on frame 0 before Compose renders
        val initialRtl = viewModel.appLanguage.value.isRtl
        window.decorView.layoutDirection =
            if (initialRtl) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

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
    val iranNoVpnMode by viewModel.iranNoVpnMode.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()

    // Strict Onboarding Guard & User Profile State (v2.0.0)
    val userProfile by viewModel.userProfile.collectAsState()
    val otpMode by viewModel.otpMode.collectAsState()
    val googleDriveWebhookUrl by viewModel.googleDriveWebhookUrl.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val isSyncingDrive by viewModel.isSyncingDrive.collectAsState()
    val generatedFallbackOtp by viewModel.generatedFallbackOtp.collectAsState()
    val otpStatusBanner by viewModel.otpStatusBanner.collectAsState()
    val driveSyncStatusMessage by viewModel.driveSyncStatusMessage.collectAsState()
    val loginLogs by viewModel.loginLogs.collectAsState()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()
    val telemetryConsentGranted by viewModel.telemetryConsentGranted.collectAsState()

    // 3-Day Free Trial State (v1.9.0)
    val trialStartedAtMs by viewModel.trialStartedAtMs.collectAsState()
    val isTrialSessionActive by viewModel.isTrialSessionActive.collectAsState()
    val trialRemainingMs by viewModel.trialRemainingMs.collectAsState()
    val isTrialExpired by viewModel.isTrialExpired.collectAsState()

    val editorialBanners by viewModel.editorialBanners.collectAsState()
    val homeLastUpdatedMs by viewModel.homeLastUpdatedMs.collectAsState()
    val isLiveProviderBacked by viewModel.isLiveProviderBacked.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val newReleaseTracks by viewModel.newReleaseTracks.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val trendingErrorMessage by viewModel.trendingErrorMessage.collectAsState()
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
    val isRetrying by viewModel.isRetrying.collectAsState()
    val retryCount by viewModel.retryCount.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()
    val hasQualityVariant by viewModel.hasQualityVariant.collectAsState()
    val effectiveBitrateLabel by viewModel.effectiveBitrateLabel.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()

    val isFullPlayerExpanded by viewModel.isFullPlayerExpanded.collectAsState()
    val isCountryPickerVisible by viewModel.isCountryPickerVisible.collectAsState()
    val isCreatePlaylistVisible by viewModel.isCreatePlaylistVisible.collectAsState()
    val trackForPlaylist by viewModel.trackForPlaylist.collectAsState()

    val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    // =========================================================================
    // STRICT ONBOARDING GUARD (5-Stage Mandatory Flow)
    // =========================================================================
    val activeProfile = userProfile
    val hasValidTrialAccess = isTrialSessionActive && !isTrialExpired
    val hasAuthenticatedAccount = activeProfile != null && activeProfile.isAuthenticated

    val mustShowGoogleLogin = (!hasAuthenticatedAccount && !hasValidTrialAccess) || currentScreen is Screen.Login
    val mustShowPhoneVerification = !mustShowGoogleLogin && !hasValidTrialAccess &&
        (!activeProfile!!.canProceedPastPhoneStep || currentScreen is Screen.PhoneVerification)
    val mustShowCountrySelection = !mustShowGoogleLogin && !mustShowPhoneVerification &&
        ((!hasValidTrialAccess && !activeProfile!!.isCountrySelectedStrict) || !onboardingCompleted || currentScreen is Screen.Onboarding)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides appLanguage
    ) {
        when {
            mustShowGoogleLogin -> {
                GoogleLoginScreen(
                    isSyncing = isSyncingDrive,
                    errorMessage = authErrorMessage,
                    trialStartedAtMs = trialStartedAtMs,
                    trialRemainingMs = trialRemainingMs,
                    isTrialExpired = isTrialExpired,
                    onStartTrialClick = {
                        viewModel.startThreeDayTrial()
                    },
                    onGoogleLoginSuccess = { uid, email, displayName, photoUrl ->
                        viewModel.onGoogleSignedIn(uid, email, displayName, photoUrl)
                    },
                    onGoogleLoginError = { msg ->
                        viewModel.setAuthErrorMessage(msg)
                    },
                    onToggleLanguage = { lang ->
                        viewModel.setAppLanguage(lang)
                    }
                )
            }

            mustShowPhoneVerification -> {
                if (activeProfile?.canProceedPastPhoneStep == true) {
                    BackHandler {
                        viewModel.navigateTo(Screen.Profile)
                    }
                }
                PhoneVerificationScreen(
                    userProfile = activeProfile,
                    otpMode = otpMode,
                    isBusy = isSyncingDrive,
                    generatedFallbackOtp = generatedFallbackOtp,
                    statusBannerMessage = otpStatusBanner,
                    errorMessage = authErrorMessage,
                    onOtpModeChange = { mode -> viewModel.setOtpMode(mode) },
                    onRequestOtp = { dialCode, rawPhone ->
                        viewModel.requestPhoneOtp(dialCode, rawPhone)
                    },
                    onVerifyOtp = { normalizedPhone, code ->
                        viewModel.verifyPhoneOtp(normalizedPhone, code)
                    },
                    onContinueWithoutSmsBackend = { dialCode, rawPhone ->
                        viewModel.continueWithoutSmsVerification(dialCode, rawPhone)
                    },
                    onSignOutAccount = {
                        viewModel.signOut()
                    }
                )
            }

            mustShowCountrySelection -> {
                OnboardingScreen(
                    onSelectAndProceed = { country ->
                        viewModel.completeOnboarding(country)
                    }
                )
            }

            else -> {
                // Back handler for sub-screens inside Main Music Player
                if (currentScreen !is Screen.Home) {
                    BackHandler {
                        viewModel.navigateTo(Screen.Home)
                    }
                }

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
                                        onExpandClick = { viewModel.setFullPlayerExpanded(true) },
                                        isRetrying = isRetrying,
                                        retryCount = retryCount,
                                        playbackError = playbackError,
                                        onManualRetry = { viewModel.retryPlaybackManually() }
                                    )
                                }

                                // Bottom Navigation Bar
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

                                GlassyBottomNavBar(
                                    navItems = navItems,
                                    currentScreen = currentScreen,
                                    onSelectScreen = { screen -> viewModel.navigateTo(screen) }
                                )
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
                                    onProfileClick = { viewModel.navigateTo(Screen.Profile) },
                                    onMoodClick = { mood ->
                                        viewModel.selectMood(if (activeMood?.id == mood.id) null else mood)
                                    },
                                    onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                    onArtistClick = { artist -> viewModel.openArtistDetail(artist) },
                                    onStopTrackClick = { viewModel.stopAndClearPlayback() },
                                    onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                                    onAddToPlaylistClick = { track -> viewModel.openAddToPlaylist(track) },
                                    trendingErrorMessage = trendingErrorMessage,
                                    onRetryTrending = { viewModel.refreshHomeFeed() },
                                    editorialBanners = editorialBanners,
                                    onEditorialBannerClick = { banner -> viewModel.onEditorialBannerClick(banner) },
                                    homeLastUpdatedMs = homeLastUpdatedMs,
                                    isLiveProviderBacked = isLiveProviderBacked,
                                    onRefreshHome = { viewModel.refreshHomeFeed() }
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
                                    onAddToPlaylistClick = { track -> viewModel.openAddToPlaylist(track) },
                                    onSubmitSearch = { viewModel.submitSearchNow() },
                                    onClearAllFilters = { viewModel.clearAllSearchFilters() }
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

                            is Screen.Profile -> {
                                ProfileScreen(
                                    userProfile = activeProfile,
                                    googleDriveWebhookUrl = googleDriveWebhookUrl,
                                    otpMode = otpMode,
                                    isSyncing = isSyncingDrive,
                                    syncStatusMessage = driveSyncStatusMessage,
                                    isAdminUnlocked = isAdminUnlocked,
                                    telemetryConsentGranted = telemetryConsentGranted,
                                    onToggleTelemetryConsent = { granted ->
                                        viewModel.setTelemetryConsentGranted(granted)
                                    },
                                    onOpenAdminDashboard = { viewModel.navigateTo(Screen.AdminDashboard) },
                                    onBack = { viewModel.navigateTo(Screen.Home) },
                                    onSaveProfile = { displayName, bio, genres ->
                                        viewModel.saveUserProfileDetails(displayName, bio, genres)
                                    },
                                    onChangePhoneClick = {
                                        viewModel.navigateTo(Screen.PhoneVerification)
                                    },
                                    onChangeCountryClick = {
                                        viewModel.setCountryPickerVisible(true)
                                    },
                                    onSaveWebhookUrl = { url ->
                                        viewModel.setGoogleDriveWebhookUrl(url)
                                    },
                                    onChangeOtpMode = { mode ->
                                        viewModel.setOtpMode(mode)
                                    },
                                    onSignOut = {
                                        viewModel.signOut()
                                    }
                                )
                            }

                            is Screen.AdminDashboard -> {
                                AdminDashboardScreen(
                                    logs = loginLogs,
                                    adminEndpointUrl = googleDriveWebhookUrl,
                                    isSyncing = isSyncingDrive,
                                    syncStatusMessage = driveSyncStatusMessage,
                                    onRefreshLogs = { viewModel.refreshAdminTelemetryLogs() },
                                    onSaveEndpointUrl = { url -> viewModel.setGoogleDriveWebhookUrl(url) },
                                    onBackClick = { viewModel.navigateTo(Screen.Profile) }
                                )
                            }

                            is Screen.Settings -> {
                                SettingsScreen(
                                    selectedCountry = selectedCountry,
                                    audioQuality = audioQuality,
                                    appLanguage = appLanguage,
                                    onCountryChangeClick = { viewModel.setCountryPickerVisible(true) },
                                    onAudioQualityChanged = { quality -> viewModel.setAudioQuality(quality) },
                                    onLanguageChanged = { lang -> viewModel.setAppLanguage(lang) },
                                    iranNoVpnMode = iranNoVpnMode,
                                    onIranNoVpnModeChanged = { enabled -> viewModel.setIranNoVpnMode(enabled) },
                                    onClearCacheClick = { viewModel.clearTemporaryCache() },
                                    authState = authState,
                                    userProfile = activeProfile,
                                    isTrialActive = hasValidTrialAccess,
                                    trialRemainingMs = trialRemainingMs,
                                    onOpenProfileClick = { viewModel.navigateTo(Screen.Profile) },
                                    onOpenAdminDashboardClick = {
                                        if (isAdminUnlocked || activeProfile?.isAdminUser == true) {
                                            viewModel.navigateTo(Screen.AdminDashboard)
                                        } else {
                                            viewModel.navigateTo(Screen.Profile)
                                        }
                                    },
                                    onSignInClick = { viewModel.navigateTo(Screen.Login) },
                                    onSignOutClick = { viewModel.signOut() },
                                    onDeleteAccountClick = { viewModel.deleteAccount() }
                                )
                            }

                            else -> {}
                        }
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
                },
                isRetrying = isRetrying,
                playbackError = playbackError,
                onManualRetry = { viewModel.retryPlaybackManually() },
                hasQualityVariant = hasQualityVariant,
                effectiveBitrateLabel = effectiveBitrateLabel
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
