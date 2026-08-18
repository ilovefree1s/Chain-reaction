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
- **Each player has their own 52-card deck**, shuffled independently. Duplicate cards across players are expected and fine.
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

Implementation: let `lo = min(scores)`, `hi = max(scores)`.
- if `lo === hi` → 1
- else if `myScore === lo` → 0
- else if `myScore === hi` → 2
- else → 1

## Screens

### 1. Setup (first run only)
- Four name fields.
- User taps which one is them ("ME"). Store this index — the app needs it to compute the local player's draw.
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

### 4. Double Wheel (modal, launched from Hand)
Card #48's mechanic. Two-stage reveal, in this order:
1. Spin a random card from the deck's 52 — **reveal the effect first**.
2. Then spin a wheel of the four player names.

That named player is the **only one exempt**. Everyone else, including the player who played the card, must carry out the effect. The order matters: the whole group sees what's at stake before the name lands.

**Exclude card ids 12, 18, and 27** from the wheel pool only — they're reaction cards with nothing to react to when drawn this way. They remain fully playable as normal cards.

### 5. Rules reference
Static screen: the rules above, plus a browsable list of all 52 cards grouped by timing.

## Design direction

Built to be read outdoors in bright sun, one-handed, between throws. High contrast, large tap targets (min 44pt), no small text. Dark ground so it isn't blinding at dusk.

Color-code cards by function — this is the main visual system:

| Function | Meaning |
|---|---|
| `attack` | played on an opponent |
| `self` | benefits you |
| `dual` | works either as self-help or as an attack, depending on target |
| `react` | played in response to another card |
| `group` | affects everyone |

Suggested palette: deep pine ground (`#0C1A14`), panels (`#132A21`), muted sage text (`#8AA79A`), off-white (`#F2F5F1`). Function colors: attack `#FF5A4D`, self `#FFD23F`, dual `#FF57C1`, react `#4DD9E8`, group `#A78BFA`. Condensed heavy display face for card names, plain sans for body.

## What NOT to build

- No sync, multiplayer, or networking.
- No enforcement of card effects.
- No stroke penalties applied automatically. Cards like GAMBLE!, Code Words!, Commentator, RUSH ATTACK!, Trade Offer and Me and You all rewrite scores — players enter those manually with the ± steppers.
- No money tracking. FIVE DOLLARS! mentions a pot; it's honor-system.

## Card data

