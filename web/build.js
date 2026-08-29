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

const kinds = new Set(["attack", "self", "dual", "react", "group", "sabotage"]);
data.cards.forEach((c) => {
  if (!kinds.has(c.kind)) problems.push(`card ${c.id} has unknown kind "${c.kind}"`);
  if (!data.timings.includes(c.timing)) problems.push(`card ${c.id} has unlisted timing "${c.timing}"`);
  if (!c.name || !c.text) problems.push(`card ${c.id} is missing a name or text`);
});
data.wheelExcludes.forEach((id) => {
  if (!ids.includes(id)) problems.push(`wheelExcludes references unknown card ${id}`);
});
(data.wheelOnly || []).forEach((id) => {
  if (!ids.includes(id)) problems.push(`wheelOnly references unknown card ${id}`);
  // A card can't be wheel-only and also barred from the wheel — it'd be nowhere.
  if (data.wheelExcludes.includes(id)) problems.push(`card ${id} is both wheelOnly and wheelExcluded`);
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
// The 4 THE BOYS badge riding the hub of every wheel.
const hubLogo = copyArt("blue4theboys.png", "hublogo.png");
// Tab-bar plates, replacing the dot-and-text buttons one at a time as they land.
const navRules = copyArt("navrules.png", "navrules.png");
const navHand = copyArt("navhand.png", "navhand.png");
const navScore = copyArt("navscore.png", "navscore.png");
// The FOR THE BOYS banner riding under the title on the menu.
const ftbImage = copyArt("fortheboys.png", "fortheboys.png");
const menuSound = copyAudio("chains.mp3");
const wheelSound = copyAudio("gamble.mp3");
const wolfSound = copyAudio("lonewolf.mp3");
// The soundtrack, played back to back on the menu-side screens only.
const musicTracks = [
  copyAudio("dischoarderblues.mp3"),
  copyAudio("chainsofglory.mp3"),
  copyAudio("discgolferbeeotch.mp3"),
].filter(Boolean);

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

/*
 * The version everyone reads off the bottom of the menu. VERSION is yours to
 * bump; the build stamp is added automatically, so two phones can be compared
 * even when the version hasn't changed between deploys.
 */
const version = fs.readFileSync(path.join(__dirname, "VERSION"), "utf8").trim();
const now = new Date();
const MONTHS = "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split(" ");
const two = (n) => String(n).padStart(2, "0");
const stamp = `${now.getDate()} ${MONTHS[now.getMonth()]} ${two(now.getHours())}:${two(now.getMinutes())}`;
const versionLabel = `v${version} · ${stamp}`;

// ---- the page ----
const template = fs.readFileSync(path.join(__dirname, "template.html"), "utf8");
[
  "__APP_VERSION__",
  "/*__CARD_DATA__*/",
  "/*__COURSE_DATA__*/",
  "/*__CARD_ART__*/",
  "/*__CHARACTER_DATA__*/",
  "/*__CHARACTER_ART__*/",
  "__MENU_IMAGE__",
  "__BUTTONS_IMAGE__",
  "__GRASS_IMAGE__",
  "__HUB_LOGO__",
  "__NAV_RULES__",
  "__NAV_HAND__",
  "__NAV_SCORE__",
  "__FTB_IMAGE__",
  "__MENU_SOUND__",
  "__WHEEL_SOUND__",
  "__WOLF_SOUND__",
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
  .replace("__HUB_LOGO__", hubLogo)
  .replace("__NAV_RULES__", navRules)
  .replace("__NAV_HAND__", navHand)
  .replace("__NAV_SCORE__", navScore)
  .replace("__FTB_IMAGE__", ftbImage)
  .replace("__MENU_SOUND__", menuSound)
  .replace("__WHEEL_SOUND__", wheelSound)
  .replace("__WOLF_SOUND__", wolfSound)
  .replace("/*__MUSIC_TRACKS__*/", JSON.stringify(musicTracks))
  .replace("__APP_VERSION__", versionLabel)
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
// TWO caches, hashed separately, because they change at wildly different rates. The
// page is ~100 KB and changes on nearly every build; the assets are megabytes and
// change almost never. Under one combined hash — which is what this used to be —
// rewording a single card re-downloaded every photo and both PNGs for everyone who
// had already installed it. Split, a card edit costs the page and nothing else.
const pageFiles = ["./", "index.html", "manifest.webmanifest"];
const assetFiles = copied.map((f) => "assets/" + f);

function digest(parts) {
  const h = crypto.createHash("sha1");
  parts.forEach((p) => h.update(p));
  return h.digest("hex").slice(0, 12);
}

const pageVersion = digest([html, JSON.stringify(pageFiles)]);
const assetVersion = digest(copied.map((f) => fs.readFileSync(path.join(assetsDir, f))));

fs.writeFileSync(
  path.join(dist, "sw.js"),
  `/* Generated by web/build.js — do not edit. */
const PAGE_CACHE = "chainreaction-page-${pageVersion}";
const ASSET_CACHE = "chainreaction-assets-${assetVersion}";
const PAGE_FILES = ${JSON.stringify(pageFiles, null, 2)};
const ASSET_FILES = ${JSON.stringify(assetFiles, null, 2)};

/*
 * Only fetch what isn't already here. When the assets haven't changed their cache name
 * hasn't either, so it survives activation and this finds it already full — which is the
 * whole point of splitting them. The length check re-fills a cache left half-written by
 * an install that failed partway.
 */
async function fill(name, urls) {
  const cache = await caches.open(name);
  const have = await cache.keys();
  if (have.length >= urls.length) return;
  await cache.addAll(urls);
}

self.addEventListener("install", (event) => {
  event.waitUntil(
    Promise.all([fill(PAGE_CACHE, PAGE_FILES), fill(ASSET_CACHE, ASSET_FILES)])
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  const keep = [PAGE_CACHE, ASSET_CACHE];
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((k) => keep.indexOf(k) === -1).map((k) => caches.delete(k)),
      ))
      .then(() => self.clients.claim()),
  );
});

/*
 * The page is network-first, everything else cache-first.
 *
 * Cache-first on the page meant an edit could take two app launches to appear —
 * and, with GitHub Pages holding sw.js for ten minutes, sometimes not even
 * then: the browser kept serving a cached page from a service worker that had
 * not yet noticed it was out of date. For a deck being edited card by card,
 * that is the difference between "shipped" and "shipped to nobody".
 *
 * So a navigation now tries the network first and falls back to the cache when
 * it fails, which is exactly the case that matters on a course: no signal, open
 * the app, play the round. Assets stay cache-first — they are megabytes of
 * photographs and change almost never, and a miss falls through to the network
 * anyway, so a card or character added today loads before its cache catches up.
 */
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;

  if (event.request.mode === "navigate") {
    event.respondWith(
      // cache: "no-store" goes past the browser's HTTP cache as well as ours.
      // GitHub Pages serves the page with max-age=600, so without this a reload
      // can be handed a ten-minute-old copy and look like nothing shipped.
      fetch(event.request, { cache: "no-store" })
        .then((res) => {
          const copy = res.clone();
          caches.open(PAGE_CACHE).then((c) => c.put(event.request, copy)).catch(() => {});
          return res;
        })
        .catch(() => caches.match(event.request).then((hit) => hit || caches.match("index.html"))),
    );
    return;
  }

  // caches.match with no cacheName searches both of ours.
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
console.log(`  total        ${kb(htmlBytes + assetBytes)}`);
console.log(`  caches       page ${pageVersion} (${kb(htmlBytes)}) · assets ${assetVersion} (${kb(assetBytes)})`);
