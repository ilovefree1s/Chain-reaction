package com.chainreaction.data

/** How a card is aimed. Drives the colour system across the whole app. */
enum class CardKind(val label: String) {
    ATTACK("attack"),
    SELF("self"),
    DUAL("dual"),
    REACT("react"),
    GROUP("group"),
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

    /** Reaction cards with nothing to react to when drawn off the wheel. Still playable normally. */
    val WHEEL_EXCLUDES = setOf(12, 18, 27)

    /** Display order for the rules reference. */
    val TIMINGS = listOf(
        "Before shot",
        "Before tee shot",
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
        GameCard(1, "Before shot", CardKind.ATTACK, "Roll It!", "Force an opponent to throw a roller on the upcoming drive or approach."),
        GameCard(2, "Before shot", CardKind.ATTACK, "Tomahawk", "Force an opponent to throw a tomahawk on the upcoming drive or approach."),
        GameCard(3, "Before shot", CardKind.ATTACK, "Sidearm", "Force an opponent to throw a sidearm on the upcoming drive or approach."),
        GameCard(4, "Before shot", CardKind.ATTACK, "Choose Disc", "Choose ANY disc from any bag. That disc must be used on their next shot."),
        GameCard(5, "Before shot", CardKind.ATTACK, "Straddle Putt", "Force an opponent to straddle putt. Playable any time someone is about to throw a putter."),
        GameCard(6, "Before shot", CardKind.ATTACK, "Mini Drive", "Force an opponent to throw a mini for their upcoming drive or putt. Through the basket floor still counts as in."),
        GameCard(7, "After throw", CardKind.ATTACK, "nnnnNunan!", "Heckle an opponent during one of their shots. Reveal and discard this card after."),
        GameCard(8, "For the next hole", CardKind.ATTACK, "Caddy", "Force an opponent to carry your discs until they beat you on a hole. While they carry your bag, you can't play any more cards on them."),
        GameCard(9, "After throw · secret", CardKind.SELF, "Don't Nice Me!", "If any player says \"nice\" as your disc is in flight and it hits a tree, that throw doesn't count. Move to your disc and throw again."),
        GameCard(10, "Before tee shot", CardKind.ATTACK, "Code Words!", "An opponent can't say \"yes\" or \"no\" this hole. 1 stroke penalty every time they do."),
        GameCard(11, "Before tee shot", CardKind.ATTACK, "Too Many Choices", "Force an opponent to discard 3 cards."),
        GameCard(12, "Any time", CardKind.REACT, "Change Is Good", "Force an opponent to change the target of one of their cards, if there is another option."),
        GameCard(13, "Before tee shot", CardKind.ATTACK, "Unfamiliar Discs", "You pick the discs every other player tees off with this hole."),
        GameCard(14, "Before tee shot", CardKind.ATTACK, "Bizarro Hole Solo", "Force an opponent to drive with a putter and putt with a driver this hole."),
        GameCard(15, "After throw", CardKind.ATTACK, "Cancel Shot", "Cancel a shot an opponent just took. They throw again with a different disc of their choice. No extra stroke."),
        GameCard(16, "Before shot", CardKind.ATTACK, "Blind Putt", "Force an opponent to take the next putt with their eyes closed."),
        GameCard(17, "Before tee shot", CardKind.ATTACK, "One Disc Hole", "Force an opponent to play the hole with only 1 disc. You choose it."),
        GameCard(18, "After card", CardKind.REACT, "Rubber and Glue", "If a card targeting only you was just played, that opponent carries out the instructions instead of you."),
        GameCard(19, "After all tee", CardKind.SELF, "Lie Swap", "After all tee shots, trade lies with an opponent of your choice."),
        GameCard(20, "Before tee shot", CardKind.SELF, "1v1 Challenge", "Challenge someone. First player to get a better score on a hole gets a free immunity hole and 1 free mulligan on that hole."),
        GameCard(21, "Before tee shot", CardKind.ATTACK, "Ambidextrous Drive", "Force an opponent to take the upcoming drive with their off hand."),
        GameCard(22, "Before tee shot", CardKind.ATTACK, "Create Tee", "Use 2 of your discs to build a new tee for every opponent. Within 10 paces (30 ft) of the original."),
        GameCard(23, "Before tee shot", CardKind.SELF, "Create Tee Solo", "Use 2 of your discs to build a new tee for yourself. Within 10 paces (30 ft) of the original. You may pick 1 person to join you."),
        GameCard(24, "Before tee shot", CardKind.ATTACK, "Thief'n", "Steal someone's disc. It's yours for the rest of the round."),
        GameCard(25, "After throw", CardKind.ATTACK, "Bad Lie", "Move an opponent's lie 10 paces (30 ft) in any direction, as long as it isn't out of bounds."),
        GameCard(26, "After throw", CardKind.SELF, "Better Lie", "Move your own lie 10 paces (30 ft) in any direction."),
        GameCard(27, "After card", CardKind.REACT, "No Way", "Cancel any card just played. That card goes to the discard pile."),
        GameCard(28, "Before tee shot", CardKind.ATTACK, "Standstill", "No run-up on an opponent's next throw."),
        GameCard(29, "Before shot", CardKind.ATTACK, "Turbo Time", "An opponent's next putt must be a turbo putt."),
        GameCard(30, "Before tee shot", CardKind.ATTACK, "Mando Maker", "Choose a reasonable object the target player must pass on a side you specify."),
        GameCard(31, "Before tee shot", CardKind.ATTACK, "Do Not Pass Go", "Whoever has the shortest drive on the next hole gets no cards for that hole."),
        GameCard(32, "Before tee shot", CardKind.SELF, "Tree Insurance", "Play before your tee shot. If you hit a tree, take a free mulligan."),
        GameCard(33, "Before tee shot", CardKind.SELF, "Call Your Shot", "Call CTP. If you win it, nobody can play cards on you next hole, and your next card affects everyone even if it doesn't say so."),
        GameCard(34, "After throw", CardKind.SELF, "GAMBLE!", "Take a second chance at a putt you just missed. Make it and it was a free mulligan. Miss and the second stroke counts too — play whichever disc landed farthest away, then add +1 stroke after the hole ends."),
        GameCard(35, "After a hole", CardKind.ATTACK, "FIVE DOLLARS!", "Play on a player who failed to birdie a hole every other player birdied. Five dollars in the pot, and they only draw 1 card next turn."),
        GameCard(36, "After all tee", CardKind.ATTACK, "I'm In Control", "You decide who plays whose tee shots. More than one player can be sent to the same lie."),
        GameCard(37, "Before tee shot", CardKind.SELF, "Clean Slate", "Remove any and all card effects currently on you."),
        GameCard(38, "Before tee shot", CardKind.SELF, "Trade Offer", "Ask another player to team up for the next hole. Birdie counts as an eagle, no birdie is +1. Cards against one of you count against both. They may refuse, then you ask someone else. You both take the same score."),
        GameCard(39, "After all play a card", CardKind.GROUP, "Group Hug", "Only playable after every other player has played a negative card this turn. Each of them carries out their own card instead of the player they aimed it at."),
        GameCard(40, "Before shot", CardKind.SELF, "Metal Counts", "Play before a C2 putt only. If you hit metal and it doesn't go in, it still counts."),
        GameCard(41, "Before tee shot", CardKind.ATTACK, "Commentator", "Another player has to announce every shot you take this hole. If they forget one, they take +1 stroke."),
        GameCard(42, "Before shot", CardKind.ATTACK, "Trust Me Bro", "Give another player advice for their tee shot. They have to follow it as best they can."),
        GameCard(43, "Before tee shot", CardKind.GROUP, "RUSH ATTACK!", "Everyone throws from anywhere near the tee pad. No throw order, rush ready golf. First to finish gets birdie, 2nd and 3rd get par, last gets bogey. The catch: NO RUNNING."),
        GameCard(44, "After throw", CardKind.ATTACK, "Walk of Shame", "After a missed putt inside C1, that player carries their putter in their off hand until the next tee."),
        GameCard(45, "Before shot", CardKind.DUAL, "Shoe Golf", "On yourself: putt with your own shoe at no stroke cost. On another player: they putt with a shoe and it still counts as a stroke."),
        GameCard(46, "Before shot", CardKind.DUAL, "Behind the Back Putt", "On yourself: putt behind the back at no stroke cost. On another player: they putt behind the back and it counts as a normal stroke."),
        GameCard(47, "Before shot", CardKind.DUAL, "Spin to Win", "Spin rapidly 10 times, then putt within 3 seconds. Free on yourself, a normal stroke on an opponent."),
        GameCard(48, "Before tee shot", CardKind.GROUP, "Double Wheel", "Spin a random card effect and reveal it, then spin the name wheel. That player is the only one it doesn't apply to. Everyone else, including you, has to do it."),
        GameCard(49, "Before shot", CardKind.DUAL, "Aerobie", "On yourself: use an aerobie for your drive. On another player: they drive with the aerobie using their off hand. Normal stroke either way."),
        GameCard(50, "Any time", CardKind.DUAL, "Do-Over", "On yourself: retake any shot for free. On another player: force them to re-throw a shot that was too good."),
        GameCard(51, "After a hole", CardKind.DUAL, "Me and You", "On yourself: tie the best score on the hole for free. On another player: they tie the worst score on the hole."),
        GameCard(52, "After throw", CardKind.SELF, "Big Putt!", "If you make a putt from outside C1 while a player still to putt is inside C1, they have to putt with their off hand."),
    )

    private val byId: Map<Int, GameCard> = ALL.associateBy { it.id }

    /** The wheel pool for card #48. Excludes the reaction cards. */
    val WHEEL_POOL: List<GameCard> = ALL.filter { it.id !in Rules.WHEEL_EXCLUDES }

    fun card(id: Int): GameCard = byId.getValue(id)

    /** A fresh 52-card deck, shuffled. Every player has their own. */
    fun freshShuffledDeck(): List<Int> = ALL.map { it.id }.shuffled()
}
