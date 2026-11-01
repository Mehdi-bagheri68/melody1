package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.AudioQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("melogroove_prefs", Context.MODE_PRIVATE)

    private val _onboardingCompleted =
        MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _selectedCountryCode =
        MutableStateFlow(prefs.getString(KEY_COUNTRY_CODE, "IR") ?: "IR")
    val selectedCountryCode: StateFlow<String> = _selectedCountryCode.asStateFlow()

    private val _audioQuality =
        MutableStateFlow(
            AudioQuality.valueOf(
                prefs.getString(KEY_AUDIO_QUALITY, AudioQuality.HIGH.name) ?: AudioQuality.HIGH.name
            )
        )
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()

    private val _appLanguage =
        MutableStateFlow(
            try {
                AppLanguage.valueOf(
                    prefs.getString(KEY_APP_LANGUAGE, AppLanguage.FA.name) ?: AppLanguage.FA.name
                )
            } catch (e: Exception) {
                AppLanguage.FA
            }
        )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setSelectedCountryCode(code: String) {
        prefs.edit().putString(KEY_COUNTRY_CODE, code).apply()
        _selectedCountryCode.value = code
    }

    fun setAudioQuality(quality: AudioQuality) {
        prefs.edit().putString(KEY_AUDIO_QUALITY, quality.name).apply()
        _audioQuality.value = quality
    }

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _appLanguage.value = language
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_COUNTRY_CODE = "selected_country_code"
        private const val KEY_AUDIO_QUALITY = "audio_quality"
        private const val KEY_APP_LANGUAGE = "app_language"
    }
}
