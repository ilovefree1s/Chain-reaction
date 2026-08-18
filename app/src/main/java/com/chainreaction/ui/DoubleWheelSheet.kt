package com.chainreaction.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.CardDeck
import kotlin.random.Random

private enum class Stage { IDLE, NAME_SPIN, NAME_DONE, CARD_SPIN, DONE }

/** One long decelerating sweep — most of the drama is in the last quarter turn. */
private const val SPIN_MS = 3600
private val SpinEasing = CubicBezierEasing(0.12f, 0.8f, 0.08f, 1f)

/** Ten real cards ride the effect wheel; the winner is drawn from the full pool. */
private const val CARD_SEGMENTS = 10

/**
 * Card #48's mechanic, staged on actual wheels. The name spins first: the group
 * learns who's exempt before anyone knows what they're exempt from. The name
 * wheel carries Player 1..N twice over so it reads as a full wheel; the effect
 * wheel shows ten real cards, but the landing card was drawn from the whole
 * pool up front — every card keeps its full-pool odds. The wheel is theatre.
 */
@Composable
fun DoubleWheelSheet(players: List<String>, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Let the sheet paint under the status bar so it reads as a full screen,
            // not a black-topped panel. Insets are handled by safeDrawingPadding below.
            decorFitsSystemWindows = false,
        ),
    ) {
        var stage by remember { mutableStateOf(Stage.IDLE) }
        var namePlayer by remember { mutableIntStateOf(-1) } // which player landed
        var nameSeg by remember { mutableIntStateOf(-1) } // which segment landed

        // Each player twice around the wheel, so even 3 players fill it out.
        val nameLabels = remember { List(players.size * 2) { "Player ${it % players.size + 1}" } }

        // Decided at open, revealed by the spin.
        val winningCard = remember { CardDeck.WHEEL_POOL.random() }
        val ringCards = remember {
            val others = (CardDeck.WHEEL_POOL - winningCard).shuffled().take(CARD_SEGMENTS - 1)
            (others + winningCard).shuffled()
        }
        val cardIdx = remember { ringCards.indexOf(winningCard) }

        val nameRot = remember { Animatable(0f) }
        val cardRot = remember { Animatable(0f) }

        LaunchedEffect(stage) {
            if (stage == Stage.NAME_SPIN) {
                namePlayer = players.indices.random()
                // The player owns two opposite segments — land on either.
                nameSeg = namePlayer + players.size * Random.nextInt(2)
                nameRot.animateTo(
                    targetRotation(nameLabels.size, nameSeg),
                    tween(SPIN_MS, easing = SpinEasing),
                )
                stage = Stage.NAME_DONE
            }
            if (stage == Stage.CARD_SPIN) {
                cardRot.animateTo(
                    targetRotation(ringCards.size, cardIdx),
                    tween(SPIN_MS, easing = SpinEasing),
                )
                stage = Stage.DONE
            }
        }

        val nameSettled = stage == Stage.NAME_DONE || stage == Stage.CARD_SPIN || stage == Stage.DONE

        Box(
            Modifier
                .fillMaxSize()
                .background(NeonBg)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "DOUBLE WHEEL",
                    color = NeonIce,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 28.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "The name first, then the effect. The named player is the only one exempt.",
                    color = NeonBody,
                    fontSize = 16.sp,
                )

                // ---- stage 1: the name ----
                NeonSectionLabel("1 · Who's exempt")
                NeonWheel(
                    labels = nameLabels,
                    rotation = nameRot.value,
                    winner = if (nameSettled) nameSeg else null,
                    labelSize = 15.sp,
                )
                Spacer(Modifier.height(12.dp))

                if (stage == Stage.IDLE) {
                    NeonBlueButton("Spin the name") { stage = Stage.NAME_SPIN }
                }
                if (nameSettled) {
                    Text(
                        "${players[namePlayer]} is exempt",
                        color = NeonOrange,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // ---- stage 2: the effect ----
                if (nameSettled) {
                    NeonSectionLabel("2 · The effect")
                    NeonWheel(
                        labels = ringCards.map { it.name },
                        rotation = cardRot.value,
                        winner = if (stage == Stage.DONE) cardIdx else null,
                        labelSize = 11.sp,
                    )
                    Spacer(Modifier.height(12.dp))

                    if (stage == Stage.NAME_DONE) {
                        NeonBlueButton("Spin the effect") { stage = Stage.CARD_SPIN }
                    }
                }

                if (stage == Stage.DONE) {
                    CardTile(winningCard)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${players[namePlayer]} sits this one out. Everyone else — including whoever " +
                            "played the card — carries out the effect.",
                        color = NeonWhite,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(12.dp))
                NeonQuietButton(if (stage == Stage.DONE) "Done" else "Close", onClick = onDismiss)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Five full turns, then park the winning segment under the top pointer — landing
 * a little off-centre so the stop doesn't look rigged.
 */
private fun targetRotation(segments: Int, index: Int): Float {
    val seg = 360f / segments
    val mid = index * seg + seg / 2f
    val jitter = (Random.nextFloat() - 0.5f) * 0.6f * seg
    return 5 * 360f + (360f - mid) + jitter
}

/**
 * The wheel itself: alternating near-black wedges, labels laid along each spoke,
 * a gradient rim and a fixed orange pointer at twelve o'clock. The whole canvas
 * rotates; the pointer is a separate overlay, so it stays put.
 */
@Composable
private fun NeonWheel(
    labels: List<String>,
    rotation: Float,
    winner: Int?,
    labelSize: TextUnit,
) {
    val textPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            isFakeBoldText = true
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(6.dp)
                .graphicsLayer { rotationZ = rotation },
        ) {
            val n = labels.size
            val seg = 360f / n
            val r = size.minDimension / 2f
            val c = center
            val wedgeTopLeft = Offset(c.x - r, c.y - r)
            val wedgeSize = Size(2 * r, 2 * r)

            labels.forEachIndexed { i, label ->
                val lit = i == winner
                val start = i * seg - 90f
                drawArc(
                    color = if (lit) {
                        Color(0xFF2A1503)
                    } else if (i % 2 == 0) {
                        Color(0xFF0B1424)
                    } else {
                        NeonChipBg
                    },
                    startAngle = start,
                    sweepAngle = seg,
                    useCenter = true,
                    topLeft = wedgeTopLeft,
                    size = wedgeSize,
                )
                drawArc(
                    color = if (lit) NeonOrange else Color(0xFF22304A),
                    startAngle = start,
                    sweepAngle = seg,
                    useCenter = true,
                    topLeft = wedgeTopLeft,
                    size = wedgeSize,
                    style = Stroke(2.dp.toPx()),
                )

                // Label along the spoke, reading outward — upside down on the left
                // half, exactly like a real prize wheel.
                val short = if (label.length > 15) label.take(14) + "…" else label
                textPaint.textSize = labelSize.toPx()
                textPaint.color = (if (lit) NeonOrange else NeonWhite).toArgb()
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate(start + seg / 2f, c.x, c.y)
                    drawText(short, c.x + r * 0.28f, c.y + textPaint.textSize * 0.35f, textPaint)
                    restore()
                }
            }

            // Gradient rim, then the hub.
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(NeonBlueDeep, NeonBlue, NeonOrange, NeonBlueDeep),
                ),
                radius = r - 1.dp.toPx(),
                center = c,
                style = Stroke(4.dp.toPx()),
            )
            drawCircle(NeonChipBg, r * 0.10f, c)
            drawCircle(NeonIce, r * 0.10f, c, style = Stroke(2.dp.toPx()))
        }

        // The pointer — fixed while the wheel turns beneath it.
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(6.dp),
        ) {
            val c = center
            val w = 12.dp.toPx()
            val h = 20.dp.toPx()
            val tip = Path().apply {
                moveTo(c.x - w, 0f)
                lineTo(c.x + w, 0f)
                lineTo(c.x, h)
                close()
            }
            drawPath(tip, NeonOrange)
        }
    }
}
