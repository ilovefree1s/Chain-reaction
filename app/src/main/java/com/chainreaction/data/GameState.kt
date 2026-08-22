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
    /** Holes that have dealt their draw. A hole only ever deals once. */
    val dealt: List<Boolean> = emptyList(),
    /** The course being played, when one was chosen rather than pars set by hand. */
    val courseName: String? = null,
    /**
     * Chosen character per player, parallel to [players]. Null — or a short list, on a
     * round saved before characters existed — means that player just goes by their name.
     */
    val characterIds: List<Int?> = emptyList(),
) {

    val playerCount: Int get() = players.size

    /** The character [player] picked, if any. Safe for rounds that predate characters. */
    fun characterFor(player: Int): Int? = characterIds.getOrNull(player)

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
    fun drawForHole(hole: Int): Int = drawCount(scores[hole], meIndex, pars[hole])

    val handIsFull: Boolean get() = hand.size >= Rules.HAND_CAP

    val canDraw: Boolean get() = owed > 0 && !handIsFull && (deck.isNotEmpty() || discard.isNotEmpty())

    fun wasDealt(hole: Int): Boolean = dealt.getOrElse(hole) { false }

    private fun withDealt(hole: Int): List<Boolean> {
        val base = if (dealt.size == holeCount) dealt else List(holeCount) { false }
        return base.replaceAt(hole, true)
    }

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
     * Freeze the hole, resolve the local player's draw, and move to the next
     * hole. Draws land in hand immediately — nothing banks between holes, so
     * the hand cap keeps its teeth. Only what the cap blocks stays owed, and
     * becomes drawable the moment a discard makes room.
     *
     * That debt is use-it-or-lose-it: a fresh hole's deal REPLACES whatever is
     * still owed rather than adding to it. Otherwise playing a full hand for a
     * few holes banks a pile of cards to cash in later, which is exactly what
     * the hand cap exists to prevent.
     *
     * A hole only ever deals ONCE: unlocking and re-locking grants nothing
     * new, so the lock/unlock cycle can't farm cards — and because a re-lock
     * isn't a fresh deal, it doesn't wipe the debt either. On the final hole
     * the round simply completes and stays put.
     */
    fun lockAndAdvance(): GameState {
        val hole = currentHole
        if (locked[hole]) return this
        var next = copy(
            locked = locked.replaceAt(hole, true),
            owed = if (wasDealt(hole)) owed else drawForHole(hole),
            currentHole = (hole + 1).coerceAtMost(holeCount - 1),
            dealt = withDealt(hole),
        )
        while (next.canDraw) next = next.withDraw()
        return next
    }

    /**
     * Undo a lock — for the mis-tap that would otherwise ruin a round.
     * Scores reopen; the cards stay exactly where they are. The hole's deal
     * already happened and will not happen again, so there is nothing to
     * refund and nothing to farm.
     */
    fun unlock(hole: Int): GameState {
        if (!locked[hole]) return this
        return copy(
            locked = locked.replaceAt(hole, false),
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
        put(
            "characters",
            JSONArray().apply {
                players.indices.forEach { put(characterFor(it) ?: JSONObject.NULL) }
            },
        )
        put("dealt", JSONArray().apply { (0 until holeCount).forEach { put(dealt.getOrElse(it) { false }) } })
        if (courseName != null) put("courseName", courseName)
    }.toString()

    companion object {
        /**
         * 2: the deck was re-sorted alphabetically and renumbered, so a round saved
         * under version 1 holds ids that now mean different cards. Bumping drops
         * those rounds rather than dealing somebody the wrong hand mid-round.
         */
        const val SCHEMA_VERSION = 2

        fun newRound(
            players: List<String>,
            meIndex: Int,
            holeCount: Int,
            coursePars: List<Int>? = null,
            courseName: String? = null,
            characterIds: List<Int?> = emptyList(),
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
                characterIds = List(players.size) { characterIds.getOrNull(it) },
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
                    // Old saves predate the dealt list — a locked hole has dealt.
                    dealt = o.optJSONArray("dealt")?.let { arr ->
                        List(arr.length()) { arr.getBoolean(it) }
                    } ?: o.getJSONArray("locked").let { arr ->
                        List(arr.length()) { arr.getBoolean(it) }
                    },
                    // Optional, so rounds saved before courses were named still load.
                    courseName = o.optString("courseName").takeIf { it.isNotBlank() },
                    // Likewise optional: a round in progress when characters shipped
                    // keeps playing on names alone rather than being thrown away.
                    characterIds = o.optJSONArray("characters")?.let { arr ->
                        List(arr.length()) { if (arr.isNull(it)) null else arr.getInt(it) }
                    } ?: emptyList(),
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
