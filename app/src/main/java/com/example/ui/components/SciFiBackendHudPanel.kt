package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.BackendLogEntry
import com.example.ui.viewmodel.BackendLogLevel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// Bioluminescent Banana-Leaf / Phosphor Sci-Fi HUD Palette
private val HudVoidTop = Color(0xFF030A05)
private val HudVoidMid = Color(0xFF061309)
private val HudVoidBottom = Color(0xFF020804)
private val HudLeafNeon = Color(0xFF76FF03)      // Bright Banana-Leaf Neon
private val HudLeafPrimary = Color(0xFF84FF33)   // Luminous Leaf Green
private val HudLeafChartreuse = Color(0xFFAEEA00) // Tactical Header Lime
private val HudLeafMint = Color(0xFFB2FF59)      // High-contrast Chip Accent
private val HudLeafDim = Color(0xFF5EEB1C)       // Timestamp / Telemetry Readout
private val HudGridLine = Color(0x1676FF03)      // Faint Blueprint Matrix Grid
private val HudScanline = Color(0x0D76FF03)      // Holographic CRT Micro-Scanline
private val HudBorderOuter = Color(0xFF2B682E)   // Structural Frame Border
private val HudChipFill = Color(0xFF0C2210)      // Subsystem Tag Background
private val HudChipBorder = Color(0xFF39823D)    // Subsystem Tag Border

private val ShareTechMonoFamily = FontFamily(
    Font(R.font.share_tech_mono, FontWeight.Normal)
)

private val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

private data class ParsedHudLog(
    val moduleTag: String,
    val body: String
)

private fun parseHudLogMessage(raw: String): ParsedHudLog {
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
        val closeIdx = trimmed.indexOf(']')
        if (closeIdx in 2..24) {
            val tag = trimmed.substring(1, closeIdx).trim().uppercase()
            val rest = trimmed.substring(closeIdx + 1).trim()
            if (rest.isNotEmpty()) {
                return ParsedHudLog(moduleTag = tag, body = rest)
            }
        }
    }
    return ParsedHudLog(moduleTag = "SYS.IO", body = trimmed)
}

/**
 * Cinematic Sci-Fi Tactical Telemetry & Backend Process HUD Panel for Android TV.
 * Non-focusable by design so D-pad navigation remains strictly on interactive TV controls.
 */
