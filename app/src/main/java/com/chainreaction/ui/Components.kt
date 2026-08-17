package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.GameCard
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.PanelRaised
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.Sage
import com.chainreaction.ui.theme.color

/** Minimum comfortable tap target, gloved and mid-round. Well past the 44pt floor. */
val TapTarget = 56.dp

/**
 * A full-bleed card: timing tag, function label, name, full effect text.
 * [actions] hangs Play / Discard off the bottom when the card is in hand.
 */
@Composable
fun CardTile(
    card: GameCard,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    val accent = card.kind.color
    Surface(
        color = Panel,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Colour stripe — the fastest read of what kind of card this is.
            Box(
                Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Tag(text = card.kind.label.uppercase(), fg = Pine, bg = accent)
                    Tag(text = card.timing.uppercase(), fg = Sage, bg = PanelRaised)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = OffWhite,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = card.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Sage,
                )
                if (actions != null) {
                    Spacer(Modifier.height(14.dp))
                    actions()
                }
            }
        }
    }
}

@Composable
fun Tag(text: String, fg: Color, bg: Color) {
    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
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
    Box(
        modifier = Modifier
            .size(TapTarget)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) PanelRaised else Panel)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = if (enabled) OffWhite else Sage.copy(alpha = 0.4f),
        )
    }
}

/** A full-width action button — the primary verb on a screen. */
@Composable
fun BigButton(
    text: String,
    fill: Color,
    onFill: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TapTarget)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) fill else PanelRaised)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) onFill else Sage.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Sage,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        modifier = modifier,
    )
}
