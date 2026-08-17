package com.chainreaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chainreaction.data.Course
import com.chainreaction.data.GameState
import com.chainreaction.game.GameViewModel
import com.chainreaction.ui.CardsScreen
import com.chainreaction.ui.DoubleWheelSheet
import com.chainreaction.ui.HandScreen
import com.chainreaction.ui.MenuScreen
import com.chainreaction.ui.RoundRulesScreen
import com.chainreaction.ui.ResultsScreen
import com.chainreaction.ui.RulesScreen
import com.chainreaction.ui.ScoreScreen
import com.chainreaction.ui.SettingsScreen
import com.chainreaction.ui.SetupScreen
import com.chainreaction.ui.theme.Attack
import com.chainreaction.ui.theme.ChainReactionTheme
import com.chainreaction.ui.theme.Group
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.React
import com.chainreaction.ui.theme.Sage
import com.chainreaction.ui.theme.SelfCard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChainReactionTheme {
                Surface(Modifier.fillMaxSize(), color = Pine) {
                    App()
                }
            }
        }
    }
}

private enum class Screen { MENU, SETUP, ROUND, CARDS, RULES, SETTINGS }

private enum class Tab(val label: String, val accent: Color) {
    SCORE("Score", SelfCard),
    HAND("Hand", React),
    RULES("Rules", Group),
}

@Composable
private fun App(vm: GameViewModel = viewModel()) {
    var screenIndex by rememberSaveable { mutableIntStateOf(Screen.MENU.ordinal) }
    val screen = Screen.entries[screenIndex]
    fun go(target: Screen) { screenIndex = target.ordinal }

    BackHandler(enabled = screen != Screen.MENU) { go(Screen.MENU) }

    when (screen) {
        Screen.MENU -> MenuScreen(
            // The sheet's PLAY label is painted in, so the button can't announce a
            // round in progress — it still resumes one rather than starting fresh.
            onPlay = { go(if (vm.state != null) Screen.ROUND else Screen.SETUP) },
            onCards = { go(Screen.CARDS) },
            onRules = { go(Screen.RULES) },
            onSettings = { go(Screen.SETTINGS) },
        )

        Screen.SETUP -> SubScreen("New round", onBack = { go(Screen.MENU) }) { content ->
            SetupScreen(
                courses = vm.courses,
                canDelete = vm::canDelete,
                defaultPlayers = vm.settings.defaultPlayers,
                defaultMeIndex = vm.settings.defaultMeIndex,
                modifier = content,
                onSaveCourse = vm::saveCourse,
                onDeleteCourse = vm::deleteCourse,
                onStart = { players, me, holes, pars, course ->
                    vm.startRound(players, me, holes, pars, course)
                    go(Screen.ROUND)
                },
            )
        }

        Screen.CARDS -> SubScreen("Cards", onBack = { go(Screen.MENU) }) { content ->
            CardsScreen(modifier = content)
        }

        Screen.RULES -> SubScreen("Rules", onBack = { go(Screen.MENU) }) { content ->
            RulesScreen(modifier = content)
        }

        Screen.SETTINGS -> SubScreen("Settings", onBack = { go(Screen.MENU) }) { content ->
            SettingsScreen(
                settings = vm.settings,
                courses = vm.courses,
                canDelete = vm::canDelete,
                modifier = content,
                onSettingsChange = vm::updateSettings,
                onSaveCourse = vm::saveCourse,
                onDeleteCourse = vm::deleteCourse,
            )
        }

        Screen.ROUND -> {
            val state = vm.state
            // Ending a round from inside it leaves nothing to show — fall back to the menu.
            if (state == null) {
                LaunchedEffect(Unit) { go(Screen.MENU) }
            } else {
                Round(
                    state = state,
                    vm = vm,
                    onMenu = { go(Screen.MENU) },
                    onFinishRound = {
                        vm.endRound()
                        go(Screen.MENU)
                    },
                )
            }
        }
    }
}

/** A menu destination: a back bar over full-height content. */
@Composable
private fun SubScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        BackBar(title, onBack)
        content(Modifier.weight(1f))
    }
}

@Composable
private fun BackBar(title: String, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .height(48.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹  Menu", color = Sage, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        // Centred on the screen, not on the leftover space; the side padding keeps a
        // long course name from running under the Menu button.
        Text(
            title.uppercase(),
            color = OffWhite,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 96.dp),
        )
    }
}

@Composable
private fun Round(
    state: GameState,
    vm: GameViewModel,
    onMenu: () -> Unit,
    onFinishRound: () -> Unit,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(Tab.SCORE.ordinal) }
    val tab = Tab.entries[tabIndex]
    var wheelOpen by remember { mutableStateOf(false) }

    // Results take over the Score tab once every hole is locked. Reopening the scorecard
    // is a deliberate act; finishing the round again puts the result back in front.
    var showScorecard by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.roundComplete) {
        if (state.roundComplete) showScorecard = false
    }

    Scaffold(
        containerColor = Pine,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tabIndex = t.ordinal },
                        icon = {
                            TabIcon(
                                accent = t.accent,
                                selected = tab == t,
                                badge = if (t == Tab.HAND) state.owed else 0,
                            )
                        },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = OffWhite,
                            unselectedTextColor = Sage,
                            indicatorColor = Panel,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Column(Modifier.padding(inner)) {
            // The course you're on. The hole and par live in the Score screen's own header.
            BackBar(state.courseName.orEmpty(), onMenu)
            val content = Modifier.weight(1f)
            when (tab) {
                Tab.SCORE -> if (state.roundComplete && !showScorecard) {
                    ResultsScreen(
                        state = state,
                        modifier = content,
                        onViewScorecard = { showScorecard = true },
                        onFinishRound = onFinishRound,
                    )
                } else {
                    ScoreScreen(
                        state = state,
                        modifier = content,
                        onHole = vm::goToHole,
                        onScore = { player, delta -> vm.adjustScore(state.currentHole, player, delta) },
                        onLock = vm::lockAndAdvance,
                        onUnlock = { vm.unlockHole(state.currentHole) },
                    )
                }

                Tab.HAND -> HandScreen(
                    state = state,
                    modifier = content,
                    onDraw = vm::draw,
                    onResolve = vm::resolveCard,
                    onOpenWheel = { wheelOpen = true },
                )

                Tab.RULES -> RoundRulesScreen(
                    holeCount = state.holeCount,
                    pars = state.pars,
                    playingCourse = state.courseName,
                    modifier = content,
                    onSaveCourse = { name ->
                        vm.saveCourse(Course(name, state.holeCount, state.pars))
                    },
                    onEndRound = {
                        vm.endRound()
                        onMenu()
                    },
                )
            }
        }
    }

    if (wheelOpen) {
        DoubleWheelSheet(
            players = state.players,
            onDismiss = { wheelOpen = false },
        )
    }
}

/** A coloured dot per tab, with the owed-cards badge riding on Hand. */
@Composable
private fun TabIcon(accent: Color, selected: Boolean, badge: Int) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(if (selected) 14.dp else 10.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Sage),
        )
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Attack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$badge",
                    color = Pine,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
