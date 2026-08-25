# Live match plan — four phones, one round

Design settled 2026-08-24. Steps 1 and 2 built on the `multiplayer` branch and
proven against real Supabase with a real phone in the room; steps 3-4 remain.
This is the plan to build from rather than re-deriving.

The goal: when somebody plays a card on you, your phone tells you. Everything else
here exists to serve that.

## The shape

Every phone keeps its own round exactly as it does today. The connection is
**decoration over a scorecard that already works alone** — a phone with no signal
plays a complete round, it just misses the alerts. Nothing about the game depends
on the network being up.

Only three things travel:

| What | Why |
|---|---|
| Card plays | The alert. "Justin played Bag Boy! on Avery." |
| Hand counts | Each row shows `4/7` instead of a running stroke total. |
| Hole scores, at lock | Flags a disagreement. Nobody's card gets overwritten. |

Deliberately **not** synced: the scorecard itself. Every phone tracking the table
independently is what makes the app trustworthy — no phone is the referee. Syncing
scores would mean deciding who wins when two disagree, and that is a much worse
game than the one being played.

## Decisions

**Server — Supabase, the existing project.** Not a new one: free projects sleep
after about a week idle, and a group that plays when the weather is good would
come back to a dead room. The existing project is kept awake by other traffic.
Channels are namespaced `cr:<code>` so they cannot collide with anything else in
there.

**Connection — hand-rolled, no library.** About 150 lines: open a socket, join a
channel, heartbeat, read messages. The project has no third-party code in it and
this does not have to be the exception, especially to use two features out of
dozens. If Supabase ever change their message format the connection breaks and the
round carries on locally, which is a cheap failure to absorb.

**No presence — heartbeats instead.** Supabase has a presence feature that reports
joins and leaves precisely, and we deliberately don't want precision: a player who
pockets their phone between throws must not flicker out of the UI. So every phone
broadcasts `{ id, name, characterId, handCount }` on join and every 15s after, and
a phone nobody has heard from in 45s goes quiet. Twenty lines, and the timeout is
the behaviour we wanted anyway.

**Identity — the saved profile.** Name and face are already in Settings, already
drawn from the shared roster. Joining announces them. Nobody types a name, nobody
picks a row, nobody matches "Ave" to "Avery". The roster builds itself as people
join; Setup's typing path survives only for a phone that is not connecting.

**Disconnects are the steady state, not an error.** Phones go in pockets to throw,
and iOS freezes a backgrounded page within seconds. So: reconnect automatically and
silently, store the room code with the round so it is never asked for twice, and
say nothing in the UI during the normal in-and-out. Because the room holds no state
— plays are fire-and-forget, counts are self-reported — everybody dropping at once
costs nothing. They rejoin the same channel and it rebuilds in a second.

**Wake lock.** Screen Wake Lock keeps the page alive while the phone is in a hand,
which is where it is 99% of the time. Re-acquire on visibility change.

**Alerts — both routes, deduped.** The realtime channel feeds everything on screen.
A push goes out separately so a pocketed phone still buzzes. A phone may receive
both, so every event carries an id and a repeat is ignored.

**Android and iPhone both play the web build.** The Kotlin app does not speak this
protocol and does not need to; the one Android user joins on the web build like
everyone else. Note his native app's saved profile and custom courses do not follow
him — the two apps have separate storage.

## Not settled

- **The iPhone foreground test.** Does iOS buzz for a notification while you are
  looking at the app, or suppress it as redundant? Deferred until there is
  something to test. If it suppresses, an iPhone in hand gets the banner and the
  chain rattle but no haptic; everything else is unaffected.
