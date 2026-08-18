# Chain Reaction

A disc golf card game companion: a card dealer plus a scorecard, for a group playing a round
together in person. Two independent builds from one spec:

- **`app/`** — native Android, Kotlin + Jetpack Compose. The phone app.
- **`web/`** — sources for an installable, offline PWA, built into `docs/`. For the iPhone friends.

Neither one syncs, networks, or adjudicates card effects. Every device runs its own deck and
its own copy of the scorecard, exactly as [BUILD_SPEC.md](BUILD_SPEC.md) describes.

## Where the rules live

[BUILD_SPEC.md](BUILD_SPEC.md) is the source of truth for the 54 cards. The web build reads
its card data straight out of the spec's ```json block at build time, so the spec and the web
app can't drift. The Android build has the same data transcribed into
[GameCard.kt](app/src/main/java/com/chainreaction/data/GameCard.kt), verified against the spec
by unit test (`deck is 54 cards with unique ids one through fifty-four`).

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

Run the tests (34 of them, covering the draw table, the hand cap, deck reshuffling, locking,
finishing a round and the course library):

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

The build validates the spec's card data (52 unique ids, known kinds, listed timings, wheel
exclusions that exist) and the course library, and fails loudly rather than shipping a broken
deck. Images are separate files rather than base64 in the HTML: with 50+ card images inlined
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

The service worker is cache-first and its cache name is a hash of everything precached, so a
rebuild that changes any file invalidates the old cache and one that changes nothing leaves it
alone. `web/serve.js` sends `cache-control: no-cache` for the same reason: a stale `sw.js` is
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

**Settings** holds the usual group (pre-fills every new round) and course management. The app
is deliberately silent — there are no sound effects and no audio assets in either build.

The launcher icon is `app/src/main/res/drawable-nodpi/chainreactionicon.png`, wired up as an
adaptive icon. Adaptive layers are 108dp but launchers only show the middle 72dp, so
[ic_launcher_art.xml](app/src/main/res/drawable/ic_launcher_art.xml) insets the artwork by
16.7% to sit exactly in that visible square; the background is a near-black matching the
artwork's corners so circular masks blend seamlessly. `mipmap-anydpi/` carries a plain
layer-list fallback for API 24–25, which predate adaptive icons.

## Card artwork

Card faces are drop-in. Put an image named `card_01` … `card_54` (the number is the
card's id in the spec) into `app/src/main/res/drawable-nodpi/` — PNG, WebP or JPEG —
and both builds pick it up: Android looks the drawable up by name at runtime, and
`web/build.js` copies whatever faces exist into `docs/assets/` and tells the page about
them. A card with art shows the art as its whole face (Play/Discard still hang off the
bottom in hand); a card without art keeps the interim text tile. Faces can land one at
a time — no code changes, just rebuild.

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

Five, all decided deliberately:

1. **Stack.** The spec says Expo / React Native. Built as native Android + a separate web app
   instead, per the brief. The trade is two codebases rather than one; they're kept honest by
   sharing the spec as the card-data source and by matching screen-for-screen.
2. **Players.** The spec's Setup screen describes four fixed name fields. This supports 3–5.
   The draw rule generalises unchanged — "middle" is anyone neither outright best nor outright
   worst — and the name wheel spins over whoever is actually in the round.
3. **Unlock.** Not in the spec. A mis-tapped "Lock hole & draw" on hole 3 of 18 would otherwise
   be unrecoverable, so a locked hole can be unlocked. It refunds the draw that hole granted,
   floored at zero; cards already drawn stay in hand, which matches the app's honour-system
   stance everywhere else.
4. **No colour-coding.** The spec calls per-function card colours "the main visual system";
   they were built, then removed once per-card artwork became the plan — the art will carry
   card identity, and interim cards show neutral kind/timing tags instead.
5. **Wheel order.** The spec spins the effect first, then the name. Reversed on request:
   the name lands first, so the table knows who is exempt before it learns what from.
   (Card #48's text has since been rewritten as a free spin, so nothing describes the
   old order anymore.)

Scores default to the hole's par, so a par round needs no tapping at all.

**Par is not editable during a round.** The course sets it and it stays fixed for all 18 holes;
the Score screen shows it read-only in the hole header. A wrong par gets fixed by correcting
the course — Settings → Manage courses, or save over its name — not mid-match. There is
deliberately no `withPar` on `GameState`.

## What is deliberately absent

No sync, no multiplayer, no accounts, no networking. No enforcement of card effects and no
automatic stroke penalties — GAMBLE!, Code Words!, Commentator, WALK IT DOWN!!,
GOOD GUYS VS. BAD GUYS! and Me and You all rewrite scores, and players enter those by hand
with the ± steppers. No money
tracking; FIVE FOR YOU, YOU, AND YOU! is honour-system.

The "max 2 cards on one player per hole" rule is shown on the Rules screen and tracked by the
players, not the app.
