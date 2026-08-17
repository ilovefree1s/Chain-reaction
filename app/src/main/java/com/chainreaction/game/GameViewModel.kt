package com.chainreaction.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
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
    ) {
        val fresh = GameState.newRound(players, meIndex, holeCount, coursePars, courseName)
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

    fun resolveCard(cardId: Int) = update { it.withCardResolved(cardId) }

    private inline fun update(block: (GameState) -> GameState) {
        val current = state ?: return
        val next = block(current)
        if (next != current) {
            state = next
            repo.save(next)
        }
    }
}
