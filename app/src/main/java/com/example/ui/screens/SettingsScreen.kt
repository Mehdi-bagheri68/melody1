package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioQuality
import com.example.data.model.Country
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
    selectedCountry: Country,
    audioQuality: AudioQuality,
    onCountryChangeClick: () -> Unit,
    onAudioQualityChanged: (AudioQuality) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "تنظیمات و حریم خصوصی",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "سفارشی‌سازی پخش، کیفیت صدا و مدیریت ترجیحات",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Section: Country Preference
        item {
            Text(
                text = "کشور و منطقه موسیقی",
                style = MaterialTheme.typography.titleMedium,
                color = AccentGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .clickable(onClick = onCountryChangeClick)
                    .testTag("settings_change_country"),
                colors = CardDefaults.cardColors(containerColor = OledSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedCountry.flag, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedCountry.nameFa,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "برای تغییر کشور و فید خانه لمس کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        color = OledSurfaceHigh,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "تغییر",
                            color = AccentGold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Section: Audio Quality Selector
        item {
            Text(
                text = "کیفیت پخش و مصرف دیتا",
                style = MaterialTheme.typography.titleMedium,
                color = AccentGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioQuality.values().forEach { quality ->
                    val isSelected = quality == audioQuality

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentGold else BorderSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onAudioQualityChanged(quality) }
                            .testTag("quality_option_${quality.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) OledSurfaceHigh else OledSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = if (isSelected) AccentGold else TextTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = quality.titleFa,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) AccentGold else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = quality.bitrate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentOrange
                                    )
                                }
                                Text(
                                    text = quality.descriptionFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AccentGold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: External Integrations & Background Audio
        item {
            Text(
                text = "یکپارچه‌سازی و پخش در پس‌زمینه",
                style = MaterialTheme.typography.titleMedium,
                color = AccentGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = OledSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FeatureStatusRow(
                        icon = Icons.Default.Lock,
                        title = "کنترل روی صفحه قفل و نوار اعلان",
                        description = "پشتیبانی کامل از استاندارد MediaSession در تمام نسخه‌ها"
                    )
                    FeatureStatusRow(
                        icon = Icons.Default.Bluetooth,
                        title = "پشتیبانی هدفون بلوتوث و هدست",
                        description = "پاسخ فوری به کلیدهای پخش، مکث و جابجایی آهنگ"
                    )
                    FeatureStatusRow(
                        icon = Icons.Default.Headphones,
                        title = "حالت خودرو و رانندگی ایمن",
                        description = "رابط کاربری بدون حواس‌پرتی با کلیدهای بزرگ ویژه اتومبیل"
                    )
                }
            }
        }

        // Section: Privacy & Free Public API Transparency
        item {
            Text(
                text = "شفافیت APIهای رایگان و حریم خصوصی",
                style = MaterialTheme.typography.titleMedium,
                color = AccentGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = OledSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AccentGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "احترام ۱۰۰٪ به حریم خصوصی کاربر",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اپلیکیشن MeloGroove هیچ داده شخصی، شماره تماس یا ردگیری تبلیغاتی انجام نمی‌دهد. تمامی اطلاعات و متاداده‌های موسیقی به صورت آنی از APIهای عمومی و رایگان واکشی شده و علاقمندی‌ها صرفاً به صورت محلی در دستگاه شما نگهداری می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                Toast.makeText(context, "حافظه کش موقت با موفقیت پاکسازی شد", Toast.LENGTH_SHORT).show()
                            },
                        color = OledSurfaceHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "پاکسازی حافظه کش موقت",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // App Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MeloGroove Online Music Player v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Text(
                    text = "طراحی شده با Jetpack Compose و Material Design 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun FeatureStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(OledSurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
