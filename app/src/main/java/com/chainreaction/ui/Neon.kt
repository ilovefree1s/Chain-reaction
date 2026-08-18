package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The neon system: the Rules and Settings pages drawn in code, in the menu's own
 * black / orange / blue, replacing the generated artwork they used to be. Text
 * stays text, so nothing is baked in — counts stay live, placeholders behave,
 * and the pages cost kilobytes instead of megabytes.
 */
val NeonBg = Color(0xFF05070C)
val NeonPanelBg = Color(0xFF070B14)
val NeonChipBg = Color(0xFF061022)
val NeonBlue = Color(0xFF3FA9FF)
val NeonBlueDeep = Color(0xFF1E6FFF)
val NeonIce = Color(0xFF4DC3FF)
val NeonOrange = Color(0xFFFF8A1E)
val NeonWhite = Color(0xFFF4F8FF)
val NeonBody = Color(0xFFA9B8D4)
val NeonDim = Color(0xFF5E6C88)

private val PanelShape = RoundedCornerShape(14.dp)

/** A glowing framed panel: blue-to-orange gradient border over near-black. */
fun Modifier.neonPanel(): Modifier = this
    .shadow(8.dp, PanelShape, ambientColor = NeonBlueDeep, spotColor = NeonBlueDeep)
    .clip(PanelShape)
    .background(NeonPanelBg)
    .border(
        2.dp,
        Brush.horizontalGradient(listOf(NeonBlueDeep, NeonBlue, NeonOrange)),
        PanelShape,
    )

/** Painted-header stand-in: back control on the left, italic display title centred. */
@Composable
fun NeonHeader(title: String, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹  Menu", color = NeonBody, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Text(
            title,
            color = NeonWhite,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Centred section label with the art's flanking rails: blue in, orange out. */
@Composable
fun NeonSectionLabel(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, NeonBlueDeep))),
        )
        Text(
            text.uppercase(),
            color = NeonIce,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(NeonOrange, Color.Transparent))),
        )
    }
}

enum class NeonIcon { GOLF, CARDS, SEVEN, DECK, RECYCLE, WHEEL, PEOPLE, SCALES, CAP }

/** Octagonal icon chip, blue on near-black, like the art's row markers. */
@Composable
fun NeonChip(icon: NeonIcon, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(14.dp)
    Box(
        modifier
            .size(56.dp)
            .clip(shape)
            .background(NeonChipBg)
            .border(2.dp, Brush.verticalGradient(listOf(NeonBlue, NeonBlueDeep)), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == NeonIcon.SEVEN) {
            Text("7", color = NeonIce, fontSize = 24.sp, fontWeight = FontWeight.Black)
        } else {
            androidx.compose.foundation.Canvas(Modifier.size(28.dp)) { drawNeonIcon(icon) }
        }
    }
}

private fun DrawScope.drawNeonIcon(icon: NeonIcon) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(NeonIce, Offset(w * x1, h * y1), Offset(w * x2, h * y2), stroke.width, StrokeCap.Round)

    when (icon) {
        NeonIcon.GOLF -> {
            line(0.35f, 0.10f, 0.35f, 0.88f)
            val flag = Path().apply {
                moveTo(w * 0.35f, h * 0.10f)
                lineTo(w * 0.80f, h * 0.26f)
                lineTo(w * 0.35f, h * 0.42f)
                close()
            }
            drawPath(flag, NeonIce)
            drawCircle(NeonIce, radius = w * 0.08f, center = Offset(w * 0.62f, h * 0.88f))
        }
        NeonIcon.CARDS -> {
            rotate(-10f, pivot = Offset(w * 0.38f, h * 0.5f)) {
                drawRoundRect(
                    NeonIce,
                    topLeft = Offset(w * 0.16f, h * 0.14f),
                    size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.66f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f),
                    style = stroke,
                )
            }
            rotate(10f, pivot = Offset(w * 0.66f, h * 0.55f)) {
                drawRoundRect(
                    NeonIce,
                    topLeft = Offset(w * 0.45f, h * 0.20f),
                    size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.66f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f),
                    style = stroke,
                )
            }
        }
        NeonIcon.DECK -> {
            // Three stacked card edges, isometric-ish.
            for (i in 0..2) {
                val y = 0.28f + i * 0.22f
                val p = Path().apply {
                    moveTo(w * 0.10f, h * y)
                    lineTo(w * 0.50f, h * (y - 0.16f))
                    lineTo(w * 0.90f, h * y)
                    if (i == 0) {
                        lineTo(w * 0.50f, h * (y + 0.16f))
                        close()
                    }
                }
                drawPath(p, NeonIce, style = stroke)
            }
        }
        NeonIcon.RECYCLE -> {
            drawArc(
                NeonIce,
                startAngle = -60f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(w * 0.12f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.76f),
                style = stroke,
            )
            val arrow = Path().apply {
                moveTo(w * 0.86f, h * 0.10f)
                lineTo(w * 0.92f, h * 0.38f)
                lineTo(w * 0.64f, h * 0.30f)
                close()
            }
            drawPath(arrow, NeonIce)
        }
        NeonIcon.WHEEL -> {
            // A spoked wheel — the Double Wheel's marker.
            drawCircle(NeonIce, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
            drawCircle(NeonIce, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f))
            line(0.5f, 0.5f, 0.5f, 0.10f)
            line(0.5f, 0.5f, 0.90f, 0.5f)
            line(0.5f, 0.5f, 0.5f, 0.90f)
            line(0.5f, 0.5f, 0.10f, 0.5f)
            line(0.5f, 0.5f, 0.78f, 0.22f)
            line(0.5f, 0.5f, 0.78f, 0.78f)
            line(0.5f, 0.5f, 0.22f, 0.78f)
            line(0.5f, 0.5f, 0.22f, 0.22f)
        }
        NeonIcon.PEOPLE -> {
            drawCircle(NeonIce, w * 0.13f, Offset(w * 0.32f, h * 0.30f), style = stroke)
            drawCircle(NeonIce, w * 0.13f, Offset(w * 0.68f, h * 0.30f), style = stroke)
            drawArc(
                NeonIce, 180f, 180f, false,
                topLeft = Offset(w * 0.08f, h * 0.55f),
                size = androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.60f),
                style = stroke,
            )
            drawArc(
                NeonIce, 180f, 180f, false,
                topLeft = Offset(w * 0.44f, h * 0.55f),
                size = androidx.compose.ui.geometry.Size(w * 0.48f, h * 0.60f),
                style = stroke,
            )
        }
        NeonIcon.SCALES -> {
            line(0.50f, 0.10f, 0.50f, 0.82f)
            line(0.28f, 0.88f, 0.72f, 0.88f)
            line(0.12f, 0.22f, 0.88f, 0.22f)
            val panL = Path().apply {
                moveTo(w * 0.12f, h * 0.22f)
                lineTo(w * 0.02f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.50f)
                close()
            }
            val panR = Path().apply {
                moveTo(w * 0.88f, h * 0.22f)
                lineTo(w * 0.78f, h * 0.50f)
                lineTo(w * 0.98f, h * 0.50f)
                close()
            }
            drawPath(panL, NeonIce, style = stroke)
            drawPath(panR, NeonIce, style = stroke)
        }
        NeonIcon.CAP -> {
            val board = Path().apply {
                moveTo(w * 0.06f, h * 0.38f)
                lineTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.94f, h * 0.38f)
                lineTo(w * 0.50f, h * 0.60f)
                close()
            }
            drawPath(board, NeonIce, style = stroke)
            drawArc(
                NeonIce, 0f, 180f, false,
                topLeft = Offset(w * 0.24f, h * 0.30f),
                size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.58f),
                style = stroke,
            )
            line(0.94f, 0.38f, 0.94f, 0.66f)
        }
        NeonIcon.SEVEN -> Unit // rendered as text in the chip
    }
}

