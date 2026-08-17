package com.chainreaction.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.PanelRaised
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.React
import com.chainreaction.ui.theme.Sage
import com.chainreaction.ui.theme.SelfCard

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
            .padding(20.dp),
    ) {
        Text(
            "CHAIN\nREACTION",
            style = MaterialTheme.typography.displaySmall,
            color = OffWhite,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Disc golf card game",
            color = Sage,
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel("Players")
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap ME on your own name.",
            color = Sage,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))

        names.forEachIndexed { i, value ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { names[i] = it },
                    singleLine = true,
                    placeholder = { Text("Player ${i + 1}", color = Sage.copy(alpha = 0.6f)) },
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
                    modifier = Modifier.weight(1f),
                )
                MeToggle(selected = meIndex == i, onClick = { meIndex = i })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (names.size < Rules.MAX_PLAYERS) {
                SmallAction("+ Add player") { names.add("") }
            }
            if (names.size > Rules.MIN_PLAYERS) {
                SmallAction("− Remove") {
                    names.removeAt(names.lastIndex)
                    if (meIndex > names.lastIndex) meIndex = names.lastIndex
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Course")
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Panel)
                .clickable { parsOpen = true }
                .padding(16.dp),
        ) {
            Text(
                courseName ?: if (courseReady) "Custom pars" else "Choose a course",
                style = MaterialTheme.typography.titleLarge,
                color = if (courseName != null) SelfCard else OffWhite,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (courseReady) {
                    "$holeCount holes · par ${pars.take(holeCount).sum()}"
                } else {
                    "Pick one, or set the pars yourself"
                },
                color = Sage,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(36.dp))
        BigButton(
            text = "Start round",
            fill = SelfCard,
            onFill = Pine,
            enabled = ready,
            onClick = { onStart(trimmed, meIndex, holeCount, pars.take(holeCount), courseName) },
        )
        if (!ready) {
            Spacer(Modifier.height(10.dp))
            Text(
                if (!namesReady) "Every player needs a name." else "Choose a course first.",
                color = Sage,
                style = MaterialTheme.typography.bodyLarge,
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
        )
    }
}

@Composable
private fun MeToggle(selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(TapTarget)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) SelfCard else Panel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "ME",
            color = if (selected) Pine else Sage,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(TapTarget)
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
