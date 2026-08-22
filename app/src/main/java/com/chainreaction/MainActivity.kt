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
import com.chainreaction.data.Character
import com.chainreaction.data.Course
import com.chainreaction.data.GameState
import com.chainreaction.data.Rules
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
import com.chainreaction.ui.rememberSfx
import com.chainreaction.ui.WheelCostSheet
import com.chainreaction.ui.NeonBg
import com.chainreaction.ui.NeonBlue
import com.chainreaction.ui.NeonBody
import com.chainreaction.ui.NeonDim
import com.chainreaction.ui.NeonIce
import com.chainreaction.ui.NeonOrange
import com.chainreaction.ui.NeonPanelBg
import com.chainreaction.ui.NeonWhite
import com.chainreaction.ui.theme.ChainReactionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChainReactionTheme {
                Surface(Modifier.fillMaxSize(), color = NeonBg) {
                    App()
                }
            }
        }
    }
}

private enum class Screen { MENU, SETUP, ROUND, CARDS, RULES, SETTINGS }

private enum class Tab(val label: String, val accent: Color) {
    SCORE("Score", NeonOrange),
    HAND("Hand", NeonIce),
    RULES("Rules", NeonBlue),
}

@Composable
private fun App(vm: GameViewModel = viewModel()) {
    var screenIndex by rememberSaveable { mutableIntStateOf(Screen.MENU.ordinal) }
    val screen = Screen.entries[screenIndex]
    fun go(target: Screen) { screenIndex = target.ordinal }

    // The app's only noise, and only for starting a round. Wrapped round the tap
    // target rather than baked into MenuScreen, which stays a dumb sheet of tap
    // zones over artwork.
    val sfx = rememberSfx()

    BackHandler(enabled = screen != Screen.MENU) { go(Screen.MENU) }

    when (screen) {
        Screen.MENU -> MenuScreen(
            // The sheet's PLAY label is painted in, so the button can't announce a
            // round in progress — it still resumes one rather than starting fresh.
            // The chains only rattle for a new round; resuming is not an occasion.
            onPlay = {
                val resuming = vm.state != null
                if (!resuming) sfx.menuTap(vm.settings.sfxVolume)
                go(if (resuming) Screen.ROUND else Screen.SETUP)
            },
            onCards = { go(Screen.CARDS) },
            onRules = { go(Screen.RULES) },
            onSettings = { go(Screen.SETTINGS) },
        )

        Screen.SETUP -> SubScreen("New round", onBack = { go(Screen.MENU) }) { content ->
            SetupScreen(
                courses = vm.courses,
                canDelete = vm::canDelete,
                myName = vm.settings.myName,
                myCharacter = vm.settings.myCharacter,
                characters = vm.characters,
                modifier = content,
                onSaveCourse = vm::saveCourse,
                onDeleteCourse = vm::deleteCourse,
                onStart = { players, me, holes, pars, course, picks ->
                    vm.startRound(players, me, holes, pars, course, picks)
                    go(Screen.ROUND)
                },
            )
        }

        Screen.CARDS -> SubScreen("Cards", onBack = { go(Screen.MENU) }) { content ->
            CardsScreen(modifier = content)
        }

        // No SubScreen bar: the artwork paints its own header and Menu button.
        // Black before the safe-area padding, so the status/nav bar strips match
        // the art instead of showing pine.
        Screen.RULES -> RulesScreen(
            modifier = Modifier
                .background(NeonBg)
                .safeDrawingPadding(),
            onBack = { go(Screen.MENU) },
        )

        // Art-backed like Rules: painted header, black behind the safe areas.
        Screen.SETTINGS -> SettingsScreen(
            settings = vm.settings,
            courses = vm.courses,
            canDelete = vm::canDelete,
            characters = vm.characters,
            modifier = Modifier
                .background(NeonBg)
                .safeDrawingPadding(),
            // So the slider can be heard while it's being dragged.
            onPreviewSfx = { volume -> sfx.menuTap(volume) },
            onSettingsChange = vm::updateSettings,
            onSaveCourse = vm::saveCourse,
            onDeleteCourse = vm::deleteCourse,
            onBack = { go(Screen.MENU) },
        )

        Screen.ROUND -> {
            val state = vm.state
            // Ending a round from inside it leaves nothing to show — fall back to the menu.
            if (state == null) {
                LaunchedEffect(Unit) { go(Screen.MENU) }
            } else {
                Round(
                    state = state,
                    vm = vm,
                    characters = vm.characters,
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
            Text("‹  Menu", color = NeonBody, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        // Centred on the screen, not on the leftover space; the side padding keeps a
        // long course name from running under the Menu button.
        Text(
            title.uppercase(),
            color = NeonWhite,
            fontWeight = FontWeight.Black,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
    characters: List<Character>,
    onMenu: () -> Unit,
    onFinishRound: () -> Unit,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(Tab.SCORE.ordinal) }
    val tab = Tab.entries[tabIndex]
    var wheelOpen by remember { mutableStateOf(false) }
    // Between tapping Spin and the wheel appearing: choosing which cards to pay with.
    var payingForWheel by remember { mutableStateOf(false) }
    // A spin off the Double Wheel card skips the name wheel — that card hands the choice to you.
    var wheelIsFree by remember { mutableStateOf(false) }

    // Results take over the Score tab once every hole is locked. Reopening the scorecard
    // is a deliberate act; finishing the round again puts the result back in front.
    var showScorecard by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.roundComplete) {
        if (state.roundComplete) showScorecard = false
    }

    Scaffold(
        containerColor = NeonBg,
        bottomBar = {
            NavigationBar(containerColor = NeonPanelBg) {
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
                            selectedTextColor = NeonWhite,
                            unselectedTextColor = NeonDim,
                            indicatorColor = NeonPanelBg,
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
                        characters = characters,
                        modifier = content,
                        onViewScorecard = { showScorecard = true },
                        onFinishRound = onFinishRound,
                    )
                } else {
                    ScoreScreen(
                        state = state,
                        characters = characters,
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
                    onResolve = { id ->
                        vm.resolveCard(id)
                        // The Double Wheel card IS the spin — playing it opens the wheel with no
                        // further cost, which is exactly what its text promises.
                        if (id == Rules.FREE_SPIN_CARD) {
                            wheelIsFree = true
                            wheelOpen = true
                        }
                    },
                    onOpenWheel = { payingForWheel = true },
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

    if (payingForWheel) {
        WheelCostSheet(
            hand = state.hand,
            onPaid = { paid ->
                paid.forEach(vm::resolveCard)
                payingForWheel = false
                wheelIsFree = false
                wheelOpen = true
            },
            onDismiss = { payingForWheel = false },
        )
    }

    if (wheelOpen) {
        DoubleWheelSheet(
            players = state.players,
            freeSpin = wheelIsFree,
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
                .background(if (selected) accent else NeonDim),
        )
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NeonOrange),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$badge",
                    color = NeonBg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
