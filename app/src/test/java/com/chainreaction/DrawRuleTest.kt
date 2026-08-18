package com.chainreaction

import com.chainreaction.data.CardDeck
import com.chainreaction.data.Course
import com.chainreaction.data.CourseLibrary
import com.chainreaction.data.GameState
import com.chainreaction.data.Rules
import com.chainreaction.data.drawCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawRuleTest {

    // ---- the draw table, 4 players ----

    @Test
    fun `outright best draws nothing`() {
        assertEquals(0, drawCount(listOf(2, 3, 4, 5), meIndex = 0))
    }

    @Test
    fun `tied for best draws nothing`() {
        assertEquals(0, drawCount(listOf(3, 3, 4, 5), meIndex = 0))
        assertEquals(0, drawCount(listOf(3, 3, 4, 5), meIndex = 1))
    }

    @Test
    fun `middle of the pack draws one`() {
        val scores = listOf(2, 3, 4, 5)
        assertEquals(1, drawCount(scores, meIndex = 1))
        assertEquals(1, drawCount(scores, meIndex = 2))
    }

    @Test
    fun `last draws two`() {
        assertEquals(2, drawCount(listOf(2, 3, 4, 5), meIndex = 3))
    }

    @Test
    fun `tied for last draws two each`() {
        val scores = listOf(3, 4, 5, 5)
        assertEquals(2, drawCount(scores, meIndex = 2))
        assertEquals(2, drawCount(scores, meIndex = 3))
    }

    @Test
    fun `everyone tied draws one each, overriding best-draws-nothing`() {
        val scores = listOf(4, 4, 4, 4)
        scores.indices.forEach { i ->
            assertEquals("player $i", 1, drawCount(scores, meIndex = i))
        }
    }

    // ---- 3 and 5 players ----

    @Test
    fun `rule holds for three players`() {
        val scores = listOf(3, 4, 5)
        assertEquals(0, drawCount(scores, meIndex = 0))
        assertEquals(1, drawCount(scores, meIndex = 1))
        assertEquals(2, drawCount(scores, meIndex = 2))
        assertEquals(1, drawCount(listOf(4, 4, 4), meIndex = 2))
    }

    @Test
    fun `rule holds for five players`() {
        val scores = listOf(2, 3, 3, 4, 6)
        assertEquals(0, drawCount(scores, meIndex = 0))
        assertEquals(1, drawCount(scores, meIndex = 1))
        assertEquals(1, drawCount(scores, meIndex = 2))
        assertEquals(1, drawCount(scores, meIndex = 3))
        assertEquals(2, drawCount(scores, meIndex = 4))
    }

    // ---- deck integrity ----

    @Test
    fun `deck is 54 cards with unique ids one through fifty-four`() {
        assertEquals(54, CardDeck.ALL.size)
        assertEquals((1..54).toSet(), CardDeck.ALL.map { it.id }.toSet())
    }

    @Test
    fun `wheel pool never contains a blacklisted card`() {
        assertEquals(37, CardDeck.WHEEL_POOL.size)
        Rules.WHEEL_EXCLUDES.forEach { excluded ->
            assertTrue(
                "card $excluded must not be in the wheel pool",
                CardDeck.WHEEL_POOL.none { it.id == excluded },
            )
        }
        // And it stays true across many spins.
        repeat(500) {
            assertTrue(CardDeck.WHEEL_POOL.random().id !in Rules.WHEEL_EXCLUDES)
        }
    }

    @Test
    fun `excluded cards are still in the playable deck`() {
        Rules.WHEEL_EXCLUDES.forEach { id ->
            assertTrue(CardDeck.ALL.any { it.id == id })
        }
    }

    // ---- hand, deck and discard ----

    @Test
    fun `new round deals four cards and leaves the rest in the deck`() {
        val state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 18)
        assertEquals(Rules.HAND_SIZE, state.hand.size)
        assertEquals(CardDeck.ALL.size - Rules.HAND_SIZE, state.deck.size)
        assertTrue(state.discard.isEmpty())
        assertEquals(0, state.owed)
    }

    @Test
    fun `a full hand blocks drawing until a card is discarded`() {
        val full = stateWith(
            hand = CardDeck.ALL.take(Rules.HAND_CAP).map { it.id },
            deck = CardDeck.ALL.drop(Rules.HAND_CAP).map { it.id },
            discard = emptyList(),
            owed = 2,
        )
        assertTrue(full.handIsFull)
        assertTrue(!full.canDraw)
        assertEquals("draw must be a no-op at the hand cap", full, full.withDraw())

        val afterDiscard = full.withCardResolved(full.hand.first())
        assertEquals(Rules.HAND_CAP - 1, afterDiscard.hand.size)
        assertTrue(afterDiscard.canDraw)
        assertEquals(Rules.HAND_CAP, afterDiscard.withDraw().hand.size)
        assertEquals(1, afterDiscard.withDraw().owed)
    }

    @Test
    fun `an empty deck reshuffles the discard pile back in`() {
        val ids = CardDeck.ALL.map { it.id }
        val state = stateWith(
            hand = ids.take(3),
            deck = emptyList(),
            discard = ids.drop(3),
            owed = 1,
        )
        val after = state.withDraw()

        assertEquals(4, after.hand.size)
        assertEquals(ids.size - 4, after.deck.size)
        assertTrue("discard is consumed by the reshuffle", after.discard.isEmpty())
        assertEquals(0, after.owed)
        assertEquals(
            "no card is lost or duplicated in a reshuffle",
            ids.toSet(),
            (after.hand + after.deck + after.discard).toSet(),
        )
    }

    @Test
    fun `play and discard both send the card to the discard pile`() {
        val state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9)
        val card = state.hand.first()
        val after = state.withCardResolved(card)
        assertTrue(card !in after.hand)
        assertTrue(card in after.discard)
    }

    // ---- locking ----

    @Test
    fun `locking a hole draws immediately and advances`() {
        var state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9)
        // Me on 5, the others on 3 — I'm last, so two cards, drawn on the spot.
        state = state.withScoreDelta(hole = 0, player = 0, delta = 2)
        state = state.lockAndAdvance()

        assertEquals("nothing left owed", 0, state.owed)
        assertEquals(Rules.HAND_SIZE + 2, state.hand.size)
        assertEquals(1, state.currentHole)
        assertTrue(state.locked[0])
        assertEquals(5, state.totalFor(0))
        assertEquals(2, state.relativeToParFor(0))
    }

    @Test
    fun `totals count locked holes only`() {
        var state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9)
        state = state.withScoreDelta(hole = 0, player = 1, delta = 3)
        assertEquals("nothing locked yet", 0, state.totalFor(1))
        state = state.lockAndAdvance()
        assertEquals(6, state.totalFor(1))
    }

    @Test
    fun `unlocking refunds only draws still owed`() {
        // A full hand blocks the lock's auto-draw, so those stay owed —
        // and unlocking refunds them.
        var state = stateWith(
            hand = CardDeck.ALL.take(Rules.HAND_CAP).map { it.id },
            deck = CardDeck.ALL.drop(Rules.HAND_CAP).map { it.id },
            discard = emptyList(),
            owed = 0,
        ).withScoreDelta(hole = 0, player = 0, delta = 2)

        state = state.lockAndAdvance()
        assertEquals("the cap blocked the auto-draw", 2, state.owed)
        assertEquals(Rules.HAND_CAP, state.hand.size)

        state = state.unlock(0)
        assertEquals(0, state.owed)
        assertTrue(!state.locked[0])
        assertEquals(0, state.currentHole)

        // Cards already in hand are never clawed back.
        assertEquals(Rules.HAND_CAP, state.hand.size)
    }

    @Test
    fun `a locked hole rejects score edits`() {
        val state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9)
            .lockAndAdvance()
        assertEquals(state, state.withScoreDelta(hole = 0, player = 1, delta = 1))
    }

    @Test
    fun `pars are fixed for the whole round once it starts`() {
        val pars = listOf(3, 4, 3, 3, 4, 3, 4, 3, 3)
        var state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9, coursePars = pars)

        // Scoring, locking and unlocking must never move a par.
        state = state.withScoreDelta(0, 0, 2).lockAndAdvance()
        state = state.withScoreDelta(1, 1, 1).lockAndAdvance().unlock(1)
        assertEquals(pars, state.pars)
    }

    @Test
    fun `score never drops below one`() {
        val state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9)
            .withScoreDelta(hole = 0, player = 0, delta = -10)
        assertEquals(1, state.scores[0][0])
    }

    @Test
    fun `an eighteen hole round of always coming last never breaks the deck`() {
        var state = GameState.newRound(listOf("A", "B", "C", "D"), meIndex = 0, holeCount = 18)
        val ids = CardDeck.ALL.map { it.id }.toSet()

        repeat(18) {
            // Come last every hole: +2 on me.
            state = state.withScoreDelta(state.currentHole, 0, 2).lockAndAdvance()
            // Draw everything owed, discarding whenever the cap gets in the way.
            while (state.owed > 0) {
                if (state.handIsFull) state = state.withCardResolved(state.hand.first())
                val before = state.owed
                state = state.withDraw()
                assertNotEquals("draw made no progress", before, state.owed)
            }
            assertEquals(
                "all 54 cards still accounted for",
                ids,
                (state.hand + state.deck + state.discard).toSet(),
            )
        }
        assertEquals(36, state.locked.count { it } * 2)
    }

    // ---- finishing a round ----

    /** Locks every hole after applying [strokesOverPar] to each player on hole 0. */
    private fun playedOut(strokesOverPar: List<Int>): GameState {
        var state = GameState.newRound(
            List(strokesOverPar.size) { "P$it" },
            meIndex = 0,
            holeCount = 9,
        )
        strokesOverPar.forEachIndexed { player, delta ->
            if (delta != 0) state = state.withScoreDelta(0, player, delta)
        }
        repeat(9) { state = state.lockAndAdvance() }
        return state
    }

    @Test
    fun `a round is only complete once every hole is locked`() {
        var state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9)
        repeat(8) { state = state.lockAndAdvance() }
        assertTrue("eight of nine is not done", !state.roundComplete)
        state = state.lockAndAdvance()
        assertTrue(state.roundComplete)
    }

    @Test
    fun `the lowest total wins and the rest rank behind it`() {
        val state = playedOut(listOf(2, 0, 1))
        assertEquals(listOf(1), state.winners)
        assertEquals(listOf(1, 2, 0), state.standings)
        assertEquals(27, state.totalFor(1))
        assertEquals(29, state.totalFor(0))
    }

    @Test
    fun `a tie for the lowest total leaves every tied player a winner`() {
        val state = playedOut(listOf(0, 0, 3))
        assertEquals(listOf(0, 1), state.winners)
        assertEquals(0, state.standings.first())
    }

    @Test
    fun `everyone level means everyone wins`() {
        val state = playedOut(listOf(0, 0, 0, 0))
        assertEquals(listOf(0, 1, 2, 3), state.winners)
    }

    @Test
    fun `unlocking a hole reopens a finished round`() {
        val state = playedOut(listOf(1, 0, 2))
        assertTrue(state.roundComplete)
        assertTrue(!state.unlock(8).roundComplete)
    }

    // ---- courses ----

    @Test
    fun `a course's pars carry into the round and set the starting scores`() {
        val pars = listOf(3, 4, 3, 3, 4, 3, 4, 3, 3)
        val state = GameState.newRound(listOf("A", "B", "C"), meIndex = 0, holeCount = 9, coursePars = pars)

        assertEquals(pars, state.pars)
        // Everyone starts each hole on that hole's par, so a par round needs no tapping.
        assertEquals(listOf(4, 4, 4), state.scores[1])
        assertEquals(0, state.relativeToParFor(0))
        assertEquals(30, state.pars.sum())
    }

    @Test
    fun `the course name rides along with the round`() {
        val state = GameState.newRound(
            listOf("A", "B", "C"),
            meIndex = 0,
            holeCount = 9,
            coursePars = List(9) { 3 },
            courseName = "Avon Town Hall",
        )
        assertEquals("Avon Town Hall", state.courseName)
        // Pars set by hand leave it unnamed rather than inventing one.
        assertEquals(null, GameState.newRound(listOf("A", "B", "C"), 0, 9).courseName)
    }

    @Test
    fun `a course that disagrees with the hole count falls back to all par three`() {
        val state = GameState.newRound(
            listOf("A", "B", "C"),
            meIndex = 0,
            holeCount = 18,
            coursePars = listOf(3, 4, 3), // a 9-hole card against an 18-hole round
        )
        assertEquals(List(18) { Rules.DEFAULT_PAR }, state.pars)
    }

    @Test
    fun `tapping a par cell cycles three, four, five and back`() {
        assertEquals(4, Course.nextPar(3))
        assertEquals(5, Course.nextPar(4))
        assertEquals(3, Course.nextPar(5))
    }

    @Test
    fun `a par set outside the cycle snaps back rather than getting stuck`() {
        // The Score screen's steppers allow 1..10; the grid must still be usable after that.
        assertEquals(3, Course.nextPar(9))
        assertEquals(3, Course.nextPar(1))
    }

    @Test
    fun `saving over a built-in course name replaces it rather than duplicating`() {
        val builtIn = listOf(
            Course("Gibbs", 18, List(18) { 3 }),
            Course("Other", 9, List(9) { 3 }),
        )
        val mine = listOf(Course("gibbs", 18, List(18) { 4 }))
        val merged = CourseLibrary.merge(builtIn, mine)

        assertEquals(2, merged.size)
        assertEquals(listOf("Other", "gibbs"), merged.map { it.name })
        assertEquals(72, merged.first { it.name == "gibbs" }.totalPar)
    }

    @Test
    fun `courses with no saved override are all still offered`() {
        val builtIn = listOf(Course("A", 9, List(9) { 3 }), Course("B", 9, List(9) { 3 }))
        assertEquals(2, CourseLibrary.merge(builtIn, emptyList()).size)
        assertEquals(3, CourseLibrary.merge(builtIn, listOf(Course("C", 9, List(9) { 3 }))).size)
    }

    private fun stateWith(
        hand: List<Int>,
        deck: List<Int>,
        discard: List<Int>,
        owed: Int,
    ) = GameState(
        players = listOf("A", "B", "C"),
        meIndex = 0,
        holeCount = 9,
        currentHole = 0,
        pars = List(9) { Rules.DEFAULT_PAR },
        scores = List(9) { List(3) { Rules.DEFAULT_PAR } },
        locked = List(9) { false },
        hand = hand,
        deck = deck,
        discard = discard,
        owed = owed,
    )
}
