package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.auth.AuthState
import com.example.data.model.AppLanguage
import com.example.data.model.AudioQuality
import com.example.data.model.Country
import com.example.data.model.UserProfile
import com.example.ui.AppStrings.tr
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.GlassTealGlow
import com.example.ui.theme.GlassyActionButton
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UnifiedStyles
import com.example.ui.theme.glassyCard
import com.example.ui.theme.glassyIconOrb

@Composable
fun SettingsScreen(
    selectedCountry: Country,
    audioQuality: AudioQuality,
    appLanguage: AppLanguage,
    onCountryChangeClick: () -> Unit,
    onAudioQualityChanged: (AudioQuality) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    iranNoVpnMode: Boolean = true,
    onIranNoVpnModeChanged: (Boolean) -> Unit = {},
    onClearCacheClick: () -> Unit = {},
    authState: AuthState = AuthState.SignedOut,
    userProfile: UserProfile? = null,
    isTrialActive: Boolean = false,
    trialRemainingMs: Long = 0L,
    onOpenProfileClick: () -> Unit = {},
    onOpenAdminDashboardClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {}
) {
    var cacheCleared by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = OledSurfaceVariant,
            title = {
                Text(
                    text = tr(appLanguage, "حذف دائمی حساب کاربری", "Delete Account Permanently"),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = tr(
                        appLanguage,
                        "آیا از حذف دائمی حساب کاربری و تمام اطلاعات مرتبط مطمئن هستید؟ این عمل قابل بازگشت نیست.",
                        "Are you sure you want to permanently delete your account and associated data? This action cannot be undone."
                    ),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccountClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_account_btn")
                ) {
                    Text(tr(appLanguage, "حذف دائمی", "Delete Permanently"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(tr(appLanguage, "انصراف", "Cancel"), color = GlassTealGlow)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UnifiedStyles.screenBackgroundBrush())
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = tr(appLanguage, "تنظیمات و شخصی‌سازی", "Settings & Preferences"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = tr(
                        appLanguage,
                        "مدیریت پروفایل کاربری، زبان برنامه، کشور، کیفیت پخش و اتصال مستقیم",
                        "Manage user profile, language, country, streaming quality & direct mode"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Section 0: User Profile & Account
        item {
            SettingsGroupTitle(
                icon = Icons.Default.AccountCircle,
                title = tr(appLanguage, "پروفایل و حساب کاربری", "User Profile & Account")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 20.dp, isHighlighted = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeEmail = userProfile?.email?.takeIf { it.isNotBlank() }
                        ?: (authState as? AuthState.SignedIn)?.email
                    val activeName = userProfile?.displayName?.takeIf { it.isNotBlank() }
                        ?: (authState as? AuthState.SignedIn)?.displayName.orEmpty()
                    val activePhone = userProfile?.phoneNumber.orEmpty()

                    if (!activeEmail.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .glassyIconOrb(isSelected = true),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeName.take(2).uppercase().ifBlank { "MG" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeName.ifBlank { activeEmail },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlassTealGlow
                                )
                                if (activePhone.isNotBlank()) {
                                    Text(
                                        text = "\u200E$activePhone ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF81C784)
                                    )
                                }
                            }
                            Surface(
                                color = Color(0x3300E676),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tr(appLanguage, "تایید شده", "Verified"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onOpenProfileClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E6B66),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_profile_btn")
                        ) {
                            Text(
                                text = tr(
                                    appLanguage,
                                    "مشاهده و ویرایش پروفایل کاربری",
                                    "View & Edit User Profile"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSignOutClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_sign_out_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = tr(appLanguage, "خروج از حساب", "Sign Out"),
                                    tint = GlassTealGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr(appLanguage, "خروج از حساب", "Sign Out"))
                            }

                            OutlinedButton(
                                onClick = { showDeleteAccountDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_delete_account_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = tr(appLanguage, "حذف حساب", "Delete Account"),
                                    tint = AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr(appLanguage, "حذف حساب", "Delete Account"))
                            }
                        }
                    } else {
                        if (isTrialActive && trialRemainingMs > 0L) {
                            val remainingText = com.example.data.model.AppConfig.formatTrialRemaining(
                                trialRemainingMs,
                                appLanguage
                            )
                            Surface(
                                color = Color(0x33E53935),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE53935), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = tr(
                                            appLanguage,
                                            "حالت آزمایشی ۳ روزه فعال ($remainingText)",
                                            "3-Day Free Trial Active ($remainingText)"
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFF8A80),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = tr(
                                            appLanguage,
                                            "شما بدون ثبت اکانت وارد شده‌اید. هر زمان مایل بودید می‌توانید حساب گوگل خود را متصل کنید.",
                                            "You are using the 3-day trial without an account. Sign in with Google anytime to save your profile."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(appLanguage, "ورود با حساب گوگل", "Sign In with Google"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        appLanguage,
                                        "برای همگام‌سازی خودکار پروفایل و علاقه‌مندی‌ها وارد شوید",
                                        "Sign in to automatically sync your profile & preferences"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            GlassyActionButton(
                                text = tr(appLanguage, "ورود گوگل", "Sign In"),
                                onClick = onSignInClick,
                                height = 42.dp,
                                testTag = "settings_sign_in_btn"
                            )
                        }
                    }
                }
            }
        }

        // Section 1: App Language (فارسی / English with automatic RTL/LTR layout switch)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle(
                icon = Icons.Default.Language,
                title = tr(appLanguage, "زبان برنامه (App Language)", "App Language (زبان برنامه)")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 20.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    AppLanguage.entries.forEach { langOption ->
                        val isSelected = langOption == appLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onLanguageChanged(langOption) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("lang_${langOption.code}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onLanguageChanged(langOption) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GlassTealGlow,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = langOption.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) GlassTealGlow else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (langOption == AppLanguage.FA)
                                        tr(appLanguage, "چیدمان راست‌به‌چپ (RTL)", "Right-to-Left (RTL) Layout")
                                    else
                                        tr(appLanguage, "چیدمان چپ‌به‌راست (LTR)", "Left-to-Right (LTR) Layout"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                            Surface(
                                color = if (isSelected) Color(0xFF2E6B66) else OledSurfaceHigh,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (langOption == AppLanguage.FA) "فارسی 🇮🇷" else "EN 🌐",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Country & Region
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle(
                icon = Icons.Default.Public,
                title = tr(appLanguage, "منطقه و کشور موسیقی", "Music Country & Region")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 20.dp)
                    .clickable(onClick = onCountryChangeClick)
                    .testTag("settings_country_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedCountry.flag, fontSize = 34.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(
                                appLanguage,
                                "کشور فعلی: ${selectedCountry.nameFa}",
                                "Current Country: ${selectedCountry.nameEn}"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(appLanguage, "برای تغییر کشور و لیست ترندها ضربه بزنید", "Tap to change country & trending chart"),
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTealGlow
                        )
                    }
                }
            }
        }

        // Section 3: Audio Quality
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle(
                icon = Icons.Default.HighQuality,
                title = tr(appLanguage, "کیفیت پخش آنلاین و دانلود", "Streaming & Audio Quality")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 20.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    AudioQuality.entries.forEach { quality ->
                        val isSelected = quality == audioQuality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAudioQualityChanged(quality) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("quality_${quality.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onAudioQualityChanged(quality) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GlassTealGlow,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quality.getLabel(appLanguage),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) GlassTealGlow else TextPrimary
                                )
                                Text(
                                    text = tr(
                                        appLanguage,
                                        "نرخ بیت: ${quality.bitrate}",
                                        "Bitrate: ${quality.bitrate}"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Playback & Direct No-VPN Connection Settings
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle(
                icon = Icons.Default.Equalizer,
                title = tr(appLanguage, "تنظیمات اتصال بدون فیلترشکن", "Direct No-VPN & Playback")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = tr(appLanguage, "پخش مستقیم بدون فیلترشکن", "Direct No-VPN Streaming"),
                                    tint = GlassTealGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr(appLanguage, "پخش مستقیم بدون فیلترشکن (ایران)", "Direct No-VPN Streaming (Iran)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = tr(
                                    appLanguage,
                                    "استفاده از سرورهای مستقیم داخلی و کاورهای آفلاین بدون نیاز به VPN",
                                    "Uses direct unfiltered servers and bundled covers without VPN"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = iranNoVpnMode,
                            onCheckedChange = onIranNoVpnModeChanged,
                            modifier = Modifier.testTag("switch_iran_no_vpn"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2E6B66)
                            )
                        )
                    }
                }
            }
        }

        // Section 5: Storage & Cache
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle(
                icon = Icons.Default.Storage,
                title = tr(appLanguage, "حافظه و کش", "Storage & Cache")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 20.dp)
                    .clickable {
                        onClearCacheClick()
                        cacheCleared = true
                    }
                    .testTag("clear_cache_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (cacheCleared)
                                tr(appLanguage, "کش موقت پاکسازی شد!", "Temporary cache cleared!")
                            else
                                tr(appLanguage, "پاکسازی کش موقت جستجو و استریم", "Clear Search & Stream Cache"),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (cacheCleared) GlassTealGlow else TextPrimary
                        )
                        Text(
                            text = tr(
                                appLanguage,
                                "پلی‌لیست‌ها و علاقه‌مندی‌های شما حذف نمی‌شوند",
                                "Your playlists and favorite tracks will be preserved"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (cacheCleared) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = tr(appLanguage, "پاکسازی انجام شد", "Cache Cleared"),
                            tint = GlassTealGlow
                        )
                    }
                }
            }
        }

        // Section 6: Admin User & Device Telemetry Panel Shortcut
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsGroupTitle(
                icon = Icons.Default.AdminPanelSettings,
                title = tr(appLanguage, "مدیریت و تله‌متری نشست‌ها", "Session & Device Telemetry")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassyCard(cornerRadius = 20.dp)
                    .clickable(onClick = onOpenAdminDashboardClick)
                    .testTag("settings_admin_panel_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .glassyIconOrb(isSelected = true),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(appLanguage, "پنل مدیریت و ردیابی نشست‌های کاربران", "Admin User & Device Telemetry Dashboard"),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    appLanguage,
                                    "مشاهده آدرس IP، مدل دستگاه، شماره موبایل و تاریخچه ورود",
                                    "Inspect IP address, device model, phone number & login logs"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Clean App Version Footer
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = tr(appLanguage, "نسخه برنامه", "App Version"),
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MeloGroove v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupTitle(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = GlassTealGlow,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = GlassTealGlow,
            fontWeight = FontWeight.Bold
        )
    }
}
