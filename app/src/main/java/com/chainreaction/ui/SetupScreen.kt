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
import com.chainreaction.data.Course
import com.chainreaction.data.Rules

@Composable
fun SetupScreen(
    courses: List<Course>,
    canDelete: (Course) -> Boolean,
    defaultPlayers: List<String>,
    defaultMeIndex: Int,
    modifier: Modifier = Modifier,
    onSaveCourse: (Course) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onStart: (
        players: List<String>,
        meIndex: Int,
        holeCount: Int,
        pars: List<Int>,
        courseName: String?,
    ) -> Unit,
) {
    val names = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
        // Pre-filled from the group saved in Settings, so the usual four aren't retyped.
    ) { defaultPlayers.ifEmpty { List(4) { "" } }.toMutableStateList() }

    val pars: SnapshotStateList<Int> = rememberSaveable(
        saver = listSaver<SnapshotStateList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { List(18) { Rules.DEFAULT_PAR }.toMutableStateList() }

    var meIndex by rememberSaveable { mutableIntStateOf(defaultMeIndex) }
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

    val trimmed = names.map { it.trim() }
    val namesReady = trimmed.all { it.isNotEmpty() } && meIndex in trimmed.indices
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
            "Tap ME on your own name.",
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
                NeonTextField(
                    value = value,
                    onValueChange = { names[i] = it },
                    placeholder = "Player ${i + 1}",
                    modifier = Modifier.weight(1f),
                )
                NeonMeChip(selected = meIndex == i) { meIndex = i }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (names.size < Rules.MAX_PLAYERS) {
                NeonSmallAction("+ Add player") { names.add("") }
            }
            if (names.size > Rules.MIN_PLAYERS) {
                NeonSmallAction("− Remove") {
                    names.removeAt(names.lastIndex)
                    if (meIndex > names.lastIndex) meIndex = names.lastIndex
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
            onStart(trimmed, meIndex, holeCount, pars.take(holeCount), courseName)
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
                onStart(trimmed, meIndex, course.holeCount, course.pars, course.name)
            },
        )
    }
}
