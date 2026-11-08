package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppConfig
import com.example.data.model.AppLanguage
import com.example.data.model.AudioQuality
import com.example.data.model.OtpMode
import com.example.data.model.UserProfile
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

    private val _iranNoVpnMode =
        MutableStateFlow(prefs.getBoolean(KEY_IRAN_NO_VPN_MODE, true))
    val iranNoVpnMode: StateFlow<Boolean> = _iranNoVpnMode.asStateFlow()

    private val _otpMode =
        MutableStateFlow(
            try {
                OtpMode.valueOf(
                    prefs.getString(KEY_OTP_MODE, AppConfig.DEFAULT_OTP_MODE.name)
                        ?: AppConfig.DEFAULT_OTP_MODE.name
                )
            } catch (_: Exception) {
                AppConfig.DEFAULT_OTP_MODE
            }
        )
    val otpMode: StateFlow<OtpMode> = _otpMode.asStateFlow()

    private val _googleDriveWebhookUrl =
        MutableStateFlow(
            prefs.getString(KEY_GOOGLE_DRIVE_WEBHOOK_URL, AppConfig.DEFAULT_GOOGLE_DRIVE_WEBHOOK_URL)
                ?: AppConfig.DEFAULT_GOOGLE_DRIVE_WEBHOOK_URL
        )
    val googleDriveWebhookUrl: StateFlow<String> = _googleDriveWebhookUrl.asStateFlow()

    private val _userProfile =
        MutableStateFlow(UserProfile.fromJsonString(prefs.getString(KEY_ACTIVE_USER_PROFILE_JSON, null)))
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // 3-Day Free Trial State (v1.9.0)
    private val _trialStartedAtMs =
        MutableStateFlow(prefs.getLong(KEY_TRIAL_STARTED_AT_MS, 0L))
    val trialStartedAtMs: StateFlow<Long> = _trialStartedAtMs.asStateFlow()

    private val _isTrialSessionActive =
        MutableStateFlow(
            prefs.getBoolean(KEY_TRIAL_SESSION_ACTIVE, false) &&
                AppConfig.isTrialValid(prefs.getLong(KEY_TRIAL_STARTED_AT_MS, 0L))
        )
    val isTrialSessionActive: StateFlow<Boolean> = _isTrialSessionActive.asStateFlow()

    /**
     * Starts a new 3-day trial (if never started) or resumes an existing 3-day trial if still valid.
     * Returns true if the 3-day trial is active and valid, or false if the 3-day window has expired.
     */
    fun startOrResumeTrial(nowMs: Long = System.currentTimeMillis()): Boolean {
        var startMs = _trialStartedAtMs.value
        if (startMs <= 0L) {
            startMs = nowMs
            prefs.edit().putLong(KEY_TRIAL_STARTED_AT_MS, startMs).apply()
            _trialStartedAtMs.value = startMs
        }
        val valid = AppConfig.isTrialValid(startMs, nowMs)
        prefs.edit().putBoolean(KEY_TRIAL_SESSION_ACTIVE, valid).apply()
        _isTrialSessionActive.value = valid
        return valid
    }

    fun endTrialSession() {
        prefs.edit().putBoolean(KEY_TRIAL_SESSION_ACTIVE, false).apply()
        _isTrialSessionActive.value = false
    }

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

    fun setOtpMode(mode: OtpMode) {
        prefs.edit().putString(KEY_OTP_MODE, mode.name).apply()
        _otpMode.value = mode
    }

    fun setGoogleDriveWebhookUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_GOOGLE_DRIVE_WEBHOOK_URL, trimmed).apply()
        _googleDriveWebhookUrl.value = trimmed
    }

    /**
     * Saves the active UserProfile in localStorage (SharedPreferences) and indexes it by both uid & email.
     */
    fun saveUserProfile(profile: UserProfile) {
        val json = profile.toJsonString()
        val editor = prefs.edit()
            .putString(KEY_ACTIVE_USER_PROFILE_JSON, json)
            .putString(KEY_AUTH_UID, profile.uid)
            .putString(KEY_AUTH_EMAIL, profile.email)
            .putString(KEY_AUTH_DISPLAY_NAME, profile.displayName)
            .putBoolean(KEY_AUTH_VERIFIED, profile.phoneVerified)
            .putBoolean(KEY_TRIAL_SESSION_ACTIVE, false)

        if (profile.uid.isNotBlank()) {
            editor.putString("$PREFIX_PROFILE_BY_UID${profile.uid}", json)
        }
        if (profile.email.isNotBlank()) {
            editor.putString("$PREFIX_PROFILE_BY_EMAIL${profile.email.lowercase()}", json)
        }
        if (profile.selectedCountry.isNotBlank()) {
            editor.putString(KEY_COUNTRY_CODE, profile.selectedCountry)
            editor.putBoolean(KEY_ONBOARDING_COMPLETED, true)
            _selectedCountryCode.value = profile.selectedCountry
            _onboardingCompleted.value = true
        }
        editor.apply()
        _isTrialSessionActive.value = false
        _userProfile.value = profile
    }

    fun getCachedProfileByUidOrEmail(uid: String, email: String): UserProfile? {
        if (uid.isNotBlank()) {
            val byUid = UserProfile.fromJsonString(prefs.getString("$PREFIX_PROFILE_BY_UID$uid", null))
            if (byUid != null) return byUid
        }
        if (email.isNotBlank()) {
            val byEmail = UserProfile.fromJsonString(
                prefs.getString("$PREFIX_PROFILE_BY_EMAIL${email.lowercase()}", null)
            )
            if (byEmail != null) return byEmail
        }
        return null
    }

    fun getCurrentUserUid(): String? = _userProfile.value?.uid ?: prefs.getString(KEY_AUTH_UID, null)
    fun getCurrentUserEmail(): String? = _userProfile.value?.email ?: prefs.getString(KEY_AUTH_EMAIL, null)
    fun getCurrentUserDisplayName(): String =
        _userProfile.value?.displayName ?: prefs.getString(KEY_AUTH_DISPLAY_NAME, "") ?: ""
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
            .remove(KEY_ACTIVE_USER_PROFILE_JSON)
            .remove(KEY_AUTH_UID)
            .remove(KEY_AUTH_EMAIL)
            .remove(KEY_AUTH_DISPLAY_NAME)
            .remove(KEY_AUTH_VERIFIED)
            .putBoolean(KEY_TRIAL_SESSION_ACTIVE, false)
            .apply()
        _userProfile.value = null
        _isTrialSessionActive.value = false
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_COUNTRY_CODE = "selected_country_code"
        private const val KEY_AUDIO_QUALITY = "audio_quality"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_IRAN_NO_VPN_MODE = "iran_no_vpn_mode"
        private const val KEY_OTP_MODE = "otp_mode"
        private const val KEY_GOOGLE_DRIVE_WEBHOOK_URL = "google_drive_webhook_url"
        private const val KEY_ACTIVE_USER_PROFILE_JSON = "active_user_profile_json"
        private const val KEY_TRIAL_STARTED_AT_MS = "trial_started_at_ms"
        private const val KEY_TRIAL_SESSION_ACTIVE = "trial_session_active"
        private const val PREFIX_PROFILE_BY_UID = "cached_user_uid_"
        private const val PREFIX_PROFILE_BY_EMAIL = "cached_user_email_"

        private const val KEY_AUTH_UID = "auth_uid"
        private const val KEY_AUTH_EMAIL = "auth_email"
        private const val KEY_AUTH_DISPLAY_NAME = "auth_display_name"
        private const val KEY_AUTH_VERIFIED = "auth_email_verified"
    }
}
