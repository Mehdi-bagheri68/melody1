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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppConfig
import com.example.data.model.CountriesData
import com.example.data.model.OtpMode
import com.example.data.model.UserProfile
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GlassTealGlow
import com.example.ui.theme.GlassyActionButton
import com.example.ui.theme.GlassyIconOrbButton
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnifiedStyles
import com.example.ui.theme.glassyCard
import com.example.ui.theme.glassyIconOrb

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    googleDriveWebhookUrl: String,
    otpMode: OtpMode,
    isSyncing: Boolean,
    syncStatusMessage: String?,
    isAdminUnlocked: Boolean = false,
    onUnlockAdminPin: (String) -> Boolean = { false },
    onOpenAdminDashboard: () -> Unit = {},
    onBack: () -> Unit,
    onSaveProfile: (displayName: String, bio: String, favoriteGenres: List<String>) -> Unit,
    onChangePhoneClick: () -> Unit,
    onChangeCountryClick: () -> Unit,
    onSaveWebhookUrl: (String) -> Unit,
    onChangeOtpMode: (OtpMode) -> Unit,
    onSignOut: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = userProfile ?: UserProfile()

    var displayName by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    var bio by rememberSaveable(profile.bio) { mutableStateOf(profile.bio) }
    val selectedGenres = remember(profile.favoriteGenres) {
        mutableStateListOf<String>().apply { addAll(profile.favoriteGenres) }
    }

    var showAdminPinPrompt by rememberSaveable { mutableStateOf(false) }
    var adminPinInput by rememberSaveable { mutableStateOf("") }
    var adminPinError by rememberSaveable { mutableStateOf(false) }

    val countryObj = remember(profile.selectedCountry) {
        CountriesData.getCountryByCode(profile.selectedCountry.ifBlank { "IR" })
    }

    val hasAdminAccess = profile.isAdminUser || isAdminUnlocked

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UnifiedStyles.screenBackgroundBrush())
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Header with Back Button (Glassy style)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassyIconOrbButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(lang, "بازگشت", "Back"),
                        onClick = onBack,
                        testTag = "profile_back_button"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tr(lang, "پروفایل و حساب کاربری", "User Profile & Account"),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    color = Color(0x264DB6AC),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(1.dp, GlassTealGlow, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = GlassTealGlow,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr(lang, "حساب همگام‌شده", "Account Synced"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0F2F1),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1. User Avatar + Immutable Gmail + Verified Phone Summary Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 24.dp, isHighlighted = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Orb
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .glassyIconOrb(isSelected = true),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.photoURL.startsWith("http")) {
                            AsyncImage(
                                model = profile.photoURL,
                                contentDescription = profile.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            val initial = profile.displayName.trim().firstOrNull()?.uppercase()
                                ?: profile.email.firstOrNull()?.uppercase() ?: "U"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile.displayName.ifBlank { tr(lang, "کاربر MeloGroove", "MeloGroove User") },
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Immutable Gmail Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = GlassTealGlow,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = tr(lang, "ایمیل غیرقابل تغییر", "Immutable Gmail"),
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verified Phone Badge + Change Button
                    Surface(
                        color = if (profile.phoneVerified) Color(0x224CAF50) else Color(0x22FF9800),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (profile.phoneVerified) Color(0xFF4CAF50) else AccentGold,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(onClick = onChangePhoneClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (profile.phoneVerified) Color(0xFF81C784) else AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\u200E${profile.phoneNumber.ifBlank { "---" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (profile.phoneVerified) Color(0xFF81C784) else AccentGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(lang, "(ویرایش)", "(Edit)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTealGlow
                            )
                        }
                    }
                }
            }
        }

        // 2. Editable Profile Form (displayName, bio, favoriteGenres, selectedCountry)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = tr(lang, "ویرایش مشخصات پروفایل کاربری", "Edit User Profile Details"),
                        style = MaterialTheme.typography.titleMedium,
                        color = GlassTealGlow,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(tr(lang, "نام نمایشی (displayName)", "Display Name")) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = GlassTealGlow)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassTealGlow,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text(tr(lang, "توضیحات دلخواه کاربر (bio)", "User Bio / About")) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassTealGlow,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Country Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassyCard(cornerRadius = 16.dp)
                            .clickable(onClick = onChangeCountryClick)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = GlassTealGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "کشور انتخاب‌شده: ${countryObj.flag} ${countryObj.nameFa}",
                                    "Selected Country: ${countryObj.flag} ${countryObj.nameEn}"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = tr(lang, "تغییر کشور", "Change"),
                            style = MaterialTheme.typography.labelMedium,
                            color = GlassTealGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Favorite Genres Multi-Select Chips
                    Text(
                        text = tr(lang, "سبک‌های موسیقی مورد علاقه (favoriteGenres):", "Favorite Genres:"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppConfig.availableFavoriteGenres.forEach { (genreKey, genreLabel) ->
                            val isSelected = genreKey in selectedGenres
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelected) UnifiedStyles.glowingOrbBrush()
                                        else UnifiedStyles.glassCardBrush(isElevated = false)
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = UnifiedStyles.glassBorderBrush(isHighlighted = isSelected),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        if (isSelected && selectedGenres.size > 1) {
                                            selectedGenres.remove(genreKey)
                                        } else if (!isSelected) {
                                            selectedGenres.add(genreKey)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = genreLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            onSaveProfile(displayName.trim(), bio.trim(), selectedGenres.toList())
                        },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_to_drive_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E6B66),
                            contentColor = Color.White
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "ذخیره تغییرات پروفایل کاربری",
                                    "Save User Profile Changes"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!syncStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0x224DB6AC),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = syncStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0F2F1),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Admin User & Device Telemetry Dashboard Access Card
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 22.dp, isHighlighted = hasAdminAccess)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .glassyIconOrb(isSelected = hasAdminAccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tr(
                                        lang,
                                        "پنل مدیریت و ردیابی نشست‌ها (Admin Telemetry)",
                                        "Admin User & Device Telemetry Panel"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "مشاهده لاگ ورود کاربران، آدرس IP، مدل گوشی و وضعیت نشست",
                                        "View user login logs, IP address, device model & active status"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        GlassyActionButton(
                            text = if (hasAdminAccess) {
                                tr(lang, "ورود به پنل", "Open Panel")
                            } else {
                                tr(lang, "احراز ادمین", "Admin PIN")
                            },
                            onClick = {
                                if (hasAdminAccess) {
                                    onOpenAdminDashboard()
                                } else {
                                    showAdminPinPrompt = !showAdminPinPrompt
                                }
                            },
                            height = 40.dp,
                            testTag = "profile_admin_panel_button"
                        )
                    }

                    AnimatedVisibility(visible = showAdminPinPrompt && !hasAdminAccess) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = adminPinInput,
                                onValueChange = {
                                    adminPinInput = it
                                    adminPinError = false
                                },
                                label = {
                                    Text(tr(lang, "کد امنیتی مدیریت (پیش‌فرض: 2026)", "Admin Access PIN (Default: 2026)"))
                                },
                                singleLine = true,
                                isError = adminPinError,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_admin_pin_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GlassTealGlow,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            if (adminPinError) {
                                Text(
                                    text = tr(lang, "کد امنیتی وارد شده معتبر نیست.", "Invalid Admin PIN."),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF8A80)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                GlassyActionButton(
                                    text = tr(lang, "تایید و باز کردن پنل مدیریت", "Unlock & Open Admin Panel"),
                                    onClick = {
                                        if (onUnlockAdminPin(adminPinInput.trim())) {
                                            showAdminPinPrompt = false
                                            onOpenAdminDashboard()
                                        } else {
                                            adminPinError = true
                                        }
                                    },
                                    height = 42.dp,
                                    testTag = "profile_unlock_admin_submit_button"
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Sign Out Card
        item {
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedButton(
                onClick = {
                    performGoogleSignOut(context, scope) {
                        onSignOut()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
                    .testTag("profile_sign_out_button"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFEF5350)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(lang, "خروج از حساب کاربری گوگل", "Sign Out of Google Account"),
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
