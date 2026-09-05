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
    const val MAX_PLAYERS = 4
    const val DEFAULT_PAR = 3

    /** Cards you pay to spin the GAMBLE WHEEL!! off your own bat. */
    const val WHEEL_COST = 2

    /**
     * The GAMBLE WHEEL!! card — its whole text is a free spin, so playing it opens the wheel
     * without the usual [WHEEL_COST]. The card itself is the payment.
     */
    const val FREE_SPIN_CARD = 22

    // The house blacklist: reaction cards plus everything too situational,
    // too group-shaped or too slow to land as a wheel result — and the Double
    // Wheel itself, so the wheel can never demand another wheel.
    val WHEEL_EXCLUDES = setOf(1, 3, 5, 6, 12, 13, 19, 20, 21, 22, 23, 24, 31, 34, 36, 38, 42, 43, 52, 55, 56)

    /** Cards that live on the wheel alone — never shuffled into a deck. */
    val WHEEL_ONLY = setOf(57)

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
        GameCard(2, "Before shot", CardKind.ATTACK, "69% OF THE SPLITS", "Force an opponent to straddle putt. Playable any time someone is about to throw a putter."),
        GameCard(3, "After throw", CardKind.ATTACK, "AIR HORN!", "Heckle an opponent during one of their shots. Reveal and discard this card after."),
        GameCard(4, "Before shot", CardKind.ATTACK, "Baby Discs", "Force an opponent to throw a mini for their upcoming drive or putt. Through the basket floor still counts as in."),
        GameCard(5, "Any time", CardKind.ATTACK, "Bag Boy!", "Force an opponent to carry your bag for 3 holes! If they beat you before those 3 holes are up (1st or 2nd hole), they can play one of your cards and you take your bag back."),
        GameCard(6, "Before tee shot", CardKind.ATTACK, "Bag Exchange!", "Pick a player to swap bags with. Play until the first bogey by a single player (both bogey is nothing). That player must volunteer as tribute to exchange lies with the other player the next time they go OB/hazard or miss a mando."),
        GameCard(7, "Before shot", CardKind.ATTACK, "Bag Raid!", "Choose ANY disc that belongs to anyone. The target player must use that disc for their next shot."),
        GameCard(8, "After throw", CardKind.ATTACK, "Big Ooof, Bud.", "Move an opponent's lie up to 10 paces (30 ft) in any direction, as long as it isn't out of bounds."),
        GameCard(9, "After throw", CardKind.SELF, "Big Putted!", "After making a C2 putt, everyone else must use their left hand on their next putt this hole. (Make sure everyone putts in order this hole if you want to use the card, obviously.)"),
        GameCard(10, "Before tee shot", CardKind.ATTACK, "Bizarro Golf!", "Force an opponent to drive with a putter and putt with a driver this hole."),
        GameCard(11, "Before all tee", CardKind.SELF, "Call Your Shot", "Call CTP. If you win it, you're immune to cards and bad wheel effects next hole, and your next attack card hits every opponent — not you."),
        GameCard(12, "Any time", CardKind.ATTACK, "Can I Borrow This Card?", "Pick anyone you want. Look through their cards and play one on anyone."),
        GameCard(13, "After card", CardKind.REACT, "Change Is Good", "Hijack a card as it's played and re-aim it at the player of your choice — including the player who played it."),
        GameCard(14, "Before shot", CardKind.ATTACK, "CHRIS SPECIAL!", "Force an opponent to throw a tomahawk on the upcoming drive or approach."),
        GameCard(15, "Before shot", CardKind.ATTACK, "Close 'Em", "Force an opponent to take the next putt with their eyes closed."),
        GameCard(16, "Before tee shot", CardKind.ATTACK, "Code Words!", "An opponent can't say \"yes\" or \"no\" this hole. 1 stroke penalty every time they do. ANY variation of the words yes or no counts."),
        GameCard(17, "Before tee shot", CardKind.ATTACK, "Jomez Commentator", "Another player has to announce every shot you take this hole like they are a commentator. If they forget one, they take +1 stroke."),
        GameCard(18, "Before tee shot", CardKind.ATTACK, "Dealer's Choice!", "You pick the discs every other player tees off with this hole."),
        GameCard(19, "Before tee shot", CardKind.DUAL, "Do Not Pass Go", "On another player: whoever has the shortest drive on this hole gets no free mulligan to bank while everyone else does. On yourself: play after everyone tees — if you have the shortest drive, throw your 2nd shot with the person that had the farthest drive."),
        GameCard(20, "After throw", CardKind.DUAL, "Doesn't Look Like a Penalty to Me", "Play on a shot that missed a mando or went OB. Instead of actually being OB, in a hazard, or past a mando, it can just be played where it lies, with no penalty at all."),
        GameCard(21, "After throw · secret", CardKind.SELF, "Don't Nice Me!", "If any player says any form of \"nice\" as your disc is in flight and it hits a tree, you can move to where your disc landed and throw again for a free stroke."),
        GameCard(22, "Before tee shot", CardKind.GROUP, "GAMBLE WHEEL!!", "Free spin on the GAMBLE WHEEL!! The name doesn't matter — you choose who gets the benefit or the punishment."),
        GameCard(23, "Before shot", CardKind.ATTACK, "Easily Distracted?", "Everyone can do anything they want to distract a player while they are about to putt."),
        GameCard(24, "After a hole", CardKind.SELF, "FIVE FOR YOU, YOU, AND YOU!", "Play after you birdie a hole nobody else birdied. Everyone else owes five dollars to the pot!"),
        GameCard(25, "After throw", CardKind.SELF, "Foot Wedge!", "Move your own lie up to 10 paces (30 ft) in any direction."),
        GameCard(26, "After throw", CardKind.SELF, "GAMBLE!", "Take a second chance at a putt you just missed. Make it and it was a free mulligan. Miss and the second stroke counts too — play whichever disc landed farthest away, then add +1 stroke after the hole ends."),
        GameCard(27, "After all tee", CardKind.SELF, "I'll Have What He's Having", "After all tee shots, trade lies with an opponent of your choice."),
        GameCard(28, "Before shot", CardKind.DUAL, "Globetrotter Shit", "On yourself: putt behind the back at no stroke cost. On another player: they putt behind the back and it counts as a normal stroke."),
        GameCard(29, "Before tee shot", CardKind.DUAL, "GOOD GUYS VS. BAD GUYS!", "Ask if anyone wants to team up this hole — take your pick. (If nobody says yes, you get 1 free mulligan this hole.) If you get a partner it's 2v2 for this hole, and cards hit both players on each team when played. Winners get a banked mulligan, losers take +1 extra stroke."),
        GameCard(30, "Before shot", CardKind.SELF, "If the Basket Was There It Woulda Went In", "Play before a C2 putt only. If you hit any metal and it doesn't go in, it counts as a made putt."),
        GameCard(31, "After all tee", CardKind.ATTACK, "I'm In Control", "You decide who plays whose tee shots. More than one player can be sent to the same lie."),
        GameCard(32, "Before tee shot", CardKind.ATTACK, "It's Like a Stranger Is Doing It!", "Force an opponent to take the upcoming drive with their off hand."),
        GameCard(33, "Before tee shot", CardKind.DUAL, "Lefty ON the Box", "Your first throw off the box is with your offhand, for free — that's throw 0. The player in last place gets it too. If 2 players are tied for last, they can flip a coin or roll dice to see who gets it."),
        GameCard(34, "After a hole", CardKind.DUAL, "Me and You", "On yourself: take the best score made on the hole. On another player: they take the worst score made on the hole."),
        GameCard(35, "Before tee shot", CardKind.SELF, "My Tee Pad Is Over Here!", "Use 2 of your discs to mark a new tee for yourself, up to 10 paces (30 ft) from the original. Feeling nice? You may pick 1 player to join you."),
        GameCard(36, "After card", CardKind.REACT, "No Way", "Cancel any card just played. That card goes to the discard pile."),
        GameCard(37, "Before tee shot", CardKind.ATTACK, "New mando on this hole, bud.", "Choose a reasonable object the target player must pass on a side you specify."),
        GameCard(38, "Before tee shot", CardKind.DUAL, "Not Today!", "Remove any and all card effects currently on you, or use it on another player to cancel the effects on them."),
        GameCard(39, "Before tee shot", CardKind.ATTACK, "One Disc Wonder!", "Force an opponent to play the hole with only 1 DISC this hole! (If someone already gave them THE CURD!, the TILT or the \"beater\", that counts as their 1 disc.)"),
        GameCard(40, "Before tee shot", CardKind.ATTACK, "Over Sharer", "Give everyone a disc from your own bag to tee off with on this hole."),
        GameCard(41, "Before all tee", CardKind.ATTACK, "Plant Your Feet!", "No run-up on everyone else's tee shot."),
        GameCard(42, "Any time", CardKind.DUAL, "Player 2's Turn!", "On yourself: retake any shot for free. On another player: force them to re-throw a shot that was too good."),
        GameCard(43, "After throw", CardKind.ATTACK, "Prove It", "Cancel a shot an opponent just took. They throw again with a different disc of their choice from any bag. The extra throw counts as a stroke."),
        GameCard(44, "Before shot", CardKind.ATTACK, "Roll It!", "Force an opponent to throw a roller on the upcoming drive or approach."),
        GameCard(45, "Before shot", CardKind.DUAL, "Shoe Golf", "On yourself: putt with your own shoe at no stroke cost. On another player: they putt with a shoe and it still counts as a stroke. If they refuse to take their shoe off, they take +1 stroke after the hole."),
        GameCard(46, "Before shot", CardKind.ATTACK, "Forehand only!", "Force an opponent to throw a forehand on the upcoming drive or approach."),
        GameCard(47, "Before shot", CardKind.DUAL, "That's Definitely a Gimme", "Pick up a putt as a gimme, as long as it's inside C1 — you definitely woulda made it. The player in last place gets one too. If 2 players are tied for last, they can flip a coin or roll dice to see who gets it."),
        GameCard(48, "Before tee shot", CardKind.ATTACK, "Too Many Choices", "Pick 2 discs out of 1 person's bag. They choose 1 to tee off with."),
        GameCard(49, "Before tee shot", CardKind.SELF, "Tree Insurance", "Play before your tee shot. If you hit a tree, take a free mulligan."),
        GameCard(50, "Before shot", CardKind.ATTACK, "Trust Me Bro", "Give another player advice for their tee shot. They have to follow it as best they can. (Somewhat reasonable advice — don't tell them something like throw it backwards.)"),
        GameCard(51, "Before shot", CardKind.ATTACK, "Turbo Time", "All opponents in C1 must turbo putt their next shot."),
        GameCard(52, "Before tee shot", CardKind.GROUP, "WALK IT DOWN!!", "No other cards can be played once this hits the table. Everyone tees immediately from anywhere near the tee pad — no throw order. First and second to finish get birdie, third gets par, last gets bogey. No running, and no calling foot faults."),
        GameCard(53, "After throw", CardKind.ATTACK, "Walk of Shame", "After a missed putt inside C1, that player carries their putter in either hand until they finish the next hole. (They can putt with it.) If they drop it or put it in the bag, +1 stroke."),
        GameCard(54, "Before tee shot", CardKind.ATTACK, "Your Tee Pad Is Over There!", "Use 2 of your discs to mark a new tee for everyone else, up to 10 paces (30 ft) from the original. They all tee from it."),
        GameCard(55, "After throw", CardKind.ATTACK, "I Think It's Broke", "Play on another player that just hit a tree. They lose that disc — they can't throw it again for the rest of the round. If they forget and throw it again, it's +1 stroke."),
        GameCard(56, "After card", CardKind.REACT, "UNO REVERSO", "Reverse any cards that effect you back to the person that used the card. (Doesn't work on wheel spins.)"),
        GameCard(57, "Before all tee", CardKind.GROUP, "LONE WOLF!", "Whoever it lands on is the LONE WOLF! Cards played by the wolf effect everyone else! A lone wolf win gives him the right to go through everyone's cards and play 1 card from everyone's hand that effects that player. If the 3 players win then nobody cares, cuz 3 people should beat 1 every time — but they can roll dice to see who gets 1 free banked mulligan."),
        GameCard(58, "Before tee shot", CardKind.ATTACK, "PUT EM ON TILT", "Force an opponent to play the entire hole with the TILT!"),
        GameCard(59, "Before tee shot", CardKind.ATTACK, "DESTINATION FUCKED!", "Force an opponent to play the entire hole with the \"beater\". Whoever played the card gets 30 seconds to abuse the disc before they tee off."),
        GameCard(60, "Before all tee", CardKind.ATTACK, "The Curd!", "Everyone rolls 2 dice, including whoever played this card. The lowest roll uses the cottage cheese lid as their driver this hole."),
    )

    private val byId: Map<Int, GameCard> = ALL.associateBy { it.id }

    /** The wheel pool for the GAMBLE WHEEL!! card. Excludes the reaction cards. */
    val WHEEL_POOL: List<GameCard> = ALL.filter { it.id !in Rules.WHEEL_EXCLUDES }

    /** What actually gets shuffled into hands — everything but the wheel-only cards. */
    val DEALT: List<GameCard> = ALL.filter { it.id !in Rules.WHEEL_ONLY }

    fun card(id: Int): GameCard = byId.getValue(id)

    /** A fresh deck, shuffled. Every player has their own. */
    fun freshShuffledDeck(): List<Int> = DEALT.map { it.id }.shuffled()
}
