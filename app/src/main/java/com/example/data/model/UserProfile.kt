package com.example.data.model

import android.os.Build
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Configurable OTP Verification Mode (Module 2 - Stealth Sanitized):
 * 1. SMART_FALLBACK (Default): Strict regex validation + 6-digit OTP generation + helper banner.
 * 2. EMAIL_OR_GAS_OTP: Server-side 6-digit OTP dispatch.
 * 3. FIREBASE_FREE: Uses Firebase Phone Auth.
 */
enum class OtpMode(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val subtitleEn: String
) {
    SMART_FALLBACK(
        id = "SMART_FALLBACK",
        titleFa = "پشتیبان هوشمند (Smart Fallback)",
        titleEn = "Smart Fallback (Recommended)",
        subtitleFa = "اعتبارسنجی دقیق شماره موبایل + تولید کد ۶ رقمی بدون توقف در صورت اختلال پیامک بین‌المللی",
        subtitleEn = "Regex validation + instant 6-digit OTP without international SMS blocking"
    ),
    EMAIL_OR_GAS_OTP(
        id = "EMAIL_OR_GAS_OTP",
        titleFa = "ارسال از طریق سرور پیام و ایمیل (Server OTP)",
        titleEn = "Server / Email OTP",
        subtitleFa = "تولید و ارسال کد ۶ رقمی از طریق سرویس مرکزی پیام",
        subtitleEn = "Generates and dispatches 6-digit OTP via central messaging service"
    ),
    FIREBASE_FREE(
        id = "FIREBASE_FREE",
        titleFa = "فایربیس رایگان (Firebase Phone Auth)",
        titleEn = "Firebase Free Phone Auth",
        subtitleFa = "ارسال پیامک از طریق سرویس استاندارد Firebase Phone Auth",
        subtitleEn = "Standard SMS verification via Firebase Phone Auth"
    );

    fun getTitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) titleFa else titleEn
    fun getSubtitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) subtitleFa else subtitleEn
}

data class DialCountry(
    val countryCode: String,
    val dialCode: String,
    val flag: String,
    val nameFa: String,
    val nameEn: String
) {
    fun getDisplayLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.FA) "$flag $nameFa (\u200E$dialCode)" else "$flag $nameEn ($dialCode)"
}

enum class OtpVerificationStatus {
    VERIFIED,
    INVALID_FORMAT,
    RATE_LIMITED,
    MAX_ATTEMPTS_EXCEEDED,
    EXPIRED_OR_INVALID,
    BLOCKED_NO_BACKEND
}

