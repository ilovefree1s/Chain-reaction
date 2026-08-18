package com.chainreaction.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.GameCard

/** Minimum comfortable tap target, gloved and mid-round. Well past the 44pt floor. */
val TapTarget = 56.dp

/**
 * A full-bleed card in the neon frame. If artwork named `card_NN` exists in the
 * drawables it IS the card face; otherwise the interim text tile renders — kind
 * and timing tags, name, full effect text. Art is looked up by name so faces
 * can land one at a time with zero code changes.
 * [actions] hangs Play / Discard off the bottom when the card is in hand.
 */
@Composable
fun CardTile(
    card: GameCard,
    modifier: Modifier = Modifier,
    /** Tap anywhere on the tile (buttons aside) — used to open the full face. */
    onTap: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val artId = remember(card.id) {
        context.resources.getIdentifier(
            "card_%02d".format(card.id),
            "drawable",
            context.packageName,
        )
    }
    val tappable = if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier
    if (artId != 0) {
        Column(
            modifier
                .fillMaxWidth()
                .neonPanel()
                .then(tappable),
        ) {
            Image(
                painter = painterResource(artId),
                // The art carries the face; the description keeps it readable.
                contentDescription = "${card.name}. ${card.text}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
            if (actions != null) {
                Box(Modifier.padding(12.dp)) { actions() }
            }
        }
        return
    }

    Column(
        modifier
            .fillMaxWidth()
            .neonPanel()
            .then(tappable)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Tag(card.kind.label.uppercase())
            Tag(card.timing.uppercase())
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = card.name,
            color = NeonWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = card.text,
            color = NeonBody,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
        if (actions != null) {
            Spacer(Modifier.height(14.dp))
            actions()
        }
    }
}

@Composable
fun Tag(text: String) {
    Text(
        text = text,
        color = NeonIce,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NeonChipBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Big square −/+ button. */
@Composable
fun StepperButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(TapTarget)
            .clip(shape)
            .background(NeonChipBg)
            .border(2.dp, Color(0xFF22304A), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = if (enabled) NeonWhite else NeonDim.copy(alpha = 0.5f),
        )
    }
}