@Composable
fun SciFiBackendHudPanel(
    activeStatus: String,
    isBusy: Boolean,
    logs: List<BackendLogEntry>,
    sleepTimerSeconds: Int,
    engineLabel: String = "STANDBY",
    channelCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val hudShape = remember {
        CutCornerShape(
            topStart = 12.dp,
            topEnd = 4.dp,
            bottomEnd = 12.dp,
            bottomStart = 4.dp
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scifi_hud_transition")

    // Vertical holographic laser sweep (0f -> 1f)
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isBusy) 2400 else 4800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_laser_sweep"
    )

    // Radar / Orbital Core rotation (0f -> 360f)
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isBusy) 2200 else 6500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_radar_angle"
    )

    // Blinking terminal block cursor & pulse wave phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_wave_phase"
    )

    val cursorVisible = sin(wavePhase * 2f) >= -0.15f

    val glowTitleStyle = remember {
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            shadow = Shadow(
                color = Color(0x9976FF03),
                offset = Offset.Zero,
                blurRadius = 10f
            )
        )
    }

    val glowCommandStyle = remember {
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            shadow = Shadow(
                color = Color(0x8876FF03),
                offset = Offset.Zero,
                blurRadius = 8f
            )
        )
    }

    Surface(
        shape = hudShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, HudBorderOuter),
        modifier = modifier.testTag("tv_backend_process_panel")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(hudShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(HudVoidTop, HudVoidMid, HudVoidBottom)
                    )
                )
                .drawWithCache {
                    val gridStep = 20.dp.toPx()
                    val scanStep = 3.dp.toPx()
                    val bracketLen = 14.dp.toPx()
                    val chamfer = 11.dp.toPx()
                    val strokeBracket = 2.dp.toPx()

                    // Corner reticle path (Sci-Fi tactical brackets)
                    val cornerPath = Path().apply {
                        // Top-Left angled chamfer bracket
                        moveTo(0f, chamfer + bracketLen)
                        lineTo(0f, chamfer)
                        lineTo(chamfer, 0f)
                        lineTo(chamfer + bracketLen, 0f)

                        // Top-Right sharp bracket
                        moveTo(size.width - bracketLen, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, bracketLen)

                        // Bottom-Right angled chamfer bracket
                        moveTo(size.width, size.height - chamfer - bracketLen)
                        lineTo(size.width, size.height - chamfer)
                        lineTo(size.width - chamfer, size.height)
                        lineTo(size.width - chamfer - bracketLen, size.height)

                        // Bottom-Left sharp bracket
                        moveTo(bracketLen, size.height)
                        lineTo(0f, size.height)
                        lineTo(0f, size.height - bracketLen)
                    }

                    onDrawWithContent {
                        // 1. Subtle Sci-Fi coordinate matrix grid
                        var gx = gridStep
                        while (gx < size.width) {
                            drawLine(
                                color = HudGridLine,
                                start = Offset(gx, 0f),
                                end = Offset(gx, size.height),
                                strokeWidth = 1f
                            )
                            gx += gridStep
                        }
                        var gy = gridStep
                        while (gy < size.height) {
                            drawLine(
                                color = HudGridLine,
                                start = Offset(0f, gy),
                                end = Offset(size.width, gy),
                                strokeWidth = 1f
                            )
                            gy += gridStep
                        }

                        // 2. Holographic horizontal CRT scanlines
                        var sy = 0f
                        while (sy < size.height) {
                            drawLine(
                                color = HudScanline,
                                start = Offset(0f, sy),
                                end = Offset(size.width, sy),
                                strokeWidth = 1f
                            )
                            sy += scanStep
                        }

                        // 3. Animated vertical holographic sweep beam
                        val beamY = size.height * scanProgress
                        val trailHeight = 28.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (isBusy) Color(0x2476FF03) else Color(0x1476FF03),
                                    if (isBusy) Color(0x4576FF03) else Color(0x2476FF03)
                                ),
                                startY = (beamY - trailHeight).coerceAtLeast(0f),
                                endY = beamY
                            ),
                            topLeft = Offset(0f, (beamY - trailHeight).coerceAtLeast(0f)),
                            size = Size(size.width, trailHeight)
                        )
                        drawLine(
                            color = if (isBusy) Color(0x66AEEA00) else Color(0x3876FF03),
                            start = Offset(0f, beamY),
                            end = Offset(size.width, beamY),
                            strokeWidth = 1.2f
                        )

                        // 4. Foreground UI content
                        drawContent()

                        // 5. Glowing Sci-Fi corner reticle brackets on top
                        drawPath(
                            path = cornerPath,
                            color = HudLeafNeon,
                            style = Stroke(width = strokeBracket, cap = StrokeCap.Square)
                        )
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // ============================================================
                // 1. TOP TACTICAL HUD HEADER BAR
                // ============================================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Rotating Sci-Fi Orbital Reticle
                    SciFiOrbitalReticle(
                        angle = radarAngle,
                        isBusy = isBusy,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "ব্যাকএন্ড প্রসেস স্ট্যাটাস",
                        color = HudLeafChartreuse,
                        style = glowTitleStyle
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "// HUD.SYS",
                        color = HudLeafDim.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = ShareTechMonoFamily
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (sleepTimerSeconds > 0) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .background(HudChipFill, RoundedCornerShape(3.dp))
                                .border(0.8.dp, HudChipBorder, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "T-${sleepTimerSeconds / 60}m:${(sleepTimerSeconds % 60).toString().padStart(2, '0')}s",
                                color = HudLeafNeon,
                                fontSize = 9.sp,
                                fontFamily = ShareTechMonoFamily
                            )
                        }
                    }

                    // Live Animated Signal Spectrum Bars
                    SciFiTelemetrySpectrum(
                        phase = wavePhase,
                        isBusy = isBusy,
                        modifier = Modifier
                            .width(26.dp)
                            .height(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tactical State Pill ([ EXEC // রানিং ] or [ SYNC // সচল ])
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isBusy) Color(0xFF143318) else Color(0xFF0E2612),
                                shape = CutCornerShape(topEnd = 5.dp, bottomStart = 5.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isBusy) HudLeafChartreuse else HudLeafNeon,
                                shape = CutCornerShape(topEnd = 5.dp, bottomStart = 5.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = if (isBusy) "⟳ EXEC • রানিং" else "● SYNC • সচল",
                            color = if (isBusy) HudLeafChartreuse else HudLeafNeon,
                            fontSize = 9.sp,
                            fontFamily = ShareTechMonoFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ============================================================
                // 2. ACTIVE KERNEL / PIPELINE COMMAND BAR
                // ============================================================
                val parsedActive = remember(activeStatus) { parseHudLogMessage(activeStatus) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF102B15),
                                    Color(0xFF08180B),
                                    Color(0xFF051007)
                                )
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(0.8.dp, Color(0xFF235827), RoundedCornerShape(4.dp))
                ) {
                    // Glowing Left Power Bar
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(22.dp)
                            .background(if (isBusy) HudLeafChartreuse else HudLeafNeon)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "CMD»",
                        color = HudLeafChartreuse,
                        fontSize = 9.5.sp,
                        fontFamily = ShareTechMonoFamily,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "[${parsedActive.moduleTag}]",
                        color = HudLeafMint,
                        fontSize = 9.5.sp,
                        fontFamily = ShareTechMonoFamily,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = buildString {
                            append(parsedActive.body)
                            append(if (cursorVisible) " ▮" else "  ")
                        },
                        color = HudLeafNeon,
                        style = glowCommandStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 3.dp, horizontal = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ============================================================
                // 3. LIVE TELEMETRY MATRIX LOG STREAM
                // ============================================================
                LazyColumn(
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(2.5.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(logs.take(12), key = { _, item -> item.id }) { index, item ->
                        val parsed = remember(item.message) { parseHudLogMessage(item.message) }
                        val rowAlpha = (1f - (index * 0.055f)).coerceAtLeast(0.52f)
                        val isLatest = index == 0

                        val statusGlyph = when (item.level) {
                            BackendLogLevel.SUCCESS -> "◈"
                            BackendLogLevel.WORKING -> "⟳"
                            BackendLogLevel.WARNING -> "▲"
                            BackendLogLevel.INFO -> "›"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isLatest) {
                                        Modifier
                                            .background(Color(0x1476FF03), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    } else {
                                        Modifier.padding(horizontal = 3.dp)
                                    }
                                )
                        ) {
                            // Sequence Hex + Timestamp
                            Text(
                                text = "${item.id.toString(16).uppercase().padStart(2, '0')}│${item.time}",
                                color = HudLeafDim.copy(alpha = rowAlpha),
                                fontSize = 8.8.sp,
                                fontFamily = ShareTechMonoFamily
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            // Status Glyph
                            Text(
                                text = statusGlyph,
                                color = HudLeafChartreuse.copy(alpha = rowAlpha),
                                fontSize = 9.5.sp,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // Sci-Fi Subsystem Microchip Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = HudChipFill.copy(alpha = rowAlpha),
                                        shape = RoundedCornerShape(2.5.dp)
                                    )
                                    .border(
                                        width = 0.6.dp,
                                        color = HudChipBorder.copy(alpha = rowAlpha),
                                        shape = RoundedCornerShape(2.5.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = parsed.moduleTag.take(11),
                                    color = HudLeafMint.copy(alpha = rowAlpha),
                                    fontSize = 8.2.sp,
                                    fontFamily = ShareTechMonoFamily,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(5.dp))

                            // Detailed Programming Step Message in Bright Leaf Green
                            Text(
                                text = parsed.body,
                                color = (if (isLatest) HudLeafNeon else HudLeafPrimary).copy(alpha = rowAlpha),
                                fontSize = 9.6.sp,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // ============================================================
                // 4. BOTTOM DIAGNOSTIC TELEMETRY STRIP
                // ============================================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFF1A471D), RoundedCornerShape(2.dp))
                        .background(Color(0xFF040E06), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "ENG:$engineLabel",
                        color = HudLeafChartreuse.copy(alpha = 0.9f),
                        fontSize = 8.2.sp,
                        fontFamily = ShareTechMonoFamily
                    )
                    Text(
                        text = "  │  NODES:$channelCount  │  BUF.LOG:${logs.size}/45  │  AUDIO:100%",
                        color = HudLeafDim.copy(alpha = 0.85f),
                        fontSize = 8.2.sp,
                        fontFamily = ShareTechMonoFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Segmented Sci-Fi Power Ticks
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(6) { idx ->
                            val activeTick = if (isBusy) {
                                ((scanProgress * 6).toInt() == idx)
                            } else {
                                idx < 5
                            }
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(6.dp)
                                    .background(
                                        if (activeTick) HudLeafNeon else Color(0xFF183D1B)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SciFiOrbitalReticle(
    angle: Float,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 1.4.dp.toPx()
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer segmented orbital arcs
        rotate(degrees = angle, pivot = center) {
            drawArc(
                color = HudLeafNeon,
                startAngle = 0f,
                sweepAngle = 110f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = HudLeafChartreuse,
                startAngle = 180f,
                sweepAngle = 110f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Counter-rotating inner ring when busy
        rotate(degrees = -angle * 1.4f, pivot = center) {
            val innerR = radius * 0.62f
            drawArc(
                color = HudLeafDim,
                startAngle = 45f,
                sweepAngle = if (isBusy) 200f else 300f,
                useCenter = false,
                topLeft = Offset(center.x - innerR, center.y - innerR),
                size = Size(innerR * 2f, innerR * 2f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Glowing central reactor core dot
        drawCircle(
            color = if (isBusy) HudLeafChartreuse else HudLeafNeon,
            radius = radius * 0.24f,
            center = center
        )
    }
}

@Composable
private fun SciFiTelemetrySpectrum(
    phase: Float,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 7
        val spacing = 1.8.dp.toPx()
        val barWidth = ((size.width - spacing * (barCount - 1)) / barCount).coerceAtLeast(1.5f)

        for (i in 0 until barCount) {
            val freq = if (isBusy) 2.2f else 1.0f
            val wave = abs(sin(phase * freq + i * 0.75f))
            val minRatio = if (isBusy) 0.25f else 0.35f
            val heightRatio = (minRatio + wave * (1f - minRatio)).coerceIn(0.2f, 1f)
            val barH = size.height * heightRatio
            val left = i * (barWidth + spacing)
            val top = size.height - barH

            drawRect(
                color = if (i % 2 == 0) HudLeafNeon else HudLeafChartreuse,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH)
            )
        }
    }
}
