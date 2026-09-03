/*
 * Removes one card from the deck and closes the gap.
 *
 *   node tools/remove-card.js <id>
 *
 * Ids must run 1..N with no gaps, so deleting a card renumbers every card above
 * it and moves every id-keyed list with them: the wheel blacklist, the free-spin
 * card, and the web build's targeting/wording sets. Saved rounds hold ids that
 * would now mean different cards, so the storage schema version is bumped too.
 *
 * Single pass with a mapping function, never sequential replaces — going
 * ascending would re-apply to ids already moved.
 */
const fs = require("fs");
const path = require("path");

const REPO = path.join(__dirname, "..");
const GONE = Number(process.argv[2]);
if (!Number.isInteger(GONE)) { console.error("usage: node tools/remove-card.js <id>"); process.exit(1); }

const map = (n) => (n === GONE ? null : n > GONE ? n - 1 : n);
const shift = (list) => list.map(map).filter((n) => n !== null);

function edit(rel, fn) {
  const p = path.join(REPO, rel);
  const before = fs.readFileSync(p, "utf8");
  const after = fn(before);
  fs.writeFileSync(p, after);
  console.log(`  ${rel}${before === after ? " (unchanged)" : ""}`);
  return after;
}

// ---- BUILD_SPEC.md: the source of truth ----
let removedName = null;
let count = 0;
edit("BUILD_SPEC.md", (s) => {
  // Drop the card's own line. The repo checks out CRLF, so the line ending is
  // matched explicitly rather than assumed — `.*\n` silently misses on \r\n,
  // which renumbers the deck without removing anything.
  const line = new RegExp(`^[ \\t]*\\{ "id": ${GONE},.*?\\r?\\n`, "m");
  if (!line.test(s)) throw new Error(`card ${GONE} not found in BUILD_SPEC.md`);
  s = s.replace(line, (m) => {
    removedName = (m.match(/"name": "(.*?)"/) || [])[1];
    return "";
  });
  // Renumber the rest, one pass.
  s = s.replace(/"id": (\d+)/g, (m, n) => `"id": ${map(Number(n))}`);
  count = (s.match(/"id": \d+/g) || []).length;
  // Lists of ids.
  s = s.replace(/"wheelExcludes": \[([^\]]*)\]/, (m, body) =>
    `"wheelExcludes": [${shift(body.split(",").map(Number)).join(", ")}]`);
  s = s.replace(/"freeSpinCard": (\d+)/, (m, n) => `"freeSpinCard": ${map(Number(n))}`);
  // Wheel-only ids move with everything else. This was missed once, which
  // quietly made the wrong card undealtable and dealt the wheel-only one into
  // hands — nothing errors, the deck is just subtly wrong.
  s = s.replace(/"wheelOnly": \[([^\]]*)\]/, (m, body) =>
    `"wheelOnly": [${shift(body.split(",").map(Number)).join(", ")}]`);
  // Deck size in prose.
  s = s.replace(/\b\d+-card deck\b/g, `${count}-card deck`);
  return s;
});

// ---- the Android transcription ----
edit("app/src/main/java/com/chainreaction/data/GameCard.kt", (s) => {
  const kline = new RegExp(`^[ \\t]*GameCard\\(${GONE}, .*?\\r?\\n`, "m");
  if (!kline.test(s)) throw new Error(`card ${GONE} not found in GameCard.kt`);
  s = s.replace(kline, "");
  s = s.replace(/GameCard\((\d+), /g, (m, n) => `GameCard(${map(Number(n))}, `);
  s = s.replace(/WHEEL_EXCLUDES = setOf\(([^)]*)\)/, (m, body) =>
    `WHEEL_EXCLUDES = setOf(${shift(body.split(",").map(Number)).join(", ")})`);
  s = s.replace(/FREE_SPIN_CARD = (\d+)/, (m, n) => `FREE_SPIN_CARD = ${map(Number(n))}`);
  s = s.replace(/WHEEL_ONLY = setOf\(([^)]*)\)/, (m, body) =>
    `WHEEL_ONLY = setOf(${shift(body.split(",").map(Number)).join(", ")})`);
  return s;
});

// ---- the web build's id-keyed sets ----
/*
 * Every id-keyed set in the web build. A new card behaviour means a new set here
 * too — one that is left out does not error, it quietly starts describing the
 * wrong card, and the deck plays wrong in a way nobody can see from the screen.
 * SAYS_IT_ALL and WHEEL_TEXT were missed once and spent a removal pointing one
 * card too high.
 */
