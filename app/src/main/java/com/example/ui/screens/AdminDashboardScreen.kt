package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfig
import com.example.data.model.UserLoginLog
import com.example.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class AdminLogFilter(val label: String) {
    ALL("همه نشست‌ها"),
    ACTIVE("فعال (Active)"),
    LOGGED_OUT("خارج شده"),
    TODAY("ورودهای امروز")
}

@Composable
fun AdminDashboardScreen(
    logs: List<UserLoginLog>,
    adminEndpointUrl: String,
    isSyncing: Boolean,
    syncStatusMessage: String?,
    onRefreshLogs: () -> Unit,
    onSaveEndpointUrl: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AdminLogFilter.ALL) }
    var showEndpointConfig by remember { mutableStateOf(false) }
    var endpointDraft by remember(adminEndpointUrl) { mutableStateOf(adminEndpointUrl) }

    val todayDate = remember { LocalDate.now(ZoneId.systemDefault()) }

    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                AdminLogFilter.ALL -> true
                AdminLogFilter.ACTIVE -> log.status.equals("Active", ignoreCase = true)
                AdminLogFilter.LOGGED_OUT -> log.status.equals("LoggedOut", ignoreCase = true)
                AdminLogFilter.TODAY -> {
                    runCatching {
                        val instant = Instant.parse(log.loginTimestamp)
                        val logDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                        logDate == todayDate
                    }.getOrDefault(true)
                }
            }

            val q = searchQuery.trim()
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                log.phoneNumber.contains(q, ignoreCase = true) ||
                    log.accountUsername.contains(q, ignoreCase = true) ||
                    log.userId.contains(q, ignoreCase = true) ||
                    log.ipAddress.contains(q, ignoreCase = true) ||
                    log.deviceInfo.model.contains(q, ignoreCase = true) ||
                    log.deviceInfo.brand.contains(q, ignoreCase = true) ||
                    log.deviceInfo.deviceId.contains(q, ignoreCase = true) ||
                    log.deviceInfo.networkType.contains(q, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }
    }

    val activeCount = remember(logs) { logs.count { it.status.equals("Active", ignoreCase = true) } }
    val uniqueDevicesCount = remember(logs) { logs.map { it.deviceInfo.deviceId }.distinct().size }
    val pendingQueueCount = remember(logs) { logs.count { !it.isSyncedToCloud } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UnifiedStyles.screenBackgroundBrush())
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassyIconOrbButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBackClick,
                        testTag = "admin_back_button"
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "پنل مدیریت و تله‌متری کاربران",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "User & Device Telemetry • v${AppConfig.APP_VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTealGlow
                        )
                    }

                    GlassyIconOrbButton(
                        icon = Icons.Default.Sync,
                        contentDescription = "Sync Telemetry Queue",
                        onClick = onRefreshLogs,
                        isSelected = isSyncing,
                        testTag = "admin_refresh_logs_button"
                    )
                }
            }

            // Summary Telemetry Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        modifier = Modifier.weight(1f),
                        title = "کل نشست‌ها",
                        value = logs.size.toString(),
                        icon = Icons.Default.Assessment,
                        accentColor = GlassTealGlow
                    )
                    TelemetryStatCard(
                        modifier = Modifier.weight(1f),
                        title = "نشست فعال",
                        value = activeCount.toString(),
                        icon = Icons.Default.VerifiedUser,
                        accentColor = Color(0xFF4ADE80)
                    )
                    TelemetryStatCard(
                        modifier = Modifier.weight(1f),
                        title = "دستگاه یکتا",
                        value = uniqueDevicesCount.toString(),
                        icon = Icons.Default.Smartphone,
                        accentColor = AccentBlue
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Sync Queue & Export Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .glassyCard(cornerRadius = 20.dp, isHighlighted = pendingQueueCount == 0)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .glassyIconOrb(isSelected = true),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (pendingQueueCount == 0) {
                                            "صف همگام‌سازی پس‌زمینه کاملاً بروز است"
                                        } else {
                                            "$pendingQueueCount رکورد در صف ارسال خودکار پس‌زمینه"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = syncStatusMessage ?: "ثبت آنی IP، مدل دستگاه، نسخه اندروید و وضعیت نشست",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        val jsonPayload = UserLoginLog.toJsonList(filteredLogs)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Melody1_Telemetry_Logs", jsonPayload))
                                        Toast.makeText(
                                            context,
                                            "خروجی JSON لاگ‌های تله‌متری کپی شد (${filteredLogs.size} مورد)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GlassSurfaceSubtle)
                                        .testTag("admin_export_json_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy JSON Logs",
                                        tint = GlassTealGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showEndpointConfig = !showEndpointConfig },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GlassSurfaceSubtle)
                                        .testTag("admin_toggle_endpoint_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsEthernet,
                                        contentDescription = "Configure Telemetry Webhook",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showEndpointConfig) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = GlassBorderDim)
                                Text(
                                    text = "آدرس سرور دریافت تله‌متری پس‌زمینه (فقط قابل مشاهده توسط ادمین):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTealGlow
                                )
                                OutlinedTextField(
                                    value = endpointDraft,
                                    onValueChange = { endpointDraft = it },
                                    placeholder = {
                                        Text(
                                            "https://...",
                                            color = TextSecondary.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_endpoint_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GlassTealGlow,
                                        unfocusedBorderColor = GlassBorderDim,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    GlassyActionButton(
                                        text = "ذخیره و همگام‌سازی صف",
                                        icon = Icons.Default.Check,
                                        onClick = {
                                            onSaveEndpointUrl(endpointDraft.trim())
                                            showEndpointConfig = false
                                        },
                                        height = 42.dp,
                                        testTag = "admin_save_endpoint_button"
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Search Input & Filter Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = GlassTealGlow
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                text = "جستجو با شماره موبایل، ایمیل، IP، مدل گوشی یا شناسه...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_search_input"),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GlassCapsuleMid,
                            unfocusedContainerColor = GlassCapsuleTop.copy(alpha = 0.7f),
                            focusedBorderColor = GlassTealGlow,
                            unfocusedBorderColor = GlassBorderLight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminLogFilter.entries.forEach { filter ->
                            val isSelected = selectedFilter == filter
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
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("admin_filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Empty State or Telemetry Log List
            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp)
                            .glassyCard(cornerRadius = 24.dp)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageSearch,
                                contentDescription = null,
                                tint = GlassTealGlow,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "هیچ لاگ نشستی مطابق فیلتر فعلی یافت نشد",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تمامی ورودهای کاربران، تایید موبایل و ورودهای آزمایشی به صورت خودکار در این بخش ثبت می‌شوند.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.logId }) { log ->
                    UserTelemetryLogCard(
                        log = log,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .glassyCard(cornerRadius = 20.dp)
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UserTelemetryLogCard(
    log: UserLoginLog,
    modifier: Modifier = Modifier
) {
    val isActive = log.status.equals("Active", ignoreCase = true)
    val statusColor = if (isActive) Color(0xFF4ADE80) else Color(0xFFF87171)

    Box(
        modifier = modifier
            .glassyCard(
                cornerRadius = 22.dp,
                isHighlighted = isActive
            )
            .padding(16.dp)
            .testTag("telemetry_log_card_${log.logId}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Account & Status Badge
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
                            .glassyIconOrb(isSelected = isActive),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.userId.startsWith("trial_")) Icons.Default.Timer else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = log.accountUsername,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "UID: ${log.userId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active / LoggedOut Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.16f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = if (isActive) "Active" else "LoggedOut",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = GlassBorderDim)

            // Telemetry Grid
            TelemetryKeyValueRow(
                icon = Icons.Default.PhoneIphone,
                label = "شماره موبایل:",
                value = log.phoneNumber
            )
            TelemetryKeyValueRow(
                icon = Icons.Default.Public,
                label = "آدرس IP:",
                value = log.ipAddress,
                isMonospace = true
            )
            TelemetryKeyValueRow(
                icon = Icons.Default.Devices,
                label = "مدل و برند دستگاه:",
                value = "${log.deviceInfo.brand} • ${log.deviceInfo.model}"
            )
            TelemetryKeyValueRow(
                icon = Icons.Default.Android,
                label = "سیستم‌عامل و نسخه اپ:",
                value = "${log.deviceInfo.osVersion} | App ${log.deviceInfo.appVersion}"
            )
            TelemetryKeyValueRow(
                icon = Icons.Default.WifiTethering,
                label = "نوع شبکه و اپراتور:",
                value = log.deviceInfo.networkType
            )
            TelemetryKeyValueRow(
                icon = Icons.Default.Fingerprint,
                label = "شناسه سخت‌افزاری (Device ID):",
                value = log.deviceInfo.deviceId,
                isMonospace = true
            )
            TelemetryKeyValueRow(
                icon = Icons.Default.Schedule,
                label = "زمان دقیق ورود:",
                value = "${log.localTimeDisplay} (${log.loginTimestamp})",
                isMonospace = true
            )
        }
    }
}

@Composable
private fun TelemetryKeyValueRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlassTealGlow,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
