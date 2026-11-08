package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.BuildConfig
import com.example.MeloGrooveApp
import com.example.data.local.UserPreferences
import com.example.data.model.AppConfig
import com.example.data.model.DeviceTelemetryInfo
import com.example.data.model.OtpMode
import com.example.data.model.OtpVerificationStatus
import com.example.data.model.UserLoginLog
import com.example.data.model.UserProfile
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DriveSyncOutcome(
    val profile: UserProfile,
    val syncedToRemoteDrive: Boolean,
    val statusMessageFa: String,
    val statusMessageEn: String
)

data class OtpDispatchResult(
    val success: Boolean,
    val status: OtpVerificationStatus = OtpVerificationStatus.BLOCKED_NO_BACKEND,
    val normalizedPhone: String,
    val generatedCodeForFallbackOrTest: String? = null,
    val messageFa: String,
    val messageEn: String
)

/**
 * Cloud Synchronization, OTP Verification Gate, & Privacy-Safe Session Telemetry Service.
 * Remediated per Section 4.1, 4.2, and 4.3 of MELOGROOVE_PRODUCT_AUDIT_AND_REMEDIATION_PLAN:
 * - Zero hardcoded OTP backdoors (`"123456"` removed).
 * - Never leaks generated OTP secrets to client UI or logs (`generatedCodeForFallbackOrTest = null`).
 * - Never claims SMS phone verification via email; explicitly reports `BLOCKED_NO_BACKEND` when no SMS gateway is configured.
 * - Enforces 60-second request cooldown, 5-minute OTP expiry, and max 5 verification attempts per issued challenge.
 * - Uses pseudonymous app-scoped installation IDs instead of hardware `ANDROID_ID` and respects user telemetry consent.
 */
