package com.chainreaction.data

/** How a card is aimed. Drives the colour system across the whole app. */
enum class CardKind(val label: String) {
    ATTACK("attack"),
    SELF("self"),

    /**
     * Lands on you or on somebody else, and you choose which. Some spell out two
     * halves — a favour on yourself, the same thing as a punishment on someone
     * else — and some are pure favours that simply need not go to another player.
     */
    DUAL("dual"),
    REACT("react"),
    GROUP("group"),

    /**
     * You, at your own expense. The table gets something and you don't — the only kind
     * whose whole point is that it costs the person holding it.
     */
    SABOTAGE("sabotage"),
    ;

    /**
     * Whether playing this lands on one named person, and so is worth asking about.
     * Self is you, group is everyone, sabotage is everyone but you, and a reaction answers
     * a card rather than a player — none of those have anyone to name.
     */
    val aimedAtSomeone: Boolean get() = this == ATTACK || this == DUAL
}

data class GameCard(
    val id: Int,
    val timing: String,
    val kind: CardKind,
    val name: String,
    val text: String,
)

object Rules {
    const val HAND_SIZE = 4
    const val HAND_CAP = 7
    const val MAX_CARDS_ON_ONE_PLAYER_PER_HOLE = 2
    const val MIN_PLAYERS = 3
    const val MAX_PLAYERS = 5
    const val DEFAULT_PAR = 3

    /** Cards you pay to spin the Double Wheel off your own bat. */
    const val WHEEL_COST = 2

    /**
     * The Double Wheel card — its whole text is a free spin, so playing it opens the wheel
     * without the usual [WHEEL_COST]. The card itself is the payment.
     */
    const val FREE_SPIN_CARD = 23

    // The house blacklist: reaction cards plus everything too situational,
    // too group-shaped or too slow to land as a wheel result — and the Double
    // Wheel itself, so the wheel can never demand another wheel.
    val WHEEL_EXCLUDES = setOf(1, 4, 6, 7, 10, 12, 13, 14, 19, 20, 21, 22, 23, 24, 25, 28, 30, 31, 32, 33, 36, 38, 40, 42, 45, 47, 50, 52, 53, 56, 59)

    /** Display order for the rules reference. */
    val TIMINGS = listOf(
        "Before shot",
        "Before tee shot",
        "Before all tee",
        "On draw",
        "After throw",
        "After throw · secret",
        "After all tee",
        "After card",
        "After a hole",
        "After all play a card",
        "For the next hole",
        "Any time",
    )
}

