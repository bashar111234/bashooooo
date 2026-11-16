package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PortalConstants
import com.example.model.QuotaAlertLevel
import com.example.model.SpeedProfile
import com.example.model.ThemeMode
import com.example.model.WALLET_GUIDES
import com.example.model.WalletGuide
import com.example.ui.HotspotUiState
import com.example.ui.components.AlertMessageBanner
import com.example.ui.components.AppFooter
import com.example.ui.components.BannerTone
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassPanel
import com.example.ui.components.PanelHeader
import com.example.ui.components.SpeedometerGaugeSection
import com.example.ui.components.TopMetaClockBar
import com.example.ui.theme.AlqanasMonoFont
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LocalAlqanasPalette
import com.example.ui.theme.WarningAmber
import com.example.util.MikrotikParser
import kotlin.math.roundToInt

@Composable
fun StatusScreen(
    state: HotspotUiState,
    onSelectSpeed: (SpeedProfile) -> Unit,
    onToggleBlockUpdates: (Boolean) -> Unit,
    onApplySpeedChanges: () -> Unit,
    onRefreshStatus: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onNotificationClick: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenWalletVideo: (WalletGuide) -> Unit,
    onLogout: () -> Unit,
    onLogoutAndNewCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val analysis = state.quotaAnalysis

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isWide = maxWidth >= 780.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (isWide) 980.dp else 520.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TopMetaClockBar()

                BrandHeader(
                    subtitle = "لوحة متابعة الكرت · اتصال آمن",
                    themeMode = state.themeMode,
                    onSelectTheme = onSelectTheme,
                    onNotificationClick = onNotificationClick,
                    onSupportClick = onOpenSupport,
                    notificationContentDesc = "تجربة إشعار الرصيد"
                )

                if (analysis.alertLevel == QuotaAlertLevel.CRIT_15 && analysis.activePct != null) {
                    AlertMessageBanner(
                        message = "تحذير حرج (تبقى ${analysis.activePct.roundToInt()}%): قارب رصيدك أو وقتك على النفاد تماماً — ${analysis.summaryText}",
                        tone = BannerTone.CRITICAL,
                        modifier = Modifier.testTag("quota_critical_banner")
                    )
                } else if (analysis.alertLevel == QuotaAlertLevel.WARN_30 && analysis.activePct != null) {
                    AlertMessageBanner(
                        message = "تنبيه مبكر (تبقى ${analysis.activePct.roundToInt()}%): وصل استهلاكك إلى 70% أو أكثر — ${analysis.summaryText}",
                        tone = BannerTone.WARNING,
                        modifier = Modifier.testTag("quota_warning_banner")
                    )
                }

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EntertainmentPanel()
                            SpeedControlPanel(
                                state = state,
                                onSelectSpeed = onSelectSpeed,
                                onToggleBlockUpdates = onToggleBlockUpdates,
                                onApplySpeedChanges = onApplySpeedChanges
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SessionMetricsPanel(
                                state = state,
                                onRefreshStatus = onRefreshStatus,
                                onNotificationClick = onNotificationClick
                            )
                        }
                    }
                } else {
                    SessionMetricsPanel(
                        state = state,
                        onRefreshStatus = onRefreshStatus,
                        onNotificationClick = onNotificationClick
                    )
                    EntertainmentPanel()
                    SpeedControlPanel(
                        state = state,
                        onSelectSpeed = onSelectSpeed,
                        onToggleBlockUpdates = onToggleBlockUpdates,
                        onApplySpeedChanges = onApplySpeedChanges
                    )
                }

                BottomWalletsAndLogoutPanel(
                    isLoggingOut = state.isLoggingOut,
                    onOpenWalletVideo = onOpenWalletVideo,
                    onLogout = onLogout,
                    onLogoutAndNewCard = onLogoutAndNewCard
                )

                AppFooter()
            }
        }
    }
}

