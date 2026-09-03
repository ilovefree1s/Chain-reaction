/*
 * Sets a card's rarity in BUILD_SPEC.md, by name.
 *
 *   node tools/set-rarity.js "Baby Discs" epic
 *   node tools/set-rarity.js "Baby Discs" common     (removes the field)
 *
 * Common is the absence of the field rather than a value, so most of the deck
 * says nothing and only the cards that matter carry one. Setting a card back to
 * common takes the field out again rather than writing "common" into it.
 *
 * By name, because that is how the deck gets talked about — ids move whenever a
 * card is removed, and a rarity written against the wrong id is exactly the kind
 * of drift nothing errors on.
 */
const fs = require("fs");
const path = require("path");

const P = path.join(__dirname, "..", "BUILD_SPEC.md");
const TIERS = ["common", "uncommon", "rare", "epic", "legendary"];
const name = process.argv[2];
const tier = (process.argv[3] || "").toLowerCase();

if (!name || TIERS.indexOf(tier) === -1) {
  console.error('usage: node tools/set-rarity.js "<card name>" <' + TIERS.join("|") + ">");
  process.exit(1);
}

const src = fs.readFileSync(P, "utf8");
const lines = src.split(/\r?\n/);
const nl = src.includes("\r\n") ? "\r\n" : "\n";

// The card's own line in the JSON block, found by its exact name.
const needle = '"name": "' + name + '"';
const at = lines.findIndex((l) => l.indexOf(needle) !== -1 && l.indexOf('"id":') !== -1);
if (at === -1) {
  console.error('no card named "' + name + '" — names are exact, quotes and all');
  process.exit(1);
}

let row = lines[at];
const had = (row.match(/"rarity": "([a-z]+)"/) || [])[1] || "common";
row = row.replace(/, "rarity": "[a-z]+"/, "");
if (tier !== "common") {
  // Sits after kind and before name, which is the order a card gets described in.
  row = row.replace(/("kind": "[a-z]+")/, '$1, "rarity": "' + tier + '"');
}
lines[at] = row;
fs.writeFileSync(P, lines.join(nl), "utf8");

const card = JSON.parse(row.trim().replace(/,$/, ""));
console.log(card.name + ":  " + had + " -> " + (card.rarity || "common"));
