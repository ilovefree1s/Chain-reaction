package com.chainreaction.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Preferences that outlive rounds: the usual group, and how loud the app is.
 *
 * [defaultPlayers] pre-fills Setup — the same four people play most rounds, and retyping
 * their names every time is the kind of friction that gets an app left in the car.
 */
data class Settings(
    val defaultPlayers: List<String> = emptyList(),
    val defaultMeIndex: Int = 0,
    /** The character each usual player goes by, parallel to [defaultPlayers]. */
    val defaultCharacters: List<Int?> = emptyList(),
    /** Sound effect volume, 0f silent to 1f full. Phones live in pockets on a course. */
    val sfxVolume: Float = DEFAULT_SFX_VOLUME,
) {
    val hasRoster: Boolean
        get() = defaultPlayers.size in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS &&
            defaultPlayers.all { it.isNotBlank() }

    /** The saved character for roster slot [index], if that slot has one. */
    fun characterFor(index: Int): Int? = defaultCharacters.getOrNull(index)

    fun toJson(): String = JSONObject().apply {
        put("players", JSONArray().also { a -> defaultPlayers.forEach { a.put(it) } })
        put("meIndex", defaultMeIndex)
        put(
            "characters",
            JSONArray().also { a ->
                defaultPlayers.indices.forEach { a.put(characterFor(it) ?: JSONObject.NULL) }
            },
        )
        put("sfxVolume", sfxVolume.toDouble())
    }.toString()

    companion object {
        /** Quiet enough not to startle a group standing round a tee pad. */
        const val DEFAULT_SFX_VOLUME = 0.4f

        fun fromJson(raw: String): Settings = try {
            val o = JSONObject(raw)
            val arr = o.getJSONArray("players")
            Settings(
                defaultPlayers = List(arr.length()) { arr.getString(it) },
                defaultMeIndex = o.optInt("meIndex", 0),
                // Absent on a group saved before characters existed.
                defaultCharacters = o.optJSONArray("characters")?.let { c ->
                    List(c.length()) { if (c.isNull(it)) null else c.getInt(it) }
                } ?: emptyList(),
                // Absent on settings saved before the app made any noise.
                sfxVolume = o.optDouble("sfxVolume", DEFAULT_SFX_VOLUME.toDouble())
                    .toFloat().coerceIn(0f, 1f),
            )
        } catch (_: Exception) {
            Settings()
        }
    }
}
