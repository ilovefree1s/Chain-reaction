package com.chainreaction.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Preferences that outlive rounds: the usual group, and whether the app makes noise.
 *
 * [defaultPlayers] pre-fills Setup — the same four people play most rounds, and retyping
 * their names every time is the kind of friction that gets an app left in the car.
 */
data class Settings(
    val defaultPlayers: List<String> = emptyList(),
    val defaultMeIndex: Int = 0,
) {
    val hasRoster: Boolean
        get() = defaultPlayers.size in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS &&
            defaultPlayers.all { it.isNotBlank() }

    fun toJson(): String = JSONObject().apply {
        put("players", JSONArray().also { a -> defaultPlayers.forEach { a.put(it) } })
        put("meIndex", defaultMeIndex)
    }.toString()

    companion object {
        fun fromJson(raw: String): Settings = try {
            val o = JSONObject(raw)
            val arr = o.getJSONArray("players")
            Settings(
                defaultPlayers = List(arr.length()) { arr.getString(it) },
                defaultMeIndex = o.optInt("meIndex", 0),
            )
        } catch (_: Exception) {
            Settings()
        }
    }
}
