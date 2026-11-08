package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppConfig
import com.example.data.model.CountriesData
import com.example.data.model.OtpMode
import com.example.data.model.UserProfile
import com.example.data.remote.DriveStorageService
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    googleDriveWebhookUrl: String,
    otpMode: OtpMode,
    isSyncing: Boolean,
    syncStatusMessage: String?,
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

    var webhookUrlInput by rememberSaveable(googleDriveWebhookUrl) { mutableStateOf(googleDriveWebhookUrl) }
    var showAppsScriptCode by rememberSaveable { mutableStateOf(false) }
    var scriptCopied by remember { mutableStateOf(false) }
    var showRawJsonPreview by rememberSaveable { mutableStateOf(false) }

    val countryObj = remember(profile.selectedCountry) {
        CountriesData.getCountryByCode(profile.selectedCountry.ifBlank { "IR" })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Header with Back Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr(lang, "بازگشت", "Back"),
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(lang, "پروفایل کاربری و Google Drive", "User Profile & Google Drive"),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    color = Color(0x264DB6AC),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF4DB6AC), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF4DB6AC),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr(lang, "متصل به درایو", "Drive Active"),
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = OledSurface)
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
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF3B6E6C), Color(0xFF1B3534))
                                )
                            )
                            .border(2.dp, AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.photoURL.startsWith("http")) {
                            AsyncImage(
                                model = profile.photoURL,
                                contentDescription = profile.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initial = profile.displayName.trim().firstOrNull()?.uppercase()
                                ?: profile.email.firstOrNull()?.uppercase() ?: "U"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineLarge,
                                color = AccentGold,
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
                            tint = AccentGold,
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (profile.phoneVerified) Color(0xFF4CAF50) else AccentGold,
                                RoundedCornerShape(12.dp)
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
                                color = AccentGold
                            )
                        }
                    }
                }
            }
        }

        // 2. Editable Profile Form (displayName, bio, favoriteGenres, selectedCountry)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = OledSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = tr(lang, "ویرایش مشخصات پروفایل کاربری", "Edit User Profile Details"),
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(tr(lang, "نام نمایشی (displayName)", "Display Name")) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(OledSurfaceVariant)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .clickable(onClick = onChangeCountryClick)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = AccentGold,
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
                            color = AccentGold,
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
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentGold else BorderSubtle,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (isSelected && selectedGenres.size > 1) {
                                            selectedGenres.remove(genreKey)
                                        } else if (!isSelected) {
                                            selectedGenres.add(genreKey)
                                        }
                                    },
                                color = if (isSelected) Color(0x28FFB800) else OledSurfaceVariant
                            ) {
                                Text(
                                    text = genreLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) AccentGold else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "ذخیره و همگام‌سازی در Google Drive",
                                    "Save & Sync to Google Drive"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!syncStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0x224DB6AC),
                            shape = RoundedCornerShape(10.dp),
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

        // 3. Google Drive Webhook Endpoint & User JSON Schema Inspector (Module 3)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = OledSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF4DB6AC),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "تنظیمات اتصال به Google Drive مالک پروژه",
                                "Admin Google Drive Webhook Configuration"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = tr(
                            lang,
                            "آدرس Web App اسکریپت Google Apps Script اکانت خود را در کادر زیر قرار دهید تا اطلاعات تمامی کاربران به صورت خودکار در پوشه Google Drive و جدول Google Sheet شما ذخیره شود:",
                            "Enter your Google Apps Script Web App URL (GOOGLE_DRIVE_WEBHOOK_URL) to store all user JSON files and Google Sheet rows in your Google Drive:"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = webhookUrlInput,
                            onValueChange = { webhookUrlInput = it },
                            label = { Text("GOOGLE_DRIVE_WEBHOOK_URL (https://script.google.com/.../exec)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drive_webhook_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4DB6AC),
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveWebhookUrl(webhookUrlInput) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF26A69A),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = tr(lang, "ثبت URL و همگام‌سازی", "Save URL & Sync"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showAppsScriptCode = !showAppsScriptCode },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr(lang, "کد اسکریپت درایو", "Apps Script Code"),
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = showAppsScriptCode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "MeloGroove Google Apps Script",
                                            DriveStorageService.GOOGLE_APPS_SCRIPT_CODE.trim()
                                        )
                                    )
                                    scriptCopied = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGold,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (scriptCopied) {
                                        tr(lang, "کد اسکریپت کپی شد! در script.google.com جایگذاری کنید", "Copied to Clipboard!")
                                    } else {
                                        tr(lang, "کپی کامل کد Google Apps Script (Code.gs)", "Copy Full Google Apps Script (Code.gs)")
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle Raw User Data Schema JSON Preview
                    OutlinedButton(
                        onClick = { showRawJsonPreview = !showRawJsonPreview },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = tr(
                                lang,
                                "مشاهده ساختار JSON پرونده کاربر (User Data Schema)",
                                "View User Data JSON Schema"
                            ),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    AnimatedVisibility(visible = showRawJsonPreview) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Surface(
                                color = Color(0xFF0D0F12),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = profile.toPrettyJsonString(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF80CBC4),
                                    modifier = Modifier.padding(12.dp)
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