class DriveStorageService(
    private val userPreferences: UserPreferences? = null
) {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val random = SecureRandom()

    private data class PendingOtpChallenge(
        val code: String,
        val issuedAtMs: Long,
        val expiresAtMs: Long,
        val failedAttempts: Int = 0
    )

    private val pendingOtps = ConcurrentHashMap<String, PendingOtpChallenge>()

    private val _lastSyncStatus = MutableStateFlow<String?>(null)
    val lastSyncStatus: StateFlow<String?> = _lastSyncStatus.asStateFlow()

    /**
     * Records a privacy-safe session diagnostic log when the user has opted into telemetry
     * (Section 4.3). Never blocks discovery or playback and never collects hardware ANDROID_ID.
     */
    suspend fun recordUserSessionTelemetry(
        userId: String,
        accountUsername: String,
        phoneNumber: String,
        status: String = "Active"
    ): UserLoginLog = withContext(Dispatchers.IO) {
        val prefs = userPreferences
        val consentGranted = prefs?.telemetryConsentGranted?.value == true
        val deviceInfo = collectDeviceTelemetryInfo()

        val logEntry = UserLoginLog(
            log_id = UUID.randomUUID().toString(),
            user_id = userId.ifBlank { "anonymous" },
            account_username = if (consentGranted) {
                accountUsername.ifBlank { "Guest / Trial" }
            } else {
                maskIdentifier(accountUsername.ifBlank { "Guest / Trial" })
            },
            phone_number = if (consentGranted && phoneNumber.isNotBlank()) {
                maskPhone(phoneNumber)
            } else {
                "Redacted (Privacy Mode)"
            },
            ip_address = if (consentGranted) "Client-Network (Redacted IP)" else "Redacted (Consent Off)",
            device_info = deviceInfo,
            login_timestamp = AppConfig.nowIsoTimestamp(),
            local_time_display = AppConfig.nowLocalTimeFormatted(),
            status = status,
            synced_to_server = false
        )

        if (consentGranted && prefs != null) {
            prefs.appendOrUpdateLoginLog(logEntry)
            flushPendingTelemetryQueue()
        }

        logEntry
    }

    private fun maskIdentifier(raw: String): String {
        val clean = raw.trim()
        if (!clean.contains("@")) return clean
        val userPart = clean.substringBefore("@")
        val domainPart = clean.substringAfter("@")
        val prefix = userPart.take(2)
        return "$prefix***@$domainPart"
    }

    private fun maskPhone(raw: String): String {
        val clean = raw.trim()
        if (clean.length < 7) return "***"
        return "${clean.take(4)}****${clean.takeLast(3)}"
    }

    /**
     * Flushes any unsynced [UserLoginLog] entries from the local offline queue to the remote endpoint
     * only when telemetry consent is enabled and an authenticated UID is present.
     */
    suspend fun flushPendingTelemetryQueue(): Int = withContext(Dispatchers.IO) {
        val prefs = userPreferences ?: return@withContext 0
        if (!prefs.telemetryConsentGranted.value) return@withContext 0
        val webhookUrl = prefs.googleDriveWebhookUrl.value.trim()
        if (!webhookUrl.startsWith("https://")) return@withContext 0
        val callerUid = prefs.getCurrentUserUid().orEmpty()
        if (callerUid.isBlank()) return@withContext 0

        val unsynced = prefs.getUnsyncedLoginLogs()
        var flushedCount = 0
        for (log in unsynced) {
            try {
                val payloadJson = UserProfile.moshi
                    .adapter(TelemetryPushPayload::class.java)
                    .toJson(TelemetryPushPayload(clientUid = callerUid, log = log))
                val req = Request.Builder()
                    .url(webhookUrl)
                    .header("X-Client-Uid", callerUid)
                    .header("X-MeloGroove-Client", "Android-${AppConfig.APP_VERSION_NAME}")
                    .post(payloadJson.toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        prefs.appendOrUpdateLoginLog(log.copy(synced_to_server = true))
                        flushedCount++
                    }
                }
            } catch (_: Exception) {
                // Silent failure; never logs PII to logcat
            }
        }
        flushedCount
    }

    /**
     * Collects non-invasive device metadata using an app-scoped rotatable installation ID
     * (never hardware `Settings.Secure.ANDROID_ID`).
     */
    fun collectDeviceTelemetryInfo(context: Context? = null): DeviceTelemetryInfo {
        val appContext = try {
            context ?: MeloGrooveApp.instance.applicationContext
        } catch (_: Exception) {
            null
        }

        val brand = try {
            (Build.MANUFACTURER ?: Build.BRAND ?: "Android").replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            "Android"
        }

        val model = try {
            Build.MODEL ?: "Generic Device"
        } catch (_: Exception) {
            "Generic Device"
        }

        val osVersion = try {
            "Android ${Build.VERSION.RELEASE ?: "14"} (API ${Build.VERSION.SDK_INT})"
        } catch (_: Exception) {
            "Android OS"
        }

        val pseudonymousId = try {
            userPreferences?.getOrCreateInstallationId() ?: "inst_ephemeral"
        } catch (_: Exception) {
            "inst_ephemeral"
        }

        val appVersion = try {
            "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        } catch (_: Exception) {
            "2.0.0"
        }

        val networkType = detectNetworkTransport(appContext)

        return DeviceTelemetryInfo(
            brand = brand,
            model = model,
            os_version = osVersion,
            device_id = pseudonymousId,
            app_version = appVersion,
            network_type = networkType
        )
    }

    private fun detectNetworkTransport(context: Context?): String {
        if (context == null) return "Unknown Network"
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null
            when {
                caps == null -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN / Tunnel"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Online"
            }
        } catch (_: Exception) {
            "Online"
        }
    }

    /**
     * Step 2 of Strict Onboarding Guard:
     * Synchronizes user profile with the authenticated remote endpoint (if configured) & local cache.
     */
    suspend fun syncUserFromDrive(
        uid: String,
        email: String,
        initialDisplayName: String,
        initialPhotoUrl: String
    ): DriveSyncOutcome = withContext(Dispatchers.IO) {
        val nowIso = AppConfig.nowIsoTimestamp()
        val cached = userPreferences?.getCachedProfileByUidOrEmail(uid, email)
        val webhookUrl = userPreferences?.googleDriveWebhookUrl?.value?.trim().orEmpty()

        var remoteProfile: UserProfile? = null
        var remoteSucceeded = false

        if (webhookUrl.startsWith("https://") && uid.isNotBlank()) {
            try {
                val getUserReq = mapOf(
                    "action" to "get_user",
                    "clientUid" to uid,
                    "uid" to uid,
                    "email" to email
                )
                val reqJson = UserProfile.moshi.adapter(Map::class.java).toJson(getUserReq)
                val req = Request.Builder()
                    .url(webhookUrl)
                    .header("X-Client-Uid", uid)
                    .header("X-MeloGroove-Client", "Android-${AppConfig.APP_VERSION_NAME}")
                    .post(reqJson.toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string().orEmpty()
                        if (bodyStr.isNotBlank()) {
                            val envAdapter = UserProfile.moshi.adapter(GasUserEnvelope::class.java)
                            val parsedEnv = envAdapter.fromJson(bodyStr)
                            if (parsedEnv?.found == true && parsedEnv.user != null && parsedEnv.user.uid == uid) {
                                remoteProfile = parsedEnv.user
                                remoteSucceeded = true
                            } else if (parsedEnv?.status == "ok") {
                                remoteSucceeded = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("ProfileSync", "Remote get_user sync skipped or offline")
            }
        }

        val mergedProfile = when {
            remoteProfile != null -> {
                val rp = remoteProfile!!
                rp.copy(
                    uid = uid.ifBlank { rp.uid },
                    email = email.ifBlank { rp.email },
                    displayName = rp.displayName.ifBlank { initialDisplayName.ifBlank { email.substringBefore("@") } },
                    photoURL = rp.photoURL.ifBlank { initialPhotoUrl },
                    lastLoginAt = nowIso,
                    deviceInfo = AppConfig.defaultDeviceInfo()
                )
            }
            cached != null -> {
                cached.copy(
                    uid = uid.ifBlank { cached.uid },
                    email = email.ifBlank { cached.email },
                    displayName = cached.displayName.ifBlank { initialDisplayName.ifBlank { email.substringBefore("@") } },
                    photoURL = cached.photoURL.ifBlank { initialPhotoUrl },
                    lastLoginAt = nowIso,
                    deviceInfo = AppConfig.defaultDeviceInfo()
                )
            }
            else -> {
                UserProfile(
                    uid = uid,
                    email = email,
                    displayName = initialDisplayName.ifBlank { email.substringBefore("@") },
                    photoURL = initialPhotoUrl,
                    phoneNumber = "",
                    phoneVerified = false,
                    phoneVerificationBypassedNoBackend = false,
                    selectedCountry = "",
                    favoriteGenres = listOf("Pop", "Traditional"),
                    bio = "",
                    createdAt = nowIso,
                    lastLoginAt = nowIso,
                    deviceInfo = AppConfig.defaultDeviceInfo()
                )
            }
        }

        userPreferences?.saveUserProfile(mergedProfile)
        val pushOk = pushProfileToWebhook(webhookUrl, mergedProfile)
        val synced = remoteSucceeded || pushOk

        val outcome = DriveSyncOutcome(
            profile = mergedProfile,
            syncedToRemoteDrive = synced,
            statusMessageFa = "اطلاعات حساب کاربری با موفقیت همگام‌سازی شد",
            statusMessageEn = "Account profile synchronized successfully"
        )
        _lastSyncStatus.value = outcome.statusMessageFa
        outcome
    }

    /**
     * Saves/updates the user's profile in local cache and remote endpoint (scoped to user.uid).
     */
    suspend fun saveOrUpdateUserProfile(profile: UserProfile): DriveSyncOutcome = withContext(Dispatchers.IO) {
        val updated = profile.copy(
            lastLoginAt = AppConfig.nowIsoTimestamp(),
            deviceInfo = AppConfig.defaultDeviceInfo()
        )
        userPreferences?.saveUserProfile(updated)

        val webhookUrl = userPreferences?.googleDriveWebhookUrl?.value?.trim().orEmpty()
        val synced = pushProfileToWebhook(webhookUrl, updated)

        val outcome = DriveSyncOutcome(
            profile = updated,
            syncedToRemoteDrive = synced,
            statusMessageFa = "تغییرات پروفایل کاربری با موفقیت ذخیره شد",
            statusMessageEn = "User profile changes saved successfully"
        )
        _lastSyncStatus.value = outcome.statusMessageFa
        outcome
    }

    private data class GasUserEnvelope(
        val status: String? = null,
        val found: Boolean? = null,
        val user: UserProfile? = null
    )

    private data class GasUpsertPayload(
        val action: String = "upsert_user",
        val clientUid: String,
        val user: UserProfile
    )

    private data class TelemetryPushPayload(
        val action: String = "log_telemetry",
        val clientUid: String,
        val log: UserLoginLog
    )

    private fun pushProfileToWebhook(webhookUrl: String, profile: UserProfile): Boolean {
        if (!webhookUrl.startsWith("https://") || profile.uid.isBlank()) return false
        return try {
            val payloadJson = UserProfile.moshi
                .adapter(GasUpsertPayload::class.java)
                .toJson(GasUpsertPayload(clientUid = profile.uid, user = profile))
            val req = Request.Builder()
                .url(webhookUrl)
                .header("X-Client-Uid", profile.uid)
                .header("X-MeloGroove-Client", "Android-${AppConfig.APP_VERSION_NAME}")
                .post(payloadJson.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(req).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Section 4.1: Secure OTP request flow.
     * - Validates E.164 / Iran mobile format.
     * - Enforces 60-second cooldown rate limit per phone number.
     * - NEVER returns raw generated OTP codes to the client UI or logs (`generatedCodeForFallbackOrTest = null`).
     * - Never claims an email is an SMS verification.
     * - When no verified SMS backend is configured, returns `status = OtpVerificationStatus.BLOCKED_NO_BACKEND`
     *   so the UI can display an honest `BLOCKED_NO_BACKEND` notice.
     */
    suspend fun requestOtp(
        dialCode: String,
        rawPhoneInput: String,
        email: String,
        mode: OtpMode,
        nowMs: Long = System.currentTimeMillis()
    ): OtpDispatchResult = withContext(Dispatchers.IO) {
        if (!AppConfig.isValidPhoneNumber(dialCode, rawPhoneInput)) {
            return@withContext OtpDispatchResult(
                success = false,
                status = OtpVerificationStatus.INVALID_FORMAT,
                normalizedPhone = "",
                generatedCodeForFallbackOrTest = null,
                messageFa = "فرمت شماره موبایل نامعتبر است (مثال صحیح ایران: 09123456789)",
                messageEn = "Invalid mobile phone number format (e.g., 09123456789)"
            )
        }

        val normalizedPhone = AppConfig.normalizePhoneNumber(dialCode, rawPhoneInput)
        val existing = pendingOtps[normalizedPhone]
        if (existing != null && (nowMs - existing.issuedAtMs) < AppConfig.OTP_RESEND_COOLDOWN_MS) {
            return@withContext OtpDispatchResult(
                success = false,
                status = OtpVerificationStatus.RATE_LIMITED,
                normalizedPhone = normalizedPhone,
                generatedCodeForFallbackOrTest = null,
                messageFa = "لطفاً ۶۰ ثانیه پیش از درخواست مجدد کد صبر کنید.",
                messageEn = "Please wait 60 seconds before requesting another code."
            )
        }

        // Section 4.1: Without a configured real SMS provider (Firebase Phone Auth SMS quota or verified SMS gateway),
        // phone verification is explicitly marked BLOCKED_NO_BACKEND rather than faking local/email OTP.
        OtpDispatchResult(
            success = false,
            status = OtpVerificationStatus.BLOCKED_NO_BACKEND,
            normalizedPhone = normalizedPhone,
            generatedCodeForFallbackOrTest = null,
            messageFa = "وضعیت سرویس پیامک: BLOCKED_NO_BACKEND — درگاه پیامکی سرور فعال نیست. کد تایید هرگز به‌صورت محلی یا از طریق ایمیل جعل نمی‌شود. می‌توانید بدون تایید پیامکی ادامه دهید.",
            messageEn = "SMS Verification Status: BLOCKED_NO_BACKEND — No verified SMS gateway is configured. Client-side or email-disguised phone OTP is disabled for security. You may continue with unverified phone status."
        )
    }

    /**
     * Test/Server-only challenge registration helper (used by unit tests to verify expiry, single-use,
     * attempt limits, and rejection of `"123456"` without exposing any code to the client UI).
     */
    internal fun registerServerIssuedChallengeForTest(
        normalizedPhone: String,
        code: String,
        issuedAtMs: Long = System.currentTimeMillis(),
        expiresAtMs: Long = issuedAtMs + AppConfig.OTP_EXPIRY_MS
    ) {
        pendingOtps[normalizedPhone] = PendingOtpChallenge(
            code = code,
            issuedAtMs = issuedAtMs,
            expiresAtMs = expiresAtMs,
            failedAttempts = 0
        )
    }

    fun verifyOtpDetailed(
        normalizedPhone: String,
        enteredCode: String,
        mode: OtpMode,
        nowMs: Long = System.currentTimeMillis()
    ): OtpVerificationStatus {
        val cleanedCode = AppConfig.sanitizeDigits(enteredCode)
        if (cleanedCode.length != 6) return OtpVerificationStatus.INVALID_FORMAT

        val pending = pendingOtps[normalizedPhone] ?: return OtpVerificationStatus.EXPIRED_OR_INVALID

        if (nowMs > pending.expiresAtMs) {
            pendingOtps.remove(normalizedPhone)
            return OtpVerificationStatus.EXPIRED_OR_INVALID
        }

        if (pending.failedAttempts >= AppConfig.OTP_MAX_ATTEMPTS) {
            pendingOtps.remove(normalizedPhone)
            return OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED
        }

        // Section 4.1: Hardcoded "123456" backdoor is strictly prohibited and removed.
        if (pending.code == cleanedCode) {
            pendingOtps.remove(normalizedPhone) // One-time use enforced
            return OtpVerificationStatus.VERIFIED
        } else {
            val nextAttempts = pending.failedAttempts + 1
            if (nextAttempts >= AppConfig.OTP_MAX_ATTEMPTS) {
                pendingOtps.remove(normalizedPhone)
                return OtpVerificationStatus.MAX_ATTEMPTS_EXCEEDED
            } else {
                pendingOtps[normalizedPhone] = pending.copy(failedAttempts = nextAttempts)
                return OtpVerificationStatus.EXPIRED_OR_INVALID
            }
        }
    }

    fun verifyOtp(
        normalizedPhone: String,
        enteredCode: String,
        mode: OtpMode,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return verifyOtpDetailed(normalizedPhone, enteredCode, mode, nowMs) == OtpVerificationStatus.VERIFIED
    }
}
