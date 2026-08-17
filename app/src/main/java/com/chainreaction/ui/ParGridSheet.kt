package com.chainreaction.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.ui.theme.Attack
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.PanelRaised
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.React
import com.chainreaction.ui.theme.Sage
import com.chainreaction.ui.theme.SelfCard

private const val COLUMNS = 6

/**
 * Set every hole's par in one screen. Tapping a cell cycles 3 → 4 → 5, so a course
 * is a handful of taps rather than a trip through eighteen steppers. Anything more
 * exotic than a par 5 is still reachable from the Score screen's ± steppers.
 */
@Composable
fun ParGridSheet(
    holeCount: Int,
    pars: List<Int>,
    courses: List<Course>,
    canDelete: (Course) -> Boolean,
    onHoleCount: (Int) -> Unit,
    onParsChange: (List<Int>) -> Unit,
    onLoadCourse: (Course) -> Unit,
    onSaveCourse: (String) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onDismiss: () -> Unit,
    /** The course currently chosen for the round, highlighted in the list. */
    selectedCourse: String? = null,
    /** Round-start shortcut on the selected row. Null (Settings) hides it. */
    onPlayCourse: ((Course) -> Unit)? = null,
    playEnabled: Boolean = true,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        var name by remember { mutableStateOf("") }

        Box(
            Modifier
                .fillMaxSize()
                .background(Pine)
                .safeDrawingPadding()
                .padding(20.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "COURSE & PARS",
                    style = MaterialTheme.typography.displaySmall,
                    color = OffWhite,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap a hole to cycle its par: 3 → 4 → 5.",
                    color = Sage,
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (courses.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Saved courses")
                    Spacer(Modifier.height(10.dp))
                    courses.forEach { course ->
                        val selected = course.name == selectedCourse
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) PanelRaised else Panel)
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, SelfCard, RoundedCornerShape(14.dp))
                                    } else Modifier,
                                ),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable { onLoadCourse(course) }
                                        .padding(14.dp),
                                ) {
                                    // Course names carry the tee too, so they need room to breathe.
                                    Text(
                                        course.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (selected) SelfCard else OffWhite,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${course.holeCount} holes · par ${course.totalPar}",
                                        color = Sage,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                // Courses that ship with the app have no delete — saving over
                                // the same name is how you correct one.
                                if (canDelete(course)) {
                                    Box(
                                        Modifier
                                            .size(TapTarget)
                                            .clickable { onDeleteCourse(course.name) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("×", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Attack)
                                    }
                                }
                            }
                            // Straight from the list to the tee.
                            if (selected && onPlayCourse != null) {
                                BigButton(
                                    text = "Play",
                                    fill = SelfCard,
                                    onFill = Pine,
                                    enabled = playEnabled,
                                    onClick = { onPlayCourse(course) },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                                if (!playEnabled) {
                                    Text(
                                        "Every player needs a name first.",
                                        color = Sage,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SectionLabel(if (courses.isEmpty()) "Set up a course" else "Or set up a new one")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(9, 18).forEach { n ->
                        Box(Modifier.weight(1f)) {
                            BigButton(
                                text = "$n holes",
                                fill = if (holeCount == n) SelfCard else Panel,
                                onFill = if (holeCount == n) Pine else OffWhite,
                                onClick = { onHoleCount(n) },
                            )
                        }
                    }
                }

                // Nothing to edit until we know how long the course is.
                if (holeCount > 0) {
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Pars", Modifier.weight(1f))
                        Text(
                            "Total ${pars.sum()}",
                            color = React,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    (0 until holeCount).chunked(COLUMNS).forEach { rowHoles ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowHoles.forEach { hole ->
                                ParCell(
                                    hole = hole,
                                    par = pars.getOrElse(hole) { Rules.DEFAULT_PAR },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    onParsChange(pars.cycledAt(hole))
                                }
                            }
                            // Keep the last row's cells the same width as every other row's.
                            repeat(COLUMNS - rowHoles.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    BigButton(
                        text = "Reset to all par 3",
                        fill = Panel,
                        onFill = OffWhite,
                        onClick = { onParsChange(List(holeCount) { Rules.DEFAULT_PAR }) },
                    )

                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Save for next time")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
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
                        text = "Save as course",
                        fill = SelfCard,
                        onFill = Pine,
                        enabled = name.isNotBlank(),
                        onClick = {
                            onSaveCourse(name.trim())
                            name = ""
                        },
                    )
                }

                Spacer(Modifier.height(24.dp))
                BigButton(
                    text = "Done",
                    fill = Panel,
                    onFill = OffWhite,
                    onClick = onDismiss,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ParCell(hole: Int, par: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // Anything off the default reads as raised, so a glance finds the long holes.
    val isDefault = par == Rules.DEFAULT_PAR
    Column(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDefault) Panel else PanelRaised)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${hole + 1}",
            color = Sage,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "$par",
            color = if (isDefault) OffWhite else SelfCard,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun List<Int>.cycledAt(index: Int): List<Int> =
    toMutableList().also { it[index] = Course.nextPar(getOrElse(index) { Rules.DEFAULT_PAR }) }
