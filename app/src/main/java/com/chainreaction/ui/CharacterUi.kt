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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.Character

/*
 * Characters are personalisation and nothing else — a face and a name beside a player
 * on the scorecard. No abilities, no modifiers, nothing the group has to remember.
 *
 * Artwork is drop-in exactly like the card faces: character_01 … in drawable-nodpi,
 * looked up by name at runtime. A character with no art yet draws as its own colour
 * with its initial, so the roster is playable before the art lands.
 */

/** Columns in the picker. Three keeps the faces big enough to tell apart at arm's length. */
private const val PICKER_COLUMNS = 3

/**
 * The character with [id], or null — which covers both "nobody picked one" and a saved
 * id that is no longer in the roster. Rosters are a handful of entries, so a scan is free.
 */
fun List<Character>.character(id: Int?): Character? =
    id?.let { wanted -> firstOrNull { it.id == wanted } }

/**
 * A character's face. [character] null draws the empty slot — a quiet ring with a "+",
 * which is what a player who hasn't picked shows on Setup.
 */
@Composable
fun CharacterBadge(
    character: Character?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    selected: Boolean = false,
) {
    val context = LocalContext.current
    val artId = remember(character?.id) {
        character?.let {
            context.resources.getIdentifier(it.artName, "drawable", context.packageName)
        } ?: 0
    }
    val tint = character?.let { Color(it.color.toInt()) } ?: NeonDim
    val fill = when {
        artId != 0 -> NeonPanelBg
        character != null -> tint
        else -> tint.copy(alpha = 0.12f)
    }

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(if (selected) 2.dp else 1.dp, if (selected) NeonOrange else tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            artId != 0 -> Image(
                painter = painterResource(artId),
                contentDescription = character?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            character != null -> Text(
                character.initial,
                // On a saturated badge the near-black reads better than white does.
                color = NeonBg,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Black,
            )
            else -> Text(
                "+",
                color = NeonDim,
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

/**
 * A player's face plus the tap target that changes it. Used on Setup and in Settings,
 * where a character is being chosen rather than just displayed.
 */
@Composable
fun CharacterPickerButton(
    character: Character?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CharacterBadge(
        character = character,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        size = 48.dp,
    )
}

/**
 * Full-screen character select. One tap picks and closes — this sits between a player
 * and starting a round, so it must not turn into a form.
 *
 * [takenBy] maps a character id to the player already using it. Those are shown but not
 * pickable: the whole point is telling everyone apart on one scorecard.
 */
@Composable
fun CharacterSheet(
    characters: List<Character>,
    selected: Int?,
    takenBy: Map<Int, String>,
    onPick: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(NeonBg)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            NeonHeader("CHARACTER", backLabel = "Back", onBack = onDismiss)

            Spacer(Modifier.height(16.dp))

            if (characters.isEmpty()) {
                Text(
                    "No characters in the roster yet — they live in shared/characters.json.",
                    color = NeonDim,
                    fontSize = 16.sp,
                )
            }

            characters.chunked(PICKER_COLUMNS).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { character ->
                        val taken = takenBy[character.id]
                        val mine = character.id == selected
                        val pickable = mine || taken == null
                        Column(
                            Modifier
                                .weight(1f)
                                .alpha(if (pickable) 1f else 0.35f)
                                .clickable(enabled = pickable) {
                                    // Tapping your own pick clears it, so a character can
                                    // be given up without a separate control.
                                    onPick(if (mine) null else character.id)
                                    onDismiss()
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CharacterBadge(character, size = 88.dp, selected = mine)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                character.name,
                                color = if (mine) NeonOrange else NeonWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp,
                            )
                            if (taken != null && !mine) {
                                Text(
                                    taken,
                                    color = NeonDim,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    // Keeps a short last row aligned with the columns above it.
                    repeat(PICKER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(10.dp))
            NeonQuietButton(if (selected == null) "Close" else "Play without a character") {
                if (selected != null) onPick(null)
                onDismiss()
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
