# Build spec — disc golf card game companion

Build a mobile app that runs a house-rules disc golf card game for a group of up to 4 friends playing a round together in person.

**Important framing:** all players are standing next to each other on the course. They announce card plays out loud. The app does **not** sync between devices and does **not** enforce or adjudicate card effects. Each phone runs its own independent deck. The app is a card dealer plus a scorecard, nothing more.

## Stack

Expo / React Native, TypeScript. Local persistence only (AsyncStorage). No backend, no auth, no networking. Must survive app backgrounding and restart mid-round.

If a PWA is easier to sideload, that's acceptable — but it must work offline, since courses often have no signal.

## Core rules

- **Stroke play.** Lowest total wins.
- **Starting hand:** 4 cards, dealt at round start.
- **Mulligans:** a mulligan is a free extra throw, not a shot you are forced to play. Cards hand them out; taking one is always the thrower's choice, and they can keep the throw they already made.
- **Hand cap:** 7. Cannot draw past it — must discard first.
- **Each player has their own 59-card deck**, shuffled independently. Duplicate cards across players are expected and fine.
- **Played and discarded cards** go to that player's own discard pile. Reshuffle the discard back into the deck if the deck ever empties.
- **The GAMBLE WHEEL!! outranks everything.** A wheel result is top priority and cannot be overridden by a played card.
- **Card rarity.** Five tiers — common, uncommon, rare, epic, legendary — each rarer than the last and each with its own colour on the face. A card carries a `rarity` field only when it is not common; anything without one is common. Rarity is not a restriction: every card is in every deck exactly once. It sets how likely that card is to be the one a draw hands you.
- **Bad rounds draw better cards.** The further down the card a player is, the more the draw favours the rare end: last place most, second-to-last a little less, everyone above them not at all. It multiplies through the tiers, so it barely touches commons and is felt most on legendaries. Alongside last place's extra card every hole, this is the game's whole answer to a round getting away from somebody.
- **Max 2 attack cards may be played on any one player per hole.** Cancels, reverses and anything else that doesn't change how a player actually throws are free of the count. The app does not enforce this — players track it themselves — but show it in an in-app rules screen.

### Draw rule (this is the one piece of real logic)

At the end of each hole, based on that player's finish position for the hole:

| Finish | Cards drawn |
|---|---|
| Best score, or tied for best | 0 |
| 2nd or 3rd | 1 |
| Last | 2 |
| Tied for last (2+ players) | 2 each |
| All players tied | 1 each — overrides "best draws nothing" |
| Double bogey or worse | +1 bonus card, on top of the above |
| Last in the round, or tied for last | +1 bonus card, every hole, on top of the above |

Implementation: let `lo = min(scores)`, `hi = max(scores)`.
- if `lo === hi` → 1
- else if `myScore === lo` → 0
- else if `myScore === hi` → 2
- else → 1

## Screens

### 1. Setup (first run only)
- Four name fields.
- The local player is always player 1, so there is nothing to tap. The draw still works off an index; it is simply fixed at 0. (The spec had a "ME" toggle per name; it asked the same question four times and only ever got one answer.)
- 9 or 18 holes.
- Start round.

### 2. Score
UDisc-style. All four players are tracked on every phone so nobody miscounts or cheats.
- Hole navigation (prev/next), current hole indicator.
- Par per hole, adjustable, default 3.
- One row per player: name, score relative to par, and big −/+ steppers. Minimum score 1.
- Highlight the local player's row.
- **Lock hole & draw** button: freezes the hole, computes the local player's draw count, queues it, and advances to the next hole.
- Round totals below: each player's running total and relative-to-par, counting locked holes only.

