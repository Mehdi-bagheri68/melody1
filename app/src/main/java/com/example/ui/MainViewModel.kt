package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MeloGrooveApp
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthState
import com.example.data.model.AppLanguage
import com.example.data.model.ArtistSortOption
import com.example.data.model.AudioQuality
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
import com.example.data.model.OtpMode
import com.example.data.model.PlaybackError
import com.example.data.model.PlaybackMode
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.model.UserLoginLog
import com.example.data.model.UserProfile
import com.example.util.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object PhoneVerification : Screen("phone_verification")
    object Profile : Screen("profile")
    object AdminDashboard : Screen("admin_dashboard")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object VerifyEmail : Screen("verify_email")
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Driving : Screen("driving")
    object Settings : Screen("settings")
    data class PlaylistDetail(val playlist: Playlist) : Screen("playlist_detail")
    data class ArtistDetail(val artistName: String) : Screen("artist_detail")
}

class MainViewModel : ViewModel() {

    private val app = MeloGrooveApp.instance
    private val repository = app.repository
    private val userPreferences = app.userPreferences
    private val authRepository = app.authRepository
    private val driveStorageService = app.driveStorageService
    private val playerController = app.playerController

    val appLanguage: StateFlow<AppLanguage> = userPreferences.appLanguage
    val authState: StateFlow<AuthState> = authRepository.state

    // Centralized User Profile & Background Sync State (v2.0.0)
    val userProfile: StateFlow<UserProfile?> = userPreferences.userProfile
    val otpMode: StateFlow<OtpMode> = userPreferences.otpMode
    val googleDriveWebhookUrl: StateFlow<String> = userPreferences.googleDriveWebhookUrl
    val onboardingCompleted: StateFlow<Boolean> = userPreferences.onboardingCompleted
    val loginLogs: StateFlow<List<UserLoginLog>> = userPreferences.loginLogs
    val telemetryConsentGranted: StateFlow<Boolean> = userPreferences.telemetryConsentGranted

    private val _isAdminUnlocked = MutableStateFlow(
        userPreferences.userProfile.value?.isAdminUser == true
    )
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    // 3-Day Free Trial State (v1.9.0)
    val trialStartedAtMs: StateFlow<Long> = userPreferences.trialStartedAtMs
    val isTrialSessionActive: StateFlow<Boolean> = userPreferences.isTrialSessionActive

    private val _trialRemainingMs = MutableStateFlow(
        com.example.data.model.AppConfig.computeTrialRemainingMs(userPreferences.trialStartedAtMs.value)
    )
    val trialRemainingMs: StateFlow<Long> = _trialRemainingMs.asStateFlow()

    private val _isTrialExpired = MutableStateFlow(
        userPreferences.trialStartedAtMs.value > 0L &&
            !com.example.data.model.AppConfig.isTrialValid(userPreferences.trialStartedAtMs.value)
    )
    val isTrialExpired: StateFlow<Boolean> = _isTrialExpired.asStateFlow()

    private val _isSyncingDrive = MutableStateFlow(false)
    val isSyncingDrive: StateFlow<Boolean> = _isSyncingDrive.asStateFlow()

    private val _generatedFallbackOtp = MutableStateFlow<String?>(null)
    val generatedFallbackOtp: StateFlow<String?> = _generatedFallbackOtp.asStateFlow()

    private val _otpStatusBanner = MutableStateFlow<String?>(null)
    val otpStatusBanner: StateFlow<String?> = _otpStatusBanner.asStateFlow()

