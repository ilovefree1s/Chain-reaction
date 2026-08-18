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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.ui.theme.Attack

private const val COLUMNS = 6

/**
 * Course & pars, in the neon style. Tapping a cell cycles its par 3 → 4 → 5, so a
 * course is a handful of taps rather than a trip through eighteen steppers; anything
 * more exotic is still reachable from the Score screen's ± steppers.
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
                .background(NeonBg)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // No header — straight into the list; the section labels say it all.
                if (courses.isNotEmpty()) {
                    NeonSectionLabel("Saved courses")
                    courses.forEach { course ->
                        val selected = course.name == selectedCourse
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .then(if (selected) Modifier.neonPanelOrange() else Modifier.neonPanel()),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable { onLoadCourse(course) }
                                        .padding(14.dp),
                                ) {
                                    // Course names carry the tee too — room to breathe.
                                    Text(
                                        course.name,
                                        color = if (selected) NeonOrange else NeonWhite,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${course.holeCount} holes · par ${course.totalPar}",
                                        color = NeonBody,
                                        fontSize = 15.sp,
                                    )
                                }
                                // Built-ins have no delete — saving over the name corrects one.
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
                                NeonBigButton(
                                    "Play",
                                    enabled = playEnabled,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) { onPlayCourse(course) }
                                if (!playEnabled) {
                                    Text(
                                        "Every player needs a name first.",
                                        color = NeonBody,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }

                NeonSectionLabel(if (courses.isEmpty()) "Set up a course" else "Or set up a new one")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(9, 18).forEach { n ->
                        NeonToggle(
                            "$n holes",
                            on = holeCount == n,
                            modifier = Modifier.weight(1f),
                        ) { onHoleCount(n) }
                    }
                }

                // Nothing to edit until we know how long the course is.
                if (holeCount > 0) {
                    NeonSectionLabel("Pars · total ${pars.sum()}")

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
                            // Keep the last row's cells the same width as the others'.
                            repeat(COLUMNS - rowHoles.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    NeonQuietButton("Reset to all par 3") {
                        onParsChange(List(holeCount) { Rules.DEFAULT_PAR })
                    }

                    NeonSectionLabel("Save for next time")
                    NeonTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Course name",
                    )
                    Spacer(Modifier.height(10.dp))
                    NeonBigButton("Save as course", enabled = name.isNotBlank()) {
                        onSaveCourse(name.trim())
                        name = ""
                    }
                }

                Spacer(Modifier.height(24.dp))
                NeonQuietButton("Done", onClick = onDismiss)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ParCell(hole: Int, par: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // Anything off the default reads as lit orange, so a glance finds the long holes.
    val isDefault = par == Rules.DEFAULT_PAR
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .height(64.dp)
            .clip(shape)
            .background(if (isDefault) NeonChipBg else Color(0xFF130C05))
            .border(2.dp, if (isDefault) Color(0xFF22304A) else NeonOrange, shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${hole + 1}",
            color = NeonDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "$par",
            color = if (isDefault) NeonWhite else NeonOrange,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/** 3 → 4 → 5 → 3. Values outside the cycle (set from the Score screen) snap back to 3. */
private fun List<Int>.cycledAt(index: Int): List<Int> =
    toMutableList().also { it[index] = Course.nextPar(getOrElse(index) { Rules.DEFAULT_PAR }) }
