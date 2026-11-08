package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfig
import com.example.data.model.DialCountry
import com.example.data.model.OtpMode
import com.example.data.model.UserProfile
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Module 2: Mandatory Phone Verification & Free OTP Screen (`PhoneVerificationScreen`)
 * Step 3 of the Strict Onboarding Guard:
 * User cannot proceed to Country Selection or Main Player without verifying their mobile number.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneVerificationScreen(
    userProfile: UserProfile?,
    otpMode: OtpMode,
    isBusy: Boolean,
    generatedFallbackOtp: String?,
    statusBannerMessage: String?,
    errorMessage: String?,
    onOtpModeChange: (OtpMode) -> Unit,
    onRequestOtp: (dialCode: String, rawPhone: String) -> Unit,
    onVerifyOtp: (normalizedPhone: String, code: String) -> Unit,
    onContinueWithoutSmsBackend: (dialCode: String, rawPhone: String) -> Unit = { _, _ -> },
    onSignOutAccount: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val dialCountries = AppConfig.dialCountries
    var selectedDialCountry by remember { mutableStateOf(dialCountries.first()) }
    var rawPhoneInput by rememberSaveable {
        mutableStateOf(userProfile?.phoneNumber?.removePrefix("+98").orEmpty())
    }
    var isOtpSentStep by rememberSaveable { mutableStateOf(false) }
    var normalizedTargetPhone by rememberSaveable { mutableStateOf("") }
    var countdownSeconds by rememberSaveable { mutableIntStateOf(0) }

    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // 60-second countdown timer when OTP step is active
    LaunchedEffect(isOtpSentStep, countdownSeconds) {
        if (isOtpSentStep && countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds -= 1
        }
    }

    // Focus first box when transitioning to OTP step
    LaunchedEffect(isOtpSentStep) {
        if (isOtpSentStep) {
            try {
                focusRequesters[0].requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF141820), OledBackground, OledBackground)
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Logged-in Google user badge + Switch Account button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = OledSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4DB6AC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = userProfile?.email?.ifBlank { "Google Account" } ?: "Google Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }

                TextButton(
                    onClick = onSignOutAccount,
                    modifier = Modifier.testTag("phone_screen_signout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(lang, "تغییر حساب", "Switch Account"),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Step Strict Onboarding Progress Indicator (Step 2 Active)
            OnboardingStepsBanner(currentStep = 2, lang = lang)

            Spacer(modifier = Modifier.height(18.dp))

            // Main Phone & OTP Card
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isOtpSentStep) Icons.Default.Sms else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(38.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = tr(
                            lang,
                            "مرحله ۲: ثبت و تایید شماره موبایل",
                            "Step 2: Mobile Phone Registration & Verification"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!isOtpSentStep) {
                            tr(
                                lang,
                                "برای تکمیل پرونده کاربری و ورود به مرحله انتخاب کشور، شماره موبایل خود را وارد کنید.",
                                "Enter your mobile number to complete your profile and proceed to Country Selection."
                            )
                        } else {
                            tr(
                                lang,
                                "وضعیت درگاه پیامکی برای شماره \u200E$normalizedTargetPhone را در زیر مشاهده کنید.",
                                "Review the SMS verification status for $normalizedTargetPhone below."
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Honest BLOCKED_NO_BACKEND Status Banner (Section 4.1)
                    Surface(
                        color = Color(0x26FFA000),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = tr(
                                    lang,
                                    "وضعیت درگاه پیامک: BLOCKED_NO_BACKEND",
                                    "SMS Gateway Status: BLOCKED_NO_BACKEND"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "برای حفظ امنیت، تولید کد OTP محلی یا ارسال کد پیامکی از طریق ایمیل غیرفعال شده است. در صورت عدم اتصال درگاه پیامکی سرور، می‌توانید شماره خود را به‌صورت «تاییدنشده با پیامک» ثبت کرده و به مرحله انتخاب کشور بروید.",
                                    "For security, local OTP generation and email-disguised SMS codes are disabled. When no server SMS gateway is configured, you may save your number as unverified and continue."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isOtpSentStep) {
                        // ================= PART 1: COUNTRY DIAL CODE + PHONE INPUT =================
                        Text(
                            text = tr(lang, "انتخاب پیش‌شماره کشور:", "Select Country Dial Code:"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dialCountries.forEach { dc ->
                                val isSelected = dc.countryCode == selectedDialCountry.countryCode
                                DialCountryChip(
                                    dialCountry = dc,
                                    isSelected = isSelected,
                                    onClick = { selectedDialCountry = dc }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Phone Number Input Field (Always LTR inside for numbers)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = rawPhoneInput,
                                onValueChange = { rawPhoneInput = it },
                                label = {
                                    Text(
                                        tr(
                                            lang,
                                            "شماره موبایل (مثال: 9123456789 یا 09123456789)",
                                            "Mobile Number (e.g. 9123456789)"
                                        )
                                    )
                                },
                                prefix = {
                                    Text(
                                        text = "${selectedDialCountry.flag} ${selectedDialCountry.dialCode} ",
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_number_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGold,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (AppConfig.isValidPhoneNumber(selectedDialCountry.dialCode, rawPhoneInput)) {
                                    normalizedTargetPhone = AppConfig.normalizePhoneNumber(
                                        selectedDialCountry.dialCode,
                                        rawPhoneInput
                                    )
                                    for (i in 0 until 6) otpDigits[i] = ""
                                    countdownSeconds = 60
                                    isOtpSentStep = true
                                }
                                onRequestOtp(selectedDialCountry.dialCode, rawPhoneInput)
                            },
                            enabled = !isBusy && rawPhoneInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("request_otp_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            )
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(Icons.Default.Key, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(lang, "بررسی درگاه پیامک (Request SMS OTP)", "Request SMS OTP"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                onContinueWithoutSmsBackend(selectedDialCountry.dialCode, rawPhoneInput)
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("continue_without_sms_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = tr(
                                    lang,
                                    "ادامه بدون تایید پیامکی (BLOCKED_NO_BACKEND)",
                                    "Continue Without SMS Verification (BLOCKED_NO_BACKEND)"
                                ),
                                color = Color(0xFF4DB6AC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // ================= PART 2: 6-BOX OTP INPUT + HONEST STATUS BANNER =================

                        AnimatedVisibility(visible = !statusBannerMessage.isNullOrBlank()) {
                            Surface(
                                color = Color(0x264DB6AC),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF4DB6AC), RoundedCornerShape(14.dp))
                                    .padding(bottom = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = statusBannerMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE0F2F1),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 6 Individual OTP Boxes with Auto-Focus Jump (Always LTR left-to-right order)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (index in 0 until 6) {
                                    OtpDigitBox(
                                        value = otpDigits[index],
                                        focusRequester = focusRequesters[index],
                                        tag = "otp_box_$index",
                                        onValueChange = { newText ->
                                            val clean = AppConfig.sanitizeDigits(newText)
                                                .filter { it.isDigit() }
                                            if (clean.length > 1) {
                                                // Pasted full or partial code
                                                for (k in clean.indices) {
                                                    if (index + k < 6) {
                                                        otpDigits[index + k] = clean[k].toString()
                                                    }
                                                }
                                                val targetIdx = (index + clean.length).coerceAtMost(5)
                                                try {
                                                    focusRequesters[targetIdx].requestFocus()
                                                } catch (_: Exception) {
                                                }
                                            } else if (clean.length == 1) {
                                                otpDigits[index] = clean
                                                if (index < 5) {
                                                    try {
                                                        focusRequesters[index + 1].requestFocus()
                                                    } catch (_: Exception) {
                                                    }
                                                }
                                            } else {
                                                otpDigits[index] = ""
                                            }
                                        },
                                        onBackspaceEmpty = {
                                            if (otpDigits[index].isEmpty() && index > 0) {
                                                otpDigits[index - 1] = ""
                                                try {
                                                    focusRequesters[index - 1].requestFocus()
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 60-Second Countdown Timer + Resend Button + Edit Phone Number Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    isOtpSentStep = false
                                },
                                modifier = Modifier.testTag("edit_phone_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(lang, "ویرایش شماره موبایل", "Edit Phone Number"),
                                    color = AccentGold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            if (countdownSeconds > 0) {
                                val secFormatted = "%02d".format(java.util.Locale.US, countdownSeconds)
                                Text(
                                    text = tr(
                                        lang,
                                        "ارسال مجدد تا 00:$secFormatted",
                                        "Resend in 00:$secFormatted"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        for (i in 0 until 6) otpDigits[i] = ""
                                        countdownSeconds = 60
                                        onRequestOtp(selectedDialCountry.dialCode, rawPhoneInput)
                                    },
                                    modifier = Modifier.testTag("resend_otp_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color(0xFF4DB6AC),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr(lang, "ارسال مجدد کد", "Resend Code"),
                                        color = Color(0xFF4DB6AC),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val combinedOtp = otpDigits.joinToString("")

                        Button(
                            onClick = {
                                onVerifyOtp(normalizedTargetPhone, combinedOtp)
                            },
                            enabled = !isBusy && combinedOtp.length == 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("verify_otp_submit_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            )
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(
                                        lang,
                                        "تایید و ثبت در سیستم (Verify & Save)",
                                        "Verify & Save Profile"
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
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
                }
            }
        }
    }
}

@Composable
private fun DialCountryChip(
    dialCountry: DialCountry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AccentGold else BorderSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0x28FFB800) else OledSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = dialCountry.flag, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "\u200E${dialCountry.dialCode}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) AccentGold else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun OtpDigitBox(
    value: String,
    focusRequester: FocusRequester,
    tag: String,
    onValueChange: (String) -> Unit,
    onBackspaceEmpty: () -> Unit
) {
    val isFilled = value.isNotEmpty()
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFilled) Color(0x26FFB800) else OledSurfaceVariant)
            .border(
                width = if (isFilled) 2.dp else 1.dp,
                color = if (isFilled) AccentGold else BorderSubtle,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next
            ),
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(AccentGold),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag(tag)
                .onKeyEvent { event ->
                    if (event.key == Key.Backspace && value.isEmpty()) {
                        onBackspaceEmpty()
                        true
                    } else {
                        false
                    }
                }
        )
    }
}
