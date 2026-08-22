/*
 * Builds docs/ — a hostable, installable, offline PWA.
 *
 *   node web/build.js
 *
 * Output goes to docs/ rather than somewhere tidier because GitHub Pages will only
 * serve from the repo root or /docs. Settings -> Pages -> Deploy from branch -> main
 * -> /docs, and that folder is the live site.
 *
 * Card text comes straight out of BUILD_SPEC.md and the course library and character
 * roster out of shared/, so those stay the single source of truth for both platforms.
 * Artwork is copied out of the Android drawables rather than duplicated, so there is
 * one copy of each image in the repo.
 *
 * Images are separate files, not base64 in the HTML: with 50+ card images inlined the
 * page would be tens of megabytes and slow to first paint. A service worker caches
 * everything on first visit, so it still works with no signal on a course.
 */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const root = path.join(__dirname, "..");
const dist = path.join(root, "docs");
const assetsDir = path.join(dist, "assets");

// ---- card text, out of the spec ----
const spec = fs.readFileSync(path.join(root, "BUILD_SPEC.md"), "utf8");
const match = spec.match(/```json\s*([\s\S]*?)```/);
if (!match) {
  console.error("No ```json block found in BUILD_SPEC.md");
  process.exit(1);
}
const data = JSON.parse(match[1]);

// ---- sanity checks, so a bad edit fails here and not on a course ----
const problems = [];

// Ids must run 1..N with no gaps — the deck size is whatever the spec says it is,
// so adding a card is an edit to one file rather than a hunt for hardcoded counts.
const ids = data.cards.map((c) => c.id);
const expected = Array.from({ length: data.cards.length }, (_, i) => i + 1);
const missing = expected.filter((i) => !ids.includes(i));
if (missing.length) {
  problems.push(`card ids must run 1-${data.cards.length}; missing: ${missing.join(", ")}`);
}
if (new Set(ids).size !== ids.length) problems.push("duplicate card ids");

const kinds = new Set(["attack", "self", "dual", "react", "group", "gift"]);
data.cards.forEach((c) => {
  if (!kinds.has(c.kind)) problems.push(`card ${c.id} has unknown kind "${c.kind}"`);
  if (!data.timings.includes(c.timing)) problems.push(`card ${c.id} has unlisted timing "${c.timing}"`);
  if (!c.name || !c.text) problems.push(`card ${c.id} is missing a name or text`);
});
data.wheelExcludes.forEach((id) => {
  if (!ids.includes(id)) problems.push(`wheelExcludes references unknown card ${id}`);
});
if (!Number.isInteger(data.wheelCost) || data.wheelCost < 0) {
  problems.push("wheelCost must be a whole number of cards");
}
// The free spin has to be a real card, and one you could actually be dealt.
if (!ids.includes(data.freeSpinCard)) {
  problems.push(`freeSpinCard ${data.freeSpinCard} is not a card in the deck`);
}

// ---- the built-in course library, shared with the Android app ----
const library = JSON.parse(fs.readFileSync(path.join(root, "shared", "courses.json"), "utf8"));
const courses = library.courses;

courses.forEach((c, i) => {
  const where = c.name || `course #${i + 1}`;
  if (!c.name) problems.push(`course #${i + 1} has no name`);
  if (!Array.isArray(c.pars)) problems.push(`${where} has no pars array`);
  else if (c.pars.length !== c.holeCount) {
    problems.push(`${where}: holeCount ${c.holeCount} but ${c.pars.length} pars`);
  } else if (c.pars.some((p) => !Number.isInteger(p) || p < 1 || p > 10)) {
    problems.push(`${where} has a par outside 1-10`);
  }
});
const dupes = courses.map((c) => c.name).filter((n, i, a) => a.indexOf(n) !== i);
if (dupes.length) problems.push(`duplicate course names: ${dupes.join(", ")}`);

