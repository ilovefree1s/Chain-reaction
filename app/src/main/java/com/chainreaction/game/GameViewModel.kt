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
     * Play or discard — both send the card to the discard pile. [played] and [target]
     * are kept because the play flow still asks who a card landed on; nothing records
     * them while stats are on hold.
     */
    fun resolveCard(cardId: Int, played: Boolean, target: String? = null) {
        update { it.withCardResolved(cardId) }
    }

    /** A completed round. Only reachable from the results screen. */
    fun finishRound() = endRound()

    private inline fun update(block: (GameState) -> GameState) {
        val current = state ?: return
        val next = block(current)
        if (next != current) {
            state = next
            repo.save(next)
        }
    }
}
