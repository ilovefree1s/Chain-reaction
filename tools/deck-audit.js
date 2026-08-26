/*
 * Builds the deck audit sheet.
 *
 *   node tools/deck-audit.js
 *
 * Everything is read from the repo — card data from BUILD_SPEC.md, and the
 * targeting/wording sets straight out of web/template.html — so the sheet can
 * never drift from what actually ships. The only thing written here is the
 * SETTLED map: decisions already made and built, which drop off the list.
 */
const fs = require("fs");
const path = require("path");

const REPO = path.join(__dirname, "..");
const spec = fs.readFileSync(path.join(REPO, "BUILD_SPEC.md"), "utf8");
const tpl = fs.readFileSync(path.join(REPO, "web", "template.html"), "utf8");
const data = JSON.parse(spec.match(/```json\s*([\s\S]*?)```/)[1]);

// ---- read the app's own lists, so the sheet mirrors the shipped behaviour ----
function idList(name) {
  const m = tpl.match(new RegExp("var " + name + "\\s*=\\s*\\[([\\s\\S]*?)\\];"));
  if (!m) return [];
  return (m[1].match(/\b\d+\b/g) || []).map(Number);
}
function mapKeys(name) {
  const m = tpl.match(new RegExp("var " + name + "\\s*=\\s*\\{([\\s\\S]*?)\\n\\};"));
  if (!m) return [];
  return (m[1].match(/^\s*(\d+):/gm) || []).map(s => parseInt(s, 10));
}
const PLAY_ON_ALL = idList("PLAY_ON_ALL");
const INVITE_CARDS = idList("INVITE_CARDS");
const NO_SELF_CARDS = idList("NO_SELF_CARDS");
const AIMED_CARDS = idList("AIMED_CARDS");
const HAS_WORDS = [...new Set([...mapKeys("ALERT_TAUNTS"), ...mapKeys("ALERT_LINES")])];

// The second wording, for when a card comes off the wheel. Read whole
// rather than by key, since the review is about the words themselves.
function textMap(name) {
  const m = tpl.match(new RegExp("var " + name + "\\s*=\\s*\\{([\\s\\S]*?)\\n\\};"));
  if (!m) return {};
  const out = {};
  m[1].replace(/^\s*(\d+):\s*("(?:[^"\\]|\\.)*")\s*,?\s*$/gm, (line, id, str) => {
    out[Number(id)] = JSON.parse(str);
    return line;
  });
  return out;
}
const WHEEL_TEXT = textMap("WHEEL_TEXT");