// ---- the character roster, shared with the Android app ----
// Cosmetic only: a face and a name beside a player. An empty roster is legal —
// the picker simply doesn't appear and a round plays on names alone.
const characters = JSON.parse(
  fs.readFileSync(path.join(root, "shared", "characters.json"), "utf8"),
).characters;

const charIds = characters.map((c) => c.id);
if (new Set(charIds).size !== charIds.length) problems.push("duplicate character ids");
characters.forEach((c, i) => {
  const where = c.name || `character #${i + 1}`;
  if (!Number.isInteger(c.id) || c.id < 1 || c.id > 99) {
    // Two digits, because the artwork is character_01 … character_99.
    problems.push(`${where} has an id outside 1-99`);
  }
  if (!c.name) problems.push(`character #${i + 1} has no name`);
  if (c.color && !/^#[0-9a-fA-F]{6}$/.test(c.color)) {
    problems.push(`${where} has a colour that is not #RRGGBB`);
  }
});

if (problems.length) {
  console.error("Shared data is not valid:");
  problems.forEach((p) => console.error("  - " + p));
  process.exit(1);
}

// ---- artwork, copied from the Android drawables ----
fs.rmSync(dist, { recursive: true, force: true });
fs.mkdirSync(assetsDir, { recursive: true });

const artDir = path.join(root, "app", "src", "main", "res", "drawable-nodpi");
const copied = [];

function copyArt(source, target) {
  const from = path.join(artDir, source);
  if (!fs.existsSync(from)) {
    console.warn(`! ${source} not found — building without it.`);
    return "";
  }
  fs.copyFileSync(from, path.join(assetsDir, target));
  copied.push(target);
  return "assets/" + target;
}

// ---- audio, from the Android raw resources ----
// Same one-copy-in-the-repo rule as the artwork: Android owns the file, the web
// build takes a copy. Precached with everything else, so it works with no signal.
const rawDir = path.join(root, "app", "src", "main", "res", "raw");
function copyAudio(name) {
  const from = path.join(rawDir, name);
  if (!fs.existsSync(from)) {
    console.warn(`! ${name} not found — building without it.`);
    return "";
  }
  fs.copyFileSync(from, path.join(assetsDir, name));
  copied.push(name);
  return "assets/" + name;
}

const menuImage = copyArt("chainreactionmain.png", "menu.png");
const buttonsImage = copyArt("mainbuttons.png", "buttons.png");
const grassImage = copyArt("moregrass.png", "grass.png");
const iconImage = copyArt("chainreactionicon.png", "icon.png");
const menuSound = copyAudio("chains.mp3");

// ---- card faces: card_01 .. one per card, dropped into the Android drawables ----
// Optional, per card — any card without art keeps its text tile. Alphabetical
// order means .webp beats .png beats .jpg when the same card has several.
const cardArt = {};
fs.readdirSync(artDir).sort().forEach((f) => {
  const m = /^card_(\d{2})\.(png|webp|jpe?g)$/.exec(f);
  if (!m) return;
  const id = parseInt(m[1], 10);
  if (!ids.includes(id)) {
    problems.push(`card art ${f} does not match any card id`);
    return;
  }
  fs.copyFileSync(path.join(artDir, f), path.join(assetsDir, f));
  copied.push(f);
  cardArt[id] = "assets/" + f;
});
// ---- character faces: character_01 … dropped into the Android drawables ----
// Optional, per character — one without art draws as its colour and initial.
const characterArt = {};
fs.readdirSync(artDir).sort().forEach((f) => {
  const m = /^character_(\d{2})\.(png|webp|jpe?g)$/.exec(f);
  if (!m) return;
  const id = parseInt(m[1], 10);
  if (!charIds.includes(id)) {
    problems.push(`character art ${f} does not match any character in the roster`);
    return;
  }
  fs.copyFileSync(path.join(artDir, f), path.join(assetsDir, f));
  copied.push(f);
  characterArt[id] = "assets/" + f;
});

if (problems.length) {
  problems.forEach((p) => console.error("  - " + p));
  process.exit(1);
}