```json
{
  "handSize": 4,
  "handCap": 7,
  "maxCardsOnOnePlayerPerHole": 2,
  "wheelExcludes": [7, 8, 11, 12, 13, 15, 18, 19, 27, 31, 36, 37, 39, 43, 48, 51],
  "timings": [
    "Before shot",
    "Before tee shot",
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
    { "id": 1, "timing": "Before shot", "kind": "attack", "name": "Roll It!", "text": "Force an opponent to throw a roller on the upcoming drive or approach." },
    { "id": 2, "timing": "Before shot", "kind": "attack", "name": "CHRIS SPECIAL!", "text": "Force an opponent to throw a tomahawk on the upcoming drive or approach." },
    { "id": 3, "timing": "Before shot", "kind": "attack", "name": "Sidearm", "text": "Force an opponent to throw a sidearm on the upcoming drive or approach." },
    { "id": 4, "timing": "Before shot", "kind": "attack", "name": "Bag Raid!", "text": "Choose ANY disc from any bag. That disc must be used on their next shot." },
    { "id": 5, "timing": "Before shot", "kind": "attack", "name": "39% OF THE SPLITS", "text": "Force an opponent to straddle putt. Playable any time someone is about to throw a putter." },
    { "id": 6, "timing": "Before shot", "kind": "attack", "name": "Baby Discs", "text": "Force an opponent to throw a mini for their upcoming drive or putt. Through the basket floor still counts as in." },
    { "id": 7, "timing": "After throw", "kind": "attack", "name": "AIR HORN!", "text": "Heckle an opponent during one of their shots. Reveal and discard this card after." },
    { "id": 8, "timing": "For the next hole", "kind": "attack", "name": "Bag Boy!", "text": "Force an opponent to carry your discs until they beat you on a hole. While they carry your bag, you can't play any more cards on them." },
    { "id": 9, "timing": "After throw · secret", "kind": "self", "name": "Don't Nice Me!", "text": "If any player says \"nice\" as your disc is in flight and it hits a tree, that throw doesn't count. Take it again." },
    { "id": 10, "timing": "Before tee shot", "kind": "attack", "name": "Code Words!", "text": "An opponent can't say \"yes\" or \"no\" this hole. 1 stroke penalty every time they do." },
    { "id": 11, "timing": "Before tee shot", "kind": "attack", "name": "Too Many Choices", "text": "Force an opponent to discard 3 cards." },
    { "id": 12, "timing": "Any time", "kind": "react", "name": "Change Is Good", "text": "Force an opponent to change the target of one of their cards, if there is another option." },
    { "id": 13, "timing": "Before tee shot", "kind": "attack", "name": "Dealer's Choice!", "text": "You pick the discs every other player tees off with this hole." },
    { "id": 14, "timing": "Before tee shot", "kind": "attack", "name": "Bizarro Golf!", "text": "Force an opponent to drive with a putter and putt with a driver this hole." },
    { "id": 15, "timing": "After throw", "kind": "attack", "name": "Prove It", "text": "Cancel a shot an opponent just took. They throw again with a different disc of their choice. No extra stroke." },
    { "id": 16, "timing": "Before shot", "kind": "attack", "name": "Close 'Em", "text": "Force an opponent to take the next putt with their eyes closed." },
    { "id": 17, "timing": "Before tee shot", "kind": "attack", "name": "One Disc Wonder!", "text": "Force an opponent to play the hole with only 1 disc. You choose it." },
    { "id": 18, "timing": "After card", "kind": "react", "name": "Rubber and Glue", "text": "If a card targeting only you was just played, that opponent carries out the instructions instead of you." },
    { "id": 19, "timing": "After all tee", "kind": "self", "name": "Gimme Dat", "text": "After all tee shots, trade lies with an opponent of your choice." },
    { "id": 20, "timing": "Before tee shot", "kind": "self", "name": "1v1 Challenge", "text": "Challenge someone. It runs until one of you beats the other's score on a hole — a tie settles nothing. The winner gets a free immunity hole and 1 free mulligan on that hole." },
    { "id": 21, "timing": "Before tee shot", "kind": "attack", "name": "It's Like a Stranger Is Doing It!", "text": "Force an opponent to take the upcoming drive with their off hand." },
    { "id": 22, "timing": "Before tee shot", "kind": "attack", "name": "Your Tee Pad Is Over There!", "text": "Use 2 of your discs to mark a new tee for an opponent of your choice, within 10 paces (30 ft) of the original. They tee from it." },
    { "id": 23, "timing": "Before tee shot", "kind": "self", "name": "My Tee Pad Is Over Here!", "text": "Use 2 of your discs to mark a new tee for yourself, within 10 paces (30 ft) of the original. Feeling nice? You may pick 1 player to join you." },
    { "id": 24, "timing": "Before tee shot", "kind": "attack", "name": "Thief'n", "text": "Steal someone's disc. It's yours to throw for the rest of the round." },
    { "id": 25, "timing": "After throw", "kind": "attack", "name": "Big Ooof, Bud.", "text": "Move an opponent's lie 10 paces (30 ft) in any direction, as long as it isn't out of bounds." },
    { "id": 26, "timing": "After throw", "kind": "self", "name": "Foot Wedge!", "text": "Move your own lie 10 paces (30 ft) in any direction." },
    { "id": 27, "timing": "After card", "kind": "react", "name": "No Way", "text": "Cancel any card just played. That card goes to the discard pile." },
    { "id": 28, "timing": "Before tee shot", "kind": "attack", "name": "Plant Your Feet!", "text": "No run-up on an opponent's next throw." },
    { "id": 29, "timing": "Before shot", "kind": "attack", "name": "Turbo Time", "text": "An opponent's next putt must be a turbo putt." },
    { "id": 30, "timing": "Before tee shot", "kind": "attack", "name": "Not the Recommended Route", "text": "Choose a reasonable object the target player must pass on a side you specify." },
    { "id": 31, "timing": "Before tee shot", "kind": "attack", "name": "Do Not Pass Go", "text": "Whoever has the shortest drive on the next hole gets no cards for that hole." },
    { "id": 32, "timing": "Before tee shot", "kind": "self", "name": "Tree Insurance", "text": "Play before your tee shot. If you hit a tree, take a free mulligan." },
    { "id": 33, "timing": "Before tee shot", "kind": "self", "name": "Call Your Shot", "text": "Call CTP. If you win it, nobody can play cards on you next hole, and your next attack card hits every opponent — not you." },
    { "id": 34, "timing": "After throw", "kind": "self", "name": "GAMBLE!", "text": "Take a second chance at a putt you just missed. Make it and it was a free mulligan. Miss and the second stroke counts too — play whichever disc landed farthest away, then add +1 stroke after the hole ends." },
    { "id": 35, "timing": "After a hole", "kind": "self", "name": "FIVE DOLLARS!", "text": "Play after you birdie a hole nobody else birdied. Everyone else owes five dollars to the pot!" },
    { "id": 36, "timing": "After all tee", "kind": "attack", "name": "I'm In Control", "text": "You decide who plays whose tee shots. More than one player can be sent to the same lie." },
    { "id": 37, "timing": "Before tee shot", "kind": "self", "name": "Not Today!", "text": "Remove any and all card effects currently on you." },
    { "id": 38, "timing": "Before tee shot", "kind": "self", "name": "GOOD GUYS VS. BAD GUYS!", "text": "Ask another player to team up for the next hole. Birdie counts as an eagle, no birdie is +1. Cards against one of you count against both, and you both take the same score. If they refuse, ask someone else — if everyone refuses, discard this card with no effect." },
    { "id": 39, "timing": "After all play a card", "kind": "group", "name": "Group Hug", "text": "Only playable after every other player has hit you with a negative card this turn. Each card bounces back onto the player who played it, and you choose one of the effects to take yourself." },
    { "id": 40, "timing": "Before shot", "kind": "self", "name": "If the Basket Was There It Woulda Went In", "text": "Play before a C2 putt only. If you hit metal and it doesn't go in, it still counts." },
    { "id": 41, "timing": "Before tee shot", "kind": "attack", "name": "Commentator", "text": "Another player has to announce every shot you take this hole. If they forget one, they take +1 stroke." },
    { "id": 42, "timing": "Before shot", "kind": "attack", "name": "Trust Me Bro", "text": "Give another player advice for their tee shot. They have to follow it as best they can." },
    { "id": 43, "timing": "Before tee shot", "kind": "group", "name": "WALK IT DOWN!!", "text": "No other cards can be played once this hits the table. Everyone tees immediately from anywhere near the tee pad — no throw order. First and second to finish get birdie, third gets par, last gets bogey. No running, and no calling foot faults." },
    { "id": 44, "timing": "After throw", "kind": "attack", "name": "Walk of Shame", "text": "After a missed putt inside C1, that player carries their putter until they finish the next hole. If they drop it or put it in the bag, +1 stroke." },
    { "id": 45, "timing": "Before shot", "kind": "dual", "name": "Shoe Golf", "text": "On yourself: putt with your own shoe at no stroke cost. On another player: they putt with a shoe and it still counts as a stroke." },
    { "id": 46, "timing": "Before shot", "kind": "dual", "name": "Globetrotter Shit", "text": "On yourself: putt behind the back at no stroke cost. On another player: they putt behind the back and it counts as a normal stroke." },
    { "id": 47, "timing": "Before shot", "kind": "dual", "name": "Spin to Win", "text": "Spin rapidly 10 times, then putt within 3 seconds. Free on yourself, a normal stroke on an opponent." },
    { "id": 48, "timing": "Before tee shot", "kind": "group", "name": "Double Wheel", "text": "Free spin on the Double Wheel. If you get a negative effect, just ignore it." },
    { "id": 49, "timing": "Before shot", "kind": "dual", "name": "Aerobie", "text": "On yourself: use an aerobie for your drive. On another player: they drive with the aerobie using their off hand. Normal stroke either way." },
    { "id": 50, "timing": "Any time", "kind": "dual", "name": "Player 2's Turn!", "text": "On yourself: retake any shot for free. On another player: force them to re-throw a shot that was too good." },
    { "id": 51, "timing": "After a hole", "kind": "dual", "name": "Me and You", "text": "On yourself: take the best score made on the hole. On another player: they take the worst score made on the hole." },
    { "id": 52, "timing": "After throw", "kind": "self", "name": "Big Putt!", "text": "If you make a putt from outside C1 while a player still to putt is inside C1, they have to putt with their off hand." }
  ]
}
```

## Acceptance checks

- Start a round, score 3 holes, force-quit the app, reopen — round state, hand, deck and discard all restored.
- Come last on a hole → 2 cards owed. Tie for last → still 2. Everyone ties → 1. Win the hole → 0.
- Hold 7 cards, come last, try to draw → blocked with a clear message until a card is discarded.
- Spin the Double Wheel 30 times → no blacklisted card (7, 8, 11, 12, 13, 15, 18, 19, 27, 31, 36, 37, 39, 43, 48, 51) ever appears.
- Play through 18 holes drawing 2 every hole → deck reshuffles from discard without erroring.