/**
 * Standardized User Data Schema synchronized silently in background and cached locally.
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoURL: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val phoneVerificationBypassedNoBackend: Boolean = false,
    val selectedCountry: String = "",
    val favoriteGenres: List<String> = listOf("Pop", "Traditional"),
    val bio: String = "",
    val createdAt: String = AppConfig.nowIsoTimestamp(),
    val lastLoginAt: String = AppConfig.nowIsoTimestamp(),
    val deviceInfo: String = AppConfig.defaultDeviceInfo()
) {
    val isAuthenticated: Boolean
        get() = uid.isNotBlank() && email.isNotBlank()

    val isPhoneVerifiedStrict: Boolean
        get() = phoneVerified && phoneNumber.isNotBlank()

    /**
     * Allows proceeding past the phone onboarding step either when the phone is genuinely verified
     * OR when the user explicitly acknowledges `BLOCKED_NO_BACKEND` (while keeping `phoneVerified = false`).
     */
    val canProceedPastPhoneStep: Boolean
        get() = isPhoneVerifiedStrict || phoneVerificationBypassedNoBackend

    val isCountrySelectedStrict: Boolean
        get() = selectedCountry.isNotBlank()

    val isAdminUser: Boolean
        get() = isAuthenticated && email.trim().equals(AppConfig.ADMIN_EMAIL, ignoreCase = true)

    fun toJsonString(): String = adapter.toJson(this)

    fun toPrettyJsonString(): String = adapter.indent("  ").toJson(this)

    companion object {
        val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        private val adapter = moshi.adapter(UserProfile::class.java)

        fun fromJsonString(raw: String?): UserProfile? {
            if (raw.isNullOrBlank()) return null
            return try {
                val decoded = adapter.fromJson(raw) ?: return null
                if (decoded.favoriteGenres.isEmpty()) {
                    decoded.copy(favoriteGenres = listOf("Pop", "Traditional"))
                } else {
                    decoded
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Section 4.3: Privacy-safe, consent-gated Device Telemetry Info.
 */
data class DeviceTelemetryInfo(
    val brand: String = "",
    val model: String = "",
    val os_version: String = "",
    val device_id: String = "",
    val app_version: String = "",
    val network_type: String = ""
) {
    val osVersion: String get() = os_version
    val deviceId: String get() = device_id
    val appVersion: String get() = app_version
    val networkType: String get() = network_type
}

/**
 * Admin User & Device Login Session Log Schema.
 */
data class UserLoginLog(
    val log_id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val account_username: String = "",
    val phone_number: String = "",
    val ip_address: String = "",
    val device_info: DeviceTelemetryInfo = DeviceTelemetryInfo(),
    val login_timestamp: String = AppConfig.nowIsoTimestamp(),
    val local_time_display: String = AppConfig.nowLocalTimeFormatted(),
    val status: String = "Active",
    val synced_to_server: Boolean = false
) {
    val logId: String get() = log_id
    val userId: String get() = user_id
    val accountUsername: String get() = account_username
    val phoneNumber: String get() = phone_number
    val ipAddress: String get() = ip_address
    val deviceInfo: DeviceTelemetryInfo get() = device_info
    val loginTimestamp: String get() = login_timestamp
    val localTimeDisplay: String get() = local_time_display
    val isSyncedToCloud: Boolean get() = synced_to_server

    fun toJsonString(): String = logAdapter.toJson(this)

    companion object {
        private val logAdapter = UserProfile.moshi.adapter(UserLoginLog::class.java)
        private val listType = Types.newParameterizedType(List::class.java, UserLoginLog::class.java)
        private val listAdapter = UserProfile.moshi.adapter<List<UserLoginLog>>(listType)

        fun listToJsonString(logs: List<UserLoginLog>): String = listAdapter.toJson(logs)

        fun prettyListToJsonString(logs: List<UserLoginLog>): String =
            listAdapter.indent("  ").toJson(logs)

        fun listFromJsonString(raw: String?): List<UserLoginLog> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                listAdapter.fromJson(raw) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun toJsonList(logs: List<UserLoginLog>): String = prettyListToJsonString(logs)

        fun fromJsonList(raw: String?): List<UserLoginLog> = listFromJsonString(raw)
    }
}

object AppConfig {
    /**
     * Section 4.2: Admin Dashboard access is strictly bound to authenticated owner identity (`ADMIN_EMAIL`).
     * Hardcoded client-side PIN (`2026`) has been removed per security audit.
     */
    const val ADMIN_EMAIL: String = "bagheri.cmd@gmail.com"
    const val APP_VERSION_NAME: String = "2.0.0"
    const val OTP_EXPIRY_MS: Long = 5L * 60L * 1000L
    const val OTP_RESEND_COOLDOWN_MS: Long = 60L * 1000L
    const val OTP_MAX_ATTEMPTS: Int = 5

    /**
     * 3-Day Free Trial duration in milliseconds (3 days = 72 hours = 259,200,000 ms).
     */
    const val TRIAL_DURATION_MS: Long = 3L * 24L * 60L * 60L * 1000L

    /**
     * Computes remaining trial time in milliseconds from [trialStartedAtMs].
     * If [trialStartedAtMs] <= 0L (not started yet), returns full [TRIAL_DURATION_MS].
     */
    fun computeTrialRemainingMs(
        trialStartedAtMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        if (trialStartedAtMs <= 0L) return TRIAL_DURATION_MS
        val elapsed = (nowMs - trialStartedAtMs).coerceAtLeast(0L)
        return (TRIAL_DURATION_MS - elapsed).coerceAtLeast(0L)
    }

    /**
     * Returns true if the 3-day trial has not yet started or still has remaining time (> 0 ms).
     */
    fun isTrialValid(
        trialStartedAtMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return computeTrialRemainingMs(trialStartedAtMs, nowMs) > 0L
    }

    /**
     * Formats remaining trial milliseconds into a human-friendly Persian or English string.
     */
    fun formatTrialRemaining(remainingMs: Long, lang: AppLanguage): String {
        if (remainingMs <= 0L) {
            return if (lang == AppLanguage.FA) "پایان یافته" else "Expired"
        }
        val totalMinutes = (remainingMs / (60L * 1000L)).coerceAtLeast(1L)
        val days = totalMinutes / (24L * 60L)
        val hours = (totalMinutes % (24L * 60L)) / 60L
        val minutes = totalMinutes % 60L

        return if (lang == AppLanguage.FA) {
            when {
                days > 0L && hours > 0L -> "$days روز و $hours ساعت باقی‌مانده"
                days > 0L -> "$days روز باقی‌مانده"
                hours > 0L -> "$hours ساعت و $minutes دقیقه باقی‌مانده"
                else -> "$minutes دقیقه باقی‌مانده"
            }
        } else {
            when {
                days > 0L && hours > 0L -> "${days}d ${hours}h left"
                days > 0L -> "${days}d left"
                hours > 0L -> "${hours}h ${minutes}m left"
                else -> "${minutes}m left"
            }
        }
    }

    val DEFAULT_OTP_MODE: OtpMode = OtpMode.SMART_FALLBACK
    const val DEFAULT_GOOGLE_DRIVE_WEBHOOK_URL: String = ""

    private val IRAN_MOBILE_REGEX = Regex("^(\\+98|0)?9\\d{9}$")
    private val INTERNATIONAL_MOBILE_REGEX = Regex("^\\+?[1-9]\\d{6,14}$")

    val dialCountries: List<DialCountry> = listOf(
        DialCountry("IR", "+98", "🇮🇷", "ایران", "Iran"),
        DialCountry("TR", "+90", "🇹🇷", "ترکیه", "Turkey"),
        DialCountry("US", "+1", "🇺🇸", "آمریکا", "United States"),
        DialCountry("GB", "+44", "🇬🇧", "بریتانیا", "United Kingdom"),
        DialCountry("ES", "+34", "🇪🇸", "اسپانیا", "Spain"),
        DialCountry("FR", "+33", "🇫🇷", "فرانسه", "France"),
        DialCountry("DE", "+49", "🇩🇪", "آلمان", "Germany"),
        DialCountry("AE", "+971", "🇦🇪", "امارات", "UAE")
    )

    val availableFavoriteGenres: List<Pair<String, String>> = listOf(
        "Pop" to "پاپ (Pop)",
        "Traditional" to "سنتی و اصیل (Traditional)",
        "Rap & Hip-Hop" to "رپ و هیپ‌هاپ (Hip-Hop)",
        "Rock" to "راک و آلترناتیو (Rock)",
        "Party & Dance" to "شاد و پارتی (Dance)",
        "Chill & Acoustic" to "آرامش و آکوستیک (Chill)",
        "Electronic" to "الکترونیک (Electronic)",
        "Romance" to "عاشقانه (Romance)"
    )

    fun sanitizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input.trim()) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '0'..'9', '+' -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun isValidPhoneNumber(dialCode: String, rawPhoneInput: String): Boolean {
        val cleaned = sanitizeDigits(rawPhoneInput)
        if (cleaned.isBlank()) return false

        return if (dialCode == "+98" || cleaned.startsWith("+98") || cleaned.startsWith("09")) {
            val candidate = when {
                cleaned.startsWith("+98") -> cleaned
                cleaned.startsWith("0") -> cleaned
                else -> "0$cleaned"
            }
            IRAN_MOBILE_REGEX.matches(candidate)
        } else {
            val full = if (cleaned.startsWith("+")) cleaned else "$dialCode${cleaned.trimStart('0')}"
            INTERNATIONAL_MOBILE_REGEX.matches(full)
        }
    }

    fun normalizePhoneNumber(dialCode: String, rawPhoneInput: String): String {
        val cleaned = sanitizeDigits(rawPhoneInput)
        if (dialCode == "+98" || cleaned.startsWith("+98") || cleaned.startsWith("09")) {
            val national = when {
                cleaned.startsWith("+98") -> cleaned.removePrefix("+98")
                cleaned.startsWith("0") -> cleaned.removePrefix("0")
                else -> cleaned
            }
            return "+98$national"
        }
        return if (cleaned.startsWith("+")) {
            cleaned
        } else {
            "$dialCode${cleaned.trimStart('0')}"
        }
    }

    fun nowIsoTimestamp(): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date())
        } catch (_: Exception) {
            "2026-11-08T16:00:00.000Z"
        }
    }

    fun nowLocalTimeFormatted(): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            sdf.format(Date())
        } catch (_: Exception) {
            "2026-11-08 16:00:00"
        }
    }

    fun defaultDeviceInfo(): String {
        return try {
            val rel = Build.VERSION.RELEASE ?: "14"
            val mfg = Build.MANUFACTURER ?: "Android"
            val mdl = Build.MODEL ?: "Device"
            "Android $rel (SDK ${Build.VERSION.SDK_INT}) - $mfg $mdl"
        } catch (_: Exception) {
            "Android / Mobile Client"
        }
    }
}
