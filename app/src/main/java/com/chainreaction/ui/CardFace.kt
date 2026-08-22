package com.chainreaction.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.CardDeck
import com.chainreaction.data.CardKind
import com.chainreaction.data.GameCard

/** The floor for the shrink-to-fit body text: below this it stops being readable. */
private val MIN_BODY_SIZE = 11.sp

/**
 * The full card face, drawn entirely from the card's own data — the poster
 * version of a tile. Because it's code, every face exists automatically and
 * can never drift from the spec. If real artwork lands for a card, the
 * enlarged view shows that instead (see [CardFaceDialog]).
 */
@Composable
fun CardFace(card: GameCard, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            // A real card: 2.5 x 3.5. The height comes from the width.
            .aspectRatio(0.72f)
            .shadow(10.dp, shape, ambientColor = NeonBlueDeep, spotColor = NeonBlueDeep)
            .clip(shape)
            .background(NeonPanelBg)
            .border(
                3.dp,
                Brush.linearGradient(listOf(NeonBlueDeep, NeonBlue, NeonOrange)),
                shape,
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Timing banner between the rails.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, NeonBlueDeep))),
            )
            Text(
                card.timing.uppercase(),
                color = NeonIce,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(NeonOrange, Color.Transparent))),
            )
        }

        Spacer(Modifier.height(16.dp))
        // Long names shrink rather than wrap into a wall.
        val titleSize = when {
            card.name.length > 24 -> 24.sp
            card.name.length > 13 -> 28.sp
            else -> 34.sp
        }
        Text(
            card.name.uppercase(),
            color = NeonWhite,
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            lineHeight = titleSize * 1.15f,
        )

        Spacer(Modifier.height(20.dp))
        KindEmblem(card.kind)

        // The card is a fixed 2.5 x 3.5, so the words have to fit the space rather
        // than the space stretching to the words. Start at the size the length
        // suggests, then step down until it fits — a wordy card used to run on
        // underneath the footer and lose its last line.
        val startSize = when {
            card.text.length > 240 -> 16.sp
            card.text.length > 160 -> 17.sp
            else -> 19.sp
        }
        var bodySize by remember(card.id) { mutableStateOf(startSize) }
        var fitted by remember(card.id) { mutableStateOf(false) }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                card.text,
                color = NeonBody,
                fontSize = bodySize,
                lineHeight = bodySize * 1.4f,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    // Held back until it fits, so the step-down never flickers on screen.
                    .drawWithContent { if (fitted) drawContent() },
                onTextLayout = { result ->
                    if (!fitted) {
                        if (result.hasVisualOverflow && bodySize > MIN_BODY_SIZE) {
                            bodySize *= 0.94f
                        } else {
                            fitted = true
                        }
                    }
                },
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Plain text, not a filled chip: on a card this size the chip reads as a
            // solid block sitting on top of the explanation's last line.
            Text(
                card.kind.label.uppercase(),
                color = NeonIce,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                "${card.id} / ${CardDeck.ALL.size}",
                color = NeonDim,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Big octagon emblem carrying the kind's glyph. */
@Composable
private fun KindEmblem(kind: CardKind) {
    val shape = CutCornerShape(30.dp)
    Box(
        Modifier
            .size(124.dp)
            .clip(shape)
            .background(NeonChipBg)
            .border(3.dp, Brush.verticalGradient(listOf(NeonBlue, NeonBlueDeep)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(64.dp)) { drawKindGlyph(kind) }
    }
}

private fun DrawScope.drawKindGlyph(kind: CardKind) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.08f)

    fun person(cx: Float, cy: Float, s: Float, color: Color) {
        drawCircle(color, radius = w * 0.11f * s, center = Offset(w * cx, h * (cy - 0.13f * s)), style = stroke)
        drawArc(
            color, 180f, 180f, false,
            topLeft = Offset(w * (cx - 0.19f * s), h * (cy + 0.02f * s)),
            size = Size(w * 0.38f * s, h * 0.34f * s),
            style = stroke,
        )
    }

    when (kind) {
        // A crosshair: this card is aimed at somebody.
        CardKind.ATTACK -> {
            drawCircle(NeonOrange, radius = w * 0.30f, center = center, style = stroke)
            drawCircle(NeonOrange, radius = w * 0.05f, center = center)
            listOf(
                Offset(0.5f, 0.04f) to Offset(0.5f, 0.24f),
                Offset(0.5f, 0.76f) to Offset(0.5f, 0.96f),
                Offset(0.04f, 0.5f) to Offset(0.24f, 0.5f),
                Offset(0.76f, 0.5f) to Offset(0.96f, 0.5f),
            ).forEach { (a, b) ->
                drawLine(NeonOrange, Offset(w * a.x, h * a.y), Offset(w * b.x, h * b.y), stroke.width)
            }
        }
        CardKind.SELF -> person(0.5f, 0.55f, 1.6f, NeonIce)
        // One of ours, one of theirs.
        CardKind.DUAL -> {
            person(0.32f, 0.55f, 1.15f, NeonIce)
            person(0.68f, 0.55f, 1.15f, NeonOrange)
        }
        // A reaction bolt.
        CardKind.REACT -> {
            val bolt = Path().apply {
                moveTo(w * 0.58f, h * 0.04f)
                lineTo(w * 0.24f, h * 0.56f)
                lineTo(w * 0.46f, h * 0.56f)
                lineTo(w * 0.40f, h * 0.96f)
                lineTo(w * 0.76f, h * 0.42f)
                lineTo(w * 0.54f, h * 0.42f)
                close()
            }
            drawPath(bolt, NeonIce)
        }
        CardKind.GROUP -> {
            person(0.26f, 0.52f, 1.0f, NeonIce)
            person(0.74f, 0.52f, 1.0f, NeonIce)
            person(0.5f, 0.66f, 1.15f, NeonOrange)
        }
        // A wrapped present: this one is aimed at somebody, but as a favour.
        CardKind.GIFT -> {
            drawRect(
                NeonIce,
                topLeft = Offset(w * 0.14f, h * 0.36f),
                size = Size(w * 0.72f, h * 0.50f),
                style = stroke,
            )
            drawLine(
                NeonOrange,
                Offset(w * 0.5f, h * 0.36f),
                Offset(w * 0.5f, h * 0.86f),
                stroke.width,
            )
            drawCircle(NeonOrange, radius = w * 0.11f, center = Offset(w * 0.37f, h * 0.24f), style = stroke)
            drawCircle(NeonOrange, radius = w * 0.11f, center = Offset(w * 0.63f, h * 0.24f), style = stroke)
        }
    }
}

