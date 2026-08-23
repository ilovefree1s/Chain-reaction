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

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Character
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.data.Settings
import com.chainreaction.data.Stats
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
    /** Typing over the name beside your picked face renames that character for good. */
    onRenameCharacter: (id: Int, name: String) -> Unit = { _, _ -> },
    /** Your own history, kept on this phone. */
    stats: Stats = Stats(),
    onClearStats: () -> Unit = {},
    onSettingsChange: (Settings) -> Unit,
    onSaveCourse: (Course) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onBack: () -> Unit,
) {
    var myName by remember(settings.myName) { mutableStateOf(settings.myName) }
    var myCharacter by remember(settings.myCharacter) { mutableStateOf(settings.myCharacter) }
    var picking by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    // Volume saves on release rather than on every pixel of the drag, and isn't part
    // of the group, so it doesn't wait on Save group.
    var volume by remember(settings.sfxVolume) { mutableFloatStateOf(settings.sfxVolume) }

    var coursesOpen by remember { mutableStateOf(false) }
    var statsOpen by remember { mutableStateOf(false) }
    var holeCount by remember { mutableIntStateOf(0) }
    val pars = remember { mutableStateListOf<Int>() }
    fun resizePars(n: Int) {
        while (pars.size < n) pars.add(Rules.DEFAULT_PAR)
        while (pars.size > n) pars.removeAt(pars.lastIndex)
    }

    val valid = myName.isNotBlank()

    Column(
        modifier
            .fillMaxSize()
            .background(NeonBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        NeonHeader("SETTINGS", onBack = onBack)

        NeonSectionLabel("You")
        Text(
            if (characters.isEmpty()) "Your name pre-fills player 1 on every new round."
            else "Your name and face pre-fill player 1 on every new round.",
            color = NeonBody,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (characters.isNotEmpty()) {
                CharacterPickerButton(characters.character(myCharacter)) { picking = true }
            }
            NeonTextField(
                value = myName,
                onValueChange = { typed ->
                    myName = typed
                    saved = false
                    // Correcting the name beside a face corrects the face's name.
                    myCharacter?.let { onRenameCharacter(it, typed) }
                },
                placeholder = "Your name",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))
        NeonBigButton("Save", enabled = valid) {
            // copy(), so saving your name doesn't reset the volume.
            onSettingsChange(settings.copy(myName = myName.trim(), myCharacter = myCharacter))
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

        NeonSectionLabel("Stats")
        Row(
            Modifier
                .fillMaxWidth()
                .neonPanel()
                .clickable { statsOpen = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your stats", color = NeonWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    // Says why it's empty rather than just showing zeroes.
                    if (settings.myCharacter == null) {
                        "Lock in a profile above and they start counting"
                    } else if (stats.gamesPlayed == 0) {
                        "Nothing yet · finish a round to start"
                    } else {
                        "${stats.gamesPlayed} rounds · ${stats.wins} won · " +
                            "${stats.totalCardsPlayed} cards played"
                    },
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

    if (statsOpen) {
        StatsSheet(
            stats = stats,
            characters = characters,
            onClear = onClearStats,
            onDismiss = { statsOpen = false },
        )
    }

    if (picking) {
        CharacterSheet(
            characters = characters,
            selected = myCharacter,
            // Nobody else to clash with here — the rest of the table is set on Setup.
            takenBy = emptyMap(),
            onPick = { id ->
                myCharacter = id
                saved = false
                // Picking a face fills in its name, so the common case is no typing.
                characters.character(id)?.let { myName = it.name }
            },
            onDismiss = { picking = false },
        )
    }
}