// Decisions already made and built. Key = card id, value = what we settled on.
const SETTLED = {
  1: "Kind changed self → attack: the challenge is issued to one named person, so it opens the player picker. Text unchanged.",
  2: "Fine as-is. Attack on one player.",
  4: "Fine as-is. Attack on one player — heckle first, name the target when you play it after.",
  5: "Fine as-is. Attack on one player.",
  6: "Fine as-is. Attack on one player.",
  8: "Fine as-is. Attack on one player.",
  9: "Fine as-is. Attack on one player.",
  11: "Fine as-is. Attack on one player.",
  13: "Fine as-is — wording already rewritten. Attack on one player: the one whose hand you raid.",
  15: "Fine as-is. Attack on one player.",
  16: "Fine as-is. Attack on one player.",
  17: "Fine as-is. Attack on one player.",
  19: "Fine as-is. Whole table — All players picker.",
  20: "Now whole table — All players picker. Everyone is in the running until the drives land; only the shortest one actually loses their cards.",
  18: "Renamed Commentator → Jomez Commentator (also updated where the rules list score-rewriting cards). Targeting and text unchanged: attack on one player.",
  7: "Targeting fine. Wording extended: the lie swap is now owed “the next time they go OB or miss a mando.”",
  10: "Whole table — All players picker. Alert says: “You just got Big Putted, nerd. Now you putt this as a lefty. If you already made it, never mind — but you're still a nerd.”",
  12: "Whole table — All players picker. Announces the call to everyone.",
  21: "Fine as-is. Dual — the favour can land on you or on somebody else.",
  50: "Dual, unchanged targeting. Reworded to match Lefty Off the Box: second person, and the player in last place gets a gimme too, rock paper scissors settling a tie for last.",
  47: "Fine as-is. Dual — self or another player, and the wheel's good/bad spin applies: it can land there and spells out both halves.",
  43: "Fine as-is. Dual — self or another player, and one of the few cards the wheel's good/bad spin actually applies to, since it can land there and spells out both halves.",
  35: "Fine as-is. Dual — self or any other player. Good-vs-bad is table convention only: the app never applies scores, and the one mechanical use of the distinction (the wheel's good/bad spin) can't reach this card because it is wheel-excluded.",
  34: "Dual, unchanged targeting. Rewritten: the free offhand tee shot is named — throw 0 — and the player in last place gets one too, rock paper scissors settling a tie for last.",
  29: "Fine as-is. Dual — picker offers the table plus Myself, and the wheel's good/bad spin applies since it spells out both halves.",
  3: "Fine as-is. Dual — picker offers the table plus Myself, and the wheel's good/bad spin applies since it spells out both halves.",
  26: "Fine as-is. Self card, no target.",
  28: "Renamed Gimme Dat → “I'll Have What He's Having.” Text unchanged. NOTE: still kind self, so it plays with no picker — but it trades lies with an opponent of your choice, the same shape as 1v1 Challenge, which moved to attack.",
  31: "Fine as-is. Self card, no target.",
  36: "Fine as-is. Self card, no target.",
  52: "Fine as-is. Self card, no target.",
  39: "Kind changed self → dual, and the text with it: clear the effects on you, or use it on another player to clear theirs. Picker offers the table plus Myself.",
  27: "Fine as-is. Self card, no target.",
  22: "Self card, no target — stays that way. Any form of “nice” now counts, and the payout moved: walk to where the disc landed and throw again free, instead of replaying the throw.",
  24: "One player, locked in — and the table gets a blue “join in!” invite to help distract them.",
  25: "Whole table — All players picker. Alert says: “Everyone else owes 5 dollars to the ace pot!”",
  30: "Kind changed self → gift → dual. The team-up is agreed out loud, so the card just records who accepted: one player, no “Myself” button. Teammate reads “You're teammates next hole”; the table sees who got picked.",
  32: "Fine as-is. Whole table — All players picker.",
  33: "Fine as-is. Attack on one player.",
  41: "Fine as-is. Whole table — All players picker.",
  45: "Fine as-is. Attack on one player.",
  57: "Now whole table — All players picker. Everyone else tees from the new spot you mark.",
  56: "Targeting fine (attack on one player). Wording pins down the details: either hand, and yes they can putt with it.",
  54: "Now whole table — All players picker. Every opponent inside C1 turbo putts their next shot.",
  53: "Targeting fine (attack on one player). Added the guard rail: somewhat reasonable advice — no “throw it backwards.”",
  51: "Now whole table — All players picker. The 3 discs pass down the line: they pick 1, next person picks from the remaining 2, last person takes what's left.",
  48: "Renamed Sidearm → “Forehand only!”, and the text follows it: throw a forehand, not a sidearm. Targeting unchanged.",
  44: "Targeting fine (attack on one player). Wording widened: the replacement disc can come from any bag, and it is spelled out as a forced mulligan with no stroke.",
  42: "Now whole table — All players picker, retimed to Before all tee. Text: “No run-up on everyone else's tee shot.”",
  40: "Targeting fine (attack on one player). Wording now covers the overlap case: a disc someone else forced on them — an Aerobie or a mini — counts as their one disc.",
  38: "Renamed Not the Recommended Route → “New mando on this hole, bud.” Targeting and text unchanged: attack on one player.",
  58: "Whole table — All players picker.",
  55: "Fine as-is. No target — the race is the announcement, and everyone at the table is in it.",
  49: "Fine as-is. No target — it fires the moment it's drawn, and the table still sees the play land in everyone's alerts.",
  23: "Fine as-is. No target — playing it opens the wheel for a free spin, and the spin itself already alerts every phone at the table.",
  37: "Fine as-is. Reaction, no target — it answers whatever card was just played, so the app has nothing to ask.",
  46: "Fine as-is. Reaction, no target — the card it bounces already names who threw it.",
  14: "Now opens the one-player picker, the first reaction that does. Reworded as theft: you hijack the card as it's played and re-aim it at whoever you choose, the player who played it included. Kind stays react.",
};

/*
 * Pass two: the wheel wording, reviewed the same way — a card drops off this
 * list once its wheel words are agreed. Separate map from SETTLED, because a
 * card can be finished as a play and still read wrong off the wheel.
 *
 * Ruled once, for the seven cards that need somebody to choose (which disc,
 * which mando, which direction, whose shots get commentated): the exempt
 * player chooses, for everybody. They are sitting the card out anyway, and the
 * alternative — each player choosing for themselves — empties the card out.
 * Neither of those cards leaves the wheel over it. Don't reopen this per card.
 */
