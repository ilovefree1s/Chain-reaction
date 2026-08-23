package com.chainreaction.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Character
import com.chainreaction.data.Course
import com.chainreaction.data.Rules

/** No character picked. A sentinel rather than null so the list survives rememberSaveable. */
internal const val NO_CHARACTER = -1

/** You are always player 1. Nothing in Setup asks, so nothing can get it wrong. */
private const val ME_INDEX = 0

@Composable
fun SetupScreen(
    courses: List<Course>,
    canDelete: (Course) -> Boolean,
    /** Your own name and face, from Settings. You are always player 1. */
    myName: String,
    myCharacter: Int?,
    characters: List<Character>,
    modifier: Modifier = Modifier,
    onSaveCourse: (Course) -> Unit,
    onDeleteCourse: (String) -> Unit,
    /** Typing over the name beside a picked face renames that character for good. */
    onRenameCharacter: (id: Int, name: String) -> Unit = { _, _ -> },
    onStart: (
        players: List<String>,
        meIndex: Int,
        holeCount: Int,
        pars: List<Int>,
        courseName: String?,
        characterIds: List<Int?>,
    ) -> Unit,
) {
    val names = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
        // Only you are pre-filled — the rest of the table changes round to round.
    ) { mutableListOf(myName, "", "", "").toMutableStateList() }

    val pars: SnapshotStateList<Int> = rememberSaveable(
        saver = listSaver<SnapshotStateList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { List(18) { Rules.DEFAULT_PAR }.toMutableStateList() }

    // Parallel to [names]. Only yours is pre-filled, from Settings.
    val picks: SnapshotStateList<Int> = rememberSaveable(
        saver = listSaver<SnapshotStateList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) {
        List(names.size) { if (it == 0) myCharacter ?: NO_CHARACTER else NO_CHARACTER }
            .toMutableStateList()
    }
    var picking by rememberSaveable { mutableIntStateOf(-1) }


    // 0 until a course is picked or its length is chosen — the course decides how
    // many holes there are, so there's nothing to toggle on this screen.
    var holeCount by rememberSaveable { mutableIntStateOf(0) }
    var courseName by rememberSaveable { mutableStateOf<String?>(null) }
    var parsOpen by rememberSaveable { mutableStateOf(false) }

    fun resizePars(n: Int) {
        while (pars.size < n) pars.add(Rules.DEFAULT_PAR)
        while (pars.size > n) pars.removeAt(pars.lastIndex)
    }

    fun setPars(next: List<Int>) {
        pars.clear()
        pars.addAll(next)
        courseName = null // hand-edited pars are no longer "that saved course"
    }

    /** The sentinel back to null at the boundary — the round stores "none" as null. */
    fun chosenCharacters(): List<Int?> =
        names.indices.map { i -> picks.getOrNull(i)?.takeIf { it != NO_CHARACTER } }

    val trimmed = names.map { it.trim() }
    val namesReady = trimmed.all { it.isNotEmpty() }
    val courseReady = holeCount > 0
    val ready = namesReady && courseReady

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // No title block — the back bar already says NEW ROUND.
        NeonSectionLabel("Players")
        Text(
            // You are always player 1, so there is nothing to mark — the old ME chips
            // said the same thing four times over.
            if (characters.isEmpty()) "You're player 1. Name the rest of the group."
            else "You're player 1. Name the rest, and tap a face to pick a character.",
            color = NeonBody,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        names.forEachIndexed { i, value ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (characters.isNotEmpty()) {
                    CharacterPickerButton(
                        characters.character(picks.getOrNull(i)?.takeIf { it != NO_CHARACTER }),
                    ) { picking = i }
                }
                NeonTextField(
                    value = value,
                    onValueChange = { typed ->
                        names[i] = typed
                        // Correcting the name beside a face corrects the face's name.
                        picks.getOrNull(i)?.takeIf { it != NO_CHARACTER }
                            ?.let { onRenameCharacter(it, typed) }
                    },
                    placeholder = if (i == 0) "You" else "Player ${i + 1}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (names.size < Rules.MAX_PLAYERS) {
                NeonSmallAction("+ Add player") {
                    names.add("")
                    picks.add(NO_CHARACTER)
                }
            }
            if (names.size > Rules.MIN_PLAYERS) {
                NeonSmallAction("− Remove") {
                    names.removeAt(names.lastIndex)
                    if (picks.size > names.size) picks.removeAt(picks.lastIndex)
                }
            }
        }

        NeonSectionLabel("Course")
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (courseName != null) Modifier.neonPanelOrange() else Modifier.neonPanel())
                .clickable { parsOpen = true }
                .padding(16.dp),
        ) {
            Text(
                courseName ?: if (courseReady) "Custom pars" else "Choose a course",
                color = if (courseName != null) NeonOrange else NeonWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (courseReady) {
                    "$holeCount holes · par ${pars.take(holeCount).sum()}"
                } else {
                    "Pick one, or set the pars yourself"
                },
                color = NeonBody,
                fontSize = 15.sp,
            )
        }

        Spacer(Modifier.height(36.dp))
        NeonBigButton("Start round", enabled = ready) {
            onStart(trimmed, ME_INDEX, holeCount, pars.take(holeCount), courseName, chosenCharacters())
        }
        if (!ready) {
            Spacer(Modifier.height(10.dp))
            Text(
                if (!namesReady) "Every player needs a name." else "Choose a course first.",
                color = NeonBody,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (parsOpen) {
        ParGridSheet(
            holeCount = holeCount,
            pars = pars.take(holeCount),
            courses = courses,
            canDelete = canDelete,
            onHoleCount = { n ->
                holeCount = n
                resizePars(n)
                courseName = null
            },
            onParsChange = ::setPars,
            onLoadCourse = { course ->
                holeCount = course.holeCount
                pars.clear()
                pars.addAll(course.pars)
                courseName = course.name
            },
            onSaveCourse = { name ->
                onSaveCourse(Course(name, holeCount, pars.take(holeCount)))
                courseName = name
            },
            onDeleteCourse = onDeleteCourse,
            onDismiss = { parsOpen = false },
            selectedCourse = courseName,
            playEnabled = namesReady,
            // Straight from the course list into the round, skipping the trip back
            // through Setup's own Start button.
            onPlayCourse = { course ->
                onStart(trimmed, ME_INDEX, course.holeCount, course.pars, course.name, chosenCharacters())
            },
        )
    }

    if (picking in names.indices) {
        val slot = picking
        CharacterSheet(
            characters = characters,
            selected = picks.getOrNull(slot)?.takeIf { it != NO_CHARACTER },
            takenBy = takenBy(picks, names, slot),
            onPick = { id ->
                while (picks.size <= slot) picks.add(NO_CHARACTER)
                picks[slot] = id ?: NO_CHARACTER
                // Picking a face fills in its name, so the common case is no typing
                // at all. Clearing a pick leaves whatever name is already there.
                characters.character(id)?.let { names[slot] = it.name }
            },
            onDismiss = { picking = -1 },
        )
    }
}

/**
 * Who already holds each character, excluding [slot] itself. Two players sharing a face
 * would defeat the point, so the picker greys those out.
 */
internal fun takenBy(picks: List<Int>, names: List<String>, slot: Int): Map<Int, String> =
    picks.indices
        .filter { it != slot && picks[it] != NO_CHARACTER }
        .associate { picks[it] to (names.getOrNull(it)?.trim()?.takeIf(String::isNotEmpty) ?: "Player ${it + 1}") }
