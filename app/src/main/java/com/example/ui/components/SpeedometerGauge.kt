package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedProfile
import com.example.ui.theme.AlqanasMonoFont
import com.example.ui.theme.LocalAlqanasPalette

@Composable
fun SpeedometerGaugeSection(
    selectedSpeed: SpeedProfile?,
    blockUpdates: Boolean,
    onSelectSpeed: (SpeedProfile) -> Unit,
    onToggleBlockUpdates: (Boolean) -> Unit,
    captionTitle: String = "السرعة المحددة",
    modifier: Modifier = Modifier
) {
    val palette = LocalAlqanasPalette.current
    val targetProgress = (selectedSpeed?.arcProgress ?: 0.08f).coerceIn(0.03f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = 0.78f
        ),
        label = "gauge_arc_progress"
    )
    // Lock needle angle directly to animatedProgress so needle tip and arc end never drift apart
    val syncedAngle = -90f + 180f * animatedProgress

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(248.dp)
                .height(136.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .width(248.dp)
                    .height(130.dp)
            ) {
                val strokePx = 12.dp.toPx()
                val arcDiameter = 184.dp.toPx().coerceAtMost(size.width - strokePx * 2.5f)
                val arcRadius = arcDiameter / 2f
                val pivot = Offset(size.width / 2f, size.height - 16.dp.toPx())
                val topLeft = Offset(
                    x = pivot.x - arcRadius,
                    y = pivot.y - arcRadius
                )
                val arcSize = Size(arcDiameter, arcDiameter)

                // Subtle outer dial tick marks (0% to 100% along top semicircle)
                val tickCount = 16
                val outerTickRadius = arcRadius + strokePx * 0.85f
                for (i in 0..tickCount) {
                    val fraction = i / tickCount.toFloat()
                    val isMajor = i % 4 == 0
                    val tickDeg = -90f + 180f * fraction
                    val innerTickRadius = outerTickRadius - if (isMajor) 6.dp.toPx() else 3.5.dp.toPx()
                    rotate(degrees = tickDeg, pivot = pivot) {
                        drawLine(
                            color = palette.textMuted.copy(alpha = if (isMajor) 0.55f else 0.28f),
                            start = Offset(pivot.x, pivot.y - outerTickRadius),
                            end = Offset(pivot.x, pivot.y - innerTickRadius),
                            strokeWidth = if (isMajor) 1.6.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Background track arc
                drawArc(
                    color = if (palette.isDark) {
                        palette.textPrimary.copy(alpha = 0.14f)
                    } else {
                        palette.textPrimary.copy(alpha = 0.12f)
                    },
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Active progress arc
                val sweep = (180f * animatedProgress).coerceIn(3f, 180f)
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            palette.textSecondary,
                            palette.accent
                        )
                    ),
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Tapered needle pointing directly at sweep tip
                rotate(degrees = syncedAngle, pivot = pivot) {
                    val needleLength = arcRadius - 17.dp.toPx()
                    val needlePath = Path().apply {
                        moveTo(pivot.x - 3.dp.toPx(), pivot.y)
                        lineTo(pivot.x + 3.dp.toPx(), pivot.y)
                        lineTo(pivot.x + 0.6.dp.toPx(), pivot.y - needleLength)
                        lineTo(pivot.x - 0.6.dp.toPx(), pivot.y - needleLength)
                        close()
                    }
                    drawPath(path = needlePath, color = palette.accent)
                }

                // Center pivot hub
                drawCircle(
                    color = palette.accent.copy(alpha = 0.22f),
                    radius = 10.dp.toPx(),
                    center = pivot
                )
                drawCircle(color = palette.accent, radius = 6.5.dp.toPx(), center = pivot)
                drawCircle(color = palette.accentInk, radius = 2.6.dp.toPx(), center = pivot)
            }
        }

        Text(
            text = captionTitle,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = selectedSpeed?.title ?: "اختر السرعة",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = palette.surfaceSubtle,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Text(
                text = selectedSpeed?.rate ?: "—",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = AlqanasMonoFont,
                    fontWeight = FontWeight.Bold
                ),
                color = palette.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Speed Selector Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceSubtle)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpeedProfile.entries.forEach { profile ->
                val isSelected = selectedSpeed == profile
                val icon: ImageVector = when (profile) {
                    SpeedProfile.ECO -> Icons.Rounded.AttachMoney
                    SpeedProfile.DEFAULT -> Icons.Rounded.Wifi
                    SpeedProfile.HIGH -> Icons.Rounded.Bolt
                    SpeedProfile.VERY -> Icons.Rounded.Star
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .testTag("speed_btn_${profile.key}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) palette.accent else ColorTransparent,
                    contentColor = if (isSelected) palette.accentInk else palette.textSecondary,
                    border = if (isSelected) {
                        BorderStroke(1.dp, palette.accent)
                    } else {
                        BorderStroke(1.dp, palette.border.copy(alpha = 0.45f))
                    },
                    onClick = { onSelectSpeed(profile) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = profile.title,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = profile.shortLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = profile.rate,
                            fontSize = 9.5.sp,
                            fontFamily = AlqanasMonoFont,
                            maxLines = 1,
                            color = if (isSelected) palette.accentInk.copy(alpha = 0.85f) else palette.textMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Auto-Updates Blocker Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceSubtle)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .clickable(
                    role = Role.Switch,
                    onClick = { onToggleBlockUpdates(!blockUpdates) }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("block_updates_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "إيقاف التحديثات التلقائية",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "إيقاف التحديثات التلقائية",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = "حجب متجر التطبيقات وتوفير الرصيد",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Switch(
                checked = blockUpdates,
                onCheckedChange = onToggleBlockUpdates,
                modifier = Modifier.testTag("block_updates_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.accentInk,
                    checkedTrackColor = palette.accent,
                    uncheckedThumbColor = palette.textSecondary,
                    uncheckedTrackColor = palette.surface,
                    uncheckedBorderColor = palette.border
                )
            )
        }
    }
}

private val ColorTransparent = androidx.compose.ui.graphics.Color.Transparent
