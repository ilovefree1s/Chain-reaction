package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chainreaction.data.CardDeck
import com.chainreaction.data.CardKind
import com.chainreaction.data.Rules
import com.chainreaction.ui.theme.Attack
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.PanelRaised
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.React
import com.chainreaction.ui.theme.Sage
import com.chainreaction.ui.theme.SelfCard
import com.chainreaction.ui.theme.color

private val houseRules = listOf(
    "Stroke play" to "Lowest total wins.",
    "Starting hand" to "4 cards, dealt at the start of the round.",
    "Hand cap" to "7 cards. You cannot draw past it — discard first.",
    "Your own deck" to
        "Every player shuffles their own 52-card deck. Duplicates across players are " +
        "expected and fine. This phone only tracks your deck.",
    "Discard" to
        "Played and discarded cards go to your own discard pile. If your deck runs out, " +
        "the discard is reshuffled back in.",
    "Two per player, per hole" to
        "At most ${Rules.MAX_CARDS_ON_ONE_PLAYER_PER_HOLE} cards may be played on any one " +
        "player per hole. The app does not enforce this — track it yourselves.",
    "The app doesn't referee" to
        "It deals cards and keeps score. It never applies card effects or stroke penalties. " +
        "Cards that rewrite scores get entered by hand with the ± steppers.",
)

private val drawTable = listOf(
    "Best score, or tied for best" to "0",
    "Middle of the pack" to "1",
    "Last, or tied for last" to "2",
    "Everyone tied" to "1 each",
)

/** The house rules, the draw table and the colour key. Shared by the menu and the round tab. */
fun LazyListScope.houseRulesItems() {
    item {
        Spacer(Modifier.height(12.dp))
        SectionLabel("How it works")
    }

    items(houseRules) { (title, body) ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Panel)
                .padding(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = OffWhite)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = Sage)
        }
    }

    item {
        Spacer(Modifier.height(8.dp))
        SectionLabel("Cards drawn at the end of a hole")
    }

    items(drawTable) { (finish, cards) ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(finish, style = MaterialTheme.typography.bodyLarge, color = Sage)
            Text(cards, style = MaterialTheme.typography.titleLarge, color = OffWhite)
        }
    }

    item {
        Spacer(Modifier.height(8.dp))
        SectionLabel("What the colours mean")
    }

    items(CardKind.entries.toList()) { kind ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Tag(kind.label.uppercase(), fg = Pine, bg = kind.color)
            Text(
                when (kind) {
                    CardKind.ATTACK -> "Played on an opponent"
                    CardKind.SELF -> "Benefits you"
                    CardKind.DUAL -> "Self-help or attack, depending on the target"
                    CardKind.REACT -> "Played in response to another card"
                    CardKind.GROUP -> "Affects everyone"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Sage,
            )
        }
    }
}

/** All 52 cards, grouped by timing. */
fun LazyListScope.cardLibraryItems() {
    Rules.TIMINGS.forEach { timing ->
        val group = CardDeck.ALL.filter { it.timing == timing }
        if (group.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionLabel("$timing · ${group.size}")
            }
            items(group, key = { it.id }) { card -> CardTile(card) }
        }
    }
}

/** Menu destination: the rules on their own. */
@Composable
fun RulesScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        houseRulesItems()
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** Menu destination: the full deck to browse before a round. */
@Composable
fun CardsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            SectionLabel("All ${CardDeck.ALL.size} cards")
        }
        cardLibraryItems()
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** The in-round Rules tab: everything above, plus the round-specific actions. */
@Composable
fun RoundRulesScreen(
    holeCount: Int,
    pars: List<Int>,
    playingCourse: String?,
    modifier: Modifier = Modifier,
    onSaveCourse: (String) -> Unit,
    onEndRound: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    // Pre-filled with the course being played, so correcting its pars is a one-tap save.
    var courseName by remember { mutableStateOf(playingCourse.orEmpty()) }
    var savedAs by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        houseRulesItems()
        cardLibraryItems()

        // You usually learn a course's real pars by playing it. Capture them here,
        // once, and the next round on this course starts already set up.
        item {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Save this course")
            Spacer(Modifier.height(4.dp))
            Text(
                "Keeps these $holeCount pars (par ${pars.sum()}) for next time.",
                color = Sage,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it; savedAs = null },
                singleLine = true,
                placeholder = { Text("Course name", color = Sage.copy(alpha = 0.6f)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel,
                    focusedBorderColor = SelfCard,
                    unfocusedBorderColor = PanelRaised,
                    cursorColor = SelfCard,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            BigButton(
                text = "Save course",
                fill = SelfCard,
                onFill = Pine,
                enabled = courseName.isNotBlank(),
                onClick = {
                    val name = courseName.trim()
                    onSaveCourse(name)
                    savedAs = name
                    courseName = ""
                },
            )
            savedAs?.let {
                Text(
                    "Saved as \"$it\".",
                    color = React,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            BigButton(
                text = "End round",
                fill = Panel,
                onFill = Attack,
                onClick = { confirming = true },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = Panel,
            titleContentColor = OffWhite,
            textContentColor = Sage,
            title = { Text("End the round?") },
            text = {
                Text("Scores, your hand, deck and discard are all cleared. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onEndRound()
                }) { Text("End round", color = Attack) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Keep playing", color = Sage) }
            },
        )
    }
}
