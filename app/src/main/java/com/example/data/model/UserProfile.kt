package com.example.data.model

import android.os.Build
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Configurable OTP Verification Mode (Module 2):
 * 1. FIREBASE_FREE: Uses Firebase Phone Auth (signInWithPhoneNumber / PhoneAuthProvider).
 * 2. EMAIL_OR_GAS_OTP: Requests a 6-digit OTP from the Admin Google Apps Script (Google Drive / Email).
 * 3. SMART_FALLBACK (Default): Strict regex validation + 6-digit OTP generation + Dev/Smart helper banner
 *    so users are never blocked by international SMS carrier disruptions.
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
        titleFa = "ارسال از طریق اسکریپت گوگل درایو / ایمیل (GAS OTP)",
        titleEn = "Google Apps Script / Email OTP",
        subtitleFa = "تولید و ارسال کد ۶ رقمی از طریق وب‌اپ Google Drive ادمین",
        subtitleEn = "Generates and dispatches 6-digit OTP via Admin Google Drive Web App"
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

/**
 * Standardized User Data Schema stored in the Project Owner's Google Drive (JSON + Google Sheet)
 * and cached locally in SharedPreferences / localStorage (Module 3).
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoURL: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
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

    val isCountrySelectedStrict: Boolean
        get() = selectedCountry.isNotBlank()

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

object AppConfig {
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

    /**
     * Default OTP mode: SMART_FALLBACK ensures users are never blocked if international SMS is filtered.
     */
    val DEFAULT_OTP_MODE: OtpMode = OtpMode.SMART_FALLBACK

    /**
     * Default Google Apps Script Web App URL for storing user profiles in the Project Owner's Google Drive.
     * Can be customized at runtime in Profile/Settings or set via GOOGLE_DRIVE_WEBHOOK_URL.
     */
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

    /**
     * Converts Persian/Arabic digits to English ASCII digits and strips spaces/hyphens.
     */
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

    /**
     * Validates phone number format:
     * - For Iran (+98): strictly checks `^(\+98|0)?9\d{9}$`
     * - For other countries: checks 7..14 digits after dial code
     */
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

    /**
     * Normalizes a valid phone number into canonical E.164 format (e.g. `+989120000000`).
     */
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
            "2026-11-07T18:00:00.000Z"
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