@Composable
private fun SessionMetricsPanel(
    state: HotspotUiState,
    onRefreshStatus: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val palette = LocalAlqanasPalette.current
    val session = state.session
    val isSyncingProvisional = session?.isProvisional == true
    val maskedUser = MikrotikParser.maskCardNumber(session?.username.orEmpty()).ifBlank { "—" }
    val profileText = when {
        isSyncingProvisional -> "تم تسجيل الدخول · جاري مزامنة عدادات الراوتر..."
        session?.activeProfile?.isNotBlank() == true -> "باقة: ${session.activeProfile} · جلسة فعّالة"
        else -> "جلسة اتصال فعّالة"
    }
    val analysis = state.quotaAnalysis

    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceSubtle)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = palette.textPrimary
                    )
                }
                Column {
                    Text(
                        text = maskedUser,
                        fontSize = 17.sp,
                        fontFamily = AlqanasMonoFont,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("status_masked_username")
                    )
                    Text(
                        text = profileText,
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LiveGreen)
                    )
                    Text(
                        text = "متصل الآن",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }

                Surface(
                    onClick = onRefreshStatus,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("status_refresh_button"),
                    shape = RoundedCornerShape(10.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isRefreshingStatus) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = palette.textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "تحديث الحالة",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                label = "الوقت المتبقي",
                value = if (isSyncingProvisional) "جاري المزامنة..." else (session?.sessionTimeLeft ?: "—"),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "الرصيد المتبقي",
                value = if (isSyncingProvisional) "جاري المزامنة..." else (session?.remainBytes ?: "—"),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                label = "إجمالي التنزيل",
                value = if (isSyncingProvisional) "—" else (session?.bytesIn ?: "—"),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "إجمالي الرفع",
                value = if (isSyncingProvisional) "—" else (session?.bytesOut ?: "—"),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Uptime Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surfaceSubtle)
                .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مدة الاتصال الحالية",
                fontSize = 12.5.sp,
                color = palette.textSecondary
            )
            Text(
                text = if (isSyncingProvisional) "جاري المزامنة..." else (session?.uptime ?: "—"),
                fontFamily = AlqanasMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = palette.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quota Threshold Engine Monitor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceSubtle.copy(alpha = 0.7f))
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.hasNotificationPermission) {
                        "تنبيهات 30% و 15% مفعلة ✓"
                    } else {
                        "الإشعارات غير مفعلة"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("notif_action_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = palette.accentInk
                    )
                ) {
                    Text(
                        text = if (state.hasNotificationPermission) "تجربة الإشعار" else "تفعيل الإشعارات",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            QuotaProgressBarRow(
                label = "نسبة الرصيد المتبقي",
                pct = analysis.bytesPct,
                isSyncing = isSyncingProvisional
            )
            Spacer(modifier = Modifier.height(10.dp))
            QuotaProgressBarRow(
                label = "نسبة الوقت المتبقي",
                pct = analysis.timePct,
                isSyncing = isSyncingProvisional
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("حد 30%:", fontSize = 11.sp, color = palette.textSecondary)
                    Text(
                        text = if (analysis.alert30Sent) "تم الإشعار (≤ 30%) ✓" else "في المراقبة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (analysis.alert30Sent) WarningAmber else palette.textPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("حد 15%:", fontSize = 11.sp, color = palette.textSecondary)
                    Text(
                        text = if (analysis.alert15Sent) "تحذير حرج (≤ 15%) ✓" else "في المراقبة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (analysis.alert15Sent) DangerRed else palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalAlqanasPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceSubtle)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.5.sp,
            fontFamily = AlqanasMonoFont,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary
        )
    }
}

@Composable
private fun QuotaProgressBarRow(
    label: String,
    pct: Double?,
    isSyncing: Boolean = false
) {
    val palette = LocalAlqanasPalette.current
    val rounded = pct?.roundToInt()?.coerceIn(0, 100)
    val targetFraction = when {
        isSyncing -> 0.15f
        rounded != null -> rounded / 100f
        else -> 1f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "quota_bar_fill"
    )
    val barColor = when {
        isSyncing -> palette.textSecondary.copy(alpha = 0.5f)
        rounded == null -> palette.accent
        rounded <= 15 -> DangerRed
        rounded <= 30 -> WarningAmber
        else -> palette.accent
    }
    val readoutText = when {
        isSyncing -> "جاري المزامنة..."
        rounded != null -> "$rounded%"
        else -> "غير محدود"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 11.5.sp, color = palette.textSecondary)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when {
                    isSyncing || rounded == null -> palette.surface
                    rounded <= 15 -> DangerRed.copy(alpha = 0.15f)
                    rounded <= 30 -> WarningAmber.copy(alpha = 0.18f)
                    else -> palette.surface
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isSyncing || rounded == null -> palette.border
                        rounded <= 15 -> DangerRed.copy(alpha = 0.5f)
                        rounded <= 30 -> WarningAmber.copy(alpha = 0.55f)
                        else -> palette.border
                    }
                )
            ) {
                Text(
                    text = readoutText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontFamily = AlqanasMonoFont,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSyncing -> palette.textSecondary
                        rounded != null && rounded <= 15 -> DangerRed
                        rounded != null && rounded <= 30 -> WarningAmber
                        else -> palette.textPrimary
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(999.dp))
        ) {
            if (animatedFraction > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
private fun EntertainmentPanel() {
    val context = LocalContext.current

    GlassPanel {
        PanelHeader(
            title = "الترفيه والبث",
            subtitle = "مشاهدة الأفلام والمسلسلات والمباريات عبر الشبكة المحلية"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EntertainmentButton(
                title = "الاستراحة (أفلام ومسلسلات)",
                subtitle = "مكتبة الأفلام والمسلسلات",
                icon = Icons.Rounded.Movie,
                testTag = "entertainment_movies_button",
                modifier = Modifier.weight(1f),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PortalConstants.ENTERTAINMENT_MOVIES_URL))
                        )
                    }
                }
            )
            EntertainmentButton(
                title = "البث المباشر (مباريات)",
                subtitle = "قنوات البث الرياضي المباشر",
                icon = Icons.Rounded.LiveTv,
                testTag = "entertainment_tv_button",
                modifier = Modifier.weight(1f),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PortalConstants.ENTERTAINMENT_TV_URL))
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun EntertainmentButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAlqanasPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(76.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = palette.accentInk,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    maxLines = 2,
                    lineHeight = 15.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SpeedControlPanel(
    state: HotspotUiState,
    onSelectSpeed: (SpeedProfile) -> Unit,
    onToggleBlockUpdates: (Boolean) -> Unit,
    onApplySpeedChanges: () -> Unit
) {
    val palette = LocalAlqanasPalette.current

    GlassPanel {
        PanelHeader(
            title = "التحكم بالسرعة والتحديثات",
            subtitle = "بدّل السرعة فوراً دون الحاجة لإعادة إدخال الكرت"
        )

        SpeedometerGaugeSection(
            selectedSpeed = state.selectedSpeed ?: SpeedProfile.DEFAULT,
            blockUpdates = state.blockUpdates,
            onSelectSpeed = onSelectSpeed,
            onToggleBlockUpdates = onToggleBlockUpdates,
            captionTitle = "السرعة الحالية"
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onApplySpeedChanges,
            enabled = !state.isApplyingSpeed,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("apply_speed_changes_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = palette.accentInk
            )
        ) {
            if (state.isApplyingSpeed) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = palette.accentInk
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جاري تطبيق الإعدادات...",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تطبيق التغييرات",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp
                )
            }
        }
    }
}

@Composable
private fun BottomWalletsAndLogoutPanel(
    isLoggingOut: Boolean,
    onOpenWalletVideo: (WalletGuide) -> Unit,
    onLogout: () -> Unit,
    onLogoutAndNewCard: () -> Unit
) {
    val palette = LocalAlqanasPalette.current

    GlassPanel {
        PanelHeader(
            title = "خيارات تسجيل الخروج وشراء الكروت",
            subtitle = "شاهد شرح الشراء عبر المحافظ الإلكترونية أو أنهِ الجلسة الحالية"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "قسم المحافظ (فيديو شرح الشراء)",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WALLET_GUIDES.forEach { guide ->
                Surface(
                    onClick = { onOpenWalletVideo(guide) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag("status_wallet_video_${guide.name}"),
                    shape = RoundedCornerShape(10.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = guide.name,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "شرح الشراء",
                            fontSize = 10.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "إدارة الجلسة",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onLogout,
            enabled = !isLoggingOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_normal_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, palette.border),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = palette.surfaceSubtle,
                contentColor = palette.textPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onLogoutAndNewCard,
            enabled = !isLoggingOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_new_card_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = palette.accentInk
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("الخروج والدخول بكرت جديد", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
        }
    }
}