const WHEEL_SETTLED = {
  1: "Stays off the wheel. Playable from a hand only.",
  4: "Stays off the wheel. Playable from a hand only.",
  6: "Stays off the wheel. Playable from a hand only.",
  7: "Stays off the wheel. Playable from a hand only.",
  20: "Stays off the wheel. Playable from a hand only.",
  24: "Stays off the wheel. Playable from a hand only.",
  32: "Stays off the wheel. Playable from a hand only.",
  44: "Stays off the wheel. Playable from a hand only.",
  22: "Stays off the wheel. Playable from a hand only.",
  25: "Stays off the wheel. Playable from a hand only.",
  13: "Renamed Can I Borrow This? → \"Can I Borrow This Card?\" Stays off the wheel. Playable from a hand only.",
  2: "On the wheel: straddle putting starts once you're inside Circle 2, and runs for every putt from there.",
  12: "Joins the wheel, nobody exempt: the whole table throws for CTP and the winner takes the immunity plus a table-wide attack card.",
  10: "Joins the wheel, and lets nobody off: the first player to make a putt sets it going, so the spun name means nothing. The screen says \"Nobody sits this one out\" instead of naming somebody.",
  23: "Renamed Double Wheel → GAMBLE WHEEL!!, everywhere it appears — card, buttons, rules screen, spec.",
  57: "On the wheel: the exempt player marks the new tee and everyone else plays off it. No 2-disc requirement — that's the hand version.",
  56: "On the wheel: it sits on the whole table but only bites whoever misses inside C1 — the exempt player is safe either way.",
  54: "On the wheel: no C1 limit — everyone but the exempt player turbo putts their next putt from wherever they are.",
  53: "On the wheel: the exempt player advises every other player, and can give each of them something different.",
  51: "Joins the wheel. The exempt player's own discs go round the table until everyone has one to tee with — a 4th gets added at 5 players.",
  48: "On the wheel: everyone but the exempt player throws forehand.",
  45: "On the wheel: everyone but the exempt player rolls their drive or approach.",
  42: "On the wheel: everyone but the exempt player is planted for the next tee shot.",
  41: "Joins the wheel. The exempt player hands out discs from their own bag and everyone else tees with them.",
  40: "On the wheel: the exempt player puts everyone else on one disc, and a disc forced on them by another card still counts as that one.",
  38: "On the wheel: the exempt player sets a mando on the NEXT hole and everyone else plays it — not this hole, since a spin can land with players already past where a mando would sit.",
  33: "On the wheel: everyone but the exempt player drives off hand.",
  19: "Joins the wheel — first card taken off the blacklist. The exempt player picks the disc everyone else tees with. Close to Bag Raid on the wheel, but from your own bag and this hole rather than the next.",
  18: "On the wheel: it flips — the exempt player is the one being commentated, and everyone else has to call their shots. Written as \"anyone who forgets\" rather than \"any one of the 3\", since the table can be 3, 4 or 5.",
  17: "On the wheel: everyone but the exempt player is off the words for the hole. Kept the stroke penalty and the any-variation clause — without them there's nothing to enforce.",
  16: "On the wheel: everyone but the exempt player takes their next putt with their eyes closed.",
  15: "On the wheel: everyone but the exempt player throws a tomahawk on their drive or approach.",
  11: "On the wheel: worded \"everyone but [exempt]\" outright. That split the token in two — [chooser] for whoever makes the picks, [exempt] for whoever is sitting out — since a free spin has a spinner but no exempt player.",
  9: "On the wheel: the exempt player moves every other player's next lie, up to 10 paces, still not out of bounds.",
  8: "On the wheel: the exempt player picks a disc out of any bag for each other player, and it's their tee shot on the next hole — not the next throw, so the whole table is on the same shot.",
  5: "On the wheel: it rides until you've thrown one or the other, since a spin can land mid-hole with some players already off the tee and others still putting.",
};

const AIMED = new Set(["attack", "dual"]);
const TABLE_RE = /every other player|everyone|each (?:other )?player|they all|the whole table|every opponent/i;
// Written as an instruction aimed at somebody else. Off the wheel, where
// everyone but the exempt player carries out the effect, that reads wrong.
const AIMED_RE = /\b(force an opponent|force a player|an opponent|another player|a player|someone|target player|allow a player|choose any disc)\b/i;

