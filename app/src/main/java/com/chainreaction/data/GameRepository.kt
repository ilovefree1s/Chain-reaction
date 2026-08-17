package com.chainreaction.data

import android.content.Context

/**
 * Local persistence, nothing else. No backend, no network — courses have no signal.
 *
 * Writes use commit() rather than apply(): saves are small and infrequent (one per tap),
 * and the round has to survive the phone being force-quit in a pocket mid-round.
 */
class GameRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): GameState? = prefs.getString(KEY_ROUND, null)?.let { GameState.fromJson(it) }

    fun save(state: GameState) {
        prefs.edit().putString(KEY_ROUND, state.toJson()).commit()
    }

    fun clear() {
        prefs.edit().remove(KEY_ROUND).commit()
    }

    /** Saved courses outlive rounds — ending a round must not lose them. */
    fun loadCourses(): List<Course> =
        prefs.getString(KEY_COURSES, null)?.let { Course.listFromJson(it) } ?: emptyList()

    fun saveCourses(courses: List<Course>) {
        prefs.edit().putString(KEY_COURSES, Course.listToJson(courses)).commit()
    }

    fun loadSettings(): Settings =
        prefs.getString(KEY_SETTINGS, null)?.let { Settings.fromJson(it) } ?: Settings()

    fun saveSettings(settings: Settings) {
        prefs.edit().putString(KEY_SETTINGS, settings.toJson()).commit()
    }

    private companion object {
        const val PREFS = "chainreaction"
        const val KEY_ROUND = "round"
        const val KEY_COURSES = "courses"
        const val KEY_SETTINGS = "settings"
    }
}
