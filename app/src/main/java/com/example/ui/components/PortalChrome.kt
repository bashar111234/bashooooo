package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ThemeMode
import com.example.ui.theme.AlqanasMonoFont
import com.example.ui.theme.LocalAlqanasPalette
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAlqanasPalette.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun PanelHeader(
    title: String,
    subtitle: String? = null,
    kicker: String? = null
) {
    val palette = LocalAlqanasPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
            if (!kicker.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(
                        text = kicker,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun TopMetaClockBar(modifier: Modifier = Modifier) {
    val palette = LocalAlqanasPalette.current
    var clockText by remember { mutableStateOf("00:00:00 م") }
    var dateText by remember { mutableStateOf("اليوم") }

    LaunchedEffect(Unit) {
        val days = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val months = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        while (true) {
            val cal = Calendar.getInstance()
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
            val s = cal.get(Calendar.SECOND).toString().padStart(2, '0')
            val ampm = if (h >= 12) "م" else "ص"
            val dh = (h % 12).let { if (it == 0) 12 else it }.toString().padStart(2, '0')
            clockText = "$dh:$m:$s $ampm"
            val dayName = days.getOrElse(cal.get(Calendar.DAY_OF_WEEK) - 1) { "اليوم" }
            val monthName = months.getOrElse(cal.get(Calendar.MONTH)) { "" }
            dateText = "$dayName، ${cal.get(Calendar.DAY_OF_MONTH)} $monthName"
            delay(1000L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface.copy(alpha = 0.55f))
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = clockText,
                fontSize = 12.sp,
                fontFamily = AlqanasMonoFont,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary
            )
            Text(
                text = "·",
                fontSize = 12.sp,
                color = palette.textMuted
            )
            Text(
                text = dateText,
                fontSize = 12.sp,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = stringResource(R.string.portal_domain),
            fontSize = 11.5.sp,
            fontFamily = AlqanasMonoFont,
            color = palette.textSecondary
        )
    }
}

@Composable
fun BrandHeader(
    subtitle: String,
    themeMode: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onNotificationClick: () -> Unit,
    onSupportClick: () -> Unit,
    notificationContentDesc: String = "إعدادات إشعارات الرصيد",
    modifier: Modifier = Modifier
) {
    val palette = LocalAlqanasPalette.current
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_art_1794843245343),
                contentDescription = stringResource(R.string.brand_title),
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.brand_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                val themeIcon = when (themeMode) {
                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                    ThemeMode.SYSTEM -> Icons.Rounded.Computer
                }
                Surface(
                    onClick = { themeMenuExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("theme_toggle_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "تغيير المظهر",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false },
                    modifier = Modifier.background(palette.surfaceSolid)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Rounded.Computer
                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                        }
                        val active = mode == themeMode
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    color = palette.textPrimary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (active) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = palette.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("theme_option_${mode.key}"),
                            onClick = {
                                onSelectTheme(mode)
                                themeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Surface(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("notification_header_button"),
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceSubtle,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = notificationContentDesc,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                onClick = onSupportClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("support_header_button"),
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceSubtle,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = "الدعم الفني والمبيعات",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

enum class BannerTone {
    WARNING,
    CRITICAL
}

@Composable
fun AlertMessageBanner(
    message: String,
    tone: BannerTone,
    modifier: Modifier = Modifier
) {
    val palette = LocalAlqanasPalette.current
    val bg: Color
    val border: Color
    val textColor: Color
    val icon = if (tone == BannerTone.CRITICAL) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber

    if (tone == BannerTone.CRITICAL) {
        bg = palette.critBg
        border = palette.critBorder
        textColor = palette.critText
    } else {
        bg = palette.warnBg
        border = palette.warnBorder
        textColor = palette.warnText
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Text(
                text = message,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun AppFooter(modifier: Modifier = Modifier) {
    val palette = LocalAlqanasPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.footer_rights),
            fontSize = 12.sp,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.footer_credit),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}
