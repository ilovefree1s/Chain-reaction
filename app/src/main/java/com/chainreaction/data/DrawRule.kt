package com.chainreaction.data

/**
 * How many cards a player draws at the end of a hole, from their finish position.
 *
 *   best (or tied for best) -> 0
 *   2nd / 3rd / middle      -> 1
 *   last (or tied for last) -> 2
 *   everyone tied           -> 1 each, which overrides "best draws nothing"
 *   double bogey or worse   -> +1 bonus on top of the above
 *
 * Generalises to any player count: with 3-5 players "middle" is simply
 * anyone who is neither the outright best nor the outright worst.
 *
 * Deliberately free of Android imports so it can be unit tested on the JVM.
 */
fun drawCount(holeScores: List<Int>, meIndex: Int, par: Int? = null): Int {
    require(holeScores.isNotEmpty()) { "no scores for hole" }
    require(meIndex in holeScores.indices) { "meIndex $meIndex out of range" }

    val lo = holeScores.min()
    val hi = holeScores.max()
    val mine = holeScores[meIndex]

    val base = when {
        lo == hi -> 1
        mine == lo -> 0
        mine == hi -> 2
        else -> 1
    }
    // A rough hole feeds the hand: double bogey or worse earns a bonus card.
    val bonus = if (par != null && mine - par >= 2) 1 else 0
    return base + bonus
}
