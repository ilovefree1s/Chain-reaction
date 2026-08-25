# Chain Reaction

A disc golf card game companion: a card dealer plus a scorecard, for a group playing a round
together in person. Two independent builds from one spec:

- **`app/`** — native Android, Kotlin + Jetpack Compose. The phone app.
- **`web/`** — sources for an installable, offline PWA, built into `docs/`. For the iPhone friends.

Neither one adjudicates card effects, and every device runs its own deck and its own copy of
the scorecard, exactly as [BUILD_SPEC.md](BUILD_SPEC.md) describes. The web build can
additionally join a **live room** ([MULTIPLAYER_PLAN.md](MULTIPLAYER_PLAN.md)): names and card
plays leave the phone for the first time, but only as decoration over a scorecard that still
works entirely alone — scores are never synced, and no phone is the referee.

## Where the rules live

[BUILD_SPEC.md](BUILD_SPEC.md) is the source of truth for the 58 cards, held in alphabetical
order by name with ids running 1-N to match. The web build reads
its card data straight out of the spec's ```json block at build time, so the spec and the web
app can't drift. The Android build has the same data transcribed into
[GameCard.kt](app/src/main/java/com/chainreaction/data/GameCard.kt), verified against the spec
by unit test (`deck is 58 cards with unique ids one through 61`).

Ids are positional, not permanent: re-sorting the deck renumbers them. Nothing in either
build hardcodes one — `wheelExcludes` and `freeSpinCard` live in the spec beside the cards,
and prose names cards rather than numbering them. `GameState.SCHEMA_VERSION` is bumped
whenever ids move, so a round saved under the old numbering is dropped rather than dealing
somebody the wrong hand.

The one piece of real logic — how many cards you draw at the end of a hole — is in
[DrawRule.kt](app/src/main/java/com/chainreaction/data/DrawRule.kt), deliberately free of
Android imports so it unit tests on the JVM.

## Android

```bash
./gradlew assembleDebug
```

Install on a connected device or running emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the tests (45 of them, covering the draw table, the hand cap and its use-it-or-lose-it
debt, deck reshuffling, locking, finishing a round, the course library and the character
roster):

```bash
./gradlew testDebugUnitTest
```

State persists to `SharedPreferences` as a single JSON blob, written with `commit()` rather
than `apply()` so a round survives the phone being force-quit in a pocket. Verified by
force-stopping the app mid-round and relaunching.

## Web

