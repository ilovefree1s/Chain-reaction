/*
 * Reads both decks and reports whether every card's name/text/kind match.
 *
 *   node tools/sync-check.js [id]
 *
 * BUILD_SPEC.md is the source of truth; GameCard.kt is a transcription of it,
 * and the two drift the moment an edit lands in only one. Pass a card id to
 * print that card's stored JSON back — worth doing after every wording change,
 * since a mangled apostrophe reads as success everywhere else.
 */
const fs = require("fs");
const path = require("path");
const ROOT = path.join(__dirname, "..") + "/";
const spec = fs.readFileSync(ROOT + "BUILD_SPEC.md", "utf8");
const kt = fs.readFileSync(ROOT + "app/src/main/java/com/chainreaction/data/GameCard.kt", "utf8");

const cards = [];
spec.replace(/^\s*(\{ "id": \d+,.*?\})\,?\s*$/gm, (m, obj) => { cards.push(JSON.parse(obj)); return m; });

let bad = 0;
for (const c of cards) {
  const line = kt.split(/\r?\n/).find((l) => l.includes(`GameCard(${c.id}, `));
  if (!line) { console.log(`missing in kotlin: ${c.id}`); bad++; continue; }
  if (!line.includes(`"${c.name}"`)) { console.log(`name  ${c.id}: ${line.trim()}`); bad++; }
  // Kotlin escapes the quotes inside a card's words; the JSON holds them plain.
  const ktText = c.text.replace(/"/g, '\\"');
  if (!line.includes(`"${ktText}"`)) { console.log(`text  ${c.id}: ${line.trim()}`); bad++; }
  if (!line.includes(`CardKind.${c.kind.toUpperCase()}`)) { console.log(`kind  ${c.id}`); bad++; }
}
console.log(`cards: ${cards.length}  kotlin in sync: ${bad === 0}`);

/*
 * The web build keeps several lists of card ids, each id commented with the card
 * it means. Removing a card renumbers the deck under them, and a list the
 * removal tool has not been told about silently starts naming the wrong cards —
 * nothing errors, the wheel just describes cards it isn't showing. IGNORES_EXEMPT
 * spent a removal like that.
 *
 * So the comments are the check: every one of them has to still name the card its
 * id points at. Found by shape rather than by a list of list names, so a new one
 * is covered the day it is written.
 */
const web = fs.readFileSync(ROOT + "web/template.html", "utf8");
const byId = {};
cards.forEach((c) => { byId[c.id] = c.name; });
let drift = 0, lists = 0;
const listRe = /var ([A-Z][A-Z0-9_]+)\s*=\s*\[([^\]]*)\];/g;
let m;
while ((m = listRe.exec(web))) {
  const rows = [...m[2].matchAll(/^\s*(\d+),?\s*(?:\/\/\s*(.*))?$/gm)];
  if (!rows.length) continue;
  lists++;
  for (const r of rows) {
    const claim = (r[2] || "").trim();
    if (!claim) continue;
    const real = byId[Number(r[1])] || "";
    if (claim.toLowerCase().startsWith(real.toLowerCase().slice(0, 8))) continue;
    console.log(`${m[1]} ${r[1]} is "${real || "no such card"}" — comment says "${claim}"`);
    drift++;
  }
}
console.log(`id lists: ${lists}  pointing at the right cards: ${drift === 0}`);

const one = cards.find((c) => c.id === Number(process.argv[2]));
if (one) console.log(JSON.stringify(one, null, 1));
if (bad || drift) process.exit(1);