### 3. Hand
- Counters: cards in hand, cards in deck, cards in discard.
- A draw button that appears only when cards are owed. If the hand is at 7, it should say so and stay disabled until the player discards.
- Cards listed as full-bleed cards showing: timing tag, function label, name, full effect text.
- Each card has **Play** and **Discard** — both move it to the discard pile. (Play exists so the action reads naturally; there's no functional difference.)
- A badge on the Hand tab showing how many cards are owed.

### 4. GAMBLE WHEEL!! (modal, launched from Hand)
The GAMBLE WHEEL!! card’s mechanic. Two-stage reveal, in this order:
1. Spin a random card from the deck's 52 — **reveal the effect first**.
2. Then spin a wheel of the four player names.

That named player is the **only one exempt**. Everyone else, including the player who played the card, must carry out the effect. The order matters: the whole group sees what's at stake before the name lands.

**Exclude the reaction cards** (Change Is Good, Rubber and Glue, No Way) from the wheel pool only — they have nothing to react to when drawn this way. They remain fully playable as normal cards. Named rather than numbered: ids move when the deck is re-sorted.

### 5. Rules reference
Static screen: the rules above, plus a browsable list of every card, grouped by timing.

## Design direction

Built to be read outdoors in bright sun, one-handed, between throws. High contrast, large tap targets (min 44pt), no small text. Dark ground so it isn't blinding at dusk.

Color-code cards by function — this is the main visual system:

| Function | Meaning |
|---|---|
| `attack` | played on an opponent |
| `self` | benefits you |
| `dual` | lands on you or on another player — a favour either way, or self-help and an attack depending on who you pick |
| `react` | played in response to another card |
| `group` | affects everyone |
| `sabotage` | costs you, to everybody else's benefit |

Suggested palette: deep pine ground (`#0C1A14`), panels (`#132A21`), muted sage text (`#8AA79A`), off-white (`#F2F5F1`). Function colors: attack `#FF5A4D`, self `#FFD23F`, dual `#FF57C1`, react `#4DD9E8`, group `#A78BFA`. Condensed heavy display face for card names, plain sans for body.

## What NOT to build

- No score sync. (Since amended: the web build's live room shares card plays and hand counts — see MULTIPLAYER_PLAN.md — but the scorecard stays local on every phone, and no phone referees another.)
- No enforcement of card effects.
- No stroke penalties applied automatically. Cards like GAMBLE!, Code Words!, Jomez Commentator, RUSH ATTACK!, Trade Offer and Me and You all rewrite scores — players enter those manually with the ± steppers.
- No money tracking. FIVE FOR YOU, YOU, AND YOU! mentions a pot; it's honor-system.

## Card data

```json
{
  "handSize": 4,
  "handCap": 7,
  "maxCardsOnOnePlayerPerHole": 2,
  "wheelCost": 2,
  "freeSpinCard": 22,
  "wheelExcludes": [1, 3, 5, 6, 12, 13, 19, 20, 21, 22, 23, 24, 31, 34, 36, 38, 42, 43, 52, 55, 56],
  "wheelOnly": [57],
  "rarities": [
    { "id": "common",    "color": "#E6ECF5", "weight": 86 },
    { "id": "uncommon",  "color": "#47D97F", "weight": 58 },
    { "id": "rare",      "color": "#4D8BFF", "weight": 30 },
    { "id": "epic",      "color": "#B96BFF", "weight": 18 },
    { "id": "legendary", "color": "#FFC72E", "weight": 9 }
  ],
  "drawLuck": { "last": 1.67, "nextToLast": 1.52, "midTable": 1.1 },
  "timings": [
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
    "Any time"
  ],
  "cards": [
    { "id": 1, "timing": "Before tee shot", "kind": "attack", "name": "1v1 Challenge", "text": "Challenge someone. It runs until one of you beats the other's score on a hole — a tie settles nothing. The winner gets immunity from all attack cards next hole and banks a mulligan to use on any hole, whenever they need it." },
    { "id": 2, "timing": "Before shot", "kind": "attack", "name": "69% OF THE SPLITS", "text": "Force an opponent to straddle putt. Playable any time someone is about to throw a putter." },
    { "id": 3, "timing": "After throw", "kind": "attack", "name": "AIR HORN!", "text": "Heckle an opponent during one of their shots. Reveal and discard this card after." },
    { "id": 4, "timing": "Before shot", "kind": "attack", "rarity": "epic", "name": "Baby Discs", "text": "Force an opponent to throw a mini for their upcoming drive or putt. Through the basket floor still counts as in." },
    { "id": 5, "timing": "For the next hole", "kind": "attack", "rarity": "uncommon", "name": "Bag Boy!", "text": "Force an opponent to carry your bag for 3 holes! If they beat you before those 3 holes are up (1st or 2nd hole), they can play one of your cards and you take your bag back." },
    { "id": 6, "timing": "Before tee shot", "kind": "attack", "name": "Bag Exchange!", "text": "Pick a player to swap bags with. Play until the first bogey by a single player (both bogey is nothing). That player must volunteer as tribute to exchange lies with the other player the next time they go OB/hazard or miss a mando." },
    { "id": 7, "timing": "Before shot", "kind": "attack", "rarity": "uncommon", "name": "Bag Raid!", "text": "Choose ANY disc that belongs to anyone. The target player must use that disc for their next shot." },
    { "id": 8, "timing": "After throw", "kind": "attack", "rarity": "epic", "name": "Big Ooof, Bud.", "text": "Move an opponent's lie up to 10 paces (30 ft) in any direction, as long as it isn't out of bounds." },
    { "id": 9, "timing": "After throw", "kind": "self", "rarity": "uncommon", "name": "Big Putted!", "text": "If you make a putt from outside C1 while any other players are inside C1, they all must use their left hand to putt this hole." },
    { "id": 10, "timing": "Before tee shot", "kind": "attack", "name": "Bizarro Golf!", "text": "Force an opponent to drive with a putter and putt with a driver this hole." },
    { "id": 11, "timing": "Before all tee", "kind": "self", "name": "Call Your Shot", "text": "Call CTP. If you win it, you're immune to cards and bad wheel effects next hole, and your next attack card hits every opponent — not you." },
    { "id": 12, "timing": "Any time", "kind": "attack", "rarity": "uncommon", "name": "Can I Borrow This Card?", "text": "Pick anyone you want. Look through their cards and play one on anyone." },
    { "id": 13, "timing": "Any time", "kind": "react", "rarity": "epic", "name": "Change Is Good", "text": "Hijack a card as it's played and re-aim it at the player of your choice — including the player who played it." },
    { "id": 14, "timing": "Before shot", "kind": "attack", "name": "CHRIS SPECIAL!", "text": "Force an opponent to throw a tomahawk on the upcoming drive or approach." },
    { "id": 15, "timing": "Before shot", "kind": "attack", "rarity": "uncommon", "name": "Close 'Em", "text": "Force an opponent to take the next putt with their eyes closed." },
    { "id": 16, "timing": "Before tee shot", "kind": "attack", "rarity": "uncommon", "name": "Code Words!", "text": "An opponent can't say \"yes\" or \"no\" this hole. 1 stroke penalty every time they do. ANY variation of the words yes or no counts." },
    { "id": 17, "timing": "Before tee shot", "kind": "attack", "name": "Jomez Commentator", "text": "Another player has to announce every shot you take this hole like they are a commentator. If they forget one, they take +1 stroke." },
    { "id": 18, "timing": "Before tee shot", "kind": "attack", "rarity": "epic", "name": "Dealer's Choice!", "text": "You pick the discs every other player tees off with this hole." },
    { "id": 19, "timing": "Before tee shot", "kind": "dual", "name": "Do Not Pass Go", "text": "On another player: whoever has the shortest drive on this hole gets no free mulligan to bank while everyone else does. On yourself: play after everyone tees — if you have the shortest drive, throw your 2nd shot with the person that had the farthest drive." },
    { "id": 20, "timing": "After throw", "kind": "dual", "name": "Doesn't Look Like a Penalty to Me", "text": "Play on a shot that missed a mando or went OB. Instead of actually being OB, in a hazard, or past a mando, it can just be played where it lies, with no penalty at all." },
    { "id": 21, "timing": "After throw · secret", "kind": "self", "name": "Don't Nice Me!", "text": "If any player says any form of \"nice\" as your disc is in flight and it hits a tree, you can move to where your disc landed and throw again for a free stroke." },
    { "id": 22, "timing": "Before tee shot", "kind": "group", "rarity": "rare", "name": "GAMBLE WHEEL!!", "text": "Free spin on the GAMBLE WHEEL!! The name doesn't matter — you choose who gets the benefit or the punishment." },
    { "id": 23, "timing": "Before shot", "kind": "attack", "rarity": "uncommon", "name": "Easily Distracted?", "text": "Everyone can do anything they want to distract a player while they are about to putt." },
    { "id": 24, "timing": "After a hole", "kind": "self", "name": "FIVE FOR YOU, YOU, AND YOU!", "text": "Play after you birdie a hole nobody else birdied. Everyone else owes five dollars to the pot!" },
    { "id": 25, "timing": "After throw", "kind": "self", "rarity": "rare", "name": "Foot Wedge!", "text": "Move your own lie up to 10 paces (30 ft) in any direction." },
    { "id": 26, "timing": "After throw", "kind": "self", "rarity": "uncommon", "name": "GAMBLE!", "text": "Take a second chance at a putt you just missed. Make it and it was a free mulligan. Miss and the second stroke counts too — play whichever disc landed farthest away, then add +1 stroke after the hole ends." },
    { "id": 27, "timing": "After all tee", "kind": "self", "rarity": "epic", "name": "I'll Have What He's Having", "text": "After all tee shots, trade lies with an opponent of your choice." },
    { "id": 28, "timing": "Before shot", "kind": "dual", "rarity": "rare", "name": "Globetrotter Shit", "text": "On yourself: putt behind the back at no stroke cost. On another player: they putt behind the back and it counts as a normal stroke." },
    { "id": 29, "timing": "Before tee shot", "kind": "dual", "name": "GOOD GUYS VS. BAD GUYS!", "text": "Ask another player to team up for the next hole. Birdie counts as an eagle, no birdie is +1. Cards against one of you count against both, and you both take the same score. If they refuse, ask someone else. The team that wins the hole banks a mulligan each — your score vs. the other side's best, and a tie means no prize." },
    { "id": 30, "timing": "Before shot", "kind": "self", "rarity": "rare", "name": "If the Basket Was There It Woulda Went In", "text": "Play before a C2 putt only. If you hit any metal and it doesn't go in, it counts as a made putt." },
    { "id": 31, "timing": "After all tee", "kind": "attack", "rarity": "legendary", "name": "I'm In Control", "text": "You decide who plays whose tee shots. More than one player can be sent to the same lie." },
    { "id": 32, "timing": "Before tee shot", "kind": "attack", "rarity": "uncommon", "name": "It's Like a Stranger Is Doing It!", "text": "Force an opponent to take the upcoming drive with their off hand." },
    { "id": 33, "timing": "Before tee shot", "kind": "dual", "name": "Lefty ON the Box", "text": "Your first throw off the box is with your offhand, for free — that's throw 0. The player in last place gets it too. If 2 players are tied for last, they can flip a coin or roll dice to see who gets it." },
    { "id": 34, "timing": "After a hole", "kind": "dual", "rarity": "epic", "name": "Me and You", "text": "On yourself: take the best score made on the hole. On another player: they take the worst score made on the hole." },
    { "id": 35, "timing": "Before tee shot", "kind": "self", "name": "My Tee Pad Is Over Here!", "text": "Use 2 of your discs to mark a new tee for yourself, up to 10 paces (30 ft) from the original. Feeling nice? You may pick 1 player to join you." },
    { "id": 36, "timing": "After card", "kind": "react", "rarity": "epic", "name": "No Way", "text": "Cancel any card just played. That card goes to the discard pile." },
    { "id": 37, "timing": "Before tee shot", "kind": "attack", "rarity": "rare", "name": "New mando on this hole, bud.", "text": "Choose a reasonable object the target player must pass on a side you specify." },
    { "id": 38, "timing": "Before tee shot", "kind": "dual", "rarity": "epic", "name": "Not Today!", "text": "Remove any and all card effects currently on you, or use it on another player to cancel the effects on them." },
    { "id": 39, "timing": "Before tee shot", "kind": "attack", "rarity": "uncommon", "name": "One Disc Wonder!", "text": "Force an opponent to play the hole with only 1 DISC this hole! (If someone already gave them a mini, the TILT or the \"beater\", that counts as their 1 disc.)" },
    { "id": 40, "timing": "Before tee shot", "kind": "attack", "rarity": "epic", "name": "Over Sharer", "text": "Give everyone a disc from your own bag to tee off with on this hole." },
    { "id": 41, "timing": "Before all tee", "kind": "attack", "name": "Plant Your Feet!", "text": "No run-up on everyone else's tee shot." },
    { "id": 42, "timing": "Any time", "kind": "dual", "rarity": "rare", "name": "Player 2's Turn!", "text": "On yourself: retake any shot for free. On another player: force them to re-throw a shot that was too good." },
    { "id": 43, "timing": "After throw", "kind": "attack", "rarity": "legendary", "name": "Prove It", "text": "Cancel a shot an opponent just took. They throw again with a different disc of their choice from any bag. The extra throw counts as a stroke." },
    { "id": 44, "timing": "Before shot", "kind": "attack", "rarity": "rare", "name": "Roll It!", "text": "Force an opponent to throw a roller on the upcoming drive or approach." },
    { "id": 45, "timing": "Before shot", "kind": "dual", "rarity": "uncommon", "name": "Shoe Golf", "text": "On yourself: putt with your own shoe at no stroke cost. On another player: they putt with a shoe and it still counts as a stroke. If they refuse to take their shoe off, they take +1 stroke after the hole." },
    { "id": 46, "timing": "Before shot", "kind": "attack", "name": "Forehand only!", "text": "Force an opponent to throw a forehand on the upcoming drive or approach." },
    { "id": 47, "timing": "Before shot", "kind": "dual", "rarity": "uncommon", "name": "That's Definitely a Gimme", "text": "Pick up a putt as a gimme, as long as it's inside C1 — you definitely woulda made it. The player in last place gets one too. If 2 players are tied for last, they can flip a coin or roll dice to see who gets it." },
    { "id": 48, "timing": "Before tee shot", "kind": "attack", "rarity": "uncommon", "name": "Too Many Choices", "text": "Pick 2 discs out of 1 person's bag. They choose 1 to tee off with." },
    { "id": 49, "timing": "Before tee shot", "kind": "self", "name": "Tree Insurance", "text": "Play before your tee shot. If you hit a tree, take a free mulligan." },
    { "id": 50, "timing": "Before shot", "kind": "attack", "rarity": "rare", "name": "Trust Me Bro", "text": "Give another player advice for their tee shot. They have to follow it as best they can. (Somewhat reasonable advice — don't tell them something like throw it backwards.)" },
    { "id": 51, "timing": "Before shot", "kind": "attack", "rarity": "rare", "name": "Turbo Time", "text": "All opponents in C1 must turbo putt their next shot." },
    { "id": 52, "timing": "Before tee shot", "kind": "group", "rarity": "uncommon", "name": "WALK IT DOWN!!", "text": "No other cards can be played once this hits the table. Everyone tees immediately from anywhere near the tee pad — no throw order. First and second to finish get birdie, third gets par, last gets bogey. No running, and no calling foot faults." },
    { "id": 53, "timing": "After throw", "kind": "attack", "name": "Walk of Shame", "text": "After a missed putt inside C1, that player carries their putter in either hand until they finish the next hole. (They can putt with it.) If they drop it or put it in the bag, +1 stroke." },
    { "id": 54, "timing": "Before tee shot", "kind": "attack", "rarity": "legendary", "name": "Your Tee Pad Is Over There!", "text": "Use 2 of your discs to mark a new tee for everyone else, up to 10 paces (30 ft) from the original. They all tee from it." },
    { "id": 55, "timing": "After throw", "kind": "attack", "rarity": "epic", "name": "I Think It's Broke", "text": "Play on another player that just hit a tree. They lose that disc — they can't throw it again for the rest of the round. If they forget and throw it again, it's +1 stroke." },
    { "id": 56, "timing": "After card", "kind": "react", "rarity": "epic", "name": "UNO REVERSO", "text": "Reverse any cards that effect you back to the person that used the card. (Doesn't work on wheel spins.)" },
    { "id": 57, "timing": "Before all tee", "kind": "group", "name": "LONE WOLF!", "text": "Whoever it lands on is the LONE WOLF! Cards played by the wolf effect everyone else! A lone wolf win gives him the right to go through everyone's cards and play 1 card from everyone's hand that effects that player. If the 3 players win then nobody cares, cuz 3 people should beat 1 every time — but they can flip fight for 1 free mulligan. Wolf calls heads or tails." },
    { "id": 58, "timing": "Before tee shot", "kind": "attack", "rarity": "rare", "name": "PUT EM ON TILT", "text": "Force an opponent to play the entire hole with the TILT!" },
    { "id": 59, "timing": "Before tee shot", "kind": "attack", "rarity": "legendary", "name": "DESTINATION FUCKED!", "text": "Force an opponent to play the entire hole with the \"beater\". Whoever played the card gets 30 seconds to abuse the disc before they tee off." }
  ]
}
```

## Acceptance checks

- Start a round, score 3 holes, force-quit the app, reopen — round state, hand, deck and discard all restored.
- Come last on a hole → 2 cards owed. Tie for last → still 2. Everyone ties → 1. Win the hole → 0. Double bogey → +1 on top of any of these.
- Whoever is last on the card overall, or tied for last, draws +1 every hole on top of all of that. Everybody level counts as nobody last.
- Sit on a full hand for several holes without drawing → the owed count never banks up; each hole's deal replaces the last one's leftovers.
- Tap "Spin the GAMBLE WHEEL!!" → asked to pick 2 cards to discard first. Holding fewer than 2 → the button is disabled. Playing GAMBLE WHEEL!! → the wheel opens with nothing else discarded, straight onto the effect wheel with no name spin.
- Hold 7 cards, come last, try to draw → blocked with a clear message until a card is discarded.
- Spin the GAMBLE WHEEL!! 30 times → no blacklisted card (1, 4, 6, 7, 9, 11, 14, 15, 16, 21, 22, 23, 24, 25, 26, 27, 28, 31, 33, 34, 35, 36, 39, 41, 43, 45, 48, 50, 55, 56, 59) ever appears.
- Play through 18 holes drawing 2 every hole → deck reshuffles from discard without erroring.
