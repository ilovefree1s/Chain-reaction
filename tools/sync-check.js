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
const one = cards.find((c) => c.id === Number(process.argv[2]));
if (one) console.log(JSON.stringify(one, null, 1));
