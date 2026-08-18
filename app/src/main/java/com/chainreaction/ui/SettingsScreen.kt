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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.data.Settings

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
    modifier: Modifier = Modifier,
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
    var saved by remember { mutableStateOf(false) }

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
        NeonHeader("SETTINGS", onBack)

        NeonSectionLabel("Your usual group")
        Text(
            "Saved here, these names pre-fill every new round.",
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
                Box(
                    Modifier
                        .weight(1f)
                        .height(TapTarget)
                        .neonPanel()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text("Player ${i + 1}", color = NeonDim, fontSize = 18.sp)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { slots[i] = it; saved = false },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = NeonWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        cursorBrush = SolidColor(NeonOrange),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MeChip(selected = meSlot == i) { meSlot = i; saved = false }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (slots.size < Rules.MAX_PLAYERS) {
                SmallNeonAction("+ Add player") {
                    slots.add("")
                    saved = false
                }
            }
            if (slots.size > Rules.MIN_PLAYERS) {
                SmallNeonAction("− Remove") {
                    slots.removeAt(slots.lastIndex)
                    if (meSlot > slots.lastIndex) meSlot = slots.lastIndex
                    saved = false
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        NeonBigButton("Save group", enabled = valid) {
            onSettingsChange(
                Settings(
                    defaultPlayers = slots.map { it.trim() },
                    defaultMeIndex = meSlot,
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
}

/** Octagonal ME toggle; orange when it's you. */
@Composable
private fun MeChip(selected: Boolean, onClick: () -> Unit) {
    val shape = CutCornerShape(14.dp)
    Box(
        Modifier
            .size(TapTarget)
            .clip(shape)
            .background(if (selected) NeonOrange.copy(alpha = 0.18f) else NeonChipBg)
            .border(
                2.dp,
                if (selected) {
                    Brush.verticalGradient(listOf(NeonOrange, Color(0xFFF7791E)))
                } else {
                    Brush.verticalGradient(listOf(NeonBlue, NeonBlueDeep))
                },
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "ME",
            color = if (selected) NeonOrange else NeonBody,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun SmallNeonAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(TapTarget)
            .neonPanel()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = NeonWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}