package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.AppLanguage
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.IdGenerator
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Module 1: Official Google Login Screen (`LoginScreen`)
 * Step 1 of the Strict Onboarding Guard:
 * Authenticates via Google OAuth 2.0 / Jetpack CredentialManager (`GetSignInWithGoogleOption`)
 * + Firebase Google Auth, extracting `uid`, `email`, `displayName`, and `photoURL`,
 * and immediately triggers `syncUserFromDrive` (Step 2).
 */
@Composable
fun GoogleLoginScreen(
    isSyncing: Boolean,
    errorMessage: String?,
    trialStartedAtMs: Long = 0L,
    trialRemainingMs: Long = com.example.data.model.AppConfig.TRIAL_DURATION_MS,
    isTrialExpired: Boolean = false,
    onStartTrialClick: () -> Unit = {},
    onGoogleLoginSuccess: (uid: String, email: String, displayName: String, photoUrl: String) -> Unit,
    onGoogleLoginError: (String) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCredentialBusy by remember { mutableStateOf(false) }
    var showEmulatorFallbackForm by rememberSaveable { mutableStateOf(false) }
    var fallbackEmail by rememberSaveable { mutableStateOf("") }
    var fallbackName by rememberSaveable { mutableStateOf("") }

    // Attempt silent auto sign-in on startup if an authorized Google account exists
    LaunchedEffect(Unit) {
        attemptSilentGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onSuccess = { uid, email, name, photo ->
                onGoogleLoginSuccess(uid, email, name, photo)
            },
            onUnauthenticated = { /* Stay on LoginScreen */ }
        )
    }

    val isBusy = isSyncing || isCredentialBusy

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141820),
                        OledBackground,
                        OledBackground
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Language Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .clickable {
                            val next = if (lang == AppLanguage.FA) AppLanguage.EN else AppLanguage.FA
                            onToggleLanguage(next)
                        }
                        .testTag("login_language_toggle"),
                    color = OledSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.FA) "English" else "فارسی",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Glowing MeloGroove / melody1 Logo Orb
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2E6B66),
                                Color(0xFF1A3A38),
                                Color(0xFF102221)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(AccentGold, Color(0xFF4DB6AC), Color.White)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "MeloGroove Logo",
                    tint = AccentGold,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MeloGroove",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tr(
                    lang,
                    "پلتفرم استریم موسیقی آنلاین با همگام‌سازی ابری در Google Drive",
                    "Online Music Streaming with Centralized Google Drive Sync"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // 3-Step Strict Onboarding Progress Indicator
            OnboardingStepsBanner(currentStep = 1, lang = lang)

            Spacer(modifier = Modifier.height(24.dp))

            // Main Google Sign-In Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = OledSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tr(
                            lang,
                            "مرحله ۱: ورود رسمی با حساب گوگل",
                            "Step 1: Sign in with Google"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = tr(
                            lang,
                            "برای ورود به برنامه و بازیابی یا ایجاد پرونده کاربری شما در Google Drive، ابتدا با حساب جیمیل خود وارد شوید.",
                            "Sign in with your Google account to sync or create your profile in Google Drive."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Official "Continue with Google" Button
                    Button(
                        onClick = {
                            isCredentialBusy = true
                            launchInteractiveGoogleSignIn(
                                context = context,
                                credentialManager = credentialManager,
                                scope = scope,
                                onSuccess = { uid, email, name, photo ->
                                    isCredentialBusy = false
                                    onGoogleLoginSuccess(uid, email, name, photo)
                                },
                                onError = { msg ->
                                    isCredentialBusy = false
                                    showEmulatorFallbackForm = true
                                    onGoogleLoginError(msg)
                                },
                                onCancelled = {
                                    isCredentialBusy = false
                                    showEmulatorFallbackForm = true
                                }
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("continue_with_google_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F)
                        )
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF1F1F1F),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "در حال ارتباط با گوگل و همگام‌سازی درایو...",
                                    "Connecting to Google & syncing Drive..."
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            // Stylized 'G' Emblem
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "ورود با حساب گوگل (Continue with Google)",
                                    "Continue with Google"
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0x33EF5350),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF8A80),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================================
                    // RED 3-DAY FREE TRIAL BUTTON (ورود آزمایشی ۳ روزه بدون نیاز به اکانت)
                    // =========================================================================
                    val remainingLabel = com.example.data.model.AppConfig.formatTrialRemaining(
                        trialRemainingMs,
                        lang
                    )
                    val trialButtonTitle = when {
                        isTrialExpired -> tr(
                            lang,
                            "مهلت تست ۳ روزه پایان یافته است",
                            "3-Day Free Trial Expired"
                        )
                        trialStartedAtMs > 0L -> tr(
                            lang,
                            "ورود آزمایشی ۳ روزه ($remainingLabel)",
                            "Continue 3-Day Trial ($remainingLabel)"
                        )
                        else -> tr(
                            lang,
                            "ورود آزمایشی ۳ روزه (بدون نیاز به اکانت)",
                            "3-Day Free Trial (No Account Required)"
                        )
                    }

                    Button(
                        onClick = onStartTrialClick,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isTrialExpired) Color(0xFF8D3B3B) else Color(0xFFFF5252),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("start_trial_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTrialExpired) Color(0xFF5D1F1F) else Color(0xFFE53935),
                            contentColor = Color.White
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = trialButtonTitle,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isTrialExpired) {
                                    tr(
                                        lang,
                                        "برای ادامه استفاده لطفاً با جیمیل یا حساب گوگل وارد شوید",
                                        "Please sign in with your Gmail/Google account to continue"
                                    )
                                } else {
                                    tr(
                                        lang,
                                        "۳ روز استفاده کامل از اپلیکیشن بدون درج جیمیل یا شماره موبایل",
                                        "Full access for 3 days without entering Gmail or phone"
                                    )
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFFFCDD2)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle for Direct Gmail Entry when testing on Emulators or devices without Play Services
                    OutlinedButton(
                        onClick = { showEmulatorFallbackForm = !showEmulatorFallbackForm },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_gmail_fallback_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "ورود مستقیم با جیمیل (ویژه شبیه‌ساز / بدون تحریم)",
                                "Direct Gmail Sign-In (Emulator / No-VPN Mode)"
                            ),
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    AnimatedVisibility(visible = showEmulatorFallbackForm) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Text(
                                text = tr(
                                    lang,
                                    "در صورتی که روی شبیه‌ساز یا دستگاه بدون حساب Google Play هستید، آدرس جیمیل و نام خود را وارد کنید:",
                                    "If your emulator/device has no Google Play account configured, enter your Gmail & display name:"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = fallbackEmail,
                                onValueChange = { fallbackEmail = it },
                                label = {
                                    Text(tr(lang, "آدرس جیمیل (مثال: user@gmail.com)", "Gmail Address (user@gmail.com)"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = AccentGold)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fallback_gmail_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGold,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = fallbackName,
                                onValueChange = { fallbackName = it },
                                label = {
                                    Text(tr(lang, "نام نمایشی در پروفایل", "Display Name"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fallback_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGold,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val cleanEmail = fallbackEmail.trim().lowercase()
                                    if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                        onGoogleLoginError(
                                            tr(
                                                lang,
                                                "لطفاً یک آدرس جیمیل معتبر وارد کنید (مثال: user@gmail.com)",
                                                "Please enter a valid Gmail address (e.g. user@gmail.com)"
                                            )
                                        )
                                        return@Button
                                    }
                                    val derivedUid = "google_${IdGenerator.stable64BitId("google_uid", cleanEmail)}"
                                    val display = fallbackName.trim().ifBlank {
                                        cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                    }
                                    onGoogleLoginSuccess(derivedUid, cleanEmail, display, "")
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_gmail_fallback_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGold,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(
                                        lang,
                                        "ثبت جیمیل و ادامه به مرحله تایید موبایل",
                                        "Continue with Gmail to Phone Verification"
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStepsBanner(
    currentStep: Int,
    lang: AppLanguage
) {
    val steps = listOf(
        Triple(1, tr(lang, "۱. ورود گوگل", "1. Google Auth"), Icons.Default.VerifiedUser),
        Triple(2, tr(lang, "۲. تایید موبایل", "2. Phone OTP"), Icons.Default.PhoneAndroid),
        Triple(3, tr(lang, "۳. انتخاب کشور", "3. Country"), Icons.Default.Public)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OledSurfaceVariant)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEach { (stepNum, label, icon) ->
            val isActive = stepNum == currentStep
            val isDone = stepNum < currentStep
            val tint = when {
                isActive -> AccentGold
                isDone -> Color(0xFF4DB6AC)
                else -> TextSecondary
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

fun attemptSilentGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: (uid: String, email: String, displayName: String, photoUrl: String) -> Unit,
    onUnauthenticated: () -> Unit
) {
    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }
    if (currentUser != null && !currentUser.email.isNullOrBlank()) {
        onSuccess(
            currentUser.uid,
            currentUser.email.orEmpty(),
            currentUser.displayName.orEmpty(),
            currentUser.photoUrl?.toString().orEmpty()
        )
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdCred.idToken, null)
                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    onSuccess(
                        user.uid,
                        user.email ?: googleIdCred.id,
                        user.displayName ?: googleIdCred.displayName.orEmpty(),
                        user.photoUrl?.toString() ?: googleIdCred.profilePictureUri?.toString().orEmpty()
                    )
                } else {
                    onUnauthenticated()
                }
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun launchInteractiveGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: (uid: String, email: String, displayName: String, photoUrl: String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onError("default_web_client_id not found: ${e.localizedMessage}")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdCred.idToken, null)
                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    onSuccess(
                        user.uid,
                        user.email ?: googleIdCred.id,
                        user.displayName ?: googleIdCred.displayName.orEmpty(),
                        user.photoUrl?.toString() ?: googleIdCred.profilePictureUri?.toString().orEmpty()
                    )
                } else {
                    onError("Google Sign-In returned empty user")
                }
            } else {
                onError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Google Sign-In failed")
        }
    }
}

fun performGoogleSignOut(
    context: Context,
    scope: CoroutineScope,
    onComplete: () -> Unit
) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                try {
                    Firebase.auth.signOut()
                } catch (_: Exception) {
                }
                onComplete()
            }
        }
    }
}
