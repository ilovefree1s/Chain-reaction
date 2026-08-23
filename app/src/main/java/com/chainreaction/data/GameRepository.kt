package com.chainreaction.data

import android.content.Context
import org.json.JSONObject

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

    /**
     * Names the player has typed over the shipped ones, id -> name.
     *
     * apply() rather than commit() here alone: this is written on every keystroke while
     * a name is being typed, and a synchronous disk write per character would jank the
     * field. Losing the last letter to a force-quit costs nothing.
     */
    fun loadCharacterNames(): Map<Int, String> = try {
        val o = JSONObject(prefs.getString(KEY_CHARACTER_NAMES, "{}") ?: "{}")
        o.keys().asSequence().mapNotNull { k ->
            k.toIntOrNull()?.let { id -> id to o.getString(k) }
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }

    fun saveCharacterNames(names: Map<Int, String>) {
        val o = JSONObject()
        names.forEach { (id, name) -> o.put(id.toString(), name) }
        prefs.edit().putString(KEY_CHARACTER_NAMES, o.toString()).apply()
    }

    /**
     * History per profile, id -> that profile's stats. Same apply() reasoning as the
     * names — small and often. Stats written before the split are migrated on read.
     */
    fun loadStats(): Map<Int, Stats> =
        prefs.getString(KEY_STATS, null)?.let { Stats.mapFromJson(it) } ?: emptyMap()

    fun saveStats(all: Map<Int, Stats>) {
        prefs.edit().putString(KEY_STATS, Stats.mapToJson(all)).apply()
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
        const val KEY_CHARACTER_NAMES = "characterNames"
        const val KEY_STATS = "stats"
    }
}
