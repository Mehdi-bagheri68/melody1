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
            try {
                AudioQuality.valueOf(
                    prefs.getString(KEY_AUDIO_QUALITY, AudioQuality.HIGH.name) ?: AudioQuality.HIGH.name
                )
            } catch (_: Exception) {
                AudioQuality.HIGH
            }
        )
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()

    private val _appLanguage =
        MutableStateFlow(
            try {
                AppLanguage.valueOf(
                    prefs.getString(KEY_APP_LANGUAGE, AppLanguage.FA.name) ?: AppLanguage.FA.name
                )
            } catch (_: Exception) {
                AppLanguage.FA
            }
        )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Fixes BUG-10: Persist Iran No-VPN Direct Mode preference and apply it in SourceResolver & Repository
    private val _iranNoVpnMode =
        MutableStateFlow(prefs.getBoolean(KEY_IRAN_NO_VPN_MODE, true))
    val iranNoVpnMode: StateFlow<Boolean> = _iranNoVpnMode.asStateFlow()

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

    fun setIranNoVpnMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IRAN_NO_VPN_MODE, enabled).apply()
        _iranNoVpnMode.value = enabled
    }

    // Auth session persistence (No-VPN compatible)
    fun getCurrentUserUid(): String? = prefs.getString(KEY_AUTH_UID, null)
    fun getCurrentUserEmail(): String? = prefs.getString(KEY_AUTH_EMAIL, null)
    fun getCurrentUserDisplayName(): String = prefs.getString(KEY_AUTH_DISPLAY_NAME, "") ?: ""
    fun isCurrentUserEmailVerified(): Boolean = prefs.getBoolean(KEY_AUTH_VERIFIED, false)

    fun saveAuthSession(uid: String, email: String, displayName: String, emailVerified: Boolean) {
        prefs.edit()
            .putString(KEY_AUTH_UID, uid)
            .putString(KEY_AUTH_EMAIL, email)
            .putString(KEY_AUTH_DISPLAY_NAME, displayName)
            .putBoolean(KEY_AUTH_VERIFIED, emailVerified)
            .apply()
    }

    fun clearAuthSession() {
        prefs.edit()
            .remove(KEY_AUTH_UID)
            .remove(KEY_AUTH_EMAIL)
            .remove(KEY_AUTH_DISPLAY_NAME)
            .remove(KEY_AUTH_VERIFIED)
            .apply()
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_COUNTRY_CODE = "selected_country_code"
        private const val KEY_AUDIO_QUALITY = "audio_quality"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_IRAN_NO_VPN_MODE = "iran_no_vpn_mode"

        private const val KEY_AUTH_UID = "auth_uid"
        private const val KEY_AUTH_EMAIL = "auth_email"
        private const val KEY_AUTH_DISPLAY_NAME = "auth_display_name"
        private const val KEY_AUTH_VERIFIED = "auth_email_verified"
    }
}