    private val _driveSyncStatusMessage = MutableStateFlow<String?>(null)
    val driveSyncStatusMessage: StateFlow<String?> = _driveSyncStatusMessage.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _authInfoMessage = MutableStateFlow<String?>(null)
    val authInfoMessage: StateFlow<String?> = _authInfoMessage.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(
        resolveInitialScreen(
            profile = userPreferences.userProfile.value,
            onboardingDone = userPreferences.onboardingCompleted.value,
            trialSessionActive = userPreferences.isTrialSessionActive.value,
            trialStartMs = userPreferences.trialStartedAtMs.value
        )
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val initialCountry = CountriesData.getCountryByCode(
        userPreferences.userProfile.value?.selectedCountry?.takeIf { it.isNotBlank() }
            ?: userPreferences.selectedCountryCode.value
    )

    private val _selectedCountry = MutableStateFlow(initialCountry)
    val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

    private val _moodCategories = MutableStateFlow(
        CountriesData.getMoodCategoriesForCountry(initialCountry)
    )
    val moodCategories: StateFlow<List<MoodCategory>> = _moodCategories.asStateFlow()

    private val _editorialBanners = MutableStateFlow(
        repository.getEditorialBannersForCountry(initialCountry)
    )
    val editorialBanners: StateFlow<List<com.example.data.provider.EditorialBanner>> = _editorialBanners.asStateFlow()

    private val _homeLastUpdatedMs = MutableStateFlow(System.currentTimeMillis())
    val homeLastUpdatedMs: StateFlow<Long> = _homeLastUpdatedMs.asStateFlow()

    private val _isLiveProviderBacked = MutableStateFlow(false)
    val isLiveProviderBacked: StateFlow<Boolean> = _isLiveProviderBacked.asStateFlow()

    // Synchronously initialized on frame 0 via GroupEngine so there is zero empty state or LTR/RTL layout jump
    private val _trendingTracks = MutableStateFlow<List<Track>>(
        repository.getInitialTrendingSync(initialCountry)
    )
    val trendingTracks: StateFlow<List<Track>> = _trendingTracks.asStateFlow()

    private val _newReleaseTracks = MutableStateFlow<List<Track>>(
        repository.getInitialNewReleasesSync(initialCountry)
    )
    val newReleaseTracks: StateFlow<List<Track>> = _newReleaseTracks.asStateFlow()

    private val _isLoadingTrending = MutableStateFlow(false)
    val isLoadingTrending: StateFlow<Boolean> = _isLoadingTrending.asStateFlow()

    private val _trendingErrorMessage = MutableStateFlow<String?>(null)
    val trendingErrorMessage: StateFlow<String?> = _trendingErrorMessage.asStateFlow()

    val listeningHistory: StateFlow<List<Track>> = repository.getListeningHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<Track>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistTracks.asStateFlow()

    private val _activeMood = MutableStateFlow<MoodCategory?>(null)
    val activeMood: StateFlow<MoodCategory?> = _activeMood.asStateFlow()

    private val _moodTracks = MutableStateFlow<List<Track>>(emptyList())
    val moodTracks: StateFlow<List<Track>> = _moodTracks.asStateFlow()

    private val _isLoadingMood = MutableStateFlow(false)
    val isLoadingMood: StateFlow<Boolean> = _isLoadingMood.asStateFlow()

    // Artist Discography State
    private val _selectedArtistName = MutableStateFlow<String?>(null)
    val selectedArtistName: StateFlow<String?> = _selectedArtistName.asStateFlow()

    private val _artistTracks = MutableStateFlow<List<Track>>(emptyList())
    val artistTracks: StateFlow<List<Track>> = _artistTracks.asStateFlow()

    private val _artistSortOption = MutableStateFlow(ArtistSortOption.TRENDING)
    val artistSortOption: StateFlow<ArtistSortOption> = _artistSortOption.asStateFlow()

    private val _isLoadingArtist = MutableStateFlow(false)
    val isLoadingArtist: StateFlow<Boolean> = _isLoadingArtist.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchGenreFilter = MutableStateFlow<String?>(null)
    val searchGenreFilter: StateFlow<String?> = _searchGenreFilter.asStateFlow()

    private val _searchYearFilter = MutableStateFlow<String?>(null)
    val searchYearFilter: StateFlow<String?> = _searchYearFilter.asStateFlow()

    private val _searchCountryFilter = MutableStateFlow<String?>(null)
    val searchCountryFilter: StateFlow<String?> = _searchCountryFilter.asStateFlow()

    // Player state from PlayerController
    val currentTrack: StateFlow<Track?> = playerController.currentTrack
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val isBuffering: StateFlow<Boolean> = playerController.isBuffering
    val isRetrying: StateFlow<Boolean> = playerController.isRetrying
    val retryCount: StateFlow<Int> = playerController.retryCount
    val playbackError: StateFlow<PlaybackError?> = playerController.playbackError
    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val playbackMode: StateFlow<PlaybackMode> = playerController.playbackMode
    val audioQuality: StateFlow<AudioQuality> = playerController.audioQuality
    val hasQualityVariant: StateFlow<Boolean> = playerController.hasQualityVariant
    val effectiveBitrateLabel: StateFlow<String> = playerController.effectiveBitrateLabel
    val iranNoVpnMode: StateFlow<Boolean> = userPreferences.iranNoVpnMode
    val queue: StateFlow<List<Track>> = playerController.queue

    // Dialog & Sheet States
    private val _isFullPlayerExpanded = MutableStateFlow(false)
    val isFullPlayerExpanded: StateFlow<Boolean> = _isFullPlayerExpanded.asStateFlow()

    private val _isCountryPickerVisible = MutableStateFlow(false)
    val isCountryPickerVisible: StateFlow<Boolean> = _isCountryPickerVisible.asStateFlow()

    private val _isCreatePlaylistVisible = MutableStateFlow(false)
    val isCreatePlaylistVisible: StateFlow<Boolean> = _isCreatePlaylistVisible.asStateFlow()

    private val _trackForPlaylist = MutableStateFlow<Track?>(null)
    val trackForPlaylist: StateFlow<Track?> = _trackForPlaylist.asStateFlow()

    // Explicit coroutine Jobs for every async flow to cancel stale concurrent requests
    private var trendingJob: Job? = null
    private var moodJob: Job? = null
    private var artistJob: Job? = null
    private var playlistTracksJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadTrendingForCountry(_selectedCountry.value, forceRefresh = false)
        startTrialExpirationMonitor()
        recordStartupTelemetryIfActive()
    }

    /**
     * Section 2.1 & 2.6: Called when the app returns to foreground (ON_START).
     * Refreshes Home feed sections only if their TTL has expired.
     */
    fun onAppForegrounded() {
        val country = _selectedCountry.value
        if (repository.isHomeSectionExpired(country.code)) {
            loadTrendingForCountry(country, forceRefresh = false)
        }
    }

