package com.chainreaction.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * A course's pars, saved under a name so it only has to be entered once.
 * You play the same handful of courses; enter Maple Hill in 15 seconds, reuse it forever.
 */
data class Course(
    val name: String,
    val holeCount: Int,
    val pars: List<Int>,
) {
    val totalPar: Int get() = pars.sum()

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("holeCount", holeCount)
        put("pars", JSONArray().also { a -> pars.forEach { a.put(it) } })
    }

    companion object {
        /** Pars cycle through these in the grid — the three that actually come up. */
        val CYCLE = listOf(3, 4, 5)

        /** Next par for a grid tap. Anything off the cycle snaps back to the start. */
        fun nextPar(current: Int): Int = CYCLE[(CYCLE.indexOf(current) + 1) % CYCLE.size]

        fun listToJson(courses: List<Course>): String =
            JSONArray().also { a -> courses.forEach { a.put(it.toJson()) } }.toString()

        fun listFromJson(raw: String): List<Course> = try {
            fromJsonArray(JSONArray(raw))
        } catch (_: Exception) {
            emptyList()
        }

        fun fromJsonArray(arr: JSONArray): List<Course> =
            (0 until arr.length()).mapNotNull {
                try {
                    fromJson(arr.getJSONObject(it))
                } catch (_: Exception) {
                    null
                }
            }

        private fun fromJson(o: JSONObject): Course? = try {
            val holeCount = o.getInt("holeCount")
            val arr = o.getJSONArray("pars")
            val pars = List(arr.length()) { arr.getInt(it) }
            val name = o.getString("name")
            if (name.isBlank() || pars.size != holeCount) null else Course(name, holeCount, pars)
        } catch (_: Exception) {
            null
        }
    }
}
