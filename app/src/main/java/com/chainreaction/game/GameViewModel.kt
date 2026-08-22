package com.chainreaction.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.chainreaction.data.Character
import com.chainreaction.data.CharacterLibrary
import com.chainreaction.data.Course
import com.chainreaction.data.CourseLibrary
import com.chainreaction.data.GameRepository
import com.chainreaction.data.GameState
import com.chainreaction.data.Settings
import com.chainreaction.data.Stats
import com.chainreaction.data.acesFor

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GameRepository(app)

    /** Null means no round in progress — show Setup. */
    var state by mutableStateOf(repo.load())
        private set

    private val builtInCourses = CourseLibrary.builtIn(app)

    private val builtInCharacters = CharacterLibrary.builtIn(app)

    /** Names typed over the shipped ones. The photo stays put; only the label changes. */
    private var characterNames by mutableStateOf(repo.loadCharacterNames())

    /** The pickable roster, with any renames applied. */
    val characters: List<Character>
        get() = builtInCharacters.map { c ->
            characterNames[c.id]?.takeIf { it.isNotBlank() }?.let { c.copy(name = it) } ?: c
        }

    /**
     * Rename a character. Typing over the name beside a picked face is how you make the
     * roster yours — the shipped names are placeholders until someone corrects them.
     * A blank drops back to the shipped name rather than leaving a nameless face.
     */
    fun renameCharacter(id: Int, name: String) {
        val trimmed = name.trim()
        val next = if (trimmed.isEmpty()) characterNames - id else characterNames + (id to trimmed)
        if (next == characterNames) return
        characterNames = next
        repo.saveCharacterNames(next)
    }

    var settings by mutableStateOf(repo.loadSettings())
        private set

    /** This phone owner's history. Nobody else's, and it never leaves the device. */
    var stats by mutableStateOf(repo.loadStats())
        private set

    fun updateSettings(next: Settings) {
        settings = next
        repo.saveSettings(next)
    }

    private var savedCourses by mutableStateOf(repo.loadCourses())

    /** What the UI shows: the shipped library plus the player's own. Outlives rounds. */
    val courses: List<Course> get() = CourseLibrary.merge(builtInCourses, savedCourses)

    /** Built-in courses can be overridden by saving over the name, but not deleted. */
    fun canDelete(course: Course): Boolean = savedCourses.any { it.name == course.name }

    fun startRound(
        players: List<String>,
        meIndex: Int,
        holeCount: Int,
        coursePars: List<Int>? = null,
        courseName: String? = null,
        characterIds: List<Int?> = emptyList(),
    ) {
        val fresh = GameState.newRound(players, meIndex, holeCount, coursePars, courseName, characterIds)
        state = fresh
        repo.save(fresh)
    }

    /** Saving under an existing name overwrites it, so re-saving a tweaked course works. */
    fun saveCourse(course: Course) {
        val next = savedCourses.filterNot { it.name.equals(course.name, ignoreCase = true) } + course
        savedCourses = next
        repo.saveCourses(next)
    }

    fun deleteCourse(name: String) {
        val next = savedCourses.filterNot { it.name == name }
        savedCourses = next
        repo.saveCourses(next)
    }

    fun endRound() {
        repo.clear()
        state = null
    }

    fun goToHole(hole: Int) = update { it.withHole(hole) }

    fun adjustScore(hole: Int, player: Int, delta: Int) = update { it.withScoreDelta(hole, player, delta) }

    fun lockAndAdvance() = update { it.lockAndAdvance() }

    fun unlockHole(hole: Int) = update { it.unlock(hole) }

    fun draw() = update { it.withDraw() }

    /**
     * Play or discard — both send the card to the discard pile, but only one of them is
     * something you did to somebody, so the stats keep them apart.
     *
     * Nothing is recorded until a profile is locked in: numbers with no owner are worse
     * than no numbers.
     */
    fun resolveCard(cardId: Int, played: Boolean, target: String? = null) {
        val profile = settings.myCharacter
        if (profile != null) {
            // Stamp the owner on the first thing recorded, not just on a finished
            // round — otherwise a card played before your first round finishes leaves
            // the stats with no name on them.
            val owned = if (stats.profile == null) stats.copy(profile = profile) else stats
            stats = if (played) owned.withCardPlayed(cardId, target) else owned.withCardDiscarded()
            repo.saveStats(stats)
        }
        update { it.withCardResolved(cardId) }
    }

    /**
     * A completed round, banked on the way out. Only reachable from the results screen,
     * and finishing clears the round, so it can't be counted twice.
     */
    fun finishRound() {
        val state = state ?: return
        val profile = settings.myCharacter
        if (profile != null && state.roundComplete) {
            val winners = state.winners
            val iWon = state.meIndex in winners
            stats = stats.withRound(
                won = iWon && winners.size == 1,
                tied = iWon && winners.size > 1,
                aces = state.acesFor(state.meIndex),
                profile = profile,
            )
            repo.saveStats(stats)
        }
        endRound()
    }

    fun clearStats() {
        stats = Stats(profile = settings.myCharacter)
        repo.saveStats(stats)
    }

    private inline fun update(block: (GameState) -> GameState) {
        val current = state ?: return
        val next = block(current)
        if (next != current) {
            state = next
            repo.save(next)
        }
    }
}
