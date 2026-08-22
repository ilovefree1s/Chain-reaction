package com.chainreaction.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Character
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.data.Settings
import kotlin.math.roundToInt

/**
 * Settings in the neon style, drawn in code rather than shipped as artwork. Real
 * controls mean real behaviour: placeholders vanish when typing starts because
 * they're actual placeholders, SAVE GROUP arms itself when every name is filled,
 * the course count stays live, and Add/Remove genuinely resize the group (3-5).
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    courses: List<Course>,
    canDelete: (Course) -> Boolean,
    characters: List<Character> = emptyList(),
    modifier: Modifier = Modifier,
    /** Plays the menu sound at the given volume, so the slider can be heard. */
    onPreviewSfx: (Float) -> Unit = {},
    onSettingsChange: (Settings) -> Unit,
    onSaveCourse: (Course) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onBack: () -> Unit,
) {
    val slots = remember(settings.defaultPlayers) {
        settings.defaultPlayers.ifEmpty { List(4) { "" } }.toMutableStateList()
    }
    var meSlot by remember(settings.defaultMeIndex) {
        mutableIntStateOf(settings.defaultMeIndex.coerceIn(0, slots.lastIndex))
    }
    // Parallel to [slots]. Saved with the group, so the usual four keep their faces
    // and a new round starts already personalised.
    val charSlots = remember(settings.defaultCharacters, settings.defaultPlayers) {
        List(slots.size) { settings.characterFor(it) ?: NO_CHARACTER }.toMutableStateList()
    }
    var picking by remember { mutableIntStateOf(-1) }
    var saved by remember { mutableStateOf(false) }

    // Volume saves on release rather than on every pixel of the drag, and isn't part
    // of the group, so it doesn't wait on Save group.
    var volume by remember(settings.sfxVolume) { mutableFloatStateOf(settings.sfxVolume) }

    var coursesOpen by remember { mutableStateOf(false) }
    var holeCount by remember { mutableIntStateOf(0) }
    val pars = remember { mutableStateListOf<Int>() }
    fun resizePars(n: Int) {
        while (pars.size < n) pars.add(Rules.DEFAULT_PAR)
        while (pars.size > n) pars.removeAt(pars.lastIndex)
    }

    val valid = slots.all { it.isNotBlank() }

    Column(
        modifier
            .fillMaxSize()
            .background(NeonBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        NeonHeader("SETTINGS", onBack = onBack)

        NeonSectionLabel("Your usual group")
        Text(
            if (characters.isEmpty()) "Saved here, these names pre-fill every new round."
            else "Saved here, these names and faces pre-fill every new round.",
            color = NeonBody,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        slots.forEachIndexed { i, value ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (characters.isNotEmpty()) {
                    CharacterPickerButton(
                        characters.character(charSlots.getOrNull(i)?.takeIf { it != NO_CHARACTER }),
                    ) { picking = i }
                }
                NeonTextField(
                    value = value,
                    onValueChange = { slots[i] = it; saved = false },
                    placeholder = "Player ${i + 1}",
                    modifier = Modifier.weight(1f),
                )
                NeonMeChip(selected = meSlot == i) { meSlot = i; saved = false }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (slots.size < Rules.MAX_PLAYERS) {
                NeonSmallAction("+ Add player") {
                    slots.add("")
                    charSlots.add(NO_CHARACTER)
                    saved = false
                }
            }
            if (slots.size > Rules.MIN_PLAYERS) {
                NeonSmallAction("− Remove") {
                    slots.removeAt(slots.lastIndex)
                    if (charSlots.size > slots.size) charSlots.removeAt(charSlots.lastIndex)
                    if (meSlot > slots.lastIndex) meSlot = slots.lastIndex
                    saved = false
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        NeonBigButton("Save group", enabled = valid) {
            onSettingsChange(
                // copy(), so saving the group doesn't reset the volume.
                settings.copy(
                    defaultPlayers = slots.map { it.trim() },
                    defaultMeIndex = meSlot,
                    defaultCharacters = slots.indices.map { i ->
                        charSlots.getOrNull(i)?.takeIf { it != NO_CHARACTER }
                    },
                ),
            )
            saved = true
        }
        if (saved) {
            Text(
                "SAVED ✓",
                color = NeonOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        NeonSectionLabel("Courses")
        Row(
            Modifier
                .fillMaxWidth()
                .neonPanel()
                .clickable { coursesOpen = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonChip(NeonIcon.CAP)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text("Manage courses", color = NeonWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${courses.size} saved · add, edit or delete",
                    color = NeonBody,
                    fontSize = 15.sp,
                )
            }
            Text("›", color = NeonIce, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }

        NeonSectionLabel("Sound")
        Text(
            if (volume <= 0f) "Sound effects  ·  off"
            else "Sound effects  ·  ${(volume * 100).roundToInt()}%",
            color = NeonBody,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Slider(
            value = volume,
            onValueChange = { volume = it },
            // Saved and sounded on release: dragging would otherwise write to disk
            // and retrigger the clip on every frame.
            onValueChangeFinished = {
                onSettingsChange(settings.copy(sfxVolume = volume))
                onPreviewSfx(volume)
            },
            colors = SliderDefaults.colors(
                thumbColor = NeonOrange,
                activeTrackColor = NeonOrange,
                inactiveTrackColor = NeonChipBg,
            ),
        )
        Spacer(Modifier.height(28.dp))
    }

    if (coursesOpen) {
        ParGridSheet(
            holeCount = holeCount,
            pars = pars.take(holeCount),
            courses = courses,
            canDelete = canDelete,
            onHoleCount = { n -> holeCount = n; resizePars(n) },
            onParsChange = { next -> pars.clear(); pars.addAll(next) },
            onLoadCourse = { course ->
                holeCount = course.holeCount
                pars.clear()
                pars.addAll(course.pars)
            },
            onSaveCourse = { name -> onSaveCourse(Course(name, holeCount, pars.take(holeCount))) },
            onDeleteCourse = onDeleteCourse,
            onDismiss = { coursesOpen = false },
        )
    }

    if (picking in slots.indices) {
        val slot = picking
        CharacterSheet(
            characters = characters,
            selected = charSlots.getOrNull(slot)?.takeIf { it != NO_CHARACTER },
            playerName = slots[slot],
            takenBy = takenBy(charSlots, slots, slot),
            onPick = { id ->
                while (charSlots.size <= slot) charSlots.add(NO_CHARACTER)
                charSlots[slot] = id ?: NO_CHARACTER
                saved = false
            },
            onDismiss = { picking = -1 },
        )
    }
}