package com.chainreaction.data

import org.json.JSONObject

/**
 * What this phone's owner has done across rounds. Yours only — the app tracks the whole
 * table's scores during a round, but nobody else's history, and it never leaves the device.
 *
 * Nothing is recorded until a profile is locked in, so the numbers always belong to
 * somebody. [profile] is the character that owned them when they started.
 */
data class Stats(
    val gamesPlayed: Int = 0,
    /** Sole lowest total. */
    val wins: Int = 0,
    /** Tied for the lowest total — the app never invents a playoff, so these stay their own thing. */
    val ties: Int = 0,
    val aces: Int = 0,
    /** Card id -> times you played it. Discards are counted separately and not by id. */
    val cardsPlayed: Map<Int, Int> = emptyMap(),
    /**
     * Opponent name -> cards you've played on them. Keyed by name because that is what
     * you typed and what you'll recognise; someone who changes their name starts a new
     * tally, which is the honest outcome rather than guessing they're the same person.
     */
    val playsAgainst: Map<String, Int> = emptyMap(),
    val cardsDiscarded: Int = 0,
    /** The character these belong to, so a profile switch can offer to start over. */
    val profile: Int? = null,
) {

    val losses: Int get() = (gamesPlayed - wins - ties).coerceAtLeast(0)

    val totalCardsPlayed: Int get() = cardsPlayed.values.sum()

    /** Most-played first; ties broken by id so the list doesn't reshuffle between opens. */
    val byMostPlayed: List<Pair<GameCard, Int>>
        get() = cardsPlayed.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .mapNotNull { (id, n) -> CardDeck.ALL.firstOrNull { it.id == id }?.let { it to n } }

    /** Gifts are a kind, not a flag, so this is just a filter rather than its own counter. */
    val giftsGiven: Int
        get() = cardsPlayed.entries.sumOf { (id, n) ->
            if (CardDeck.ALL.firstOrNull { it.id == id }?.kind == CardKind.GIFT) n else 0
        }

    fun withCardPlayed(id: Int, target: String?): Stats {
        val played = cardsPlayed + (id to (cardsPlayed[id] ?: 0) + 1)
        val victim = target?.trim()?.takeIf { it.isNotEmpty() }
        return copy(
            cardsPlayed = played,
            playsAgainst = victim?.let { playsAgainst + (it to (playsAgainst[it] ?: 0) + 1) }
                ?: playsAgainst,
        )
    }

    /** Most-hit first, then alphabetical so the order is stable. */
    val byMostTargeted: List<Pair<String, Int>>
        get() = playsAgainst.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }

    fun withCardDiscarded(): Stats = copy(cardsDiscarded = cardsDiscarded + 1)

    /**
     * One finished round. [won] is an outright win, [tied] a shared best — a tie is
     * neither a win nor a loss, because the app doesn't decide those.
     */
    fun withRound(won: Boolean, tied: Boolean, aces: Int, profile: Int?): Stats = copy(
        gamesPlayed = gamesPlayed + 1,
        wins = wins + if (won) 1 else 0,
        ties = ties + if (tied) 1 else 0,
        aces = this.aces + aces,
        profile = this.profile ?: profile,
    )

    fun toJson(): String = JSONObject().apply {
        put("gamesPlayed", gamesPlayed)
        put("wins", wins)
        put("ties", ties)
        put("aces", aces)
        put("cardsDiscarded", cardsDiscarded)
        put("profile", profile ?: JSONObject.NULL)
        put("cardsPlayed", JSONObject().also { o -> cardsPlayed.forEach { (k, v) -> o.put(k.toString(), v) } })
        put("playsAgainst", JSONObject().also { o -> playsAgainst.forEach { (k, v) -> o.put(k, v) } })
    }.toString()

    companion object {
        fun fromJson(raw: String): Stats = try {
            val o = JSONObject(raw)
            val played = o.optJSONObject("cardsPlayed") ?: JSONObject()
            Stats(
                gamesPlayed = o.optInt("gamesPlayed"),
                wins = o.optInt("wins"),
                ties = o.optInt("ties"),
                aces = o.optInt("aces"),
                cardsPlayed = played.keys().asSequence()
                    .mapNotNull { k -> k.toIntOrNull()?.let { it to played.getInt(k) } }
                    .toMap(),
                cardsDiscarded = o.optInt("cardsDiscarded"),
                playsAgainst = (o.optJSONObject("playsAgainst") ?: JSONObject()).let { a ->
                    a.keys().asSequence().associateWith { a.getInt(it) }
                },
                profile = if (o.isNull("profile")) null else o.optInt("profile"),
            )
        } catch (_: Exception) {
            Stats()
        }
    }
}

/** Holes you aced this round — a score of 1 needs no other bookkeeping. */
fun GameState.acesFor(player: Int): Int =
    (0 until holeCount).count { locked[it] && scores[it][player] == 1 }