const KINDS = {
  attack:   { color: "#FF5A4D", rule: "Picks one opponent from the player list." },
  self:     { color: "#FF8A1E", rule: "No target — plays on you immediately." },
  dual:     { color: "#C77DFF", rule: "Picks one player, or yourself." },
  react:    { color: "#4DC3FF", rule: "No target — answers another card." },
  group:    { color: "#3FA9FF", rule: "No target — the effect reaches the table." },
  sabotage: { color: "#FFC24D", rule: "No target — plays immediately." },
};
const KIND_ORDER = ["attack", "self", "dual", "react", "group", "sabotage"];

const excluded = new Set(data.wheelExcludes);

/*
 * Why a card is off the wheel today. Structural reasons are read from the card
 * itself; the rest were a judgement call in the original spec and say so,
 * rather than having a reason invented for them after the fact.
 */
function whyExcluded(c) {
  if (c.id === data.freeSpinCard) return "It is the wheel — a spin can't demand another spin.";
  if (c.kind === "react") return "A reaction has nothing to react to when it comes off a spin.";
  if (PLAY_ON_ALL.includes(c.id)) return "Already lands on the whole table when it's played.";
  if (c.kind === "group") return "Group card — it already involves everybody.";
  if (c.kind === "sabotage") return "Sabotage — it plays itself the moment you draw it.";
  return "A house call in the original spec. No reason was written down.";
}

function targetOf(c) {
  if (PLAY_ON_ALL.includes(c.id)) return { label: "Whole table", cls: "all" };
  if (AIMED.has(c.kind) || AIMED_CARDS.includes(c.id)) {
    const self = c.kind === "dual" && !NO_SELF_CARDS.includes(c.id);
    return { label: self ? "One player · or self" : "One player", cls: "one" };
  }
  return { label: "No target", cls: "none" };
}

const enriched = data.cards.map(c => {
  const t = targetOf(c);
  const settled = SETTLED[c.id] || null;
  const onWheel = !excluded.has(c.id);
  const twoHalves = /on yourself:/i.test(c.text) && /on another player:/i.test(c.text);
  return {
    id: c.id, name: c.name, kind: c.kind, timing: c.timing, text: c.text,
    target: t.label, targetCls: t.cls, settled,
    onWheel, twoHalves,
    invite: INVITE_CARDS.includes(c.id),
    hasWords: HAS_WORDS.includes(c.id),
    // Pass two. Every card that can land on the wheel is in it until its wheel
    // words are agreed; the ones with no entry read the same either way, which
    // is itself a decision to make rather than assume.
    wheelText: WHEEL_TEXT[c.id] || null,
    wheelSettled: WHEEL_SETTLED[c.id] || null,
    // Every card is in the wheel pass, excluded ones included: staying off the
    // wheel is a decision to take, not a decision already taken.
    wheelWord: !WHEEL_SETTLED[c.id],
    why: onWheel ? null : whyExcluded(c),
    // Still pointed at somebody else — a hint that the wheel wording is missing.
    wheelAimed: onWheel && !WHEEL_TEXT[c.id] && AIMED_RE.test(c.text),
    flag: !settled && !PLAY_ON_ALL.includes(c.id) && TABLE_RE.test(c.text),
  };
});

const settledCount = enriched.filter(c => c.settled).length;
const remaining = enriched.length - settledCount;
const flagged = enriched.filter(c => c.flag).length;
const wheelWork = enriched.filter(c => c.wheelWord).length;
const wheelPool = enriched.filter(c => c.onWheel).length;
const wheelSettledCount = enriched.length - wheelWork;

