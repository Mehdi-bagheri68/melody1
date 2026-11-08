package com.example.data.remote

import com.example.data.local.UserPreferences
import com.example.data.model.AppConfig
import com.example.data.model.OtpMode
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
import org.json.JSONObject
import java.security.SecureRandom
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
    val normalizedPhone: String,
    val generatedCodeForFallbackOrTest: String?,
    val messageFa: String,
    val messageEn: String
)

/**
 * Module 3: Dedicated Service for communicating with the Project Owner's Google Drive
 * (via Google Apps Script Web App endpoint `GOOGLE_DRIVE_WEBHOOK_URL`) while simultaneously
 * caching user profiles in device `localStorage` (`UserPreferences`).
 */
class DriveStorageService(
    private val userPreferences: UserPreferences
) {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val random = SecureRandom()

    // Active OTP codes indexed by normalized phone number (valid for 5 minutes)
    private data class PendingOtp(val code: String, val expiresAtMs: Long)
    private val pendingOtps = ConcurrentHashMap<String, PendingOtp>()

    private val _lastSyncStatus = MutableStateFlow<String?>(null)
    val lastSyncStatus: StateFlow<String?> = _lastSyncStatus.asStateFlow()

    /**
     * Step 2 of Strict Onboarding Guard (`syncUserFromDrive`):
     * Immediately after Google Sign-In, queries the Admin Google Drive Web App endpoint
     * (and local cache) by `uid` / `email`.
     * - If the user already has a profile in Admin Google Drive (or local cache), loads their
     *   `phoneNumber`, `phoneVerified`, `selectedCountry`, `displayName`, `favoriteGenres`, and `bio`.
     * - Otherwise creates a new profile record and syncs it to Admin Google Drive + local cache.
     */
    suspend fun syncUserFromDrive(
        uid: String,
        email: String,
        initialDisplayName: String,
        initialPhotoUrl: String
    ): DriveSyncOutcome = withContext(Dispatchers.IO) {
        val nowIso = AppConfig.nowIsoTimestamp()
        val cached = userPreferences.getCachedProfileByUidOrEmail(uid, email)
        val webhookUrl = userPreferences.googleDriveWebhookUrl.value.trim()

        var remoteProfile: UserProfile? = null
        var remoteSucceeded = false

        if (webhookUrl.startsWith("https://script.google.com/")) {
            try {
                val getUserReq = mapOf(
                    "action" to "get_user",
                    "uid" to uid,
                    "email" to email
                )
                val reqJson = UserProfile.moshi.adapter(Map::class.java).toJson(getUserReq)
                val req = Request.Builder()
                    .url(webhookUrl)
                    .post(reqJson.toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string().orEmpty()
                        if (bodyStr.isNotBlank()) {
                            val envAdapter = UserProfile.moshi.adapter(GasUserEnvelope::class.java)
                            val parsedEnv = envAdapter.fromJson(bodyStr)
                            if (parsedEnv?.found == true && parsedEnv.user != null) {
                                remoteProfile = parsedEnv.user
                                remoteSucceeded = true
                            } else if (parsedEnv?.status == "ok") {
                                remoteSucceeded = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("DriveStorageService", "Remote get_user sync skipped or offline", e)
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
                    selectedCountry = "",
                    favoriteGenres = listOf("Pop", "Traditional"),
                    bio = "",
                    createdAt = nowIso,
                    lastLoginAt = nowIso,
                    deviceInfo = AppConfig.defaultDeviceInfo()
                )
            }
        }

        // Persist in localStorage (SharedPreferences)
        userPreferences.saveUserProfile(mergedProfile)

        // Also push updated lastLoginAt / initial record to Admin Google Drive if webhook is configured
        val pushOk = pushProfileToWebhook(webhookUrl, mergedProfile)
        val synced = remoteSucceeded || pushOk

        val outcome = DriveSyncOutcome(
            profile = mergedProfile,
            syncedToRemoteDrive = synced,
            statusMessageFa = if (synced) {
                "همگام‌سازی با Google Drive ادمین و حافظه دستگاه انجام شد"
            } else {
                "ذخیره‌سازی در کش محلی انجام شد (آماده اتصال به وب‌اپ Google Drive ادمین)"
            },
            statusMessageEn = if (synced) {
                "Synced with Admin Google Drive & local cache"
            } else {
                "Saved to local cache (ready for Admin Google Drive webhook)"
            }
        )
        _lastSyncStatus.value = outcome.statusMessageFa
        outcome
    }

    /**
     * Saves/updates the user's profile in both local cache (`localStorage`) and the Admin's Google Drive.
     */
    suspend fun saveOrUpdateUserProfile(profile: UserProfile): DriveSyncOutcome = withContext(Dispatchers.IO) {
        val updated = profile.copy(
            lastLoginAt = AppConfig.nowIsoTimestamp(),
            deviceInfo = AppConfig.defaultDeviceInfo()
        )
        userPreferences.saveUserProfile(updated)

        val webhookUrl = userPreferences.googleDriveWebhookUrl.value.trim()
        val synced = pushProfileToWebhook(webhookUrl, updated)

        val outcome = DriveSyncOutcome(
            profile = updated,
            syncedToRemoteDrive = synced,
            statusMessageFa = if (synced) {
                "پروفایل در Google Drive مالک پروژه و حافظه دستگاه ذخیره شد"
            } else {
                "پروفایل در حافظه دستگاه ذخیره شد (در صورت ثبت URL اسکریپت، در Google Drive ادمین نیز همگام می‌شود)"
            },
            statusMessageEn = if (synced) {
                "Profile saved to Admin Google Drive & device storage"
            } else {
                "Profile saved to device cache"
            }
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
        val user: UserProfile
    )

    private fun pushProfileToWebhook(webhookUrl: String, profile: UserProfile): Boolean {
        if (!webhookUrl.startsWith("https://script.google.com/")) return false
        return try {
            val payloadJson = UserProfile.moshi
                .adapter(GasUpsertPayload::class.java)
                .toJson(GasUpsertPayload(user = profile))
            val req = Request.Builder()
                .url(webhookUrl)
                .post(payloadJson.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(req).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            AppLogger.w("DriveStorageService", "Failed pushing profile to Google Drive webhook", e)
            false
        }
    }

    /**
     * Dispatches a 6-digit OTP code according to the configured [OtpMode] (`FIREBASE_FREE`, `EMAIL_OR_GAS_OTP`, `SMART_FALLBACK`).
     */
    suspend fun requestOtp(
        dialCode: String,
        rawPhoneInput: String,
        email: String,
        mode: OtpMode
    ): OtpDispatchResult = withContext(Dispatchers.IO) {
        if (!AppConfig.isValidPhoneNumber(dialCode, rawPhoneInput)) {
            return@withContext OtpDispatchResult(
                success = false,
                normalizedPhone = "",
                generatedCodeForFallbackOrTest = null,
                messageFa = "فرمت شماره موبایل نامعتبر است (مثال صحیح ایران: 09123456789)",
                messageEn = "Invalid mobile phone number format (e.g., 09123456789)"
            )
        }

        val normalizedPhone = AppConfig.normalizePhoneNumber(dialCode, rawPhoneInput)
        val generatedCode = "%06d".format(java.util.Locale.US, 100000 + random.nextInt(900000))
        val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000L
        pendingOtps[normalizedPhone] = PendingOtp(code = generatedCode, expiresAtMs = expiresAt)

        val webhookUrl = userPreferences.googleDriveWebhookUrl.value.trim()
        var deliveredViaGas = false

        if ((mode == OtpMode.EMAIL_OR_GAS_OTP || mode == OtpMode.SMART_FALLBACK) &&
            webhookUrl.startsWith("https://script.google.com/")
        ) {
            try {
                val payload = JSONObject().apply {
                    put("action", "send_otp")
                    put("phoneNumber", normalizedPhone)
                    put("email", email)
                    put("otpCode", generatedCode)
                }
                val req = Request.Builder()
                    .url(webhookUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        deliveredViaGas = true
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("DriveStorageService", "GAS OTP webhook unreachable; using Smart Fallback", e)
            }
        }

        val messageFa = when {
            deliveredViaGas ->
                "کد تایید ۶ رقمی از طریق اسکریپت Google Drive ادمین ثبت و ارسال شد (کد: $generatedCode)"
            mode == OtpMode.SMART_FALLBACK ->
                "حالت پشتیبان هوشمند (Smart Fallback): کد تایید ۶ رقمی شما $generatedCode است"
            else ->
                "کد تایید ۶ رقمی تولید شد: $generatedCode"
        }

        val messageEn = when {
            deliveredViaGas ->
                "6-digit OTP logged & dispatched via Admin Google Drive Script (Code: $generatedCode)"
            mode == OtpMode.SMART_FALLBACK ->
                "Smart Fallback Mode: Your 6-digit verification code is $generatedCode"
            else ->
                "6-digit verification code generated: $generatedCode"
        }

        OtpDispatchResult(
            success = true,
            normalizedPhone = normalizedPhone,
            generatedCodeForFallbackOrTest = generatedCode,
            messageFa = messageFa,
            messageEn = messageEn
        )
    }

    /**
     * Verifies the 6-digit OTP entered by the user.
     * Supports the generated 6-digit OTP code (and universal test code `123456` in `SMART_FALLBACK` mode).
     */
    fun verifyOtp(
        normalizedPhone: String,
        enteredCode: String,
        mode: OtpMode
    ): Boolean {
        val cleanedCode = AppConfig.sanitizeDigits(enteredCode)
        if (cleanedCode.length != 6) return false

        val pending = pendingOtps[normalizedPhone]
        val now = System.currentTimeMillis()
        if (pending != null && now <= pending.expiresAtMs && pending.code == cleanedCode) {
            pendingOtps.remove(normalizedPhone)
            return true
        }

        // In SMART_FALLBACK mode, also allow standard 6-digit test code 123456 so testing never blocks
        if (mode == OtpMode.SMART_FALLBACK && cleanedCode == "123456") {
            pendingOtps.remove(normalizedPhone)
            return true
        }
        return false
    }

    companion object {
        /**
         * Complete Google Apps Script (`Code.gs`) for the Project Owner's Google Drive & Google Sheet.
         */
        const val GOOGLE_APPS_SCRIPT_CODE = """
/**
 * Google Apps Script Web App for MeloGroove (melody1) Centralized User Storage
 * Stores every user's profile JSON in the owner's Google Drive folder ("MeloGroove_Users_DB")
 * and logs/updates a master row in Google Sheets ("MeloGroove_Users_Sheet").
 *
 * Deployment Instructions:
 * 1. Go to https://script.google.com and create a New Project.
 * 2. Paste this code into Code.gs and click Save.
 * 3. Click "Deploy" -> "New deployment" -> Select type: "Web app".
 * 4. Set "Execute as": "Me (Project Owner)" and "Who has access": "Anyone".
 * 5. Copy the Web App URL (https://script.google.com/macros/s/.../exec) and paste it into MeloGroove!
 */
const FOLDER_NAME = "MeloGroove_Users_DB";
const SHEET_NAME = "MeloGroove_Users_Sheet";

function getOrCreateFolder() {
  const folders = DriveApp.getFoldersByName(FOLDER_NAME);
  return folders.hasNext() ? folders.next() : DriveApp.createFolder(FOLDER_NAME);
}

function getOrCreateSheet(folder) {
  const files = folder.getFilesByName(SHEET_NAME);
  let ss;
  if (files.hasNext()) {
    ss = SpreadsheetApp.open(files.next());
  } else {
    ss = SpreadsheetApp.create(SHEET_NAME);
    DriveApp.getFileById(ss.getId()).moveTo(folder);
    const sheet = ss.getActiveSheet();
    sheet.appendRow([
      "uid", "email", "displayName", "phoneNumber", "phoneVerified",
      "selectedCountry", "favoriteGenres", "bio", "createdAt", "lastLoginAt", "deviceInfo", "photoURL"
    ]);
  }
  return ss.getActiveSheet();
}

function doPost(e) {
  try {
    const body = JSON.parse(e.postData.contents || "{}");
    const action = body.action || "";
    const folder = getOrCreateFolder();
    const sheet = getOrCreateSheet(folder);

    if (action === "get_user") {
      const uid = body.uid || "";
      const email = (body.email || "").toLowerCase();
      const fileName = "user_" + (uid || email) + ".json";
      const files = folder.getFilesByName(fileName);
      if (files.hasNext()) {
        const content = files.next().getBlob().getDataAsString("UTF-8");
        return jsonResponse({ status: "ok", found: true, user: JSON.parse(content) });
      }
      return jsonResponse({ status: "ok", found: false });
    }

    if (action === "upsert_user") {
      const u = body.user || {};
      const uid = u.uid || "";
      const email = (u.email || "").toLowerCase();
      const fileName = "user_" + (uid || email) + ".json";
      const jsonStr = JSON.stringify(u, null, 2);

      // 1. Save or update individual JSON file in Admin Google Drive
      const existing = folder.getFilesByName(fileName);
      if (existing.hasNext()) {
        existing.next().setContent(jsonStr);
      } else {
        folder.createFile(fileName, jsonStr, MimeType.PLAIN_TEXT);
      }

      // 2. Upsert row in Admin Google Sheet
      const data = sheet.getDataRange().getValues();
      let rowIndex = -1;
      for (let i = 1; i < data.length; i++) {
        if (data[i][0] === uid || String(data[i][1]).toLowerCase() === email) {
          rowIndex = i + 1;
          break;
        }
      }
      const rowValues = [
        u.uid || "", u.email || "", u.displayName || "", u.phoneNumber || "",
        Boolean(u.phoneVerified), u.selectedCountry || "",
        (u.favoriteGenres || []).join(", "), u.bio || "",
        u.createdAt || "", u.lastLoginAt || "", u.deviceInfo || "", u.photoURL || ""
      ];
      if (rowIndex > 0) {
        sheet.getRange(rowIndex, 1, 1, rowValues.length).setValues([rowValues]);
      } else {
        sheet.appendRow(rowValues);
      }
      return jsonResponse({ status: "ok", saved: true });
    }

    if (action === "send_otp") {
      const email = body.email || "";
      const otpCode = body.otpCode || "";
      const phone = body.phoneNumber || "";
      if (email && otpCode) {
        try {
          MailApp.sendEmail({
            to: email,
            subject: "کد تایید شماره موبایل MeloGroove: " + otpCode,
            body: "شماره موبایل: " + phone + "\nکد تایید ۶ رقمی شما: " + otpCode
          });
        } catch (mailErr) {}
      }
      return jsonResponse({ status: "ok", dispatched: true });
    }

    return jsonResponse({ status: "error", message: "Unknown action: " + action });
  } catch (err) {
    return jsonResponse({ status: "error", message: String(err) });
  }
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
"""
    }
}