// ---- the page ----
const template = fs.readFileSync(path.join(__dirname, "template.html"), "utf8");
[
  "/*__CARD_DATA__*/",
  "/*__COURSE_DATA__*/",
  "/*__CARD_ART__*/",
  "/*__CHARACTER_DATA__*/",
  "/*__CHARACTER_ART__*/",
  "__MENU_IMAGE__",
  "__BUTTONS_IMAGE__",
  "__GRASS_IMAGE__",
  "__MENU_SOUND__",
].forEach((token) => {
  if (!template.includes(token)) {
    console.error(`template.html is missing the ${token} placeholder`);
    process.exit(1);
  }
});

const html = template
  .replace("/*__CARD_DATA__*/", JSON.stringify(data))
  .replace("/*__COURSE_DATA__*/", JSON.stringify(courses))
  .replace("/*__CARD_ART__*/", JSON.stringify(cardArt))
  .replace("/*__CHARACTER_DATA__*/", JSON.stringify(characters))
  .replace("/*__CHARACTER_ART__*/", JSON.stringify(characterArt))
  .replace("__MENU_IMAGE__", menuImage)
  .replace("__BUTTONS_IMAGE__", buttonsImage)
  .replace("__GRASS_IMAGE__", grassImage)
  .replace("__MENU_SOUND__", menuSound)
  ;
fs.writeFileSync(path.join(dist, "index.html"), html, "utf8");

// ---- installable ----
fs.writeFileSync(
  path.join(dist, "manifest.webmanifest"),
  JSON.stringify(
    {
      name: "Chain Reaction",
      short_name: "Chain Reaction",
      description: "Disc golf card game companion",
      start_url: ".",
      scope: ".",
      display: "standalone",
      orientation: "portrait",
      background_color: "#05070C",
      theme_color: "#05070C",
      icons: iconImage
        ? [{ src: iconImage, sizes: "1254x1254", type: "image/png", purpose: "any maskable" }]
        : [],
    },
    null,
    2,
  ),
  "utf8",
);

// ---- offline ----
// Cache version is a hash of everything precached, so a rebuild that changes any file
// invalidates the old cache and a rebuild that changes nothing leaves it alone.
const precache = ["./", "index.html", "manifest.webmanifest", ...copied.map((f) => "assets/" + f)];
const hash = crypto.createHash("sha1");
hash.update(html);
copied.forEach((f) => hash.update(fs.readFileSync(path.join(assetsDir, f))));
const version = hash.digest("hex").slice(0, 12);

fs.writeFileSync(
  path.join(dist, "sw.js"),
  `/* Generated by web/build.js — do not edit. */
const CACHE = "chainreaction-${version}";
const PRECACHE = ${JSON.stringify(precache, null, 2)};

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(PRECACHE)).then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

// Cache-first: courses have no signal, and the content only changes on redeploy.
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  event.respondWith(
    caches.match(event.request).then((hit) => hit || fetch(event.request)),
  );
});
`,
  "utf8",
);

// ---- report ----
function kb(bytes) { return (bytes / 1024).toFixed(0) + " KB"; }
const assetBytes = copied.reduce((n, f) => n + fs.statSync(path.join(assetsDir, f)).size, 0);
const htmlBytes = Buffer.byteLength(html, "utf8");

const faces = Object.keys(cardArt).length;
const charFaces = Object.keys(characterArt).length;
console.log(`Wrote ${path.relative(root, dist)}/ — installable, offline-capable.`);
console.log(`  index.html   ${kb(htmlBytes)}  (${data.cards.length} cards, ${faces} with art, ${courses.length} courses)`);
console.log(`               ${characters.length} characters, ${charFaces} with art`);
console.log(`  assets/      ${kb(assetBytes)}  (${copied.length} file${copied.length === 1 ? "" : "s"})`);
console.log(`  total        ${kb(htmlBytes + assetBytes)}   cache ${version}`);
