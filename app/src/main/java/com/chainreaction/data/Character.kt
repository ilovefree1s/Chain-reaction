package com.chainreaction.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A pickable character: pure personalisation, no rules attached. Picking one changes
 * how a player is labelled on the scorecard and nothing else — there is no ability,
 * no modifier and nothing extra for the group to remember at the course.
 *
 * [color] is the fallback face, used until `character_NN` artwork is dropped into
 * the drawables. Stored as an ARGB long so the data layer stays free of Compose.
 */
data class Character(
    val id: Int,
    val name: String,
    val color: Long,
) {
    /** The letter drawn on the badge when a character has no artwork yet. */
    val initial: String get() = name.trim().take(1).uppercase()

    /** Drawable/asset base name for this character's face. `character_01` … */
    val artName: String get() = "character_%02d".format(id)
}

/**
 * The characters that ship with the app, read from the bundled `characters.json`.
 *
 * That file lives at shared/characters.json in the repo and is wired in as an assets
 * source dir, exactly like the course library — the Android app and the web build read
 * the same roster, so adding a character means editing one file, not two.
 */
object CharacterLibrary {

    fun builtIn(context: Context): List<Character> = try {
        val raw = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        fromJsonArray(JSONObject(raw).getJSONArray("characters"))
    } catch (_: Exception) {
        // No roster is a supported state: the picker simply doesn't offer anything,
        // and a round still plays on names alone.
        emptyList()
    }

    fun fromJsonArray(arr: JSONArray): List<Character> =
        (0 until arr.length()).mapNotNull { i ->
            try {
                val o = arr.getJSONObject(i)
                val name = o.getString("name")
                if (name.isBlank()) null
                else Character(o.getInt("id"), name, parseColor(o.optString("color")))
            } catch (_: Exception) {
                null
            }
        }

    /** "#RRGGBB" or "#AARRGGBB". Anything unreadable falls back to the house blue. */
    fun parseColor(raw: String): Long = try {
        val hex = raw.removePrefix("#")
        when (hex.length) {
            6 -> 0xFF000000L or hex.toLong(16)
            8 -> hex.toLong(16)
            else -> FALLBACK
        }
    } catch (_: Exception) {
        FALLBACK
    }

    const val FALLBACK = 0xFF3FA9FFL
    private const val ASSET = "characters.json"
}
