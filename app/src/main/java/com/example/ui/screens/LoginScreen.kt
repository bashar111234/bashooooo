package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PRICING_PLANS
import com.example.model.PortalConstants
import com.example.model.SpeedProfile
import com.example.model.ThemeMode
import com.example.model.WALLET_GUIDES
import com.example.model.WALLET_MERCHANTS
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
import com.example.ui.theme.LocalAlqanasPalette
import com.example.ui.theme.WhatsAppGreen

@Composable
fun LoginScreen(
    state: HotspotUiState,
    onCardInputChange: (String) -> Unit,
    onSelectSpeed: (SpeedProfile) -> Unit,
    onToggleBlockUpdates: (Boolean) -> Unit,
    onApplyPlan: (SpeedProfile, Boolean) -> Unit,
    onSubmitLogin: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onOpenNotifGuide: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenSavedCards: () -> Unit,
    onOpenWalletVideo: (WalletGuide) -> Unit,
    onCopiedWalletCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAlqanasPalette.current
    val scrollState = rememberScrollState()
    val cardFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

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
                    subtitle = "بوابة الدخول الآمن · AL-QANNAS",
                    themeMode = state.themeMode,
                    onSelectTheme = onSelectTheme,
                    onNotificationClick = onOpenNotifGuide,
                    onSupportClick = onOpenSupport
                )

                if (!state.validationAlert.isNullOrBlank()) {
                    AlertMessageBanner(
                        message = state.validationAlert,
                        tone = BannerTone.WARNING,
                        modifier = Modifier.testTag("login_validation_banner")
                    )
                }

                if (!state.routerError.isNullOrBlank()) {
                    AlertMessageBanner(
                        message = state.routerError,
                        tone = BannerTone.CRITICAL,
                        modifier = Modifier.testTag("login_router_error_banner")
                    )
                }

                // Broadcast Notice Strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تنبيه الشبكة",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = palette.textPrimary
                        )
                        Text(text = "·", color = palette.textMuted)
                        Text(
                            text = "أهلاً بكم في شبكة القناص — اختر السرعة وأدخل كود الكرت للاتصال الآمن",
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
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
                            LoginPrimaryCard(
                                state = state,
                                cardFocusRequester = cardFocusRequester,
                                onCardInputChange = onCardInputChange,
                                onSelectSpeed = onSelectSpeed,
                                onToggleBlockUpdates = onToggleBlockUpdates,
                                onSubmitLogin = {
                                    focusManager.clearFocus()
                                    onSubmitLogin()
                                },
                                onOpenSavedCards = onOpenSavedCards
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PlansCard(
                                onApplyPlan = { speed, block ->
                                    onApplyPlan(speed, block)
                                    runCatching { cardFocusRequester.requestFocus() }
                                }
                            )
                            WalletsAndPosCard(
                                onOpenWalletVideo = onOpenWalletVideo,
                                onCopiedWalletCode = onCopiedWalletCode
                            )
                            TipsCard()
                        }
                    }
                } else {
                    LoginPrimaryCard(
                        state = state,
                        cardFocusRequester = cardFocusRequester,
                        onCardInputChange = onCardInputChange,
                        onSelectSpeed = onSelectSpeed,
                        onToggleBlockUpdates = onToggleBlockUpdates,
                        onSubmitLogin = {
                            focusManager.clearFocus()
                            onSubmitLogin()
                        },
                        onOpenSavedCards = onOpenSavedCards
                    )
                    PlansCard(
                        onApplyPlan = { speed, block ->
                            onApplyPlan(speed, block)
                            runCatching { cardFocusRequester.requestFocus() }
                        }
                    )
                    WalletsAndPosCard(
                        onOpenWalletVideo = onOpenWalletVideo,
                        onCopiedWalletCode = onCopiedWalletCode
                    )
                    TipsCard()
                }

                AppFooter()
            }
        }
    }
}

