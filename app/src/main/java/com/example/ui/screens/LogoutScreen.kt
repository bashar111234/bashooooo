package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ThemeMode
import com.example.ui.HotspotUiState
import com.example.ui.components.AppFooter
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassPanel
import com.example.ui.theme.AlqanasMonoFont
import com.example.ui.theme.LocalAlqanasPalette

@Composable
fun LogoutScreen(
    state: HotspotUiState,
    onSelectTheme: (ThemeMode) -> Unit,
    onOpenNotifGuide: () -> Unit,
    onOpenSupport: () -> Unit,
    onLoginAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAlqanasPalette.current
    val summary = state.logoutSummary
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BrandHeader(
                subtitle = "شبكة القناص · اتصال آمن",
                themeMode = state.themeMode,
                onSelectTheme = onSelectTheme,
                onNotificationClick = onOpenNotifGuide,
                onSupportClick = onOpenSupport
            )

            GlassPanel {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.surfaceSubtle)
                            .border(1.dp, palette.border, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "تم تسجيل الخروج بنجاح",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تم إنهاء جلستك وحفظ رصيدك المتبقي بأمان",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Username full-width tile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceSubtle)
                            .border(1.dp, palette.borderStrong, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "رقم الكرت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary
                        )
                        Text(
                            text = summary?.username ?: state.cardInput.ifBlank { "—" },
                            fontSize = 15.5.sp,
                            fontFamily = AlqanasMonoFont,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LogoutStatBox(
                            label = "مدة الاتصال",
                            value = summary?.uptime ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        LogoutStatBox(
                            label = "الوقت المتبقي",
                            value = summary?.timeLeft ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LogoutStatBox(
                            label = "إجمالي التنزيل",
                            value = summary?.bytesIn ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        LogoutStatBox(
                            label = "إجمالي الرفع",
                            value = summary?.bytesOut ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onLoginAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("relogin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.accentInk
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل الدخول مرة أخرى",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.textPrimary)) {
                                append("ملاحظة أمان: ")
                            }
                            append("احتفظ برقم الكرت الخاص بك دون مشاركته لضمان حماية رصيدك المتبقي.")
                        },
                        fontSize = 12.sp,
                        color = palette.textMuted,
                        lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AppFooter()
        }
    }
}

@Composable
private fun LogoutStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalAlqanasPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceSubtle)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
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
            fontSize = 14.sp,
            fontFamily = AlqanasMonoFont,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
    }
}
