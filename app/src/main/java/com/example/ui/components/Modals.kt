package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PortalConstants
import com.example.model.SavedCard
import com.example.model.SpeedProfile
import com.example.model.WalletGuide
import com.example.ui.theme.AlqanasMonoFont
import com.example.ui.theme.LocalAlqanasPalette

@Composable
fun SupportModal(onDismiss: () -> Unit) {
    val palette = LocalAlqanasPalette.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceSolid,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceSubtle)
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "الدعم الفني والمبيعات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "خدمة العملاء متوفرة لتزويدك بالكروت والدعم الفني",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${PortalConstants.SUPPORT_PHONE}"))
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("support_call_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "اتصال هاتفي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = palette.textPrimary
                        )
                        Text(
                            text = PortalConstants.SUPPORT_PHONE,
                            fontFamily = AlqanasMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = palette.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(PortalConstants.WHATSAPP_URL))
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("support_whatsapp_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "مراسلة واتساب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = palette.textPrimary
                        )
                        Text(
                            text = PortalConstants.SUPPORT_PHONE,
                            fontFamily = AlqanasMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = palette.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("support_close_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(text = "إغلاق", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SavedCardsModal(
    savedCards: List<SavedCard>,
    onSelectCard: (SavedCard) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAlqanasPalette.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceSolid,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceSubtle)
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "البطاقات المحفوظة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اختر كرتاً سابقاً لتعبئته تلقائياً",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (savedCards.isEmpty()) {
                    Text(
                        text = "لا توجد بطاقات محفوظة حتى الآن",
                        fontSize = 12.5.sp,
                        color = palette.textMuted,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedCards, key = { it.code }) { card ->
                            val speedTitle = SpeedProfile.fromKey(card.speedKey)?.title ?: "تلقائي"
                            Surface(
                                onClick = {
                                    onSelectCard(card)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("saved_card_item_${card.code}"),
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceSubtle,
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = card.code,
                                        fontFamily = AlqanasMonoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = speedTitle,
                                        fontSize = 11.5.sp,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("saved_cards_cancel_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(text = "إلغاء", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationGuideModal(
    onEnableNotifications: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAlqanasPalette.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceSolid,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceSubtle)
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "تنبيهات الرصيد والوقت (30% و 15%)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "فعّل إشعارات النظام لتصلك التنبيهات في شريط الهاتف حتى عند استخدام تطبيقات أخرى",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "1. التنبيه الأول عند تبقي 30%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "إشعار مبكر عند وصول استهلاك البيانات أو الوقت إلى 70% لتنظيم استخدامك.",
                            fontSize = 11.5.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceSubtle,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "2. التنبيه الحرج عند تبقي 15%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تحذير أخير قبل نفاد الكرت مباشرة لتجهيز كرت بديل دون انقطاع مفاجئ.",
                            fontSize = 11.5.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEnableNotifications,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("notif_modal_enable_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.accentInk
                        )
                    ) {
                        Text(text = "تفعيل الإشعارات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("notif_modal_later_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text(text = "لاحقاً", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun WalletVideoModal(
    guide: WalletGuide,
    onDismiss: () -> Unit
) {
    val palette = LocalAlqanasPalette.current
    val context = LocalContext.current
    var playbackError by remember(guide) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceSolid,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = palette.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "شرح الشراء عبر محفظة ${guide.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اتبع الخطوات التالية لشراء كرت شبكة القناص عبر ${guide.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!playbackError) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoURI(Uri.parse(guide.fullVideoUrl))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        start()
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        playbackError = true
                                        true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "تعذر تشغيل الفيديو مباشرة. تأكد من اتصالك بشبكة القناص أو افتح الرابط في المتصفح.",
                                fontSize = 12.5.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = guide.fullVideoUrl,
                                fontSize = 11.sp,
                                fontFamily = AlqanasMonoFont,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(guide.fullVideoUrl))
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("wallet_video_open_external_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.accentInk
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "تنزيل / فتح", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("wallet_video_close_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text(text = "إغلاق", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.textPrimary)
                    }
                }
            }
        }
    }
}