- ~~**Row-level security on the shared Supabase project.**~~ **Settled 2026-08-25,
  before merge.** Security Advisor: 0 errors — RLS enabled on every table, and
  Chain Reaction itself touches none (channels only). The audit also pruned the
  project: DragonPath and an abandoned Blind Ink online prototype (the mm_
  tables) were dropped outright. What remains is TimeIt's: score tables are
  append-only (INSERT+SELECT); its rooms/round_results are anon-ALL, which the
  user weighed and accepted — three players, free app, ephemeral stakes, and an
  auth-less app can't scope writes by owner anyway. Revisit only if TimeIt grows.
- Screen-level choices, better made with something on screen: where create/join
  lives, what the alert says and whether it needs acknowledging, whether group and
  sabotage cards announce to the whole table, where the discrepancy badge sits, and
  ~~whether the Double Wheel result broadcasts as a table-wide moment.~~ (Built
  2026-08-25: it does — one hot alert to every phone naming the spinner, the
  card, the good/bad half, and who's exempt.)

## Rejected, with reasons

**Phone numbers and SMS.** Would sidestep Apple entirely and lands where data is
weak, but US carriers now require registering automated senders, texts pile up in a
thread nobody can clear, and it still needs the same server-side sender. The one
use worth revisiting is a single end-of-round summary text, which push is worse at.

**Splitting by platform** — Android with Android, web with web. Makes the Android
half easy and the web half no easier, and produces two half-tables at one basket.

**Syncing scores as shared state.** See above.

## Build order

1. ~~The connection: join a room, see each other's hand counts.~~ **Built.** Lives
   entirely in `web/template.html` under "live room": a hand-rolled Phoenix
   channel client, hellos every 15s with a 45s quiet timeout, silent reconnect
   with 15s-capped backoff, wake lock, and hand counts as chips on the score
   rows. Host/Join sits on Setup and the in-round Rules tab; peers arriving
   during Setup seat themselves. The Supabase URL and anon key are the
   `LIVE_URL`/`LIVE_KEY` constants at the top of that section — empty, every
   live control hides. **Amended after the first phone test:** room codes were
   built, worked, and were removed at the user's request — one friend group
   needs one table, so everybody meets on the fixed `cr:lobby` channel and the
   Host/Join buttons differ only in their waiting copy. If two simultaneous
   groups ever become real, codes go back in at the `LIVE_ROOM` constant.
2. ~~Card plays and the in-app alert.~~ **Built and proven phone-to-PC.** A play
   broadcasts name, card and target; discards stay private beyond the hand
   count. Aimed at you: hot pulsing banner, chain rattle, vibration, stays
   until tapped. Anyone else: quiet banner, fades in 8s. Tapping a banner
   opens the card's face. Deduped by event id for the day push becomes the
   second delivery route.
3. ~~Score comparison at lock.~~ **Built and proven phone-to-PC.** No lock
   message: every hello carries the phone's locked rows keyed by hole and
   player name, so comparison is self-healing across reloads and an
   unlock-and-fix clears a flag the same way it raised it. Disagreements
   show as a red panel on whatever hole is open — locking auto-advances, so
   pinning the flag to its own hole meant nobody saw it (phone test) —
   labeled by hole, tap to jump there. Nothing is merged; the flag is the
   entire feature.

   **Grew two companions during phone testing.** Corrections settle the deal:
   deals record what they granted and which cards, every lock re-settles every
   dealt hole (skins corrections re-price later holes), shortfalls deal out on
   the spot, and over-deals send that hole's still-in-hand cards back on top
   of the deck in original order. And fresh cards wait for the table: a
   hole's newly dealt cards are un-playable (and can't buy spins) until every
   phone in the lobby has locked that hole without disagreement — so a wrong
   score's cards are always still in hand for the settlement to reclaim.
   Discarding held cards stays legal; offline rounds have no hold at all.
4. Push, which is its own project: keys, a subscriptions table, an Edge Function to
   send. This is the first server-side code and the first schema in the repo.

## What this contradicts

`BUILD_SPEC.md` lists "No sync, multiplayer, or networking" as a founding
principle, and the README repeats it. Both need rewriting when this lands, along
with a line noting that names and card plays leave the phone for the first time.
