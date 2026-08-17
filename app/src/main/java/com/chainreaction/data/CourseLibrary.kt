package com.chainreaction.data

import android.content.Context
import org.json.JSONObject

/**
 * Courses that ship with the app, read from the bundled `courses.json` asset.
 *
 * That file lives at shared/courses.json in the repo and is wired in as an assets
 * source dir, so the Android app and the web build read the exact same library —
 * adding a course means editing one file, not two.
 */
object CourseLibrary {

    fun builtIn(context: Context): List<Course> = try {
        val raw = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        Course.fromJsonArray(JSONObject(raw).getJSONArray("courses"))
    } catch (_: Exception) {
        // A malformed library must not stop a round starting — you can still set pars by hand.
        emptyList()
    }

    /**
     * Built-ins first, then the player's own. A saved course with the same name as a
     * built-in replaces it, so a corrected par sheet wins over the shipped one.
     */
    fun merge(builtIn: List<Course>, saved: List<Course>): List<Course> {
        val overridden = saved.map { it.name.lowercase() }.toSet()
        return builtIn.filterNot { it.name.lowercase() in overridden } + saved
    }

    private const val ASSET = "courses.json"
}