`node web/build.js` generates **docs/** — an installable, offline-capable PWA to host.
Edit [web/template.html](web/template.html), never `docs/`, which is wiped on every build.

```
docs/
  index.html            the app (~58 KB)
  manifest.webmanifest  name, icon, standalone display
  sw.js                 service worker, precaches everything
  assets/               artwork, copied from the Android drawables
```

The build validates the spec's card data (ids running 1-N with no gaps, known kinds, listed timings, wheel
exclusions that exist), the course library and the character roster, and fails loudly rather
than shipping a broken deck. Images are separate files rather than base64 in the HTML: with 50+ card images inlined
the page would be tens of megabytes and slow to first paint.

To test locally — a service worker only registers on https or localhost, so opening
`docs/index.html` as a file will never exercise the offline path:

```bash
node web/serve.js
```

### Hosting it

Upload the **contents** of `docs/` to any static host — GitHub Pages, Netlify Drop, S3.
There's no build step on the server and no backend. Hosting is what makes the iPhone side work
properly:

- **Add to Home Screen** needs an `http(s)` URL; the manifest then makes it open standalone.
- **Saved rounds** need `localStorage`, which Safari blocks on the opaque origin a bare file
  gets. Served from a URL they persist. (The app falls back to in-memory state rather than
  crashing if storage is unavailable.)
- **Offline** comes from the service worker, which precaches the page and every asset on first
  visit — so it works on a course with no signal.

The service worker is cache-first and keeps **two** caches, each named by its own hash: the
page (`index.html`, the manifest — about 110 KB) and the assets (artwork, character photos,
the audio — about 7 MB). They change at wildly different rates, and under one combined hash
rewording a single card re-downloaded every image for everyone who already had the app
installed. Split, a card edit costs the page and nothing else; the photos are re-fetched only
when a photo actually changes. Verified by planting a sentinel in the asset cache, shipping a
page-only change, and confirming it survived. `web/serve.js` sends `cache-control: no-cache` for the same reason: a stale `sw.js` is
the one thing that makes a redeploy invisible.

## Screens

The app opens on a **main menu** — Play / Cards / Rules / Settings — over the title art in
`app/src/main/res/drawable-nodpi/chainreactionmain.png`. That file is the single source for
both builds: Android loads it as a drawable, and `web/build.js` copies it into `docs/assets/`.
It lives in `drawable-nodpi/` deliberately — in plain
`drawable/` Android treats it as mdpi and upscales it ~3× on a modern phone. **Do not put a
second copy in `drawable/`**; two files with the same resource name in different density
buckets means the wrong one can win.

The photo is drawn full-width and top-anchored so the logo is never cropped, and `moregrass.png`
carries on below it — pulled up over the photo's bottom edge with its own top edge masked out,
which cross-fades the join rather than butting the two images together. The button sheet
(`mainbuttons.png`) is transparent and sits over the bottom 40%, with four invisible tap targets
tiled over the painted buttons; their positions were measured off the artwork's pixels. Because
the labels are painted in, Play reads "Play" even mid-round — it still resumes rather than
starting fresh. Swapping in new artwork is a straight file replace.

Once every hole is locked the Score tab is replaced by a **results screen** — winner declared,
then final standings with each player's score to par and how far back they finished. Ties are
left standing (`Sam and Alex tie`); the app doesn't invent a playoff. *Back to scorecard*
reopens the card so a mis-scored final hole can still be unlocked and fixed, and finishing the
round again puts the result back in front.

Play resumes a round in progress rather than starting a new one. Cards and Rules are separate
menu destinations; the in-round Rules tab still carries both plus the round-specific actions
(save this course, end round).

Until a character is saved, the menu carries an orange note in the gap above the buttons —
*Lock in your profile in Settings* — which taps straight through to Settings. It shows on
every launch and clears itself the moment `myCharacter` is set, so it can't be dismissed and
forgotten. It sits above the button stack rather than at the top of the screen, where it cut
across the logo.

Stats are **on hold**. Settings used to carry a *Your stats* row — rounds played, won
and lost, aces, cards played, who you played them on — and it was pulled out on 2026-08-24
rather than left half-trusted. Nothing records while it is gone. The code is whole in git
(the feature landed in `7b346fc`, and its removal is the commit after), so bringing it back
is a revert rather than a rewrite. The play flow still asks who a card landed on, because
naming the target is how the table hears it, not just how the numbers got counted.

**Settings** holds your own name and face, course management, and the sound slider. Only
*you* are stored, not the whole group — the rest of the table changes round to round, and
typing three names on Setup beats maintaining a roster you keep having to correct. You are
always player 1, and Setup pre-fills that slot. Settings saved when this held the whole
group keep whichever name was marked ME rather than making you retype it. The app
makes exactly one noise: a chain rattle on Play — starting a round or resuming one, either
way — at whatever the **Sound** slider at the bottom of Settings is set to. Cards, Rules and
Settings are silent, and nothing else in either build plays audio.

The clip lives at [app/src/main/res/raw/chains.mp3](app/src/main/res/raw/chains.mp3) —
Android loads it as `R.raw.chains` through a `SoundPool`, and `web/build.js` copies it
into `docs/assets/` and precaches it, so it still rattles with no signal. One copy in the
repo, same rule as the artwork. Android resource names allow only lowercase letters,
digits and underscores, so a new clip has to be named that way or the build fails.

Volume defaults to 40% and saves on release rather than on every pixel of the drag. Zero
is properly silent — the clip isn't played at nothing.

The launcher icon is `app/src/main/res/drawable-nodpi/chainreactionicon.png`, wired up as an
adaptive icon. Adaptive layers are 108dp but launchers only show the middle 72dp, so
[ic_launcher_art.xml](app/src/main/res/drawable/ic_launcher_art.xml) insets the artwork by
16.7% to sit exactly in that visible square; the background is a near-black matching the
artwork's corners so circular masks blend seamlessly. `mipmap-anydpi/` carries a plain
layer-list fallback for API 24–25, which predate adaptive icons.

## Card artwork

Card faces are drop-in. Put an image named `card_01` … one per card (the number is the
card's id in the spec) into `app/src/main/res/drawable-nodpi/` — PNG, WebP or JPEG —
and both builds pick it up: Android looks the drawable up by name at runtime, and
`web/build.js` copies whatever faces exist into `docs/assets/` and tells the page about
them. A card with art shows the art as its whole face (Play/Discard still hang off the
bottom in hand); a card without art keeps the interim text tile. Faces can land one at
a time — no code changes, just rebuild.

## Characters

[shared/characters.json](shared/characters.json) is the pickable roster, read by both builds
the same way the course library is. A character is **personalisation and nothing else** — a
face and a name beside a player on the scorecard. There is no ability, no modifier and nothing
extra for the group to remember at the course.

```json
{ "id": 9, "name": "Character Name", "color": "#FF8A1E" }
```

`id` is 1–99 and must be unique; `color` is `#RRGGBB`. `node web/build.js` fails the build on a
duplicate id, a missing name or a malformed colour.

Faces are drop-in exactly like the card art: put an image named `character_01` … `character_99`
(the number is the character's id) into `app/src/main/res/drawable-nodpi/` — PNG, WebP or JPEG —
and both builds pick it up. Until a character has art it draws as its own colour with its
initial, so the roster is playable before any of it is drawn. Art that matches no character in
the roster fails the web build rather than shipping unreferenced.

Characters are picked on **Setup**, by tapping the face beside a name, and in
**Settings → You**, where your own character saves and pre-fills player 1 every new round.

Picking a face fills its name into the box beside it, so the usual case is no typing at
all. Typing over that name **renames the character** — the photo stays put, the label
changes, and it sticks for every future round. That's how the shipped names get replaced
with your group's: pick a face, correct the name, done. Clearing the box entirely drops
back to the name in the JSON rather than leaving a nameless face.

Renames live in local storage per device (`characterNames`, id → name), not in
[shared/characters.json](shared/characters.json) — so one person renaming Juice doesn't
rewrite the file for everyone. Edit the JSON for names you want everybody to get.
One character per player: a face already taken is shown greyed out with its owner's name, since
the whole point is telling everyone apart on one scorecard. Tapping your own pick clears it.

An empty roster is a supported state — no picker appears and a round plays on names alone.
So is a round that predates characters, or a saved character id that has since left the roster:
both fall back to just the name rather than refusing to load.

## Courses

[shared/courses.json](shared/courses.json) is the built-in course library. Both builds read
that one file — the Android app bundles it as an asset (wired up via `sourceSets` in
[app/build.gradle.kts](app/build.gradle.kts)), and `node web/build.js` injects it into the web
page. Add a course by appending an entry:

```json
{ "name": "Course Name — Blue/Longs", "holeCount": 18, "pars": [3, 4, 3, "…"] }
```

`pars` must have exactly `holeCount` entries; `web/build.js` fails the build if it doesn't, so
a typo can't ship. Name the tees — pars differ per tee, and a course played from two tee sets
is two entries.

In the app, **Setup → Course** opens the sheet: pick a saved course, or choose 9 / 18 holes to
build a new one and tap holes to cycle 3 → 4 → 5, then name and save it. **Rules → Save this
course** captures the pars mid-round, which is usually when you actually learn them.
Player-saved courses can be deleted; built-ins can't, but saving over a built-in's name
replaces it.

There is no hole-count toggle on Setup — the course decides how long the round is, so a round
can't start until a course is chosen. Choosing 9 or 18 lives inside the sheet, where it's a
property of the course being defined.

Starting scores follow the pars, so a par round needs no tapping at all.

## Deviations from the spec

Eight, all decided deliberately:

1. **Stack.** The spec says Expo / React Native. Built as native Android + a separate web app
   instead, per the brief. The trade is two codebases rather than one; they're kept honest by
   sharing the spec as the card-data source and by matching screen-for-screen.
2. **Players.** The spec's Setup screen describes four fixed name fields plus a "ME" toggle
   on each. This supports 3–5, and drops the toggle — you are always player 1, so `meIndex`
   is fixed at 0 and Setup pre-fills that slot from Settings.
   The draw rule generalises unchanged — "middle" is anyone neither outright best nor outright
   worst — and the name wheel spins over whoever is actually in the round.
3. **Unlock.** Not in the spec. A mis-tapped "Lock hole & draw" on hole 3 of 18 would otherwise
   be unrecoverable, so a locked hole can be unlocked to fix its scores. A hole only ever
   deals cards the FIRST time it's locked — unlocking keeps what was dealt and re-locking
   grants nothing new, so the lock/unlock cycle can't farm cards. A re-lock isn't a fresh
   deal, so it doesn't wipe an outstanding debt either.
4. **No colour-coding.** The spec calls per-function card colours "the main visual system";
   they were built, then removed once per-card artwork became the plan — the art will carry
   card identity, and interim cards show neutral kind/timing tags instead.
5. **Wheel order.** The spec spins the effect first, then the name. Reversed on request:
   the name lands first, so the table knows who is exempt before it learns what from.
   (The GAMBLE WHEEL!! card’s text has since been rewritten as a free spin, so nothing describes the
   old order anymore.)
6. **Characters.** Not in the spec. A pickable face per player, added purely for
   personalisation — deliberately cosmetic, so the rules the group has to carry stay exactly
   as the spec describes them.
7. **The wheel costs cards.** The Rules screen has always said "discard 58 cards to buy a
   spin", but nothing enforced it — the wheel was free and unlimited, which made the hand cap
   beside the point. Tapping *Spin the GAMBLE WHEEL!!* now asks which two cards to give up, and
   the button is disabled when you're holding fewer than two. Playing GAMBLE WHEEL!! still spins
   for free, since that card's whole text is a free spin — the card is the payment. That
   spin also skips the name wheel: that card says the name doesn’t matter and hands the choice to
   whoever spun, so asking for a spin the card just told you to disregard would be theatre.
8. **Owed cards expire.** Not in the spec. Cards you're owed but can't hold because the hand
   is at 7 stay owed only until the next hole deals — that hole's draw *replaces* the debt
   rather than adding to it. Without this, sitting on a full hand for a few holes banks a
   pile of cards to cash in later, which is exactly what the hand cap exists to prevent. The
   Hand screen warns while a debt is outstanding.

Scores default to the hole's par, so a par round needs no tapping at all.

**Par is not editable during a round.** The course sets it and it stays fixed for all 18 holes;
the Score screen shows it read-only in the hole header. A wrong par gets fixed by correcting
the course — Settings → Manage courses, or save over its name — not mid-match. There is
deliberately no `withPar` on `GameState`.

## What is deliberately absent

No score sync and no accounts. The web build's live room shares card plays and hand counts
([MULTIPLAYER_PLAN.md](MULTIPLAYER_PLAN.md)), but the scorecard itself is never networked:
every phone tracks the table independently, because syncing scores would mean deciding whose
card wins when two disagree. No enforcement of card effects and no
automatic stroke penalties — GAMBLE!, Code Words!, Jomez Commentator, WALK IT DOWN!!,
GOOD GUYS VS. BAD GUYS! and Me and You all rewrite scores, and players enter those by hand
with the ± steppers. No money
tracking; FIVE FOR YOU, YOU, AND YOU! is honour-system.

The "max 58 cards on one player per hole" rule is shown on the Rules screen and tracked by the
players, not the app.