@Composable
private fun LoginPrimaryCard(
    state: HotspotUiState,
    cardFocusRequester: FocusRequester,
    onCardInputChange: (String) -> Unit,
    onSelectSpeed: (SpeedProfile) -> Unit,
    onToggleBlockUpdates: (Boolean) -> Unit,
    onSubmitLogin: () -> Unit,
    onOpenSavedCards: () -> Unit
) {
    val palette = LocalAlqanasPalette.current

    GlassPanel {
        PanelHeader(
            title = "تسجيل الدخول",
            subtitle = "أدخل رقم الكرت وحدد السرعة المطلوبة",
            kicker = "HTTPS آمن"
        )

        if (state.hasSelectedPlan && state.selectedSpeed != null) {
            val updatesState = if (state.blockUpdates) "موقوفة" else "مسموحة"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("selected_plan_summary"),
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceSubtle,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Text(
                    text = "الخطة المحددة: ${state.selectedSpeed.title} | التحديثات: $updatesState",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }
        }

        Text(
            text = stringResource(R.string.login_card_label),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.cardInput,
                onValueChange = onCardInputChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(cardFocusRequester)
                    .testTag("card_number_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.login_card_placeholder),
                        color = palette.textMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = AlqanasMonoFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    textDirection = TextDirection.Ltr
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmitLogin() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.border,
                    focusedContainerColor = palette.surfaceSubtle,
                    unfocusedContainerColor = palette.surfaceSubtle,
                    cursorColor = palette.accent
                )
            )

            Surface(
                onClick = onOpenSavedCards,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("saved_cards_button"),
                shape = RoundedCornerShape(14.dp),
                color = palette.surfaceSubtle,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = stringResource(R.string.saved_cards_btn),
                        tint = palette.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onSubmitLogin,
            enabled = !state.isLoggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_submit_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = palette.accentInk
            )
        ) {
            if (state.isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.2.dp,
                    color = palette.accentInk
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.login_logging_in),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.login_submit_label),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مؤشر السرعة",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.textPrimary
            )
            Text(
                text = "اختر السرعة قبل الدخول",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SpeedometerGaugeSection(
            selectedSpeed = state.selectedSpeed,
            blockUpdates = state.blockUpdates,
            onSelectSpeed = onSelectSpeed,
            onToggleBlockUpdates = onToggleBlockUpdates
        )
    }
}

@Composable
private fun PlansCard(
    onApplyPlan: (SpeedProfile, Boolean) -> Unit
) {
    val palette = LocalAlqanasPalette.current

    GlassPanel {
        PanelHeader(
            title = "فئات الكروت",
            subtitle = "اضغط على أي باقة بالجدول لتحديد السرعة المناسبة تلقائياً",
            kicker = "6 باقات"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .background(palette.surfaceSubtle)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الفئة", modifier = Modifier.weight(1.1f), fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textSecondary)
                Text("الرصيد", modifier = Modifier.weight(1f), fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textSecondary, textAlign = TextAlign.Center)
                Text("الوقت", modifier = Modifier.weight(1f), fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textSecondary, textAlign = TextAlign.Center)
                Text("الصلاحية", modifier = Modifier.weight(0.9f), fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textSecondary, textAlign = TextAlign.End)
            }

            PRICING_PLANS.forEachIndexed { index, plan ->
                HorizontalDivider(color = palette.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApplyPlan(plan.speed, plan.blockUpdates) }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag("pricing_plan_row_$index"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = plan.price,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        if (!plan.note.isNullOrBlank()) {
                            Text(
                                text = plan.note,
                                fontSize = 10.5.sp,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Text(
                        text = plan.quota,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontFamily = if (plan.quota.any { it.isDigit() }) AlqanasMonoFont else MaterialTheme.typography.bodyMedium.fontFamily,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = plan.duration,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = plan.validity,
                        modifier = Modifier.weight(0.9f),
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletsAndPosCard(
    onOpenWalletVideo: (WalletGuide) -> Unit,
    onCopiedWalletCode: (String) -> Unit
) {
    val palette = LocalAlqanasPalette.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    GlassPanel {
        PanelHeader(
            title = "نقاط البيع والمحافظ",
            subtitle = "متوفر في جميع البقالات والمحلات المجاورة للشبكة"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CreditCard,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "الشراء من المحافظ",
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
                        .height(64.dp)
                        .testTag("wallet_video_btn_${guide.name}"),
                    shape = RoundedCornerShape(12.dp),
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

        Spacer(modifier = Modifier.height(12.dp))

        WALLET_MERCHANTS.forEach { merchant ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceSubtle,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = merchant.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = merchant.subtitle,
                            fontSize = 11.sp,
                            color = palette.textSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = merchant.code,
                            fontFamily = AlqanasMonoFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = palette.textPrimary
                        )
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(merchant.code))
                                onCopiedWalletCode(merchant.code)
                            },
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("copy_wallet_code_${merchant.code}"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = palette.accentInk
                            )
                        ) {
                            Text("نسخ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(PortalConstants.WHATSAPP_ORDER_URL))
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("whatsapp_order_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WhatsAppGreen,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "للاستفسار وطلب الخدمة عبر واتساب",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TipsCard() {
    val palette = LocalAlqanasPalette.current
    val tips = remember {
        listOf(
            Icons.Rounded.Lock to "لا تشارك رقم الكرت مع أي شخص للحفاظ على رصيدك.",
            Icons.Rounded.Monitor to "أغلق التطبيقات التي تعمل في الخلفية لتوفير البطارية والبيانات.",
            Icons.Rounded.Sync to "استخدم وضع \"إيقاف التحديثات\" لتجنب استهلاك الرصيد في الخلفية.",
            Icons.Rounded.Phone to "تواصل مع الدعم الفني فوراً عند مواجهة أي مشكلة."
        )
    }

    GlassPanel {
        PanelHeader(
            title = "نصائح هامة",
            subtitle = "إرشادات للحفاظ على رصيد الكرت وأفضل جودة اتصال"
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tips.forEach { (icon, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(palette.surfaceSubtle)
                            .border(1.dp, palette.border, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = text,
                        fontSize = 12.5.sp,
                        color = palette.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