const ID_LISTS = ["NO_SELF_CARDS", "PLAY_ON_ALL", "INVITE_CARDS", "AIMED_CARDS",
  "SAYS_IT_ALL", "ONLY_EXEMPT", "TEAM_CARDS", "IGNORES_EXEMPT"];
const ID_MAPS = ["ALERT_TAUNTS", "ALERT_LINES", "WHEEL_TEXT"];

edit("web/template.html", (s) => {
  ID_LISTS.forEach((name) => {
    s = s.replace(new RegExp(`(var ${name}\\s*=\\s*\\[)([\\s\\S]*?)(\\];)`), (m, a, body, c) => {
      const kept = body.replace(/^ *(\d+),?(.*)$/gm, (line, n, rest) => {
        const to = map(Number(n));
        return to === null ? "" : `  ${to},${rest}`;
      }).replace(/\n\n+/g, "\n");
      return a + kept + c;
    });
  });
  ID_MAPS.forEach((name) => {
    s = s.replace(new RegExp(`(var ${name}\\s*=\\s*\\{)([\\s\\S]*?)(\\n\\};)`), (m, a, body, c) => {
      const kept = body.replace(/^( *)(\d+):/gm, (line, indent, n) => {
        const to = map(Number(n));
        return to === null ? `${indent}__DROP__:` : `${indent}${to}:`;
      });
      // A dropped card's wording goes with it.
      return a + kept.replace(/^ *__DROP__:[\s\S]*?(?=\n *\d+:|\n?$)/gm, "") + c;
    });
  });
  // Saved rounds hold ids that now mean different cards.
  s = s.replace(/var SCHEMA_VERSION = (\d+);/, (m, n) => `var SCHEMA_VERSION = ${Number(n) + 1};`);
  return s;
});

// ---- prose that counts the deck ----
/*
 * Prose that counts the deck — and only prose that counts the deck. A blanket
 * swap of every `N cards` in the file rewrote "discard 2 cards to buy a spin"
 * and the two-card limit into nonsense on an earlier removal, so each sentence
 * is named rather than pattern-matched.
 */
["README.md"].forEach((f) => edit(f, (s) =>
  s.replace(/source of truth for the \d+ cards/g, `source of truth for the ${count} cards`)
   .replace(/deck is \d+ cards with unique ids one through (?:\d+|[a-z-]+)/g,
            `deck is ${count} cards with unique ids one through ${count}`)));

edit("app/src/test/java/com/chainreaction/DrawRuleTest.kt", (s) =>
  s.replace(/deck is \d+ cards with unique ids one through (?:\d+|[a-z-]+)/g,
            `deck is ${count} cards with unique ids one through ${count}`)
   .replace(/assertEquals\(\d+, CardDeck\.ALL\.size\)/, `assertEquals(${count}, CardDeck.ALL.size)`)
   .replace(/assertEquals\(\(1\.\.\d+\)\.toSet\(\)/, `assertEquals((1..${count}).toSet()`));

/*
 * The check that would have caught the miss. An id past the end of the deck can
 * only mean a set did not move with everything else — nothing else produces one,
 * and nothing at runtime ever complains about it.
 */
const web = fs.readFileSync(path.join(REPO, "web/template.html"), "utf8");
const strays = [];
ID_LISTS.forEach((name) => {
  const m = web.match(new RegExp(String.raw`var ${name}\s*=\s*\[([\s\S]*?)\];`));
  if (m) [...m[1].matchAll(/^\s*(\d+),/gm)].forEach((x) => {
    if (Number(x[1]) > count) strays.push(`${name}: ${x[1]}`);
  });
});
ID_MAPS.forEach((name) => {
  const m = web.match(new RegExp(String.raw`var ${name} = \{([\s\S]*?)\n\};`));
  if (m) [...m[1].matchAll(/^\s*(\d+):/gm)].forEach((x) => {
    if (Number(x[1]) > count) strays.push(`${name}: ${x[1]}`);
  });
});
if (strays.length) {
  console.error(`Ids past the end of a ${count}-card deck — a set did not shift:`);
  strays.forEach((t) => console.error("  " + t));
  process.exit(1);
}

console.log(`\nRemoved #${GONE} "${removedName}" — deck is now ${count} cards.`);
