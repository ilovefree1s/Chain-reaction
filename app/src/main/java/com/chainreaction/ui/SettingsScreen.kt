package com.chainreaction.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.R
import com.chainreaction.data.Course
import com.chainreaction.data.Rules
import com.chainreaction.data.Settings
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.SelfCard

/*
 * The settings artwork (941x1672) paints the whole page — header, four player slots,
 * ME squares, Add/Remove, SAVE GROUP and the courses row. Real controls are laid over
 * it at fractions measured off the art's pixels:
 *
 *  - Each slot carries a transparent text field. While it's empty the painted
 *    "Player N" shows through as the placeholder; once typing starts, an opaque
 *    patch the colour of the slot interior covers the painted text.
 *  - The selected ME square and (when the group is valid) SAVE GROUP get a
 *    translucent gold overlay — the art has no painted selected/enabled state.
 *
 * The art has four slots, so the group here is 3-4 players: leave a slot empty to
 * play as three. Five-player rounds are still possible from round setup.
 */
private const val ART_ASPECT = 1672f / 941f

private val SLOT_TOP = listOf(0.2057f, 0.2883f, 0.3708f, 0.4534f)
private const val SLOT_H = 0.0610f
private const val SLOT_LEFT = 0.1254f
private const val SLOT_RIGHT = 0.7545f
private const val ME_LEFT = 0.779f
private const val ME_RIGHT = 0.879f
private const val BTNS_TOP = 0.537f
private const val BTNS_BOTTOM = 0.597f
private const val REMOVE_LEFT = 0.4548f
private const val REMOVE_RIGHT = 0.7089f
private const val SAVE_TOP = 0.6208f
private const val SAVE_BOTTOM = 0.6794f
private const val WIDE_LEFT = 0.1148f
private const val WIDE_RIGHT = 0.8842f
private const val COURSES_TOP = 0.7416f
private const val COURSES_BOTTOM = 0.8446f

/** Sampled from the slot interiors — the patch that hides the painted placeholder. */
private val SlotFill = Color(0xFF0A0A16)

/** Opaque dark gold: slot fill blended with the theme gold, hiding the grey label. */
private val SaveGold = Color(0xFF544622)

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
        List(4) { settings.defaultPlayers.getOrElse(it) { "" } }.toMutableStateList()
    }
    var meSlot by remember(settings.defaultMeIndex) {
        mutableIntStateOf(settings.defaultMeIndex.coerceIn(0, 3))
    }
    var saved by remember { mutableStateOf(false) }

    var coursesOpen by remember { mutableStateOf(false) }
    var holeCount by remember { mutableIntStateOf(0) }
    val pars = remember { mutableStateListOf<Int>() }
    fun resizePars(n: Int) {
        while (pars.size < n) pars.add(Rules.DEFAULT_PAR)
        while (pars.size > n) pars.removeAt(pars.lastIndex)
    }

    val filled = slots.map { it.trim() }.filter { it.isNotEmpty() }
    val valid = filled.size >= Rules.MIN_PLAYERS && slots[meSlot].isNotBlank()

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState()),
    ) {
        val w = maxWidth
        val h = w * ART_ASPECT

        Box(
            Modifier
                .width(w)
                .height(h),
        ) {
            Image(
                painter = painterResource(R.drawable.settings),
                contentDescription = "Settings",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
            )

            // Over the painted "< Menu".
            Box(
                Modifier
                    .width(w * 0.26f)
                    .height(h * 0.10f)
                    .clickable(onClick = onBack),
            )

            slots.forEachIndexed { i, value ->
                val top = h * SLOT_TOP[i]
                val slotH = h * SLOT_H

                // Once typing starts, hide the painted "Player N" under this patch.
                if (value.isNotEmpty()) {
                    Box(
                        Modifier
                            .offset(w * SLOT_LEFT, top)
                            .width(w * (SLOT_RIGHT - SLOT_LEFT))
                            .height(slotH)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlotFill),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { slots[i] = it; saved = false },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = OffWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(SelfCard),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            inner()
                        }
                    },
                    modifier = Modifier
                        .offset(w * (SLOT_LEFT + 0.035f), top)
                        .width(w * (SLOT_RIGHT - SLOT_LEFT - 0.06f))
                        .height(slotH),
                )
                // ME square; the gold wash marks the selected one.
                Box(
                    Modifier
                        .offset(w * ME_LEFT, top)
                        .width(w * (ME_RIGHT - ME_LEFT))
                        .height(slotH)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (meSlot == i) SelfCard.copy(alpha = 0.30f) else Color.Transparent)
                        .clickable { meSlot = i; saved = false },
                )
            }

            // Painted "- Remove": clears the last filled slot. ("+ Add player" is
            // decorative — all four slots are always on screen.)
            Box(
                Modifier
                    .offset(w * REMOVE_LEFT, h * BTNS_TOP)
                    .width(w * (REMOVE_RIGHT - REMOVE_LEFT))
                    .height(h * (BTNS_BOTTOM - BTNS_TOP))
                    .clickable {
                        val last = slots.indexOfLast { it.isNotBlank() }
                        if (last >= 0) {
                            slots[last] = ""
                            saved = false
                        }
                    },
            )

            // SAVE GROUP. Once the group is worth saving, an opaque dark-gold patch
            // covers the button's interior — translucent gold alone lets the painted
            // grey label bleed through — and the bold white label sits on top.
            Box(
                Modifier
                    .offset(w * WIDE_LEFT, h * SAVE_TOP)
                    .width(w * (WIDE_RIGHT - WIDE_LEFT))
                    .height(h * (SAVE_BOTTOM - SAVE_TOP))
                    .clickable(enabled = valid) {
                        val meWithinFiltered = slots.subList(0, meSlot).count { it.isNotBlank() }
                        onSettingsChange(
                            Settings(
                                defaultPlayers = filled,
                                defaultMeIndex = meWithinFiltered,
                            ),
                        )
                        saved = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (valid) {
                    // Inset so the art's metallic frame still shows around the patch.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 7.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SaveGold),
                    )
                    Text(
                        "SAVE GROUP",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (w.value * 0.034f).sp,
                        letterSpacing = (w.value * 0.006f).sp,
                    )
                }
            }
            if (saved) {
                Text(
                    "SAVED ✓",
                    color = SelfCard,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(y = h * (SAVE_BOTTOM + 0.004f))
                        .fillMaxWidth(),
                )
            }

            // Manage courses row.
            Box(
                Modifier
                    .offset(w * WIDE_LEFT, h * COURSES_TOP)
                    .width(w * (WIDE_RIGHT - WIDE_LEFT))
                    .height(h * (COURSES_BOTTOM - COURSES_TOP))
                    .clickable { coursesOpen = true },
            )
        }
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