const html = `<title>Deck Audit — Chain Reaction</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Barlow+Semi+Condensed:ital,wght@0,600;0,800;1,700;1,800&family=Public+Sans:wght@400;500;600&family=JetBrains+Mono:wght@500;700&display=swap">
<style>
  :root {
    --ground: #05070C; --panel: #0A101C; --panel-2: #0D1524; --edge: #22304A;
    --edge-soft: #16233a; --off: #F4F8FF; --sage: #A9B8D4; --dim: #64748f;
    --ice: #4DC3FF; --orange: #FF8A1E;
    --k-attack: #FF5A4D; --k-self: #FF8A1E; --k-dual: #C77DFF; --k-react: #4DC3FF;
    --k-group: #3FA9FF; --k-sabotage: #FFC24D; --ok: #3DDC97;
  }
  * { box-sizing: border-box; }
  html { -webkit-text-size-adjust: 100%; }
  body {
    margin: 0; background:
      radial-gradient(1200px 500px at 80% -10%, rgba(30,111,255,.10), transparent 60%),
      radial-gradient(900px 400px at 0% 0%, rgba(255,138,30,.06), transparent 55%),
      var(--ground);
    color: var(--sage);
    font-family: "Public Sans", system-ui, sans-serif;
    font-size: 16px; line-height: 1.5;
    -webkit-font-smoothing: antialiased;
  }
  .wrap { max-width: 1000px; margin: 0 auto; padding: 32px 20px 80px; }

  header { margin-bottom: 26px; }
  .eyebrow {
    font-family: "JetBrains Mono", monospace; font-size: 12px; font-weight: 700;
    letter-spacing: 3px; text-transform: uppercase; color: var(--ice);
    display: flex; align-items: center; gap: 12px; margin-bottom: 12px;
  }
  .eyebrow::after { content: ""; flex: 1; height: 1px;
    background: linear-gradient(90deg, var(--edge), transparent); }
  h1 {
    font-family: "Barlow Semi Condensed", sans-serif; font-style: italic;
    font-weight: 800; font-size: clamp(38px, 8vw, 62px); line-height: .95;
    letter-spacing: -.5px; color: var(--off); margin: 0 0 10px; text-wrap: balance;
  }
  h1 .u { color: var(--orange); }
  .lede { max-width: 62ch; color: var(--sage); margin: 0; }
  .lede b { color: var(--off); font-weight: 600; }

  .tiles { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 22px 0 8px; }
  .tile { border: 1px solid var(--edge-soft); border-radius: 12px; padding: 12px 14px;
    background: linear-gradient(180deg, var(--panel-2), var(--panel)); }
  .tile b { display: block; font-family: "Barlow Semi Condensed", sans-serif; font-weight: 800;
    font-size: 30px; color: var(--off); line-height: 1; font-variant-numeric: tabular-nums; }
  .tile span { font-family: "JetBrains Mono", monospace; font-size: 11px; font-weight: 500;
    letter-spacing: 1.5px; text-transform: uppercase; color: var(--dim); }
  .tile.flag b { color: var(--k-sabotage); }
  .tile.ok b { color: var(--ok); }
  .tile.wheel b { color: var(--ice); }
  @media (max-width: 640px) { .tiles { grid-template-columns: repeat(2, 1fr); } }

  .controls { position: sticky; top: 0; z-index: 5; margin: 20px -20px 0; padding: 14px 20px;
    background: rgba(5,7,12,.86); backdrop-filter: blur(10px);
    border-bottom: 1px solid var(--edge-soft); }
  .searchrow { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
  #q { flex: 1 1 180px; min-width: 0; background: var(--panel); color: var(--off);
    border: 1px solid var(--edge); border-radius: 10px; padding: 11px 14px;
    font-family: "Public Sans", sans-serif; font-size: 15px; }
  #q::placeholder { color: var(--dim); }
  #q:focus { outline: 2px solid var(--ice); outline-offset: 1px; border-color: transparent; }
  .toggle { border: 1px solid var(--edge); border-radius: 10px; padding: 9px 13px;
    background: var(--panel); color: var(--sage); cursor: pointer; white-space: nowrap;
    font-family: "JetBrains Mono", monospace; font-size: 12px; font-weight: 700;
    letter-spacing: .5px; display: inline-flex; align-items: center; gap: 7px; }
  .toggle .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--edge); }
  .toggle[aria-pressed="true"] { color: var(--k-sabotage); border-color: var(--k-sabotage); }
  .toggle[aria-pressed="true"] .dot { background: var(--k-sabotage); box-shadow: 0 0 8px var(--k-sabotage); }
  .toggle.wheel[aria-pressed="true"] { color: var(--ice); border-color: var(--ice); }
  .toggle.wheel[aria-pressed="true"] .dot { background: var(--ice); box-shadow: 0 0 8px var(--ice); }
  .toggle.ok[aria-pressed="true"] { color: var(--ok); border-color: var(--ok); }
  .toggle.ok[aria-pressed="true"] .dot { background: var(--ok); box-shadow: 0 0 8px var(--ok); }
  .chips { display: flex; flex-wrap: wrap; gap: 7px; }
  .chip { font-family: "JetBrains Mono", monospace; font-size: 11.5px; font-weight: 700;
    letter-spacing: .5px; text-transform: uppercase; cursor: pointer;
    border: 1px solid var(--edge); border-radius: 999px; padding: 6px 12px;
    background: var(--panel); color: var(--sage); display: inline-flex; align-items: center; gap: 7px; }
  .chip .sw { width: 9px; height: 9px; border-radius: 50%; background: var(--kc, var(--edge)); }
  .chip[aria-pressed="true"] { color: var(--off); border-color: var(--kc, var(--ice));
    background: color-mix(in srgb, var(--kc, var(--ice)) 14%, var(--panel)); }
  .chip .n { color: var(--dim); font-weight: 500; }

  section { margin-top: 30px; }
  .khead { display: flex; align-items: baseline; gap: 12px; margin: 0 0 4px;
    padding-bottom: 8px; border-bottom: 1px solid var(--edge-soft); }
  .khead h2 { font-family: "Barlow Semi Condensed", sans-serif; font-style: italic;
    font-weight: 800; font-size: 24px; letter-spacing: .3px; text-transform: uppercase;
    margin: 0; color: var(--kc); }
  .khead .krule { font-size: 13.5px; color: var(--sage); }
  .khead .kn { margin-left: auto; font-family: "JetBrains Mono", monospace; font-size: 12px;
    color: var(--dim); font-weight: 700; }

  .card { display: grid; grid-template-columns: 44px 1fr; gap: 0 14px;
    padding: 14px 16px; border: 1px solid var(--edge-soft); border-left: 3px solid var(--kc);
    border-radius: 10px; margin-top: 10px;
    background: linear-gradient(180deg, var(--panel-2), var(--panel)); }
  .card.isflag { border-color: color-mix(in srgb, var(--k-sabotage) 45%, var(--edge-soft));
    border-left-color: var(--k-sabotage); }
  .card.issettled { opacity: .78; border-left-color: var(--ok); }
  .cid { font-family: "JetBrains Mono", monospace; font-weight: 700; font-size: 13px;
    color: var(--dim); padding-top: 3px; font-variant-numeric: tabular-nums; }
  .cmain { min-width: 0; }
  .cname { font-family: "Barlow Semi Condensed", sans-serif; font-style: italic;
    font-weight: 800; font-size: 21px; line-height: 1.05; color: var(--off);
    letter-spacing: .2px; margin: 0 0 8px; }
  .badges { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 9px; }
  .b { font-family: "JetBrains Mono", monospace; font-size: 10.5px; font-weight: 700;
    letter-spacing: .8px; text-transform: uppercase; border-radius: 5px; padding: 3px 7px;
    border: 1px solid var(--edge); color: var(--sage); white-space: nowrap; }
  .b.kind { color: var(--ground); background: var(--kc); border-color: var(--kc); }
  .b.timing { color: var(--ice); border-color: color-mix(in srgb, var(--ice) 35%, var(--edge)); }
  .b.tg-all { color: var(--ground); background: var(--orange); border-color: var(--orange); }
  .b.tg-one { color: var(--off); border-color: var(--edge); }
  .b.tg-none { color: var(--dim); border-color: var(--edge-soft); }
  .b.flag { color: var(--ground); background: var(--k-sabotage); border-color: var(--k-sabotage); }
  .b.ok { color: var(--ground); background: var(--ok); border-color: var(--ok); }
  .b.wheel { color: var(--ice); border-color: color-mix(in srgb, var(--ice) 45%, var(--edge)); }
  .b.nowheel { color: var(--dim); border-color: var(--edge-soft); }
  .b.halves { color: var(--k-dual); border-color: color-mix(in srgb, var(--k-dual) 45%, var(--edge)); }
  .b.words { color: var(--orange); border-color: color-mix(in srgb, var(--orange) 45%, var(--edge)); }
  .ctext { color: var(--sage); font-size: 15px; margin: 0; max-width: 68ch; }
  .verdict { margin: 9px 0 0; padding: 8px 12px; border-radius: 8px; font-size: 14px;
    color: var(--ok); background: color-mix(in srgb, var(--ok) 9%, transparent);
    border: 1px solid color-mix(in srgb, var(--ok) 30%, var(--edge-soft)); max-width: 68ch; }
  .wheelnote { margin: 9px 0 0; padding: 8px 12px; border-radius: 8px; font-size: 14px;
    color: var(--ice); background: color-mix(in srgb, var(--ice) 8%, transparent);
    border: 1px solid color-mix(in srgb, var(--ice) 28%, var(--edge-soft)); max-width: 68ch; }
  .wheelnote b { color: var(--off); font-weight: 700; }
  /* Held off the wheel reads as a held decision, not a live wording. */
  .wheelnote.off { color: var(--dim);
    background: color-mix(in srgb, var(--dim) 7%, transparent);
    border-color: var(--edge-soft); }

  .empty { text-align: center; color: var(--dim); padding: 60px 0;
    font-family: "JetBrains Mono", monospace; letter-spacing: 1px; }
  footer { margin-top: 40px; padding-top: 18px; border-top: 1px solid var(--edge-soft);
    color: var(--dim); font-size: 13px; }
  footer code { font-family: "JetBrains Mono", monospace; color: var(--sage); }
  @media (prefers-reduced-motion: reduce) { * { transition: none !important; } }
</style>

<div class="wrap">
  <header>
    <div class="eyebrow">Chain Reaction · Deck Audit</div>
    <h1>Every card, <span class="u">both ways round</span></h1>
    <p class="lede">All ${enriched.length} cards, grouped by kind. Pass one — how a card reads when you
      <b>play it</b> at the tee, and the <b>targeting the app derives from it</b> — is done.
      Pass two is live behind the <b>wheel</b> toggle, and every card is in it. ${wheelPool} can land
      on the GAMBLE WHEEL!! and carry the second wording they show there; the other
      ${enriched.length - wheelPool} are held off it, each saying why. Reword one and it joins the
      pool — leave it out and that's a decision too.</p>

    <div class="tiles">
      <div class="tile wheel"><b>${wheelWork}</b><span>Wheel calls to review</span></div>
      <div class="tile ok"><b>${wheelSettledCount}</b><span>Wheel calls agreed</span></div>
      <div class="tile"><b>${remaining}</b><span>Plays to review</span></div>
      <div class="tile ok"><b>${settledCount}</b><span>Plays settled</span></div>
    </div>
  </header>

  <div class="controls">
    <div class="searchrow">
      <input id="q" type="text" placeholder="Search name or text…" autocomplete="off" spellcheck="false">
      <button class="toggle" id="flagOnly" aria-pressed="false"><span class="dot"></span>Table-wide</button>
      <button class="toggle wheel" id="wheelOnly" aria-pressed="true"><span class="dot"></span>Wheel pass ${wheelWork}</button>
      <button class="toggle ok" id="showSettled" aria-pressed="false"><span class="dot"></span>Settled ${settledCount}</button>
    </div>
    <div class="chips" id="chips"></div>
  </div>

  <div id="list"></div>
  <div class="empty" id="empty" hidden>No cards match.</div>

  <footer>
    Generated from <code>BUILD_SPEC.md</code> and the live lists in <code>web/template.html</code>
    (<code>PLAY_ON_ALL</code>, <code>INVITE_CARDS</code>, <code>NO_SELF_CARDS</code>,
    <code>AIMED_CARDS</code>, <code>WHEEL_TEXT</code>, <code>ALERT_TAUNTS</code>,
    <code>ALERT_LINES</code>), so it always mirrors what ships.
    ${wheelPool} of ${enriched.length} cards can land on the wheel; the rest are excluded by
    <code>wheelExcludes</code>. A wheel wording fills in <code>[chooser]</code> with whoever makes
    the picks and <code>[exempt]</code> with whoever is sitting out — the same name on a paid spin,
    different on a free one. Flags are text heuristics — a prompt to look, not a verdict.
  </footer>
</div>

<script>
  const CARDS = ${JSON.stringify(enriched)};
  const KINDS = ${JSON.stringify(KINDS)};
  const ORDER = ${JSON.stringify(KIND_ORDER)};
  // Opens on the pass that's actually running.
  const state = { q: "", kinds: new Set(ORDER), flagOnly: false, wheelOnly: true, showSettled: false };

  const chips = document.getElementById("chips");
  ORDER.forEach(k => {
    const n = CARDS.filter(c => c.kind === k).length;
    const b = document.createElement("button");
    b.className = "chip"; b.setAttribute("aria-pressed", "true");
    b.style.setProperty("--kc", KINDS[k].color);
    b.innerHTML = '<span class="sw"></span>' + k + ' <span class="n">' + n + '</span>';
    b.addEventListener("click", () => {
      if (state.kinds.has(k)) state.kinds.delete(k); else state.kinds.add(k);
      b.setAttribute("aria-pressed", state.kinds.has(k));
      render();
    });
    chips.appendChild(b);
  });

  const q = document.getElementById("q");
  q.addEventListener("input", () => { state.q = q.value.trim().toLowerCase(); render(); });
  [["flagOnly", "flagOnly"], ["wheelOnly", "wheelOnly"], ["showSettled", "showSettled"]]
    .forEach(([id, key]) => {
      const el = document.getElementById(id);
      el.addEventListener("click", () => {
        state[key] = !state[key];
        el.setAttribute("aria-pressed", state[key]);
        render();
      });
    });

  const list = document.getElementById("list");
  const empty = document.getElementById("empty");

  function matches(c) {
    // Each pass has its own idea of "done" — pass one settles how a card is
    // played, pass two settles how it reads off the wheel.
    const done = state.wheelOnly ? c.wheelSettled : c.settled;
    if (done && !state.showSettled && !state.q) return false;
    if (!state.kinds.has(c.kind)) return false;
    if (state.flagOnly && !c.flag) return false;
    if (state.q) {
      const hay = (c.name + " " + c.text + " " + (c.wheelText || "")).toLowerCase();
      if (!hay.includes(state.q)) return false;
    }
    return true;
  }

  function esc(s) { const d = document.createElement("div"); d.textContent = s; return d.innerHTML; }

  function render() {
    list.innerHTML = "";
    let shown = 0;
    ORDER.forEach(k => {
      const cs = CARDS.filter(c => c.kind === k && matches(c));
      if (!cs.length) return;
      shown += cs.length;
      const sec = document.createElement("section");
      sec.style.setProperty("--kc", KINDS[k].color);
      const head = document.createElement("div");
      head.className = "khead";
      head.innerHTML = '<h2>' + k + '</h2><span class="krule">' + KINDS[k].rule +
        '</span><span class="kn">' + cs.length + '</span>';
      sec.appendChild(head);
      cs.forEach(c => {
        const el = document.createElement("article");
        const done = state.wheelOnly ? c.wheelSettled : c.settled;
        el.className = "card" + (c.flag && !state.wheelOnly ? " isflag" : "") + (done ? " issettled" : "");
        el.style.setProperty("--kc", KINDS[c.kind].color);
        el.innerHTML =
          '<div class="cid">' + String(c.id).padStart(2, "0") + '</div>' +
          '<div class="cmain">' +
            '<h3 class="cname">' + esc(c.name) + '</h3>' +
            '<div class="badges">' +
              '<span class="b kind">' + c.kind + '</span>' +
              '<span class="b timing">' + esc(c.timing) + '</span>' +
              '<span class="b tg-' + c.targetCls + '">' + esc(c.target) + '</span>' +
              // Wheel facts stay out of the way until the wheel pass starts.
              (state.wheelOnly
                ? (c.onWheel ? '<span class="b wheel">on the wheel</span>'
                             : '<span class="b nowheel">off the wheel</span>')
                : '') +
              (c.twoHalves ? '<span class="b halves">good / bad halves</span>' : '') +
              (c.invite && !state.wheelOnly ? '<span class="b words">table invited</span>' : '') +
              (c.hasWords && !state.wheelOnly ? '<span class="b words">custom alert</span>' : '') +
              (c.flag && !state.wheelOnly ? '<span class="b flag">⚑ reads table-wide</span>' : '') +
              (c.wheelAimed && state.wheelOnly ? '<span class="b flag">⚑ still points at somebody</span>' : '') +
              (done ? '<span class="b ok">✓ settled</span>' : '') +
            '</div>' +
            '<p class="ctext">' + esc(c.text) + '</p>' +
            // In the wheel pass the card's own words are only the starting
            // point; what's under review is the line below them.
            (state.wheelOnly
              ? (!c.onWheel
                  ? '<p class="wheelnote off"><b>Off the wheel:</b> ' + esc(c.why) +
                    ' It can only be played from a hand. Reword it and it joins the pool.</p>'
                  : c.wheelText
                    ? '<p class="wheelnote"><b>On the wheel:</b> ' + esc(c.wheelText) + '</p>'
                    : '<p class="wheelnote"><b>On the wheel:</b> unchanged — it already reads as an ' +
                      'instruction to you.</p>')
              : '') +
            (done ? '<p class="verdict">' + esc(done) + '</p>' : '') +
          '</div>';
        sec.appendChild(el);
      });
      list.appendChild(sec);
    });
    empty.hidden = shown > 0;
  }
  render();
</script>`;

fs.writeFileSync(path.join(__dirname, "deck-audit.html"), html);
console.log(`wrote deck-audit.html — ${enriched.length} cards, ${settledCount} settled, ` +
  `${wheelWork} need wheel wording, ${flagged} read table-wide`);
console.log(`  read from app: PLAY_ON_ALL=[${PLAY_ON_ALL}] INVITE=[${INVITE_CARDS}] ` +
  `NO_SELF=[${NO_SELF_CARDS}] WORDS=[${HAS_WORDS}]`);
