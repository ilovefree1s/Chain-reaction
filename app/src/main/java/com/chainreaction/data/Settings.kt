package com.chainreaction.data

import org.json.JSONObject

/**
 * Preferences that outlive rounds: who you are, and how loud the app is.
 *
 * Only your own name is kept, not the whole group. The rest of the table changes from
 * round to round and typing three names on Setup is quicker than maintaining a roster
 * you have to keep correcting. You are always player 1.
 */
data class Settings(
    val myName: String = "",
    /** The character you go by, if you picked one. */
    val myCharacter: Int? = null,
    /** Sound effect volume, 0f silent to 1f full. Phones live in pockets on a course. */
    val sfxVolume: Float = DEFAULT_SFX_VOLUME,
) {

    fun toJson(): String = JSONObject().apply {
        put("myName", myName)
        put("myCharacter", myCharacter ?: JSONObject.NULL)
        put("sfxVolume", sfxVolume.toDouble())
    }.toString()

    companion object {
        /** Quiet enough not to startle a group standing round a tee pad. */
        const val DEFAULT_SFX_VOLUME = 0.4f

        fun fromJson(raw: String): Settings = try {
            val o = JSONObject(raw)
            Settings(
                myName = o.optString("myName").ifBlank { legacyName(o) },
                myCharacter = if (o.has("myCharacter") && !o.isNull("myCharacter")) {
                    o.getInt("myCharacter")
                } else {
                    legacyCharacter(o)
                },
                sfxVolume = o.optDouble("sfxVolume", DEFAULT_SFX_VOLUME.toDouble())
                    .toFloat().coerceIn(0f, 1f),
            )
        } catch (_: Exception) {
            Settings()
        }

        /**
         * Settings saved when this held the whole group: keep whichever name was
         * marked ME rather than throwing it away and making the user retype it.
         */
        private fun legacyName(o: JSONObject): String = try {
            val players = o.getJSONArray("players")
            players.getString(o.optInt("meIndex", 0).coerceIn(0, players.length() - 1))
        } catch (_: Exception) {
            ""
        }

        private fun legacyCharacter(o: JSONObject): Int? = try {
            val chars = o.getJSONArray("characters")
            val me = o.optInt("meIndex", 0)
            if (me in 0 until chars.length() && !chars.isNull(me)) chars.getInt(me) else null
        } catch (_: Exception) {
            null
        }
    }
}
