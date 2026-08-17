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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.data.Settings
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.PanelRaised
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.React
import com.chainreaction.ui.theme.Sage
import com.chainreaction.ui.theme.SelfCard

@Composable
fun SettingsScreen(
    settings: Settings,
    courses: List<Course>,
    canDelete: (Course) -> Boolean,
    modifier: Modifier = Modifier,
    onSettingsChange: (Settings) -> Unit,
    onSaveCourse: (Course) -> Unit,
    onDeleteCourse: (String) -> Unit,
) {
    val names = remember(settings.defaultPlayers) {
        settings.defaultPlayers.ifEmpty { List(4) { "" } }.toMutableStateList()
    }
    var meIndex by remember(settings.defaultMeIndex) { mutableIntStateOf(settings.defaultMeIndex) }
    var savedRoster by remember { mutableStateOf(false) }

    // The course manager reuses the setup sheet, so it needs its own working pars.
    var coursesOpen by remember { mutableStateOf(false) }
    var holeCount by remember { mutableIntStateOf(0) }
    val pars = remember { mutableListOf<Int>().toMutableStateList() }

    fun resizePars(n: Int) {
        while (pars.size < n) pars.add(Rules.DEFAULT_PAR)
        while (pars.size > n) pars.removeAt(pars.lastIndex)
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // ---- players ----
        SectionLabel("Your usual group")
        Spacer(Modifier.height(4.dp))
        Text(
            "Saved here, these names pre-fill every new round.",
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
                    onValueChange = { names[i] = it; savedRoster = false },
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
                Box(
                    Modifier
                        .size(TapTarget)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (meIndex == i) SelfCard else Panel)
                        .clickable { meIndex = i; savedRoster = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "ME",
                        color = if (meIndex == i) Pine else Sage,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (names.size < Rules.MAX_PLAYERS) {
                SmallSetting("+ Add player") { names.add(""); savedRoster = false }
            }
            if (names.size > Rules.MIN_PLAYERS) {
                SmallSetting("− Remove") {
                    names.removeAt(names.lastIndex)
                    if (meIndex > names.lastIndex) meIndex = names.lastIndex
                    savedRoster = false
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        BigButton(
            text = "Save group",
            fill = SelfCard,
            onFill = Pine,
            enabled = names.all { it.isNotBlank() },
            onClick = {
                onSettingsChange(
                    settings.copy(
                        defaultPlayers = names.map { it.trim() },
                        defaultMeIndex = meIndex,
                    ),
                )
                savedRoster = true
            },
        )
        if (savedRoster) {
            Text("Saved.", color = React, style = MaterialTheme.typography.bodyLarge)
        }

        // ---- courses ----
        Spacer(Modifier.height(28.dp))
        SectionLabel("Courses")
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Panel)
                .clickable { coursesOpen = true }
                .padding(16.dp),
        ) {
            Text(
                "Manage courses",
                style = MaterialTheme.typography.titleLarge,
                color = OffWhite,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${courses.size} saved · add, edit or delete",
                color = Sage,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(32.dp))
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
}

@Composable
private fun SmallSetting(text: String, onClick: () -> Unit) {
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
