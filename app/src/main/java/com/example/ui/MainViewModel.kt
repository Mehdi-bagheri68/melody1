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
import com.example.data.model.PlaybackMode
import com.example.data.model.Playlist
import com.example.data.model.Track
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

    // Synchronously initialized on frame 0 so there is zero empty state or LTR/RTL layout jump!
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
    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val playbackMode: StateFlow<PlaybackMode> = playerController.playbackMode
    val audioQuality: StateFlow<AudioQuality> = playerController.audioQuality
    val iranNoVpnMode: StateFlow<Boolean> = playerController.iranNoVpnMode
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

    private var searchJob: Job? = null

    init {
        loadTrendingForCountry(_selectedCountry.value)
    }

    fun setAppLanguage(language: AppLanguage) {
        userPreferences.setAppLanguage(language)
    }

    fun setIranNoVpnMode(enabled: Boolean) {
        playerController.setIranNoVpnMode(enabled)
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

    private fun loadArtistTracks(artistName: String, sortOption: ArtistSortOption) {
        viewModelScope.launch {
            _isLoadingArtist.value = true
            val tracks = repository.getTracksByArtist(artistName, sortOption)
            _artistTracks.value = tracks
            _isLoadingArtist.value = false
        }
    }

    fun completeOnboarding(country: Country) {
        userPreferences.setSelectedCountryCode(country.code)
        userPreferences.setOnboardingCompleted(true)
        _selectedCountry.value = country
        _moodCategories.value = CountriesData.getMoodCategoriesForCountry(country)
        // Populate synchronously before switching screen so HomeScreen renders in RTL with full data on frame 0
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

        // If a mood category was open, update it to the new country's corresponding category & reload its tracks
        val currentMoodId = _activeMood.value?.id
        if (currentMoodId != null) {
            val mappedMood = newCountryMoods.find { it.id == currentMoodId }
            _activeMood.value = mappedMood
            if (mappedMood != null) {
                viewModelScope.launch {
                    _isLoadingMood.value = true
                    _moodTracks.value = repository.getTracksForMood(mappedMood.query, country.code)
                    _isLoadingMood.value = false
                }
            } else {
                _moodTracks.value = emptyList()
            }
        }

        loadTrendingForCountry(country)
    }

    fun loadTrendingForCountry(country: Country) {
        viewModelScope.launch {
            val tracks = repository.getTrendingForCountry(country)
            _trendingTracks.value = tracks
            val newest = repository.getNewReleasesForCountry(country)
            _newReleaseTracks.value = newest
            _isLoadingTrending.value = false
        }
    }

    fun selectMood(mood: MoodCategory?) {
        _activeMood.value = mood
        if (mood == null) {
            _moodTracks.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoadingMood.value = true
            val tracks = repository.getTracksForMood(mood.query, _selectedCountry.value.code)
            _moodTracks.value = tracks
            _isLoadingMood.value = false
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

    fun setGenreFilter(genre: String?) {
        _searchGenreFilter.value = genre
        viewModelScope.launch { executeSearch(_searchQuery.value) }
    }

    fun setYearFilter(year: String?) {
        _searchYearFilter.value = year
        viewModelScope.launch { executeSearch(_searchQuery.value) }
    }

    fun setSearchCountryFilter(code: String?) {
        _searchCountryFilter.value = code
        if (_searchQuery.value.isNotBlank() || _searchGenreFilter.value != null || _searchYearFilter.value != null || code != null) {
            viewModelScope.launch { executeSearch(_searchQuery.value) }
        } else {
            _searchResults.value = emptyList()
        }
    }

    private suspend fun executeSearch(query: String) {
        _isSearching.value = true
        val results = repository.searchTracks(
            term = query,
            countryCode = _searchCountryFilter.value ?: _selectedCountry.value.code,
            genreFilter = _searchGenreFilter.value,
            yearFilter = _searchYearFilter.value
        )
        _searchResults.value = results
        _isSearching.value = false
    }

    // Playback Actions
    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        playerController.playTrack(track, newQueue)
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
            repository.toggleFavorite(track)
        }
    }

    // Playlist Actions
    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            _isCreatePlaylistVisible.value = false
        }
    }

    fun openPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        _currentScreen.value = Screen.PlaylistDetail(playlist)
        viewModelScope.launch {
            repository.getTracksForPlaylist(playlist.id).collect {
                _selectedPlaylistTracks.value = it
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            if (_selectedPlaylist.value?.id == playlist.id) {
                _currentScreen.value = Screen.Library
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
            repository.addTrackToPlaylist(playlistId, track)
            _trackForPlaylist.value = null
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
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