    /**
     * Section 2.1: Explicit user-initiated refresh on HomeScreen.
     */
    fun refreshHomeFeed() {
        loadTrendingForCountry(_selectedCountry.value, forceRefresh = true)
    }

    fun setTelemetryConsentGranted(granted: Boolean) {
        userPreferences.setTelemetryConsentGranted(granted)
        if (granted) {
            recordStartupTelemetryIfActive()
        }
    }

    private fun recordStartupTelemetryIfActive() {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.value
            val trialActive = userPreferences.isTrialSessionActive.value &&
                com.example.data.model.AppConfig.isTrialValid(userPreferences.trialStartedAtMs.value)

            when {
                profile != null && profile.isAuthenticated -> {
                    _isAdminUnlocked.value = profile.isAdminUser
                    driveStorageService.recordUserSessionTelemetry(
                        userId = profile.uid,
                        accountUsername = profile.email.ifBlank { profile.displayName },
                        phoneNumber = profile.phoneNumber,
                        status = "Active"
                    )
                }
                trialActive -> {
                    val trialId = "trial_${userPreferences.trialStartedAtMs.value}"
                    driveStorageService.recordUserSessionTelemetry(
                        userId = trialId,
                        accountUsername = "3-Day Free Trial User",
                        phoneNumber = "Trial (No Phone Required)",
                        status = "Active"
                    )
                }
                else -> {
                    driveStorageService.flushPendingTelemetryQueue()
                }
            }
        }
    }

    fun refreshAdminTelemetryLogs() {
        viewModelScope.launch {
            if (userPreferences.userProfile.value?.isAdminUser != true) return@launch
            _isSyncingDrive.value = true
            try {
                val flushed = driveStorageService.flushPendingTelemetryQueue()
                _driveSyncStatusMessage.value = if (flushed > 0) {
                    "$flushed رکورد جدید با موفقیت در پس‌زمینه همگام‌سازی شد."
                } else {
                    "تمامی لاگ‌های تله‌متری کاربران بروز هستند."
                }
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    private fun resolveInitialScreen(
        profile: UserProfile?,
        onboardingDone: Boolean,
        trialSessionActive: Boolean,
        trialStartMs: Long
    ): Screen {
        val trialValid = trialSessionActive && com.example.data.model.AppConfig.isTrialValid(trialStartMs)
        if (trialValid) {
            return if (onboardingDone) Screen.Home else Screen.Onboarding
        }
        return when {
            profile == null || !profile.isAuthenticated -> Screen.Login
            !profile.canProceedPastPhoneStep -> Screen.PhoneVerification
            !profile.isCountrySelectedStrict || !onboardingDone -> Screen.Onboarding
            else -> Screen.Home
        }
    }

    private fun startTrialExpirationMonitor() {
        viewModelScope.launch {
            while (true) {
                val startMs = userPreferences.trialStartedAtMs.value
                val rem = com.example.data.model.AppConfig.computeTrialRemainingMs(startMs)
                _trialRemainingMs.value = rem
                val expired = startMs > 0L && rem <= 0L
                _isTrialExpired.value = expired

                if (expired && userPreferences.isTrialSessionActive.value) {
                    userPreferences.endTrialSession()
                    val profile = userPreferences.userProfile.value
                    if (profile == null || !profile.isAuthenticated) {
                        stopAndClearPlayback()
                        _authErrorMessage.value = if (appLanguage.value == AppLanguage.FA) {
                            "مهلت استفاده آزمایشی ۳ روزه شما به پایان رسید. لطفاً برای ادامه کار با اپلیکیشن با حساب گوگل یا جیمیل خود وارد شوید."
                        } else {
                            "Your 3-day free trial has expired. Please sign in with your Google/Gmail account to continue."
                        }
                        _currentScreen.value = Screen.Login
                    }
                }
                delay(30_000L)
            }
        }
    }

    /**
     * Triggered when the user taps the Red 3-Day Free Trial button on the Login screen.
     * Allows full access to the app for 3 days (72 hours) without entering a Gmail/Google account or phone OTP.
     */
    fun startThreeDayTrial() {
        val valid = userPreferences.startOrResumeTrial()
        val startMs = userPreferences.trialStartedAtMs.value
        _trialRemainingMs.value = com.example.data.model.AppConfig.computeTrialRemainingMs(startMs)
        _isTrialExpired.value = !valid

        if (valid) {
            clearAuthMessages()
            viewModelScope.launch {
                val trialId = "trial_$startMs"
                driveStorageService.recordUserSessionTelemetry(
                    userId = trialId,
                    accountUsername = "3-Day Free Trial User",
                    phoneNumber = "Trial (No Phone Required)",
                    status = "Active"
                )
            }
            _currentScreen.value = if (userPreferences.onboardingCompleted.value) {
                Screen.Home
            } else {
                Screen.Onboarding
            }
        } else {
            _authErrorMessage.value = if (appLanguage.value == AppLanguage.FA) {
                "مهلت استفاده آزمایشی ۳ روزه به پایان رسیده است. لطفاً برای ادامه استفاده از اپلیکیشن با اکانت جیمیل یا گوگل خود وارد شوید."
            } else {
                "Your 3-day free trial has ended. Please sign in with your Google/Gmail account to continue."
            }
        }
    }

    private fun currentUserId(): String =
        userProfile.value?.uid?.takeIf { it.isNotBlank() }
            ?: (authState.value as? AuthState.SignedIn)?.uid
            ?: "local"

    fun clearAuthMessages() {
        _authErrorMessage.value = null
        _authInfoMessage.value = null
    }

    fun setAuthErrorMessage(msg: String?) {
        _authErrorMessage.value = msg
    }

    /**
     * Step 1 & Step 2 of Strict Onboarding Guard:
     * Called immediately after Google Sign-In succeeds.
     * Queries Admin Google Drive (`syncUserFromDrive`) + local cache, loads existing phone/country if present,
     * and routes strictly to Step 3 (`PhoneVerification`), Step 4 (`Onboarding`), or Step 5 (`Home`).
     */
    fun onGoogleSignedIn(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String
    ) {
        viewModelScope.launch {
            _isSyncingDrive.value = true
            _authErrorMessage.value = null
            try {
                val syncOutcome = driveStorageService.syncUserFromDrive(
                    uid = uid,
                    email = email,
                    initialDisplayName = displayName,
                    initialPhotoUrl = photoUrl
                )
                val syncedProfile = syncOutcome.profile
                _driveSyncStatusMessage.value = if (appLanguage.value == AppLanguage.FA) {
                    syncOutcome.statusMessageFa
                } else {
                    syncOutcome.statusMessageEn
                }

                // If the user already had a selected country in Admin Google Drive, restore it immediately
                if (syncedProfile.selectedCountry.isNotBlank()) {
                    val restoredCountry = CountriesData.getCountryByCode(syncedProfile.selectedCountry)
                    userPreferences.setSelectedCountryCode(restoredCountry.code)
                    userPreferences.setOnboardingCompleted(true)
                    _selectedCountry.value = restoredCountry
                    _moodCategories.value = CountriesData.getMoodCategoriesForCountry(restoredCountry)
                    _editorialBanners.value = repository.getEditorialBannersForCountry(restoredCountry)
                    _trendingTracks.value = repository.getInitialTrendingSync(restoredCountry)
                    _newReleaseTracks.value = repository.getInitialNewReleasesSync(restoredCountry)
                    loadTrendingForCountry(restoredCountry, forceRefresh = false)
                }

                // Strict Onboarding Guard routing
                _isAdminUnlocked.value = syncedProfile.isAdminUser
                driveStorageService.recordUserSessionTelemetry(
                    userId = syncedProfile.uid,
                    accountUsername = syncedProfile.email.ifBlank { syncedProfile.displayName },
                    phoneNumber = syncedProfile.phoneNumber,
                    status = "Active"
                )
                _currentScreen.value = when {
                    !syncedProfile.canProceedPastPhoneStep -> Screen.PhoneVerification
                    !syncedProfile.isCountrySelectedStrict -> Screen.Onboarding
                    else -> Screen.Home
                }
            } catch (e: Exception) {
                AppLogger.w("onGoogleSignedIn", "Error syncing user profile", e)
                _authErrorMessage.value = e.localizedMessage
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    fun setOtpMode(mode: OtpMode) {
        userPreferences.setOtpMode(mode)
    }

    fun setGoogleDriveWebhookUrl(url: String) {
        userPreferences.setGoogleDriveWebhookUrl(url)
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            _isSyncingDrive.value = true
            try {
                val outcome = driveStorageService.saveOrUpdateUserProfile(current)
                _driveSyncStatusMessage.value = if (appLanguage.value == AppLanguage.FA) {
                    outcome.statusMessageFa
                } else {
                    outcome.statusMessageEn
                }
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    /**
     * Step 3 (Part A): Request an SMS OTP for the user's mobile phone number.
     * Reports `BLOCKED_NO_BACKEND` honestly when no real SMS gateway is configured.
     */
    fun requestPhoneOtp(dialCode: String, rawPhone: String) {
        viewModelScope.launch {
            _isSyncingDrive.value = true
            _authErrorMessage.value = null
            try {
                val email = userProfile.value?.email.orEmpty()
                val result = driveStorageService.requestOtp(
                    dialCode = dialCode,
                    rawPhoneInput = rawPhone,
                    email = email,
                    mode = otpMode.value
                )
                _generatedFallbackOtp.value = null
                val msg = if (appLanguage.value == AppLanguage.FA) result.messageFa else result.messageEn
                if (result.success || result.status == com.example.data.model.OtpVerificationStatus.BLOCKED_NO_BACKEND) {
                    _otpStatusBanner.value = msg
                } else {
                    _authErrorMessage.value = msg
                }
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    /**
     * Section 4.1: Honest continuation path when SMS verification is `BLOCKED_NO_BACKEND`.
     * Records `phoneVerified = false` and `phoneVerificationBypassedNoBackend = true` (never faking verification).
     */
    fun continueWithoutSmsVerification(dialCode: String, rawPhone: String) {
        viewModelScope.launch {
            _isSyncingDrive.value = true
            _authErrorMessage.value = null
            try {
                val normalized = if (rawPhone.isNotBlank() &&
                    com.example.data.model.AppConfig.isValidPhoneNumber(dialCode, rawPhone)
                ) {
                    com.example.data.model.AppConfig.normalizePhoneNumber(dialCode, rawPhone)
                } else {
                    ""
                }
                val current = userProfile.value ?: UserProfile()
                val updated = current.copy(
                    phoneNumber = normalized,
                    phoneVerified = false,
                    phoneVerificationBypassedNoBackend = true
                )
                val outcome = driveStorageService.saveOrUpdateUserProfile(updated)
                _generatedFallbackOtp.value = null
                _otpStatusBanner.value = null
                if (!outcome.profile.isCountrySelectedStrict || !userPreferences.onboardingCompleted.value) {
                    _currentScreen.value = Screen.Onboarding
                } else {
                    _currentScreen.value = Screen.Home
                }
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    /**
     * Step 3 (Part B): Verify the 6-digit OTP code, save `phoneNumber` & `phoneVerified = true`
     * to Admin Google Drive + local cache, and route to Country Selection (or Home if already selected).
     */
    fun verifyPhoneOtp(normalizedPhone: String, enteredCode: String) {
        viewModelScope.launch {
            _isSyncingDrive.value = true
            _authErrorMessage.value = null
            try {
                val status = driveStorageService.verifyOtpDetailed(
                    normalizedPhone = normalizedPhone,
                    enteredCode = enteredCode,
                    mode = otpMode.value
                )
                if (status != com.example.data.model.OtpVerificationStatus.VERIFIED) {
                    _authErrorMessage.value = when (status) {
                        com.example.data.model.OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED ->
                            if (appLanguage.value == AppLanguage.FA) {
                                "تعداد تلاش‌های ناموفق بیش از حد مجاز (۵ بار) است. لطفاً کد جدید درخواست کنید."
                            } else {
                                "Maximum verification attempts (5) exceeded. Please request a new code."
                            }
                        else ->
                            if (appLanguage.value == AppLanguage.FA) {
                                "کد تایید ۶ رقمی وارد شده صحیح نیست یا درگاه پیامکی (BLOCKED_NO_BACKEND) فعال نیست."
                            } else {
                                "Invalid/expired 6-digit code or SMS gateway is BLOCKED_NO_BACKEND."
                            }
                    }
                    return@launch
                }

                val current = userProfile.value ?: UserProfile()
                val updated = current.copy(
                    phoneNumber = normalizedPhone,
                    phoneVerified = true,
                    phoneVerificationBypassedNoBackend = false
                )
                val outcome = driveStorageService.saveOrUpdateUserProfile(updated)
                driveStorageService.recordUserSessionTelemetry(
                    userId = outcome.profile.uid,
                    accountUsername = outcome.profile.email.ifBlank { outcome.profile.displayName },
                    phoneNumber = outcome.profile.phoneNumber,
                    status = "Active"
                )
                _generatedFallbackOtp.value = null
                _otpStatusBanner.value = null
                _driveSyncStatusMessage.value = if (appLanguage.value == AppLanguage.FA) {
                    outcome.statusMessageFa
                } else {
                    outcome.statusMessageEn
                }

                // Proceed to Step 4 (Country Selection) if country not yet chosen, else Main Player
                if (!outcome.profile.isCountrySelectedStrict || !userPreferences.onboardingCompleted.value) {
                    _currentScreen.value = Screen.Onboarding
                } else {
                    _currentScreen.value = Screen.Home
                }
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    /**
     * Module 4: Save edited User Profile fields (`displayName`, `bio`, `favoriteGenres`)
     * to both local cache (`localStorage`) and the Admin's Google Drive.
     */
    fun saveUserProfileDetails(
        displayName: String,
        bio: String,
        favoriteGenres: List<String>
    ) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            _isSyncingDrive.value = true
            try {
                val updated = current.copy(
                    displayName = displayName.ifBlank { current.displayName },
                    bio = bio,
                    favoriteGenres = favoriteGenres.ifEmpty { listOf("Pop") }
                )
                val outcome = driveStorageService.saveOrUpdateUserProfile(updated)
                _driveSyncStatusMessage.value = if (appLanguage.value == AppLanguage.FA) {
                    outcome.statusMessageFa
                } else {
                    outcome.statusMessageEn
                }
            } finally {
                _isSyncingDrive.value = false
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authInfoMessage.value = null
            try {
                when (val result = authRepository.signIn(email, password)) {
                    is AuthResult.Success -> {
                        _currentScreen.value = Screen.Home
                    }
                    is AuthResult.Failure -> {
                        _authErrorMessage.value = result.reason.getMessage(appLanguage.value)
                    }
                }
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun registerWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authInfoMessage.value = null
            try {
                when (val result = authRepository.signUp(email, password, displayName)) {
                    is AuthResult.Success -> {
                        _currentScreen.value = Screen.Home
                    }
                    is AuthResult.Failure -> {
                        _authErrorMessage.value = result.reason.getMessage(appLanguage.value)
                    }
                }
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun resetPasswordWithEmail(email: String, newPassword: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _authInfoMessage.value = null
            try {
                when (val result = authRepository.sendPasswordReset(email, newPassword)) {
                    is AuthResult.Success -> {
                        val lang = appLanguage.value
                        _authInfoMessage.value = if (lang == AppLanguage.FA) {
                            "رمز عبور با موفقیت تغییر یافت. اکنون وارد شوید."
                        } else {
                            "Password updated successfully. Please sign in."
                        }
                        _currentScreen.value = Screen.Login
                    }
                    is AuthResult.Failure -> {
                        _authErrorMessage.value = result.reason.getMessage(appLanguage.value)
                    }
                }
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun verifyEmailCode(code: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            try {
                val ok = authRepository.verifyEmailCode(code)
                if (ok) {
                    _currentScreen.value = Screen.Home
                } else {
                    val lang = appLanguage.value
                    _authErrorMessage.value = if (lang == AppLanguage.FA) {
                        "کد تأیید نامعتبر است"
                    } else {
                        "Invalid verification code"
                    }
                }
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val uid = currentUserId()
            userPreferences.markUserLogsLoggedOut(uid)
            stopAndClearPlayback()
            repository.clearSearchCache()
            authRepository.signOut()
            userPreferences.clearAuthSession()
            _isAdminUnlocked.value = false
            _generatedFallbackOtp.value = null
            _otpStatusBanner.value = null
            clearAuthMessages()
            _currentScreen.value = Screen.Login
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val uid = currentUserId()
            userPreferences.markUserLogsLoggedOut(uid)
            stopAndClearPlayback()
            repository.clearSearchCache()
            authRepository.deleteAccount()
            userPreferences.clearAuthSession()
            _isAdminUnlocked.value = false
            _generatedFallbackOtp.value = null
            _otpStatusBanner.value = null
            clearAuthMessages()
            _currentScreen.value = Screen.Login
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        userPreferences.setAppLanguage(language)
    }

    fun setIranNoVpnMode(enabled: Boolean) {
        userPreferences.setIranNoVpnMode(enabled)
        playerController.setIranNoVpnMode(enabled)
        viewModelScope.launch {
            repository.clearSearchCache()
            loadTrendingForCountry(_selectedCountry.value, forceRefresh = true)
            if (_searchQuery.value.isNotBlank() || _searchGenreFilter.value != null ||
                _searchYearFilter.value != null || _searchCountryFilter.value != null
            ) {
                executeSearch(_searchQuery.value)
            }
        }
    }

    fun clearTemporaryCache() {
        viewModelScope.launch {
            repository.clearSearchCache()
        }
    }

    fun navigateTo(screen: Screen) {
        clearAuthMessages()
        _currentScreen.value = screen
    }

    fun openArtistDetail(artistName: String) {
        _selectedArtistName.value = artistName
        _isFullPlayerExpanded.value = false
        _currentScreen.value = Screen.ArtistDetail(artistName)
        loadArtistTracks(artistName, _artistSortOption.value)
    }

    fun setArtistSortOption(sortOption: ArtistSortOption) {
        _artistSortOption.value = sortOption
        _selectedArtistName.value?.let { artist ->
            loadArtistTracks(artist, sortOption)
        }
    }

    private fun loadArtistTracks(artistName: String, sortOption: ArtistSortOption) {
        artistJob?.cancel()
        artistJob = viewModelScope.launch {
            _isLoadingArtist.value = true
            try {
                val tracks = repository.getTracksByArtist(artistName, sortOption)
                if (_selectedArtistName.value == artistName && _artistSortOption.value == sortOption) {
                    _artistTracks.value = tracks
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLogger.w("loadArtistTracks", "Error loading tracks for artist=$artistName", e)
            } finally {
                if (_selectedArtistName.value == artistName && _artistSortOption.value == sortOption) {
                    _isLoadingArtist.value = false
                }
            }
        }
    }

    /**
     * Step 4 of Strict Onboarding Guard:
     * Saves the selected country in preferences AND updates the user's profile in Admin Google Drive + local cache.
     */
    fun completeOnboarding(country: Country) {
        userPreferences.setSelectedCountryCode(country.code)
        userPreferences.setOnboardingCompleted(true)
        _selectedCountry.value = country
        _moodCategories.value = CountriesData.getMoodCategoriesForCountry(country)
        _editorialBanners.value = repository.getEditorialBannersForCountry(country)
        _trendingTracks.value = repository.getInitialTrendingSync(country)
        _newReleaseTracks.value = repository.getInitialNewReleasesSync(country)
        _activeMood.value = null
        _moodTracks.value = emptyList()
        _currentScreen.value = Screen.Home
        loadTrendingForCountry(country, forceRefresh = false)

        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            val updated = current.copy(selectedCountry = country.code)
            driveStorageService.saveOrUpdateUserProfile(updated)
        }
    }

    fun selectCountry(country: Country) {
        userPreferences.setSelectedCountryCode(country.code)
        _selectedCountry.value = country
        val newCountryMoods = CountriesData.getMoodCategoriesForCountry(country)
        _moodCategories.value = newCountryMoods
        _editorialBanners.value = repository.getEditorialBannersForCountry(country)
        _trendingTracks.value = repository.getInitialTrendingSync(country)
        _newReleaseTracks.value = repository.getInitialNewReleasesSync(country)
        _isCountryPickerVisible.value = false

        val currentMoodId = _activeMood.value?.id
        if (currentMoodId != null) {
            val mappedMood = newCountryMoods.find { it.id == currentMoodId }
            selectMood(mappedMood)
        } else {
            moodJob?.cancel()
            _isLoadingMood.value = false
        }

        loadTrendingForCountry(country, forceRefresh = false)

        // Also update the user's selectedCountry in Admin Google Drive + local cache
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            val updated = current.copy(selectedCountry = country.code)
            driveStorageService.saveOrUpdateUserProfile(updated)
        }
    }

    fun onEditorialBannerClick(banner: com.example.data.provider.EditorialBanner) {
        when (banner.actionType) {
            com.example.data.provider.EditorialBannerActionType.MOOD -> {
                val targetMood = _moodCategories.value.find {
                    it.id.equals(banner.actionTarget, ignoreCase = true) ||
                        it.query.equals(banner.actionTarget, ignoreCase = true)
                }
                if (targetMood != null) {
                    selectMood(targetMood)
                }
            }
            com.example.data.provider.EditorialBannerActionType.COUNTRY -> {
                val targetCountry = CountriesData.getCountryByCode(banner.actionTarget)
                selectCountry(targetCountry)
            }
            com.example.data.provider.EditorialBannerActionType.SEARCH -> {
                onSearchQueryChanged(banner.actionTarget)
                navigateTo(Screen.Search)
            }
            com.example.data.provider.EditorialBannerActionType.ARTIST -> {
                openArtistDetail(banner.actionTarget)
            }
        }
    }

    fun loadTrendingForCountry(country: Country, forceRefresh: Boolean = false) {
        trendingJob?.cancel()
        trendingJob = viewModelScope.launch {
            _isLoadingTrending.value = true
            _trendingErrorMessage.value = null
            try {
                val noVpn = userPreferences.iranNoVpnMode.value
                val newest = repository.getNewReleasesForCountry(
                    country = country,
                    forceRefresh = forceRefresh,
                    iranNoVpnMode = noVpn
                )
                val newestIds = newest.map { it.id }.toSet()
                val tracks = repository.getTrendingForCountry(
                    country = country,
                    forceRefresh = forceRefresh,
                    iranNoVpnMode = noVpn,
                    excludeTrackIds = newestIds
                )
                val banners = repository.getEditorialBannersForCountry(country)
                if (_selectedCountry.value.code == country.code) {
                    if (newest.isNotEmpty()) _newReleaseTracks.value = newest
                    if (tracks.isNotEmpty()) _trendingTracks.value = tracks
                    _editorialBanners.value = banners
                    _homeLastUpdatedMs.value = repository.getHomeLastUpdatedMs(country.code)
                        .takeIf { it > 0L } ?: System.currentTimeMillis()
                    _isLiveProviderBacked.value = repository.hasLiveProviderItems(country.code)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLogger.w("loadTrendingForCountry", "Error loading trending for ${country.code}", e)
                if (_selectedCountry.value.code == country.code && _trendingTracks.value.isEmpty()) {
                    _trendingErrorMessage.value = e.localizedMessage ?: "Error loading tracks"
                }
            } finally {
                if (_selectedCountry.value.code == country.code) {
                    _isLoadingTrending.value = false
                }
            }
        }
    }

    fun selectMood(mood: MoodCategory?) {
        moodJob?.cancel()
        _activeMood.value = mood
        if (mood == null) {
            _moodTracks.value = emptyList()
            _isLoadingMood.value = false
            return
        }
        val targetCountryCode = _selectedCountry.value.code
        moodJob = viewModelScope.launch {
            _isLoadingMood.value = true
            try {
                val tracks = repository.getTracksForMood(mood.query, targetCountryCode)
                if (_activeMood.value?.id == mood.id && _selectedCountry.value.code == targetCountryCode) {
                    _moodTracks.value = tracks
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLogger.w("selectMood", "Failed loading mood=${mood.id}", e)
            } finally {
                if (_activeMood.value?.id == mood.id && _selectedCountry.value.code == targetCountryCode) {
                    _isLoadingMood.value = false
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank() && _searchGenreFilter.value == null && _searchYearFilter.value == null && _searchCountryFilter.value == null) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(200)
            executeSearch(query)
        }
    }

    fun submitSearchNow() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(_searchQuery.value)
        }
    }

    fun clearAllSearchFilters() {
        _searchGenreFilter.value = null
        _searchYearFilter.value = null
        _searchCountryFilter.value = null
        searchJob?.cancel()
        if (_searchQuery.value.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
        } else {
            searchJob = viewModelScope.launch {
                executeSearch(_searchQuery.value)
            }
        }
    }

    fun setGenreFilter(genre: String?) {
        _searchGenreFilter.value = genre
        searchJob?.cancel()
        searchJob = viewModelScope.launch { executeSearch(_searchQuery.value) }
    }

    fun setYearFilter(year: String?) {
        _searchYearFilter.value = year
        searchJob?.cancel()
        searchJob = viewModelScope.launch { executeSearch(_searchQuery.value) }
    }

    fun setSearchCountryFilter(code: String?) {
        _searchCountryFilter.value = code
        searchJob?.cancel()
        if (_searchQuery.value.isNotBlank() || _searchGenreFilter.value != null || _searchYearFilter.value != null || code != null) {
            searchJob = viewModelScope.launch { executeSearch(_searchQuery.value) }
        } else {
            _searchResults.value = emptyList()
            _isSearching.value = false
        }
    }

    private suspend fun executeSearch(query: String) {
        val targetCountry = _searchCountryFilter.value ?: _selectedCountry.value.code
        val targetGenre = _searchGenreFilter.value
        val targetYear = _searchYearFilter.value
        val targetNoVpn = userPreferences.iranNoVpnMode.value

        _isSearching.value = true
        try {
            val results = repository.searchTracks(
                term = query,
                countryCode = targetCountry,
                genreFilter = targetGenre,
                yearFilter = targetYear,
                iranNoVpnMode = targetNoVpn
            )
            if (_searchQuery.value == query &&
                (_searchCountryFilter.value ?: _selectedCountry.value.code) == targetCountry &&
                _searchGenreFilter.value == targetGenre &&
                _searchYearFilter.value == targetYear
            ) {
                _searchResults.value = results
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            AppLogger.w("executeSearch", "Error searching query=$query", e)
        } finally {
            _isSearching.value = false
        }
    }

    // Playback Actions
    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        playerController.playTrack(track, newQueue)
    }

    fun retryPlaybackManually() {
        playerController.retryCurrentTrackManually()
    }

    fun togglePlayPause() {
        playerController.playPauseToggle()
    }

    fun stopAndClearPlayback() {
        _isFullPlayerExpanded.value = false
        playerController.stopAndClearPlayback()
    }

    fun removeTrackFromQueue(trackId: Long) {
        playerController.removeTrackFromQueue(trackId)
        if (playerController.currentTrack.value == null) {
            _isFullPlayerExpanded.value = false
        }
    }

    fun skipNext() {
        playerController.skipNext()
    }

    fun skipPrevious() {
        playerController.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun togglePlaybackMode() {
        playerController.togglePlaybackMode()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        playerController.setPlaybackMode(mode)
    }

    fun setAudioQuality(quality: AudioQuality) {
        userPreferences.setAudioQuality(quality)
        playerController.setAudioQuality(quality)
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            try {
                val isNowFav = repository.toggleFavorite(track, currentUserId())
                _trendingTracks.value = _trendingTracks.value.map {
                    if (it.id == track.id) it.copy(isFavorite = isNowFav) else it
                }
                _newReleaseTracks.value = _newReleaseTracks.value.map {
                    if (it.id == track.id) it.copy(isFavorite = isNowFav) else it
                }
                _moodTracks.value = _moodTracks.value.map {
                    if (it.id == track.id) it.copy(isFavorite = isNowFav) else it
                }
                _searchResults.value = _searchResults.value.map {
                    if (it.id == track.id) it.copy(isFavorite = isNowFav) else it
                }
                _artistTracks.value = _artistTracks.value.map {
                    if (it.id == track.id) it.copy(isFavorite = isNowFav) else it
                }
            } catch (e: Exception) {
                AppLogger.w("toggleFavorite", "Failed toggling favorite for track=${track.id}", e)
            }
        }
    }

    // Playlist Actions
    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.createPlaylist(name, description, currentUserId())
                _isCreatePlaylistVisible.value = false
            } catch (e: Exception) {
                AppLogger.w("createPlaylist", "Failed creating playlist", e)
            }
        }
    }

    fun openPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        _currentScreen.value = Screen.PlaylistDetail(playlist)
        playlistTracksJob?.cancel()
        playlistTracksJob = viewModelScope.launch {
            repository.getTracksForPlaylist(playlist.id).collect {
                _selectedPlaylistTracks.value = it
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlist.id)
                if (_selectedPlaylist.value?.id == playlist.id) {
                    _currentScreen.value = Screen.Library
                }
            } catch (e: Exception) {
                AppLogger.w("deletePlaylist", "Failed deleting playlist=${playlist.id}", e)
            }
        }
    }

    fun openAddToPlaylist(track: Track) {
        _trackForPlaylist.value = track
    }

    fun closeAddToPlaylist() {
        _trackForPlaylist.value = null
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            try {
                repository.addTrackToPlaylist(playlistId, track)
                _trackForPlaylist.value = null
            } catch (e: Exception) {
                AppLogger.w("addTrackToPlaylist", "Failed adding track to playlist", e)
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            try {
                repository.removeTrackFromPlaylist(playlistId, trackId)
            } catch (e: Exception) {
                AppLogger.w("removeTrackFromPlaylist", "Failed removing track from playlist", e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                repository.clearHistory()
            } catch (e: Exception) {
                AppLogger.w("clearHistory", "Failed clearing history", e)
            }
        }
    }

    fun setFullPlayerExpanded(expanded: Boolean) {
        _isFullPlayerExpanded.value = expanded
    }

    fun setCountryPickerVisible(visible: Boolean) {
        _isCountryPickerVisible.value = visible
    }

    fun setCreatePlaylistVisible(visible: Boolean) {
        _isCreatePlaylistVisible.value = visible
    }
}
