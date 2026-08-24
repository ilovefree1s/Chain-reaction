package com.chainreaction.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * How the round is won. Strokes are counted the same either way — the format decides
 * what the count means at the end.
 */
enum class RoundMode(val label: String) {
    /** Lowest total over the whole round. */
    STROKE("Stroke play"),

    /** Each hole is a skin. Tie it and nobody wins it: the skin rolls onto the next. */
    SKINS("Skins"),
    ;

    companion object {
        /** Anything unreadable, or a round saved before formats existed, is stroke play. */
        fun from(raw: String?): RoundMode = entries.firstOrNull { it.name == raw } ?: STROKE
    }
}

/** One card that left your hand, and whether you played it or just dumped it. */
data class PlayedCard(val id: Int, val played: Boolean)

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
    /** Stroke play unless the group picked otherwise on Setup. */
    val mode: RoundMode = RoundMode.STROKE,
    /**
     * Everything that has left your hand this round, oldest first. Parallel in spirit to
     * [discard], which is the same cards as a bare pile — this one remembers the order
     * and whether each was played.
     */
    val playLog: List<PlayedCard> = emptyList(),
) {

    /** What you played, most recent first. Discards are not plays. */
    val cardsPlayed: List<Int> get() = playLog.filter { it.played }.map { it.id }.reversed()

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

    // ---- skins ------------------------------------------------------------
    // A hole is worth one skin. Win it outright and you take it, plus anything that
    // rolled over; tie it and nobody does, so the pile grows for the next hole. Skins
    // still riding at the end are settled at the table, not here: the house rule is to go
    // back to the first tee and play on until they are taken. The app counts them and
    // says so — it has no way to score extra holes.

    /**
     * Who has the lowest score on [hole] outright, or null for a tie. Ignores whether
     * the hole is locked, so the payout can be shown before you commit to it.
     */
    fun holeWinner(hole: Int): Int? {
        val row = scores.getOrNull(hole) ?: return null
        val low = row.minOrNull() ?: return null
        return players.indices.filter { row.getOrNull(it) == low }.singleOrNull()
    }

    /** Who won [hole] outright, or null for a tie, an unlocked hole, or no players. */
    fun skinWinner(hole: Int): Int? =
        if (locked.getOrElse(hole) { false }) holeWinner(hole) else null

    /**
     * What [hole] is worth: one skin, plus everything the ties before it rolled over.
     * Answers "what's on this hole" while the round is still being played.
     */
    fun skinsAtStake(hole: Int): Int {
        var carry = 0
        for (h in 0 until hole.coerceAtMost(holeCount)) {
            if (!locked[h]) continue
            val at = 1 + carry
            carry = if (skinWinner(h) == null) at else 0
        }
        return 1 + carry
    }

    /** Skins [player] has taken so far. */
    fun skinsFor(player: Int): Int {
        var carry = 0
        var won = 0
        for (h in 0 until holeCount) {
            if (!locked[h]) continue
            val at = 1 + carry
            val winner = skinWinner(h)
            if (winner == null) {
                carry = at
            } else {
                if (winner == player) won += at
                carry = 0
            }
        }
        return won
    }

    /** Skins nobody has won yet, still riding on the next hole. */
    val skinsCarried: Int get() = skinsAtStake(holeCount) - 1

    /** Player indices, best first — fewest strokes, or most skins. */
    val standings: List<Int>
        get() = when (mode) {
            RoundMode.SKINS -> players.indices.sortedWith(
                // Strokes break a tie on skins: it is the only other thing we know,
                // and two players on the same skins is otherwise an arbitrary order.
                compareByDescending<Int> { skinsFor(it) }.thenBy { totalFor(it) },
            )
            RoundMode.STROKE -> players.indices.sortedBy { totalFor(it) }
        }

    /**
     * Everyone tied at the top. Ties are left standing — the app doesn't invent a
     * playoff, the group sorts that out.
     */
    val winners: List<Int>
        get() = when (mode) {
            RoundMode.SKINS -> {
                val best = players.indices.maxOf { skinsFor(it) }
                players.indices.filter { skinsFor(it) == best }
            }
            RoundMode.STROKE -> {
                val best = players.indices.minOf { totalFor(it) }
                players.indices.filter { totalFor(it) == best }
            }
        }

    /**
     * Cards the skin on [hole] pays you. Skins buy cards for everyone who didn't win
     * them: take the hole and you get nothing, lose it and you draw one card per skin
     * on it — so the hole after a run of ties pays out several at once.
     *
     * A tied hole wins nobody a skin but still deals everyone one card. Paying nothing
     * would mean a run of ties leaves the whole table with no cards to play, which is
     * the opposite of what the deck is for.
     */
    fun skinsPayoutFor(hole: Int): Int {
        val winner = holeWinner(hole) ?: return 1
        return if (winner == meIndex) 0 else skinsAtStake(hole)
    }

    /**
     * What the local player would draw for [hole] given the scores currently entered.
     * The format decides where cards come from: finishing position in stroke play, and
     * in skins the skins somebody else just took. They don't stack — in a skins round
     * the draw table doesn't apply at all.
     */
    fun drawForHole(hole: Int): Int = when (mode) {
        RoundMode.SKINS -> skinsPayoutFor(hole)
        RoundMode.STROKE -> drawCount(scores[hole], meIndex, pars[hole])
    }

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
    /**
     * Play or discard: either way the card leaves your hand for the discard pile.
     * [played] is remembered separately so the pile can say which cards you actually
     * used on somebody and which you just dumped — the same card id can be both over
     * a round, so this is a log in order, not a set.
     */
    fun withCardResolved(cardId: Int, played: Boolean = false): GameState {
        val at = hand.indexOf(cardId)
        if (at < 0) return this
        return copy(
            hand = hand.toMutableList().also { it.removeAt(at) },
            discard = discard + cardId,
            playLog = playLog + PlayedCard(cardId, played),
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
        // Written always, read optionally: a round saved before formats existed is
        // stroke play, which is what it was being played as.
        put("mode", mode.name)
        put(
            "playLog",
            JSONArray().apply {
                playLog.forEach { put(JSONObject().put("id", it.id).put("played", it.played)) }
            },
        )
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
            mode: RoundMode = RoundMode.STROKE,
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
                mode = mode,
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
                    mode = RoundMode.from(o.optString("mode").takeIf { it.isNotBlank() }),
                    // Absent on a round saved before the pile remembered anything.
                    playLog = o.optJSONArray("playLog")?.let { arr ->
                        List(arr.length()) {
                            val e = arr.getJSONObject(it)
                            PlayedCard(e.getInt("id"), e.optBoolean("played"))
                        }
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
