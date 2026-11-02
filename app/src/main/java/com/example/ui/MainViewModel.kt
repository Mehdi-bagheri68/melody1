package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MeloGrooveApp
import com.example.data.model.AppLanguage
import com.example.data.model.ArtistSortOption
import com.example.data.model.AudioQuality
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.MoodCategory
import com.example.data.model.PlaybackError
import com.example.data.model.PlaybackMode
import com.example.data.model.Playlist
import com.example.data.model.Track
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
    private val playerController = app.playerController

    val appLanguage: StateFlow<AppLanguage> = userPreferences.appLanguage

    private val _currentScreen = MutableStateFlow<Screen>(
        if (userPreferences.onboardingCompleted.value) Screen.Home else Screen.Onboarding
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val initialCountry = CountriesData.getCountryByCode(userPreferences.selectedCountryCode.value)

    private val _selectedCountry = MutableStateFlow(initialCountry)
    val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

    private val _moodCategories = MutableStateFlow(
        CountriesData.getMoodCategoriesForCountry(initialCountry)
    )
    val moodCategories: StateFlow<List<MoodCategory>> = _moodCategories.asStateFlow()

    // Synchronously initialized on frame 0 so there is zero empty state or LTR/RTL layout jump
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

    // Fixes BUG-08: Explicit coroutine Jobs for every async flow to cancel stale concurrent requests
    private var trendingJob: Job? = null
    private var moodJob: Job? = null
    private var artistJob: Job? = null
    private var playlistTracksJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadTrendingForCountry(_selectedCountry.value)
    }

    fun setAppLanguage(language: AppLanguage) {
        userPreferences.setAppLanguage(language)
    }

    fun setIranNoVpnMode(enabled: Boolean) {
        userPreferences.setIranNoVpnMode(enabled)
        playerController.setIranNoVpnMode(enabled)
        viewModelScope.launch {
            repository.clearSearchCache()
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

    // Fixes BUG-08 & BUG-13: Cancels previous artistJob, checks parameter match, and resets _isLoadingArtist in finally
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

    fun completeOnboarding(country: Country) {
        userPreferences.setSelectedCountryCode(country.code)
        userPreferences.setOnboardingCompleted(true)
        _selectedCountry.value = country
        _moodCategories.value = CountriesData.getMoodCategoriesForCountry(country)
        _trendingTracks.value = repository.getInitialTrendingSync(country)
        _newReleaseTracks.value = repository.getInitialNewReleasesSync(country)
        _activeMood.value = null
        _moodTracks.value = emptyList()
        _currentScreen.value = Screen.Home
        loadTrendingForCountry(country)
    }

    fun selectCountry(country: Country) {
        userPreferences.setSelectedCountryCode(country.code)
        _selectedCountry.value = country
        val newCountryMoods = CountriesData.getMoodCategoriesForCountry(country)
        _moodCategories.value = newCountryMoods
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

        loadTrendingForCountry(country)
    }

    // Fixes BUG-07 & BUG-08: Activates _isLoadingTrending = true at start, cancels previous job,
    // verifies country match before applying, and guarantees false in finally
    fun loadTrendingForCountry(country: Country) {
        trendingJob?.cancel()
        trendingJob = viewModelScope.launch {
            _isLoadingTrending.value = true
            _trendingErrorMessage.value = null
            try {
                val tracks = repository.getTrendingForCountry(country)
                val newest = repository.getNewReleasesForCountry(country)
                if (_selectedCountry.value.code == country.code) {
                    _trendingTracks.value = tracks
                    _newReleaseTracks.value = newest
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                AppLogger.w("loadTrendingForCountry", "Error loading trending for ${country.code}", e)
                if (_selectedCountry.value.code == country.code) {
                    _trendingErrorMessage.value = e.localizedMessage ?: "Error loading tracks"
                }
            } finally {
                if (_selectedCountry.value.code == country.code) {
                    _isLoadingTrending.value = false
                }
            }
        }
    }

    // Fixes BUG-08: Cancels previous moodJob and verifies active mood & country before setting results
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
                val isNowFav = repository.toggleFavorite(track)
                // Also update in-memory lists immediately so heart icons respond in 0ms
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
                repository.createPlaylist(name, description)
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