/** Real artwork when it exists, the code-drawn face otherwise. */
@Composable
private fun CardFaceOrArt(card: GameCard) {
    val context = LocalContext.current
    val artId = remember(card.id) {
        context.resources.getIdentifier(
            "card_%02d".format(card.id),
            "drawable",
            context.packageName,
        )
    }
    if (artId != 0) {
        Image(
            painter = painterResource(artId),
            contentDescription = "${card.name}. ${card.text}",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp)),
            contentScale = ContentScale.FillWidth,
        )
    } else {
        CardFace(card)
    }
}

/**
 * Full-screen viewer, paged: swipe left and right to walk the whole list
 * (usually the hand). Swipes turn whole pages — nothing of the neighbouring
 * cards peeks out, so showing the table one card still shows exactly one.
 * [onResolve] non-null adds Play / Discard (with the usual confirmation),
 * acting on whichever card is in front.
 */
@Composable
fun CardFaceDialog(
    cardIds: List<Int>,
    initialCardId: Int,
    onDismiss: () -> Unit,
    /**
     * Both Play and Discard send the card to the discard pile — [played] is the only
     * thing that tells them apart, and the stats need it. [target] is who it landed on,
     * null for a discard or a card that isn't aimed at anybody.
     */
    onResolve: ((id: Int, played: Boolean, target: String?) -> Unit)? = null,
    /** The table, so a card aimed at someone can ask who. Empty skips the question. */
    players: List<String> = emptyList(),
    meIndex: Int = 0,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val pagerState = rememberPagerState(
            initialPage = cardIds.indexOf(initialCardId).coerceAtLeast(0),
        ) { cardIds.size }
        var confirmingDiscard by remember { mutableStateOf(false) }
        var choosingTarget by remember { mutableStateOf(false) }
        // A half-typed discard shouldn't follow you to the next card.
        LaunchedEffect(pagerState.currentPage) {
            confirmingDiscard = false
            choosingTarget = false
        }
        val card = CardDeck.card(cardIds[pagerState.currentPage])

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
                // Centred: the empty space splits above and below the card.
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(16.dp))
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 24.dp,
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    CardFaceOrArt(CardDeck.card(cardIds[page]))
                }
                if (cardIds.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "‹   ${pagerState.currentPage + 1} / ${cardIds.size}   ›",
                        color = NeonDim,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(16.dp))

                val opponents = remember(players, meIndex) {
                    players.filterIndexed { i, name -> i != meIndex && name.isNotBlank() }
                }

                if (onResolve != null) {
                    if (choosingTarget) {
                        // Who it landed on. Only asked for cards that are aimed at
                        // somebody — self and group cards have no one to name.
                        Text(
                            "Play ${card.name} on…",
                            color = NeonWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        opponents.forEach { name ->
                            NeonBigButton(name, enabled = true) {
                                onResolve(card.id, true, name)
                                onDismiss()
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        NeonQuietButton("Never mind") { choosingTarget = false }
                    } else if (confirmingDiscard) {
                        Text(
                            "Discard ${card.name}?",
                            color = NeonWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                NeonQuietButton("Keep it") { confirmingDiscard = false }
                            }
                            Box(Modifier.weight(1f)) {
                                NeonBigButton("Discard", enabled = true) {
                                    onResolve(card.id, false, null)
                                    onDismiss()
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                NeonBigButton("Play", enabled = true) {
                                    if (card.kind.aimedAtSomeone && opponents.isNotEmpty()) {
                                        choosingTarget = true
                                    } else {
                                        onResolve(card.id, true, null)
                                        onDismiss()
                                    }
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                NeonQuietButton("Discard") { confirmingDiscard = true }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                NeonQuietButton("Close", onClick = onDismiss)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