object CardDeck {
    val ALL: List<GameCard> = listOf(
        GameCard(1, "Before tee shot", CardKind.ATTACK, "1v1 Challenge", "Challenge someone. It runs until one of you beats the other's score on a hole — a tie settles nothing. The winner gets immunity from all attack cards next hole and banks a mulligan to use on any hole, whenever they need it."),
        GameCard(2, "Before shot", CardKind.ATTACK, "39% OF THE SPLITS", "Force an opponent to straddle putt. Playable any time someone is about to throw a putter."),
        GameCard(3, "Before shot", CardKind.DUAL, "Aerobie", "On yourself: use an aerobie for your drive. On another player: they drive with the aerobie using their off hand. Normal stroke either way."),
        GameCard(4, "After throw", CardKind.ATTACK, "AIR HORN!", "Heckle an opponent during one of their shots. Reveal and discard this card after."),
        GameCard(5, "Before shot", CardKind.ATTACK, "Baby Discs", "Force an opponent to throw a mini for their upcoming drive or putt. Through the basket floor still counts as in."),
        GameCard(6, "For the next hole", CardKind.ATTACK, "Bag Boy!", "Force an opponent to carry your bag for 3 holes! If they beat you before those 3 holes are up (1st or 2nd hole), they can play one of your cards and you take your bag back."),
        GameCard(7, "Before tee shot", CardKind.ATTACK, "Bag Exchange!", "Pick a player to swap bags with. Swap back after the first bogey by either player. The player that took the bogey first must volunteer as tribute to exchange lies with the other player the next time they go OB or miss a mando."),
        GameCard(8, "Before shot", CardKind.ATTACK, "Bag Raid!", "Choose ANY disc from any bag. The target player must use that disc for their next shot."),
        GameCard(9, "After throw", CardKind.ATTACK, "Big Ooof, Bud.", "Move an opponent's lie up to 10 paces (30 ft) in any direction, as long as it isn't out of bounds."),
        GameCard(10, "After throw", CardKind.SELF, "Big Putted!", "If you make a putt from outside C1 while any other players are inside C1, they all must use their left hand to putt this hole."),
        GameCard(11, "Before tee shot", CardKind.ATTACK, "Bizarro Golf!", "Force an opponent to drive with a putter and putt with a driver this hole."),
        GameCard(12, "Before all tee", CardKind.SELF, "Call Your Shot", "Call CTP. If you win it, nobody can play cards on you next hole, and your next attack card hits every opponent — not you."),
        GameCard(13, "Any time", CardKind.ATTACK, "Can I Borrow This?", "Pick anyone you want. Look through their cards and play one on anyone."),
        GameCard(14, "Any time", CardKind.REACT, "Change Is Good", "Force an opponent to change the target of one of their cards, if there is another option."),
        GameCard(15, "Before shot", CardKind.ATTACK, "CHRIS SPECIAL!", "Force an opponent to throw a tomahawk on the upcoming drive or approach."),
        GameCard(16, "Before shot", CardKind.ATTACK, "Close 'Em", "Force an opponent to take the next putt with their eyes closed."),
        GameCard(17, "Before tee shot", CardKind.ATTACK, "Code Words!", "An opponent can't say \"yes\" or \"no\" this hole. 1 stroke penalty every time they do. ANY variation of the words yes or no counts."),
        GameCard(18, "Before tee shot", CardKind.ATTACK, "Jomez Commentator", "Another player has to announce every shot you take this hole like they are a commentator. If they forget one, they take +1 stroke."),
        GameCard(19, "Before tee shot", CardKind.ATTACK, "Dealer's Choice!", "You pick the discs every other player tees off with this hole."),
        GameCard(20, "Before tee shot", CardKind.ATTACK, "Do Not Pass Go", "Whoever has the shortest drive on the next hole gets no cards for that hole."),
        GameCard(21, "After throw", CardKind.DUAL, "Doesn't Look OB to Me", "Play on a shot that just went OB. Instead of actually being OB (or in a hazard) they can just play it where it lies, with no penalty at all."),
        GameCard(22, "After throw · secret", CardKind.SELF, "Don't Nice Me!", "If any player says any form of \"nice\" as your disc is in flight and it hits a tree, you can move to where your disc landed and throw again for a free stroke."),
        GameCard(23, "Before tee shot", CardKind.GROUP, "Double Wheel", "Free spin on the Double Wheel. The name doesn't matter — you choose who gets the benefit or the punishment."),
        GameCard(24, "Before shot", CardKind.ATTACK, "Easily Distracted?", "Everyone can do anything they want to distract a player while they are about to putt."),
        GameCard(25, "After a hole", CardKind.SELF, "FIVE FOR YOU, YOU, AND YOU!", "Play after you birdie a hole nobody else birdied. Everyone else owes five dollars to the pot!"),
        GameCard(26, "After throw", CardKind.SELF, "Foot Wedge!", "Move your own lie up to 10 paces (30 ft) in any direction."),
        GameCard(27, "After throw", CardKind.SELF, "GAMBLE!", "Take a second chance at a putt you just missed. Make it and it was a free mulligan. Miss and the second stroke counts too — play whichever disc landed farthest away, then add +1 stroke after the hole ends."),
        GameCard(28, "After all tee", CardKind.SELF, "I'll Have What He's Having", "After all tee shots, trade lies with an opponent of your choice."),
        GameCard(29, "Before shot", CardKind.DUAL, "Globetrotter Shit", "On yourself: putt behind the back at no stroke cost. On another player: they putt behind the back and it counts as a normal stroke."),
        GameCard(30, "Before tee shot", CardKind.DUAL, "GOOD GUYS VS. BAD GUYS!", "Ask another player to team up for the next hole. Birdie counts as an eagle, no birdie is +1. Cards against one of you count against both, and you both take the same score. If they refuse, ask someone else — if everyone refuses, they all discard 1 card."),
        GameCard(31, "After all play a card", CardKind.GROUP, "Group Hug", "Only play after 3 other players used an attack card. Everyone must carry out their own card they played. Do whichever one you choose as well."),
        GameCard(32, "Before shot", CardKind.SELF, "If the Basket Was There It Woulda Went In", "Play before a C2 putt only. If you hit metal and it doesn't go in, it still counts."),
        GameCard(33, "After all tee", CardKind.ATTACK, "I'm In Control", "You decide who plays whose tee shots. More than one player can be sent to the same lie."),
        GameCard(34, "Before tee shot", CardKind.ATTACK, "It's Like a Stranger Is Doing It!", "Force an opponent to take the upcoming drive with their off hand."),
        GameCard(35, "Before tee shot", CardKind.DUAL, "Lefty Off the Box", "Your first throw off the box is with your offhand, for free — that's throw 0. The player in last place gets it too. If 2 players are tied for last, they rock paper scissors for it."),
        GameCard(36, "After a hole", CardKind.DUAL, "Me and You", "On yourself: take the best score made on the hole. On another player: they take the worst score made on the hole."),
        GameCard(37, "Before tee shot", CardKind.SELF, "My Tee Pad Is Over Here!", "Use 2 of your discs to mark a new tee for yourself, up to 10 paces (30 ft) from the original. Feeling nice? You may pick 1 player to join you."),
        GameCard(38, "After card", CardKind.REACT, "No Way", "Cancel any card just played. That card goes to the discard pile."),
        GameCard(39, "Before tee shot", CardKind.ATTACK, "New mando on this hole, bud.", "Choose a reasonable object the target player must pass on a side you specify."),
        GameCard(40, "Before tee shot", CardKind.DUAL, "Not Today!", "Remove any and all card effects currently on you, or use it on another player to cancel the effects on them."),
        GameCard(41, "Before tee shot", CardKind.ATTACK, "One Disc Wonder!", "Force an opponent to play the hole with only 1 DISC this hole! (If someone already gave them an Aerobie or mini, that counts as their 1 disc.)"),
        GameCard(42, "Before tee shot", CardKind.ATTACK, "Over Sharer", "Give everyone a disc from your own bag to tee off with on this hole."),
        GameCard(43, "Before all tee", CardKind.ATTACK, "Plant Your Feet!", "No run-up on everyone else's tee shot."),
        GameCard(44, "Any time", CardKind.DUAL, "Player 2's Turn!", "On yourself: retake any shot for free. On another player: force them to re-throw a shot that was too good."),
        GameCard(45, "After throw", CardKind.ATTACK, "Prove It", "Cancel a shot an opponent just took. They throw again with a different disc of their choice from any bag. (No extra stroke, just a forced mulligan.)"),
        GameCard(46, "Before shot", CardKind.ATTACK, "Roll It!", "Force an opponent to throw a roller on the upcoming drive or approach."),
        GameCard(47, "After card", CardKind.REACT, "Rubber and Glue", "If a card targeting only you was just played, that opponent carries out the instructions instead of you."),
        GameCard(48, "Before shot", CardKind.DUAL, "Shoe Golf", "On yourself: putt with your own shoe at no stroke cost. On another player: they putt with a shoe and it still counts as a stroke. If they refuse to take their shoe off, they take +1 stroke after the hole."),
        GameCard(49, "Before shot", CardKind.ATTACK, "Forehand only!", "Force an opponent to throw a forehand on the upcoming drive or approach."),
        GameCard(50, "On draw", CardKind.SABOTAGE, "Everybody But Me", "You must play this card immediately after drawing, you can't discard it. Everyone gets a free mulligan on the next hole but me =("),
        GameCard(51, "Before shot", CardKind.DUAL, "That's Definitely a Gimme", "Pick up a putt as a gimme, as long as it's inside C1 — you definitely woulda made it. The player in last place gets one too. If 2 players are tied for last, they rock paper scissors for it."),
        GameCard(52, "Before tee shot", CardKind.ATTACK, "Too Many Choices", "Pick 3 discs out of an opponent's bag. They pick 1, then pass the other 2 to the next person, who chooses 1 of the 2 and gives the last disc to the last person. Tee off, nerds."),
        GameCard(53, "Before tee shot", CardKind.SELF, "Tree Insurance", "Play before your tee shot. If you hit a tree, take a free mulligan."),
        GameCard(54, "Before shot", CardKind.ATTACK, "Trust Me Bro", "Give another player advice for their tee shot. They have to follow it as best they can. (Somewhat reasonable advice — don't tell them something like throw it backwards.)"),
        GameCard(55, "Before shot", CardKind.ATTACK, "Turbo Time", "All opponents in C1 must turbo putt their next shot."),
        GameCard(56, "Before tee shot", CardKind.GROUP, "WALK IT DOWN!!", "No other cards can be played once this hits the table. Everyone tees immediately from anywhere near the tee pad — no throw order. First and second to finish get birdie, third gets par, last gets bogey. No running, and no calling foot faults."),
        GameCard(57, "After throw", CardKind.ATTACK, "Walk of Shame", "After a missed putt inside C1, that player carries their putter in either hand until they finish the next hole. (They can putt with it.) If they drop it or put it in the bag, +1 stroke."),
        GameCard(58, "Before tee shot", CardKind.ATTACK, "Your Tee Pad Is Over There!", "Use 2 of your discs to mark a new tee for everyone else, up to 10 paces (30 ft) from the original. They all tee from it."),
        GameCard(59, "Before all tee", CardKind.SABOTAGE, "I Think It's Broke", "The first player to hit a tree loses that disc — they can't throw it again for the rest of the round."),
    )

    private val byId: Map<Int, GameCard> = ALL.associateBy { it.id }

    /** The wheel pool for the Double Wheel card. Excludes the reaction cards. */
    val WHEEL_POOL: List<GameCard> = ALL.filter { it.id !in Rules.WHEEL_EXCLUDES }

    fun card(id: Int): GameCard = byId.getValue(id)

    /** A fresh deck, shuffled. Every player has their own. */
    fun freshShuffledDeck(): List<Int> = ALL.map { it.id }.shuffled()
}
