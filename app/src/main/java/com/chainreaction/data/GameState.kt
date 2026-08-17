package com.chainreaction.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The whole round, on this one phone.
 *
 * Scores are tracked for every player so nobody miscounts, but the deck, hand and
 * discard belong to the local player only — every phone runs its own independent deck.
 */
data class GameState(
    val players: List<String>,
    val meIndex: Int,
    val holeCount: Int,
    /** 0-based index of the hole on screen. */
    val currentHole: Int,
    val pars: List<Int>,
    /** [hole][player] */
    val scores: List<List<Int>>,
    val locked: List<Boolean>,
    val hand: List<Int>,
    val deck: List<Int>,
    val discard: List<Int>,
    val owed: Int,
    /** The course being played, when one was chosen rather than pars set by hand. */
    val courseName: String? = null,
) {

    val playerCount: Int get() = players.size

    // ---- derived scoring -------------------------------------------------

    /** Running total over locked holes only. */
    fun totalFor(player: Int): Int =
        (0 until holeCount).filter { locked[it] }.sumOf { scores[it][player] }

    /** Running score relative to par, over locked holes only. */
    fun relativeToParFor(player: Int): Int =
        (0 until holeCount).filter { locked[it] }.sumOf { scores[it][player] - pars[it] }

    val lockedHoleCount: Int get() = locked.count { it }

    val roundComplete: Boolean get() = locked.all { it }

    /** Player indices, best (lowest total) first. */
    val standings: List<Int> get() = players.indices.sortedBy { totalFor(it) }

    /**
     * Everyone tied for the lowest total. Ties are left standing — the app doesn't
     * invent a playoff, the group sorts that out.
     */
    val winners: List<Int>
        get() {
            val best = players.indices.minOf { totalFor(it) }
            return players.indices.filter { totalFor(it) == best }
        }

    /** What the local player would draw for [hole] given the scores currently entered. */
    fun drawForHole(hole: Int): Int = drawCount(scores[hole], meIndex)

    val handIsFull: Boolean get() = hand.size >= Rules.HAND_CAP

    val canDraw: Boolean get() = owed > 0 && !handIsFull && (deck.isNotEmpty() || discard.isNotEmpty())

    // ---- transitions -----------------------------------------------------

    fun withHole(hole: Int): GameState =
        copy(currentHole = hole.coerceIn(0, holeCount - 1))

    /**
     * Bump one player's score on one hole. Minimum score is 1.
     *
     * Par has no equivalent: it comes from the course and is fixed for the round.
     * Fix a wrong par by correcting the course, not mid-match.
     */
    fun withScoreDelta(hole: Int, player: Int, delta: Int): GameState {
        if (locked[hole]) return this
        val row = scores[hole]
        val updated = row.replaceAt(player, (row[player] + delta).coerceAtLeast(1))
        return copy(scores = scores.replaceAt(hole, updated))
    }

    /**
     * Freeze the hole, queue the local player's draw, and move to the next hole.
     * On the final hole the round simply completes and stays put.
     */
    fun lockAndAdvance(): GameState {
        val hole = currentHole
        if (locked[hole]) return this
        return copy(
            locked = locked.replaceAt(hole, true),
            owed = owed + drawForHole(hole),
            currentHole = (hole + 1).coerceAtMost(holeCount - 1),
        )
    }

    /**
     * Undo a lock — for the mis-tap that would otherwise ruin a round.
     * Refunds the draw that hole granted, floored at zero: any cards already
     * drawn stay in hand, which matches the app's honour-system stance elsewhere.
     */
    fun unlock(hole: Int): GameState {
        if (!locked[hole]) return this
        return copy(
            locked = locked.replaceAt(hole, false),
            owed = (owed - drawForHole(hole)).coerceAtLeast(0),
            currentHole = hole,
        )
    }

    /** Draw one owed card, reshuffling the discard back in if the deck has run out. */
    fun withDraw(): GameState {
        if (owed <= 0 || handIsFull) return this
        var d = deck
        var disc = discard
        if (d.isEmpty()) {
            if (disc.isEmpty()) return this
            d = disc.shuffled()
            disc = emptyList()
        }
        return copy(
            hand = hand + d.first(),
            deck = d.drop(1),
            discard = disc,
            owed = owed - 1,
        )
    }

    /** Play or discard — both send the card to the local discard pile. */
    fun withCardResolved(cardId: Int): GameState {
        val at = hand.indexOf(cardId)
        if (at < 0) return this
        return copy(
            hand = hand.toMutableList().also { it.removeAt(at) },
            discard = discard + cardId,
        )
    }

    // ---- persistence -----------------------------------------------------

    fun toJson(): String = JSONObject().apply {
        put("v", SCHEMA_VERSION)
        put("players", JSONArray(players))
        put("meIndex", meIndex)
        put("holeCount", holeCount)
        put("currentHole", currentHole)
        put("pars", pars.toJsonArray())
        put("scores", JSONArray().apply { scores.forEach { put(it.toJsonArray()) } })
        put("locked", JSONArray().apply { locked.forEach { put(it) } })
        put("hand", hand.toJsonArray())
        put("deck", deck.toJsonArray())
        put("discard", discard.toJsonArray())
        put("owed", owed)
        if (courseName != null) put("courseName", courseName)
    }.toString()

    companion object {
        const val SCHEMA_VERSION = 1

        fun newRound(
            players: List<String>,
            meIndex: Int,
            holeCount: Int,
            coursePars: List<Int>? = null,
            courseName: String? = null,
        ): GameState {
            val shuffled = CardDeck.freshShuffledDeck()
            // Fall back to all-par-3 if no course was chosen, or if a saved course
            // somehow disagrees with the hole count.
            val pars = coursePars?.takeIf { it.size == holeCount }
                ?: List(holeCount) { Rules.DEFAULT_PAR }
            return GameState(
                players = players,
                meIndex = meIndex,
                holeCount = holeCount,
                currentHole = 0,
                pars = pars,
                // Start every player at par for the hole; steppers take it from there.
                scores = List(holeCount) { h -> List(players.size) { pars[h] } },
                locked = List(holeCount) { false },
                hand = shuffled.take(Rules.HAND_SIZE),
                deck = shuffled.drop(Rules.HAND_SIZE),
                discard = emptyList(),
                owed = 0,
                courseName = courseName,
            )
        }

        /** Returns null on anything unreadable, so a bad blob just starts a fresh setup. */
        fun fromJson(raw: String): GameState? = try {
            val o = JSONObject(raw)
            if (o.optInt("v") != SCHEMA_VERSION) null else {
                val players = o.getJSONArray("players").toStringList()
                val holeCount = o.getInt("holeCount")
                val state = GameState(
                    players = players,
                    meIndex = o.getInt("meIndex"),
                    holeCount = holeCount,
                    currentHole = o.getInt("currentHole"),
                    pars = o.getJSONArray("pars").toIntList(),
                    scores = o.getJSONArray("scores").let { arr ->
                        List(arr.length()) { arr.getJSONArray(it).toIntList() }
                    },
                    locked = o.getJSONArray("locked").let { arr ->
                        List(arr.length()) { arr.getBoolean(it) }
                    },
                    hand = o.getJSONArray("hand").toIntList(),
                    deck = o.getJSONArray("deck").toIntList(),
                    discard = o.getJSONArray("discard").toIntList(),
                    owed = o.getInt("owed"),
                    // Optional, so rounds saved before courses were named still load.
                    courseName = o.optString("courseName").takeIf { it.isNotBlank() },
                )
                if (state.isConsistent()) state else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isConsistent(): Boolean =
        players.size in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS &&
            meIndex in players.indices &&
            pars.size == holeCount &&
            locked.size == holeCount &&
            scores.size == holeCount &&
            scores.all { it.size == players.size } &&
            currentHole in 0 until holeCount &&
            hand.size + deck.size + discard.size == CardDeck.ALL.size
}

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

private fun List<Int>.toJsonArray(): JSONArray = JSONArray().also { a -> forEach { a.put(it) } }

private fun JSONArray.toIntList(): List<Int> = List(length()) { getInt(it) }

private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }
