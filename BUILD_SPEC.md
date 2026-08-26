# Build spec — disc golf card game companion

Build a mobile app that runs a house-rules disc golf card game for a group of up to 4 friends playing a round together in person.

**Important framing:** all players are standing next to each other on the course. They announce card plays out loud. The app does **not** sync between devices and does **not** enforce or adjudicate card effects. Each phone runs its own independent deck. The app is a card dealer plus a scorecard, nothing more.

## Stack

Expo / React Native, TypeScript. Local persistence only (AsyncStorage). No backend, no auth, no networking. Must survive app backgrounding and restart mid-round.

If a PWA is easier to sideload, that's acceptable — but it must work offline, since courses often have no signal.

## Core rules

- **Stroke play.** Lowest total wins.
- **Starting hand:** 4 cards, dealt at round start.
- **Hand cap:** 7. Cannot draw past it — must discard first.
- **Each player has their own 58-card deck**, shuffled independently. Duplicate cards across players are expected and fine.
- **Played and discarded cards** go to that player's own discard pile. Reshuffle the discard back into the deck if the deck ever empties.
- **Max 2 cards may be played on any one player per hole.** The app does not enforce this — players track it themselves — but show it in an in-app rules screen.

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
  "freeSpinCard": 23,
  "wheelExcludes": [1, 4, 6, 7, 13, 14, 20, 21, 22, 23, 24, 25, 30, 31, 32, 35, 37, 39, 44, 46, 49, 52, 55, 58],
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
    { "id": 2, "timing": "Before shot", "kind": "attack", "name": "39% OF THE SPLITS", "text": "Force an opponent to straddle putt. Playable any time someone is about to throw a putter." },
    { "id": 3, "timing": "Before shot", "kind": "dual", "name": "Aerobie", "text": "On yourself: use an aerobie for your drive. On another player: they drive with the aerobie using their off hand. Normal stroke either way." },
    { "id": 4, "timing": "After throw", "kind": "attack", "name": "AIR HORN!", "text": "Heckle an opponent during one of their shots. Reveal and discard this card after." },
    { "id": 5, "timing": "Before shot", "kind": "attack", "name": "Baby Discs", "text": "Force an opponent to throw a mini for their upcoming drive or putt. Through the basket floor still counts as in." },
    { "id": 6, "timing": "For the next hole", "kind": "attack", "name": "Bag Boy!", "text": "Force an opponent to carry your bag for 3 holes! If they beat you before those 3 holes are up (1st or 2nd hole), they can play one of your cards and you take your bag back." },
    { "id": 7, "timing": "Before tee shot", "kind": "attack", "name": "Bag Exchange!", "text": "Pick a player to swap bags with. Swap back after the first bogey by either player. The player that took the bogey first must volunteer as tribute to exchange lies with the other player the next time they go OB or miss a mando." },
    { "id": 8, "timing": "Before shot", "kind": "attack", "name": "Bag Raid!", "text": "Choose ANY disc from any bag. The target player must use that disc for their next shot." },
    { "id": 9, "timing": "After throw", "kind": "attack", "name": "Big Ooof, Bud.", "text": "Move an opponent's lie up to 10 paces (30 ft) in any direction, as long as it isn't out of bounds." },
    { "id": 10, "timing": "After throw", "kind": "self", "name": "Big Putted!", "text": "If you make a putt from outside C1 while any other players are inside C1, they all must use their left hand to putt this hole." },
    { "id": 11, "timing": "Before tee shot", "kind": "attack", "name": "Bizarro Golf!", "text": "Force an opponent to drive with a putter and putt with a driver this hole." },
    { "id": 12, "timing": "Before all tee", "kind": "self", "name": "Call Your Shot", "text": "Call CTP. If you win it, nobody can play cards on you next hole, and your next attack card hits every opponent — not you." },
    { "id": 13, "timing": "Any time", "kind": "attack", "name": "Can I Borrow This Card?", "text": "Pick anyone you want. Look through their cards and play one on anyone." },
    { "id": 14, "timing": "Any time", "kind": "react", "name": "Change Is Good", "text": "Hijack a card as it's played and re-aim it at the player of your choice — including the player who played it." },
    { "id": 15, "timing": "Before shot", "kind": "attack", "name": "CHRIS SPECIAL!", "text": "Force an opponent to throw a tomahawk on the upcoming drive or approach." },
    { "id": 16, "timing": "Before shot", "kind": "attack", "name": "Close 'Em", "text": "Force an opponent to take the next putt with their eyes closed." },
    { "id": 17, "timing": "Before tee shot", "kind": "attack", "name": "Code Words!", "text": "An opponent can't say \"yes\" or \"no\" this hole. 1 stroke penalty every time they do. ANY variation of the words yes or no counts." },
    { "id": 18, "timing": "Before tee shot", "kind": "attack", "name": "Jomez Commentator", "text": "Another player has to announce every shot you take this hole like they are a commentator. If they forget one, they take +1 stroke." },
    { "id": 19, "timing": "Before tee shot", "kind": "attack", "name": "Dealer's Choice!", "text": "You pick the discs every other player tees off with this hole." },
    { "id": 20, "timing": "Before tee shot", "kind": "attack", "name": "Do Not Pass Go", "text": "Whoever has the shortest drive on the next hole gets no cards for that hole." },
    { "id": 21, "timing": "After throw", "kind": "dual", "name": "Doesn't Look OB to Me", "text": "Play on a shot that just went OB. Instead of actually being OB (or in a hazard) they can just play it where it lies, with no penalty at all." },
    { "id": 22, "timing": "After throw · secret", "kind": "self", "name": "Don't Nice Me!", "text": "If any player says any form of \"nice\" as your disc is in flight and it hits a tree, you can move to where your disc landed and throw again for a free stroke." },
    { "id": 23, "timing": "Before tee shot", "kind": "group", "name": "GAMBLE WHEEL!!", "text": "Free spin on the GAMBLE WHEEL!! The name doesn't matter — you choose who gets the benefit or the punishment." },
    { "id": 24, "timing": "Before shot", "kind": "attack", "name": "Easily Distracted?", "text": "Everyone can do anything they want to distract a player while they are about to putt." },
    { "id": 25, "timing": "After a hole", "kind": "self", "name": "FIVE FOR YOU, YOU, AND YOU!", "text": "Play after you birdie a hole nobody else birdied. Everyone else owes five dollars to the pot!" },
    { "id": 26, "timing": "After throw", "kind": "self", "name": "Foot Wedge!", "text": "Move your own lie up to 10 paces (30 ft) in any direction." },
    { "id": 27, "timing": "After throw", "kind": "self", "name": "GAMBLE!", "text": "Take a second chance at a putt you just missed. Make it and it was a free mulligan. Miss and the second stroke counts too — play whichever disc landed farthest away, then add +1 stroke after the hole ends." },
    { "id": 28, "timing": "After all tee", "kind": "self", "name": "I'll Have What He's Having", "text": "After all tee shots, trade lies with an opponent of your choice." },
    { "id": 29, "timing": "Before shot", "kind": "dual", "name": "Globetrotter Shit", "text": "On yourself: putt behind the back at no stroke cost. On another player: they putt behind the back and it counts as a normal stroke." },
    { "id": 30, "timing": "Before tee shot", "kind": "dual", "name": "GOOD GUYS VS. BAD GUYS!", "text": "Ask another player to team up for the next hole. Birdie counts as an eagle, no birdie is +1. Cards against one of you count against both, and you both take the same score. If they refuse, ask someone else — if everyone refuses, they all discard 1 card." },
    { "id": 31, "timing": "Before shot", "kind": "self", "name": "If the Basket Was There It Woulda Went In", "text": "Play before a C2 putt only. If you hit metal and it doesn't go in, it still counts." },
    { "id": 32, "timing": "After all tee", "kind": "attack", "name": "I'm In Control", "text": "You decide who plays whose tee shots. More than one player can be sent to the same lie." },
    { "id": 33, "timing": "Before tee shot", "kind": "attack", "name": "It's Like a Stranger Is Doing It!", "text": "Force an opponent to take the upcoming drive with their off hand." },
    { "id": 34, "timing": "Before tee shot", "kind": "dual", "name": "Lefty Off the Box", "text": "Your first throw off the box is with your offhand, for free — that's throw 0. The player in last place gets it too. If 2 players are tied for last, they rock paper scissors for it." },
    { "id": 35, "timing": "After a hole", "kind": "dual", "name": "Me and You", "text": "On yourself: take the best score made on the hole. On another player: they take the worst score made on the hole." },
    { "id": 36, "timing": "Before tee shot", "kind": "self", "name": "My Tee Pad Is Over Here!", "text": "Use 2 of your discs to mark a new tee for yourself, up to 10 paces (30 ft) from the original. Feeling nice? You may pick 1 player to join you." },
    { "id": 37, "timing": "After card", "kind": "react", "name": "No Way", "text": "Cancel any card just played. That card goes to the discard pile." },
    { "id": 38, "timing": "Before tee shot", "kind": "attack", "name": "New mando on this hole, bud.", "text": "Choose a reasonable object the target player must pass on a side you specify." },
    { "id": 39, "timing": "Before tee shot", "kind": "dual", "name": "Not Today!", "text": "Remove any and all card effects currently on you, or use it on another player to cancel the effects on them." },
    { "id": 40, "timing": "Before tee shot", "kind": "attack", "name": "One Disc Wonder!", "text": "Force an opponent to play the hole with only 1 DISC this hole! (If someone already gave them an Aerobie or mini, that counts as their 1 disc.)" },
    { "id": 41, "timing": "Before tee shot", "kind": "attack", "name": "Over Sharer", "text": "Give everyone a disc from your own bag to tee off with on this hole." },
    { "id": 42, "timing": "Before all tee", "kind": "attack", "name": "Plant Your Feet!", "text": "No run-up on everyone else's tee shot." },
    { "id": 43, "timing": "Any time", "kind": "dual", "name": "Player 2's Turn!", "text": "On yourself: retake any shot for free. On another player: force them to re-throw a shot that was too good." },
    { "id": 44, "timing": "After throw", "kind": "attack", "name": "Prove It", "text": "Cancel a shot an opponent just took. They throw again with a different disc of their choice from any bag. (No extra stroke, just a forced mulligan.)" },
    { "id": 45, "timing": "Before shot", "kind": "attack", "name": "Roll It!", "text": "Force an opponent to throw a roller on the upcoming drive or approach." },
    { "id": 46, "timing": "After card", "kind": "react", "name": "Rubber and Glue", "text": "If a card targeting only you was just played, that opponent carries out the instructions instead of you." },
    { "id": 47, "timing": "Before shot", "kind": "dual", "name": "Shoe Golf", "text": "On yourself: putt with your own shoe at no stroke cost. On another player: they putt with a shoe and it still counts as a stroke. If they refuse to take their shoe off, they take +1 stroke after the hole." },
    { "id": 48, "timing": "Before shot", "kind": "attack", "name": "Forehand only!", "text": "Force an opponent to throw a forehand on the upcoming drive or approach." },
    { "id": 49, "timing": "On draw", "kind": "sabotage", "name": "Everybody But Me", "text": "You must play this card immediately after drawing, you can't discard it. Everyone gets a free mulligan on the next hole but me =(" },
    { "id": 50, "timing": "Before shot", "kind": "dual", "name": "That's Definitely a Gimme", "text": "Pick up a putt as a gimme, as long as it's inside C1 — you definitely woulda made it. The player in last place gets one too. If 2 players are tied for last, they rock paper scissors for it." },
    { "id": 51, "timing": "Before tee shot", "kind": "attack", "name": "Too Many Choices", "text": "Pick 3 discs out of an opponent's bag. They pick 1, then pass the other 2 to the next person, who chooses 1 of the 2 and gives the last disc to the last person. Tee off, nerds." },
    { "id": 52, "timing": "Before tee shot", "kind": "self", "name": "Tree Insurance", "text": "Play before your tee shot. If you hit a tree, take a free mulligan." },
    { "id": 53, "timing": "Before shot", "kind": "attack", "name": "Trust Me Bro", "text": "Give another player advice for their tee shot. They have to follow it as best they can. (Somewhat reasonable advice — don't tell them something like throw it backwards.)" },
    { "id": 54, "timing": "Before shot", "kind": "attack", "name": "Turbo Time", "text": "All opponents in C1 must turbo putt their next shot." },
    { "id": 55, "timing": "Before tee shot", "kind": "group", "name": "WALK IT DOWN!!", "text": "No other cards can be played once this hits the table. Everyone tees immediately from anywhere near the tee pad — no throw order. First and second to finish get birdie, third gets par, last gets bogey. No running, and no calling foot faults." },
    { "id": 56, "timing": "After throw", "kind": "attack", "name": "Walk of Shame", "text": "After a missed putt inside C1, that player carries their putter in either hand until they finish the next hole. (They can putt with it.) If they drop it or put it in the bag, +1 stroke." },
    { "id": 57, "timing": "Before tee shot", "kind": "attack", "name": "Your Tee Pad Is Over There!", "text": "Use 2 of your discs to mark a new tee for everyone else, up to 10 paces (30 ft) from the original. They all tee from it." },
    { "id": 58, "timing": "Before all tee", "kind": "sabotage", "name": "I Think It's Broke", "text": "The first player to hit a tree loses that disc — they can't throw it again for the rest of the round." }
  ]
}
```

## Acceptance checks

- Start a round, score 3 holes, force-quit the app, reopen — round state, hand, deck and discard all restored.
- Come last on a hole → 2 cards owed. Tie for last → still 2. Everyone ties → 1. Win the hole → 0. Double bogey → +1 on top of any of these.
- Sit on a full hand for several holes without drawing → the owed count never banks up; each hole's deal replaces the last one's leftovers.
- Tap "Spin the GAMBLE WHEEL!!" → asked to pick 2 cards to discard first. Holding fewer than 2 → the button is disabled. Playing GAMBLE WHEEL!! → the wheel opens with nothing else discarded, straight onto the effect wheel with no name spin.
- Hold 7 cards, come last, try to draw → blocked with a clear message until a card is discarded.
- Spin the GAMBLE WHEEL!! 30 times → no blacklisted card (1, 4, 6, 7, 9, 11, 14, 15, 16, 21, 22, 23, 24, 25, 26, 27, 28, 31, 33, 34, 35, 36, 39, 41, 43, 45, 48, 50, 55, 56, 59) ever appears.
- Play through 18 holes drawing 2 every hole → deck reshuffles from discard without erroring.