/** Orange selected-state variant of [neonPanel] — for the picked row in a list. */
fun Modifier.neonPanelOrange(): Modifier = this
    .shadow(8.dp, PanelShape, ambientColor = NeonOrange, spotColor = NeonOrange)
    .clip(PanelShape)
    .background(Color(0xFF130C05))
    .border(
        2.dp,
        Brush.horizontalGradient(listOf(NeonOrange, Color(0xFFF7791E))),
        PanelShape,
    )

/** Full-width secondary action: dark panel, quiet border. */
@Composable
fun NeonQuietButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(TapTarget)
            .clip(shape)
            .background(NeonPanelBg)
            .border(2.dp, Color(0xFF22304A), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = NeonBody,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
        )
    }
}

/** A two-state choice chip row member: orange when chosen, quiet when not. */
@Composable
fun NeonToggle(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(TapTarget)
            .then(
                if (on) {
                    Modifier
                        .shadow(8.dp, shape, ambientColor = NeonOrange, spotColor = NeonOrange)
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color(0xFFFF9A2E), Color(0xFFF7791E))))
                } else {
                    Modifier
                        .clip(shape)
                        .background(NeonPanelBg)
                        .border(2.dp, Color(0xFF22304A), shape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = if (on) Color.White else NeonBody,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

/** Blue sibling of [NeonBigButton], for the wheel's spins — playful, not primary. */
@Composable
fun NeonBlueButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(TapTarget)
            .shadow(10.dp, shape, ambientColor = NeonBlueDeep, spotColor = NeonBlueDeep)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(NeonBlue, NeonBlueDeep)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 2.sp,
        )
    }
}

/** Single-line text entry in a framed panel: dim placeholder, orange caret. */
@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(TapTarget)
            .neonPanel()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = NeonDim, fontSize = 18.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = NeonWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(NeonOrange),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Octagonal ME toggle; orange when it's you. */
@Composable
fun NeonMeChip(selected: Boolean, onClick: () -> Unit) {
    val shape = CutCornerShape(14.dp)
    Box(
        Modifier
            .size(TapTarget)
            .clip(shape)
            .background(if (selected) NeonOrange.copy(alpha = 0.18f) else NeonChipBg)
            .border(
                2.dp,
                if (selected) {
                    Brush.verticalGradient(listOf(NeonOrange, Color(0xFFF7791E)))
                } else {
                    Brush.verticalGradient(listOf(NeonBlue, NeonBlueDeep))
                },
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "ME",
            color = if (selected) NeonOrange else NeonBody,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
    }
}

/** Self-sizing framed action for the small verbs — add a player, remove one. */
@Composable
fun NeonSmallAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(TapTarget)
            .neonPanel()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = NeonWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** The full-width primary action: live orange when armed, dark and quiet when not. */
@Composable
fun NeonBigButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(TapTarget)
            .then(
                if (enabled) {
                    Modifier
                        .shadow(10.dp, shape, ambientColor = NeonOrange, spotColor = NeonOrange)
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color(0xFFFF9A2E), Color(0xFFF7791E))))
                } else {
                    Modifier
                        .clip(shape)
                        .background(NeonPanelBg)
                        .border(2.dp, Color(0xFF22304A), shape)
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = if (enabled) Color.White else NeonDim,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 2.sp,
        )
    }
}
