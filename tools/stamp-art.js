/*
 * Re-stamps a card's art against the card as it reads now.
 *
 *   node tools/stamp-art.js 57
 *   node tools/stamp-art.js all
 *
 * The build refuses to ship a painted card whose words have moved on, because
 * the picture carries those words and would quietly start lying. This is how
 * you tell it the change never mattered — a comma, a rarity the art doesn't
 * show — rather than repainting.
 *
 * It prints what changed before it stamps, so "the art still says the old
 * thing" is a decision you make with the diff in front of you.
 */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const root = path.join(__dirname, "..");
const STAMPS = path.join(root, "web", "art-stamps.json");
const ART = path.join(root, "app", "src", "main", "res", "drawable-nodpi");

const spec = fs.readFileSync(path.join(root, "BUILD_SPEC.md"), "utf8");
const cards = JSON.parse(spec.match(/```json\s*([\s\S]*?)```/)[1]).cards;

// Must stay the same formula the build checks against, or a re-stamp writes
// something it will never accept. What the painting itself shows: the rules
// text only when the picture carries it, since a card with an empty panel has
// its words drawn by the app and can be reworded freely.
const BOXES = path.join(root, "web", "art-boxes.json");
const artBoxes = fs.existsSync(BOXES) ? JSON.parse(fs.readFileSync(BOXES, "utf8")) : {};
const artOf = (c) => [
  c.name, c.timing, c.kind, c.rarity || "common",
  artBoxes[c.id] ? "" : c.text,
].join(" ");
const stampOf = (c) => crypto.createHash("sha1").update(artOf(c)).digest("hex").slice(0, 12);

const painted = fs.readdirSync(ART)
  .map((f) => /^card_(\d{2})\./.exec(f))
  .filter(Boolean)
  .map((m) => parseInt(m[1], 10));

const arg = process.argv[2];
if (!arg) {
  console.error("usage: node tools/stamp-art.js <card id | all>");
  console.error("painted cards: " + (painted.join(", ") || "(none yet)"));
  process.exit(1);
}

const stamps = fs.existsSync(STAMPS) ? JSON.parse(fs.readFileSync(STAMPS, "utf8")) : {};
const want = arg === "all" ? painted : [Number(arg)];

let touched = 0;
for (const id of want) {
  const card = cards.find((c) => c.id === id);
  if (!card) { console.error(`no card ${id}`); process.exit(1); }
  if (painted.indexOf(id) === -1) { console.error(`card ${id} has no art`); process.exit(1); }
  const now = stampOf(card);
  const was = stamps[String(id)];
  if (was && was.stamp === now) { console.log(`#${id} ${card.name} — already matches its art`); continue; }
  console.log(`#${id} ${card.name}`);
  console.log(`   ${card.timing} · ${card.kind} · ${card.rarity || "common"}`);
  console.log(`   ${card.text}`);
  stamps[String(id)] = { stamp: now, name: card.name, stamped: new Date().toISOString().slice(0, 10) };
  touched++;
}

if (touched) {
  fs.writeFileSync(STAMPS, JSON.stringify(stamps, null, 1) + "\n");
  console.log(`\nstamped ${touched} — the build will stop again if these words move.`);
} else {
  console.log("\nnothing to stamp.");
}
