# Working in this repo

Two games live here. They share build plumbing and asset files, nothing else.

| | Chain Reaction | FORE! |
|---|---|---|
| Sport | Disc golf | Regular ball golf |
| Source | `web/template.html` | `fore/template.html` |
| Build | `node web/build.js` | `node fore/build.js` |
| Output | `docs/` | `docs/fore/` |
| Live at | https://ilovefree1s.github.io/Chain-reaction/ | https://ilovefree1s.github.io/Chain-reaction/fore/ |
| Version | `web/VERSION` | `fore/VERSION` |
| Cards | `BUILD_SPEC.md` (json block) | `fore/CARDS.md` |
| Phones | Many, synced live | **One. No sync at all.** |

Both are single-file PWAs: one `template.html` with placeholder comments that
`build.js` fills with card data, art and audio, emitting `index.html` plus
`sw.js` and `manifest.webmanifest`. Both must work **fully offline** — courses
often have no signal.

## The build flow

Edit the template (and the card file, for card data), then:

```bash
node web/build.js      # Chain Reaction
node fore/build.js     # FORE!
```

Every build bumps the last number of that game's `VERSION` automatically — it is
a build count, not a decimal, so 1.3.9 rolls to 1.3.10. The middle number moves
by hand when the game itself changes. Never edit `docs/` directly; it is output.

Then commit and push. **Use the Bash tool to push, not PowerShell** — PowerShell
fails with `could not read Username for 'https://github.com'`, Bash has the
credential helper.

## Never quote a version number you have not fetched

A local build is not a published build. Test tabs serve `docs/` off disk and show
whatever was last **built**; the user's phone shows what was last **pushed**.

Order is: build → verify → commit → push → fetch the live URL cache-busted and
check the `age` header → *only then* say a number. Quoting an unpublished version
sends the user hunting for an update that does not exist.

If they report being stuck on an old version, check what is published and what is
uncommitted **before** suggesting anything about their device. The device-side fix
that works is closing the app from the **recent-apps list** and reopening it — it
is installed as a PWA, and a backgrounded one is resumed rather than navigated, so
the browser never re-checks `sw.js`. Suggest that before clearing site data, which
wipes their saved round, hand and profile.

## Art and audio: Android owns the file

`app/` is a native Android build that is **no longer the game** — it survives as a
drop box for images and sounds. Both web builds read their assets out of
`app/src/main/res/raw/` (audio) and `app/src/main/res/drawable-nodpi/` (art), via
`copyAudio()` / the art copier in `build.js`. One copy in the repo, the web build
takes its own. To add a sound: drop the file in `raw/`, add one `copyAudio("x.mp3")`
line, rebuild.

Current audio: `chains.mp3` (general menu sound), `draw.wav` (one card leaving the
deck), `shuffle.wav` (shuffling), `dice.mp3`, `coin.mp3`, `gamble.mp3` (wheel),
`lonewolf.mp3`, `gamblegamesbeep.mp3` + `gameselected.mp3` (the game picker), and
`chainreaction.mp3` (4.8MB soundtrack, Chain Reaction only).

`playClip(audio, volume, maxSeconds, rate)` clips and pitches; `warmClips()` primes
them at startup so the first play is not late.

## Chain Reaction specifics

- **62 playable cards**, 2 retired, held alphabetically by name with ids 1-N to
  match. Ids are positional, not permanent — re-sorting renumbers them.
- Card data is the ```json block in `BUILD_SPEC.md`, read at build time so the
  spec and the app cannot drift. `GameCard.kt` has the same data transcribed for
  Android and should be kept in step when card text changes.
- Starting hand 4, hand cap 7, wheel costs 2 cards.
- Multiplayer is Supabase realtime broadcast via `liveCast(event, payload)`,
  dispatched in `sock.onmessage`. Scores are never synced and no phone is the
  referee — it is decoration over a scorecard that works alone.
- Disputes are scoped to a party: every cast carries `who`, and `inParty()` /
  `adoptParty()` guard the receiving end. A dispute must never pull in a player
  who is not in it.

## FORE! specifics

**Do not reuse Chain Reaction's game engine.** `web/template.html` is ~11,600
lines and the bulk of it is the multiplayer layer — lobby, seats, realtime sync,
party scoping, shared gamble games. FORE! runs on one phone and needs none of it.

What to copy from `web/` instead, all of it plumbing:

- the service worker generator in `build.js` — fresh-first navigation with the
  1200ms fuse, the `BUILD` stamp so update banners only fire on real updates, and
  `fillAssets()` in its own `waitUntil` so a half-finished asset sweep cannot block
  activation. This took a long time to get right; do not redesign it.
- `copyAudio`, `playClip`, `warmClips`
- the version bump and manifest generation
- `cardFaceSvg` and `cardHtml` from the template — both are state-free, data in and
  markup out

The deck is two piles: **format cards** that change the hole for the whole group
for one hole, and personal **keeps cards** you hold and spend when you want.

Service worker scope is per-path, so `docs/fore/sw.js` only controls
`/Chain-reaction/fore/`. The two apps cannot collide in each other's caches or
steal each other's update banners, and install as two separate home-screen icons.

## Card text

Dictated wording is a **small edit to existing text**. Never flip a card's meaning
and never dump raw dictation onto a card. Match the voice already on the cards.

Never write caveats about rounds being in progress — the group always starts fresh.

## Testing

Launch the preview with the `chainreaction` config in `.claude/launch.json`
(`node tools/dev-server.js`, port 5173). The user plays in the test tab, so:

- **zero the test tab's music volume** right after opening it
- **reload the tab after stubbing** anything (rAF, `Storage.prototype.setItem`,
  `liveCast`) or they hit the stubs as bugs
- background tabs throttle rAF and CSS animations — a spinner that looks frozen in
  a background tab is usually a throttled tab, not a bug
- only `localhost` and `127.0.0.1` are secure contexts, so the service worker will
  not register on a LAN IP

## Commits

End every commit message with:

```
Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
```
