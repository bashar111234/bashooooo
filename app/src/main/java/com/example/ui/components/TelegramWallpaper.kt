package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ComposeShader
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.LightBg
import com.example.ui.theme.LocalAlqanasPalette
import kotlin.math.roundToInt

@Composable
fun TelegramWallpaper(modifier: Modifier = Modifier) {
    val palette = LocalAlqanasPalette.current
    val isDark = palette.isDark
    val density = LocalDensity.current
    // CSS uses mask-size: 400px 400px over 440x440 viewBox; ~320.dp yields the crisp Telegram density on phones
    val tileSizePx = remember(density) {
        with(density) { 320.dp.toPx().roundToInt().coerceAtLeast(256) }
    }
    val tileBitmap = remember(tileSizePx) {
        buildAlqanasTileMaskBitmap(tileSizePx)
    }
    val tileShader = remember(tileBitmap) {
        BitmapShader(tileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (isDark) {
            drawRect(color = DarkBg)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x145C6BC0), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.20f),
                    radius = size.maxDimension * 0.45f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.20f),
                radius = size.maxDimension * 0.45f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x148E44AD), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.30f),
                    radius = size.maxDimension * 0.50f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.30f),
                radius = size.maxDimension * 0.50f
            )
        } else {
            // Light mode classic Telegram sage wallpaper + 4 radial glows + 135deg gradient
            drawRect(color = LightBg)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFCBD98E),
                        Color(0xFF86BF84),
                        Color(0xFFC8D88F)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            val glows = listOf(
                Triple(Offset(size.width * 0.12f, size.height * 0.15f), Color(0xFFDBD990), 0.55f),
                Triple(Offset(size.width * 0.88f, size.height * 0.18f), Color(0xFF77B884), 0.55f),
                Triple(Offset(size.width * 0.86f, size.height * 0.86f), Color(0xFFD5DD92), 0.55f),
                Triple(Offset(size.width * 0.14f, size.height * 0.85f), Color(0xFF7CBC85), 0.55f)
            )
            glows.forEach { (center, col, radFrac) ->
                val radius = size.maxDimension * radFrac
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(col, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }
        }

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            if (isDark) {
                val gradientShader = LinearGradient(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    intArrayOf(
                        0xFF5C6BC0.toInt(),
                        0xFF9B59B6.toInt(),
                        0xFF2980B9.toInt(),
                        0xFF27AE60.toInt(),
                        0xFFD35400.toInt(),
                        0xFF8E44AD.toInt()
                    ),
                    floatArrayOf(0f, 0.20f, 0.40f, 0.62f, 0.82f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                overlayPaint.shader = ComposeShader(gradientShader, tileShader, PorterDuff.Mode.DST_IN)
                overlayPaint.alpha = (0.30f * 255).roundToInt()
            } else {
                // Dark forest tint (#1b4b23) at 0.18 opacity matching .light-mode::before
                val forestShader = LinearGradient(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    intArrayOf(0xFF1B4B23.toInt(), 0xFF1B4B23.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
                overlayPaint.shader = ComposeShader(forestShader, tileShader, PorterDuff.Mode.DST_IN)
                overlayPaint.alpha = (0.18f * 255).roundToInt()
            }
            native.drawRect(0f, 0f, size.width, size.height, overlayPaint)
        }
    }
}

private fun svgPath(d: String): Path {
    return PathParser().parsePathString(d).toPath().asAndroidPath()
}

private fun polygonPath(vararg pts: Pair<Float, Float>): Path {
    return Path().apply {
        if (pts.isNotEmpty()) {
            moveTo(pts[0].first, pts[0].second)
            for (i in 1 until pts.size) {
                lineTo(pts[i].first, pts[i].second)
            }
            close()
        }
    }
}

private fun buildAlqanasTileMaskBitmap(tilePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(tilePx, tilePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val scale = tilePx / 440f

    canvas.save()
    canvas.scale(scale, scale)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.65f
    }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    fun withTransform(tx: Float, ty: Float, deg: Float = 0f, block: () -> Unit) {
        canvas.save()
        canvas.translate(tx, ty)
        if (deg != 0f) canvas.rotate(deg)
        block()
        canvas.restore()
    }

    fun strokeD(d: String, width: Float = 1.65f, alpha: Float = 1f, dash: FloatArray? = null) {
        strokePaint.strokeWidth = width
        strokePaint.alpha = (alpha * 255).roundToInt()
        strokePaint.pathEffect = dash?.let { DashPathEffect(it, 0f) }
        canvas.drawPath(svgPath(d), strokePaint)
        strokePaint.pathEffect = null
        strokePaint.alpha = 255
    }

    fun fillD(d: String, alpha: Float = 1f) {
        fillPaint.alpha = (alpha * 255).roundToInt()
        canvas.drawPath(svgPath(d), fillPaint)
        fillPaint.alpha = 255
    }

    fun strokeCircle(cx: Float, cy: Float, r: Float, width: Float = 1.65f, alpha: Float = 1f, dash: FloatArray? = null) {
        strokePaint.strokeWidth = width
        strokePaint.alpha = (alpha * 255).roundToInt()
        strokePaint.pathEffect = dash?.let { DashPathEffect(it, 0f) }
        canvas.drawCircle(cx, cy, r, strokePaint)
        strokePaint.pathEffect = null
        strokePaint.alpha = 255
    }

    fun fillCircle(cx: Float, cy: Float, r: Float, alpha: Float = 1f) {
        fillPaint.alpha = (alpha * 255).roundToInt()
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.alpha = 255
    }

    fun strokeRect(x: Float, y: Float, w: Float, h: Float, rx: Float = 0f, width: Float = 1.65f, alpha: Float = 1f, dash: FloatArray? = null) {
        strokePaint.strokeWidth = width
        strokePaint.alpha = (alpha * 255).roundToInt()
        strokePaint.pathEffect = dash?.let { DashPathEffect(it, 0f) }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), rx, rx, strokePaint)
        strokePaint.pathEffect = null
        strokePaint.alpha = 255
    }

    fun fillRect(x: Float, y: Float, w: Float, h: Float, rx: Float = 0f, alpha: Float = 1f) {
        fillPaint.alpha = (alpha * 255).roundToInt()
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), rx, rx, fillPaint)
        fillPaint.alpha = 255
    }

    fun drawMonoText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        alpha: Float = 1f,
        letterSpacingEm: Float = 0f
    ) {
        textPaint.textSize = size
        textPaint.alpha = (alpha * 255).roundToInt()
        textPaint.letterSpacing = letterSpacingEm
        canvas.drawText(text, x, y, textPaint)
        textPaint.alpha = 255
        textPaint.letterSpacing = 0f
    }

    // ==================== ROW 1: TOP ZONE ====================

    // [1] TACTICAL SNIPER CROSSHAIR (42, 42, -14)
    withTransform(42f, 42f, -14f) {
        strokeCircle(0f, 0f, 22f)
        strokeCircle(0f, 0f, 14f, dash = floatArrayOf(4f, 3f))
        strokeCircle(0f, 0f, 3.5f)
        strokeD("M-28 0h10M18 0h10M0 -28v10M0 18v10", width = 2f)
        strokeD("M-10 -2v4M-6 -2v4M6 -2v4M10 -2v4M-2 -10h4M-2 -6h4M-2 6h4M-2 10h4", width = 1.2f)
        strokeRect(-4f, -26f, 8f, 3f, rx = 1f)
        strokeRect(23f, -4f, 3f, 8f, rx = 1f)
    }

    // [2] WI-FI 3-ARC SIGNAL (122, 34, 16)
    withTransform(122f, 34f, 16f) {
        strokeD("M-22 -6a30 30 0 0 1 44 0", width = 1.9f)
        strokeD("M-15 1a20 20 0 0 1 30 0", width = 1.9f)
        strokeD("M-8 8a10 10 0 0 1 16 0", width = 1.9f)
        fillCircle(0f, 15f, 2.8f)
        strokeD("M0 -19v-5M-12 -16l-3 -4M12 -16l3 -4", width = 1.2f)
    }

    // [3] MIKROTIK LHG / DISH ANTENNA (204, 45, -18)
    withTransform(204f, 45f, -18f) {
        strokePaint.strokeWidth = 1.65f
        canvas.drawOval(RectF(-14f, -22f, 14f, 22f), strokePaint)
        strokeD("M-10 -14l20 10M-13 -4l26 12M-11 8l22 10", width = 1.1f, alpha = 0.85f)
        strokeD("M-10 14l20 -10M-13 4l26 -12M-11 -8l22 -10", width = 1.1f, alpha = 0.85f)
        strokeD("M0 -12l18 12l-18 12", width = 1.5f)
        fillRect(16f, -4f, 6f, 8f, rx = 1.5f)
        strokeD("M27 -7a12 12 0 0 1 0 14M32 -12a20 20 0 0 1 0 24", width = 1.5f)
        strokeD("M-12 12l-8 12m4 -14v16")
    }

    // [4] HIGH-VOLTAGE LIGHTNING BOLT (282, 36, 14)
    withTransform(282f, 36f, 14f) {
        strokeD("M4 -22L-12 2h11l-7 20L14 -4H2L4 -22z", width = 1.7f)
        strokeD("M-16 -10l-4 -2M16 8l4 2", width = 1.3f)
    }

    // [5] CYBER SECURITY SHIELD WITH CHECKMARK (352, 42, -12)
    withTransform(352f, 42f, -12f) {
        strokeD("M0 -22l-18 7v11c0 12 7.5 21 18 25 10.5-4 18-13 18-25v-11L0 -22z", width = 1.8f)
        strokeD("M0 -17l-13 5v8c0 9 5.5 16 13 19 7.5-3 13-10 13-19v-8L0 -17z", width = 1.1f, alpha = 0.75f)
        strokeD("M-6 0l4.5 4.5L8 -5", width = 2.2f)
    }

    // [6] RJ45 ETHERNET CONNECTOR (412, 48, 22)
    withTransform(412f, 48f, 22f) {
        strokeRect(-10f, -16f, 20f, 18f, rx = 2f)
        strokeD("M-6 -16v5M-2 -16v5M2 -16v5M6 -16v5", width = 1.3f)
        strokeD("M-5 -10h10v8h-10z", width = 1.1f)
        strokeD("M-7 2l2 8h10l2 -8")
        strokeD("M0 10c0 7 -5 10 -5 16", width = 2f)
    }

    // ==================== ROW 2: UPPER-MID ZONE ====================

    // [7] MIKROTIK ROUTERBOARD / DUAL-ANTENNA ROUTER (48, 122, 12)
    withTransform(48f, 122f, 12f) {
        strokeD("M-14 -6l-6 -20a2 2 0 0 1 3.5 -1l5.5 21")
        strokeD("M14 -6l6 -20a2 2 0 0 1 -3.5 -1l-5.5 21")
        strokeD("M-10 -24a14 14 0 0 1 20 0", dash = floatArrayOf(2f, 3f))
        strokeD("M-6 -19a8 8 0 0 1 12 0")
        strokeRect(-24f, -6f, 48f, 18f, rx = 3.5f, width = 1.8f)
        strokeRect(-19f, -1f, 6f, 5f, rx = 1f, width = 1.2f)
        strokeRect(-10f, -1f, 6f, 5f, rx = 1f, width = 1.2f)
        strokeRect(-1f, -1f, 6f, 5f, rx = 1f, width = 1.2f)
        strokeRect(8f, -1f, 6f, 5f, rx = 1f, width = 1.2f)
        fillCircle(18f, -1f, 1.2f)
        fillCircle(18f, 4f, 1.2f)
        strokeD("M-16 12v3h5v-3M11 12v3h5v-3")
    }

    // [8] BRANDED BADGE 1: ALQANAS TACTICAL BANNER (154, 114, -15)
    withTransform(154f, 114f, -15f) {
        strokeD("M-54 -16h98l10 10v14l-10 10h-98l-10 -10v-14l10 -10z", width = 1.8f)
        strokeD("M-50 -12h90l6 6v10l-6 6h-90l-6 -6v-10l6 -6z", width = 1.0f, alpha = 0.65f)
        strokeCircle(-38f, 1f, 7f)
        fillCircle(-38f, 1f, 2f)
        strokeD("M-47 1h4m10 0h4M-38 -8v4m0 10v4", width = 1.3f)
        drawMonoText("ALQANAS", -23f, 6f, 15.5f, letterSpacingEm = 0.09f)
        strokeD("M-20 -20h35l4 4M15 22h25l4 -4", width = 1.2f)
    }

    // [9] SPEEDOMETER / LOW PING GAUGE (265, 110, 18)
    withTransform(265f, 110f, 18f) {
        strokeD("M-22 10a24 24 0 1 1 44 0", width = 1.8f)
        strokeD("M-18 -2l3 2M-11 -12l2 3M0 -16v4M11 -12l-2 3M18 -2l-3 2", width = 1.4f)
        strokeD("M0 8L14 -7", width = 2.2f)
        strokeCircle(0f, 8f, 3.5f)
        drawMonoText("1000M", -14f, 19f, 7.5f)
    }

    // [10] TELECOM WISP TOWER (345, 128, -14)
    withTransform(345f, 128f, -14f) {
        strokeD("M0 -26L-14 24M0 -26L14 24M-19 24h38", width = 1.7f)
        strokeD("M-4 -12h8M-8 0h16M-11 12h22", width = 1.3f)
        strokeD("M-4 -12L8 0L-11 12L14 24M4 -12L-8 0L11 12L-14 24", width = 1.1f)
        fillCircle(0f, -28f, 2.5f)
        strokeD("M-9 -33a10 10 0 0 0 0 12M-14 -37a16 16 0 0 0 0 20", width = 1.5f)
        strokeD("M9 -33a10 10 0 0 1 0 12M14 -37a16 16 0 0 1 0 20", width = 1.5f)
    }

    // [11] CLOUD SERVER & UP/DOWN TRAFFIC (412, 134, -8)
    withTransform(412f, 134f, -8f) {
        strokeD("M-14 6h26a9 9 0 0 0 1 -17.8A13 13 0 0 0 -12 -14a10 10 0 0 0 -2 20z", width = 1.7f)
        strokeD("M-4 -4v16m-4 -4l4 4 4 -4", width = 1.5f)
        strokeD("M6 12V-4m-4 4l4 -4 4 4", width = 1.5f)
    }

    // ==================== ROW 3: CENTER HEART ZONE ====================

    // [12] BRANDED PHONE PILL: 772432226 (68, 202, -16)
    withTransform(68f, 202f, -16f) {
        strokeRect(-56f, -14f, 112f, 28f, rx = 14f, width = 1.8f)
        strokeRect(-52f, -10f, 104f, 20f, rx = 10f, width = 1.0f, alpha = 0.7f, dash = floatArrayOf(3f, 2f))
        strokeD(
            "M-41 -4c1 4 4 7 8 8l2.5 -2.5c.5-.5 1.2-.5 1.8-.2l3.5 1.8c.6.3.9 1 .7 1.7-.6 2.2-2.8 3.6-5.2 3.2-7.5-1.2-13.5-7.2-14.7-14.7-.4-2.4 1-4.6 3.2-5.2.7-.2 1.4.1 1.7.7l1.8 3.5c.3.6.3 1.3-.2 1.8L-41 -4z",
            width = 1.3f
        )
        drawMonoText("772432226", -22f, 5f, 12.5f, letterSpacingEm = 0.06f)
    }

    // [13] GLOBAL MESH NETWORK SPHERE (174, 198, 15)
    withTransform(174f, 198f, 15f) {
        strokeCircle(0f, 0f, 22f, width = 1.8f)
        strokePaint.strokeWidth = 1.65f
        canvas.drawOval(RectF(-10f, -22f, 10f, 22f), strokePaint)
        canvas.drawOval(RectF(-22f, -9f, 22f, 9f), strokePaint)
        strokeD("M-19 -11h38M-19 11h38M0 -22v44", width = 1.2f)
        fillCircle(19f, -16f, 3f)
        fillCircle(-20f, 14f, 2.5f)
    }

    // [14] PRO GAMING CONTROLLER (262, 194, -18)
    withTransform(262f, 194f, -18f) {
        strokeD(
            "M-15 -12h30c9 0 15 7 17 17 1.5 7.5-3 14-8.5 14-4 0-7-3.5-10.5-7h-16c-3.5 3.5-6.5 7-10.5 7-5.5 0-10-6.5-8.5-14 2-10 8-17 17-17z",
            width = 1.75f
        )
        strokeD("M-14 -2h8M-10 -6v8", width = 1.8f)
        fillCircle(12f, -5f, 1.6f)
        fillCircle(16f, -1f, 1.6f)
        fillCircle(8f, -1f, 1.6f)
        fillCircle(12f, 3f, 1.6f)
        strokeD("M-6 -17a9 9 0 0 1 12 0M-10 -21a14 14 0 0 1 20 0", width = 1.4f)
    }

    // [15] ASCENDING 5-BAR SIGNAL STRENGTH + BOOST ARROW (342, 206, 12)
    withTransform(342f, 206f, 12f) {
        strokeRect(-20f, 8f, 5f, 8f, rx = 1f)
        strokeRect(-11f, 2f, 5f, 14f, rx = 1f)
        strokeRect(-2f, -4f, 5f, 20f, rx = 1f)
        strokeRect(7f, -10f, 5f, 26f, rx = 1f)
        strokeRect(16f, -17f, 5f, 33f, rx = 1f)
        strokeD("M-19 2L12 -20m0 0h-7m7 0v7", width = 1.6f)
    }

    // [16] MIKROTIK CLOUD CORE SWITCH / RACK ROUTER (405, 218, -16)
    withTransform(405f, 218f, -16f) {
        strokeRect(-26f, -10f, 52f, 18f, rx = 2.5f, width = 1.7f)
        strokeD("M-26 -7h-4v12h4M26 -7h4v12h-4")
        strokeRect(-21f, -5f, 6f, 5f, rx = 0.8f, width = 1.2f)
        strokeRect(-13f, -5f, 6f, 5f, rx = 0.8f, width = 1.2f)
        strokeRect(-5f, -5f, 6f, 5f, rx = 0.8f, width = 1.2f)
        strokeRect(3f, -5f, 6f, 5f, rx = 0.8f, width = 1.2f)
        fillRect(13f, -6f, 9f, 10f, rx = 1f, alpha = 0.25f)
        strokeRect(13f, -6f, 9f, 10f, rx = 1f, width = 1.2f)
        strokeD("M-21 4h28", width = 1.2f, dash = floatArrayOf(2f, 2f))
    }

    // ==================== ROW 4: LOWER-MID ZONE ====================

    // [17] HIGH-SPEED ROCKET (46, 292, 28)
    withTransform(46f, 292f, 28f) {
        strokeD("M0 -24c8 7 11 18 11 28H-11c0-10 3-21 11-28z", width = 1.75f)
        strokeCircle(0f, -6f, 4.5f)
        strokeD("M-11 -2l-8 8v6h8M11 -2l8 8v6h-8", width = 1.6f)
        strokeD("M-6 4l3 10 3-6 3 6 3-10", width = 1.5f)
    }

    // [18] MIKROTIK SECTOR / SXT BEAM ANTENNA (126, 286, -14)
    withTransform(126f, 286f, -14f) {
        strokeRect(-12f, -20f, 24f, 34f, rx = 5f, width = 1.75f)
        strokeD("M-7 -14h14M-7 -8h14M-7 -2h14", width = 1.2f, alpha = 0.75f)
        fillCircle(-5f, 7f, 1.2f)
        fillCircle(-1f, 7f, 1.2f)
        fillCircle(3f, 7f, 1.2f)
        fillCircle(7f, 7f, 1.2f)
        strokeD("M-4 14v8h8v-8")
        strokeD("M-10 -25a15 15 0 0 1 20 0M-15 -30a22 22 0 0 1 30 0", width = 1.5f)
    }

    // [19] SNIPER TARGET LOCK + WIFI CORE (212, 288, 18)
    withTransform(212f, 288f, 18f) {
        strokePaint.strokeWidth = 1.6f
        canvas.drawPath(
            polygonPath(
                0f to -22f,
                19f to -11f,
                19f to 11f,
                0f to 22f,
                -19f to 11f,
                -19f to -11f
            ),
            strokePaint
        )
        strokeCircle(0f, 0f, 11f)
        fillCircle(0f, 0f, 2.5f)
        strokeD("M-25 0h10M15 0h10M0 -25v10M0 15v10", width = 1.8f)
    }

    // [20] BRANDED BADGE 2: ALQANAS NET + 772432226 STAMP (316, 294, -14)
    withTransform(316f, 294f, -14f) {
        strokeRect(-58f, -20f, 116f, 40f, rx = 8f, width = 1.8f)
        fillCircle(-51f, -13f, 1.3f)
        fillCircle(51f, -13f, 1.3f)
        fillCircle(-51f, 13f, 1.3f)
        fillCircle(51f, 13f, 1.3f)
        fillD("M-42 -12l-5 8h5l-3 8 9-10h-5l3-6z")
        drawMonoText("ALQANAS", -28f, -2f, 13.5f, letterSpacingEm = 0.08f)
        strokeD("M-44 3h88", width = 1.0f, alpha = 0.75f, dash = floatArrayOf(3f, 2f))
        drawMonoText("772432226", -36f, 14f, 11f, letterSpacingEm = 0.12f)
    }

    // [21] ENCRYPTED PADLOCK & KEY (408, 304, 16)
    withTransform(408f, 304f, 16f) {
        strokeRect(-13f, -4f, 26f, 20f, rx = 3.5f, width = 1.7f)
        strokeD("M-8 -4v-6a8 8 0 0 1 16 0v6", width = 1.8f)
        strokeCircle(0f, 4f, 2.2f)
        strokeD("M0 6.2v5", width = 1.8f)
    }

    // ==================== ROW 5: BOTTOM ZONE ====================

    // [22] WI-FI 6 / 5G ULTRA SPEED BADGE (52, 384, -12)
    withTransform(52f, 384f, -12f) {
        strokeRect(-26f, -15f, 52f, 30f, rx = 7f, width = 1.7f)
        strokeD("M-16 -2a12 12 0 0 1 16 0M-12 3a7 7 0 0 1 8 0", width = 1.6f)
        fillCircle(-8f, 7f, 1.6f)
        drawMonoText("6G", 5f, 7f, 13f)
        strokeD("M-34 -8h6M-36 0h8M-32 8h4", width = 1.5f)
    }

    // [23] FIBER OPTIC PULSE & SATELLITE DISH (138, 382, 16)
    withTransform(138f, 382f, 16f) {
        strokeD("M-16 6a20 20 0 0 0 32 0z", width = 1.7f)
        strokeD("M0 6l8 -14", width = 1.7f)
        fillCircle(9f, -9f, 2.5f)
        strokeD("M0 -14a14 14 0 0 1 16 14M4 -20a21 21 0 0 1 22 20", width = 1.4f)
        strokeD("M-10 16h20M0 10v6", width = 1.6f)
    }

    // [24] LAPTOP CONNECTED TO HOTSPOT (222, 388, -15)
    withTransform(222f, 388f, -15f) {
        strokeRect(-18f, -16f, 36f, 23f, rx = 2.5f, width = 1.7f)
        strokeD("M-24 7h48l-4 5h-40l-4 -5z", width = 1.7f)
        strokeD("M-8 -7a11 11 0 0 1 16 0M-5 -3a6 6 0 0 1 10 0", width = 1.4f)
        fillCircle(0f, 1f, 1.4f)
    }

    // [25] TWIN HIGH-VOLTAGE LIGHTNING & MIKROTIK WAVE (306, 388, 20)
    withTransform(306f, 388f, 20f) {
        strokeCircle(0f, 0f, 19f, width = 1.6f, dash = floatArrayOf(6f, 3f))
        strokeD("M3 -13l-10 12h8l-4 14 13 -14h-8l1 -12z", width = 1.6f)
    }

    // [26] SMARTPHONE WITH VOUCHER / HOTSPOT LOGIN (388, 390, -18)
    withTransform(388f, 390f, -18f) {
        strokeRect(-12f, -22f, 24f, 42f, rx = 4f, width = 1.75f)
        strokeD("M-4 -18h8", width = 1.3f)
        strokeCircle(0f, 15f, 2f)
        strokeCircle(-3f, -4f, 4f)
        strokeD("M1 -4h8v3m-3 -3v3", width = 1.5f)
    }

    // ==================== 45+ MICRO-DOODLES & FILLER ELEMENTS ====================

    // Tiny 4-Point Sparkle Stars
    val sparklePaths = listOf(
        "M84 16l2 5 5 2-5 2-2 5-2-5-5-2 5-2z",
        "M245 20l1.5 4 4 1.5-4 1.5-1.5 4-1.5-4-4-1.5 4-1.5z",
        "M312 72l2 5 5 2-5 2-2 5-2-5-5-2 5-2z",
        "M98 158l1.8 4.5 4.5 1.8-4.5 1.8-1.8 4.5-1.8-4.5-4.5-1.8 4.5-1.8z",
        "M218 148l1.5 4 4 1.5-4 1.5-1.5 4-1.5-4-4-1.5 4-1.5z",
        "M132 238l2 5 5 2-5 2-2 5-2-5-5-2 5-2z",
        "M372 252l1.8 4.5 4.5 1.8-4.5 1.8-1.8 4.5-1.8-4.5-4.5-1.8 4.5-1.8z",
        "M88 335l2 5 5 2-5 2-2 5-2-5-5-2 5-2z",
        "M262 342l1.5 4 4 1.5-4 1.5-1.5 4-1.5-4-4-1.5 4-1.5z",
        "M178 418l1.8 4 4 1.8-4 1.8-1.8 4-1.8-4-4-1.8 4-1.8z"
    )
    sparklePaths.forEach { strokeD(it, width = 1.2f) }

    // Mini Crosshairs & Target Ticks
    listOf(
        Triple(88f, 76f, 5f),
        Triple(222f, 88f, 4.5f),
        Triple(304f, 158f, 5f),
        Triple(18f, 248f, 5f),
        Triple(172f, 336f, 4.5f),
        Triple(348f, 348f, 5f)
    ).forEach { (cx, cy, r) ->
        withTransform(cx, cy) {
            strokeCircle(0f, 0f, r, width = 1.2f)
            val span = if (r >= 5f) 8f else 7f
            strokeD("M-${span} 0h${span * 2}M0 -${span}v${span * 2}", width = 1.2f)
        }
    }

    // Mini Lightning Sparks
    listOf(
        "M158 18l-5 8h5l-3 8",
        "M14 82l-4 7h4l-3 7",
        "M388 86l-5 8h5l-3 8",
        "M122 168l-4 7h4l-3 7",
        "M298 242l-5 8h5l-3 8",
        "M88 252l-4 7h4l-3 7",
        "M258 416l-5 8h5l-3 8"
    ).forEach { strokeD(it, width = 1.4f) }

    // Mini Wi-Fi Waves
    listOf(
        Triple(166f, 68f, -22f),
        Triple(302f, 18f, 15f),
        Triple(22f, 168f, 25f),
        Triple(222f, 238f, -18f),
        Triple(14f, 342f, 12f),
        Triple(425f, 355f, -20f)
    ).forEach { (cx, cy, rot) ->
        withTransform(cx, cy, rot) {
            strokeD("M-7 0a9 9 0 0 1 14 0M-4 4a5 5 0 0 1 8 0", width = 1.3f)
            fillCircle(0f, 7f, 1.2f)
        }
    }

    // Tech Data Labels (5GHz, PING 1ms, MikroTik, ALQANAS, 772432226)
    withTransform(58f, 74f, -12f) { drawMonoText("5GHz", 0f, 0f, 8f, alpha = 0.85f) }
    withTransform(366f, 176f, 14f) { drawMonoText("PING 1ms", 0f, 0f, 8.5f, alpha = 0.85f) }
    withTransform(155f, 252f, -15f) { drawMonoText("MikroTik", 0f, 0f, 8.5f, alpha = 0.85f) }
    withTransform(86f, 424f, -8f) { drawMonoText("ALQANAS", 0f, 0f, 8.5f, alpha = 0.85f) }
    withTransform(330f, 428f, 10f) { drawMonoText("772432226", 0f, 0f, 8f, alpha = 0.85f) }

    // Data Node Triads & Hex Nuts
    listOf(Triple(262f, 68f, 20f), Triple(88f, 294f, -25f)).forEach { (cx, cy, rot) ->
        withTransform(cx, cy, rot) {
            strokeCircle(-5f, 4f, 2f)
            strokeCircle(5f, 4f, 2f)
            strokeCircle(0f, -5f, 2f)
            strokeD("M-3 3l6 0M-4 2l3 -5M4 2l-3 -5", width = 1.1f)
        }
    }
    listOf(Triple(115f, 198f, -10f), Triple(256f, 254f, 15f)).forEach { (cx, cy, rot) ->
        withTransform(cx, cy, rot) {
            strokePaint.strokeWidth = 1.2f
            canvas.drawPath(
                polygonPath(0f to -6f, 5f to -3f, 5f to 3f, 0f to 6f, -5f to 3f, -5f to -3f),
                strokePaint
            )
            strokeCircle(0f, 0f, 2f, width = 1.2f)
        }
    }

    // Subtle Decorative Dots & Rings
    strokeCircle(15f, 14f, 2.5f)
    fillCircle(168f, 12f, 2f)
    strokeCircle(395f, 14f, 3f)
    fillCircle(428f, 92f, 2.2f)
    strokeCircle(195f, 152f, 2.5f)
    strokeCircle(12f, 208f, 2.8f)
    strokeCircle(428f, 264f, 2.5f)
    fillCircle(176f, 296f, 2f)
    strokeCircle(272f, 378f, 2.8f)
    strokeCircle(16f, 422f, 2.5f)
    strokeCircle(426f, 426f, 3f)

    canvas.restore()
    return bitmap
}
