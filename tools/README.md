# Deck tools

Small Node scripts for working on the card deck. No dependencies — run them
from the repo root with plain `node`.

| Script | What it does |
|---|---|
| `deck-audit.js` | Builds `deck-audit.html`, the card-by-card review sheet. |
| `sync-check.js` | Checks GameCard.kt still matches BUILD_SPEC.md. |
| `remove-card.js` | Removes a card and closes the gap in the ids. |
| `shift-settled.js` | Moves the audit's decision keys after a removal. |

## Reviewing the deck

```
node tools/deck-audit.js
```

Reads the cards from `BUILD_SPEC.md` and the targeting sets straight out of
`web/template.html`, so the sheet cannot drift from what actually ships. The
one thing written by hand is the `SETTLED` map near the top of the script —
the decisions already made and built, each keyed by card id. A card with an
entry drops out of the review list; everything else is still open.

## Removing a card

Ids must run 1..N with no gaps, so this is never just a deletion:

```
node tools/remove-card.js 31
node tools/shift-settled.js 31
node web/build.js
node tools/sync-check.js
```

`remove-card.js` drops the card from both decks and renumbers every card above
it, along with every id-keyed list that moves with them — the wheel blacklist,
the free-spin card, and the web build's targeting and wording sets. It also
bumps `SCHEMA_VERSION`, since a saved round holds ids that now mean different
cards. `shift-settled.js` does the same to the audit's decision keys.

Then check the renumbering landed on the right cards rather than trusting it:
open the app and confirm the free spin still points at GAMBLE WHEEL!! and the
"all players" list still names the cards you expect.

## After any wording change

```
node tools/sync-check.js 28
```

Passing an id prints that card's stored JSON back. Read it. A shell that eats
an apostrophe reports success just as cheerfully as one that doesn't.

## The version everyone reads

`web/VERSION` holds the number — `1.0` today. Bump it when you want the group
to see a new one; `web/build.js` stamps the build time next to it, so two
phones can be compared even between version bumps.

It shows at the foot of the menu and at the foot of Settings.
