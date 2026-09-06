/*
 * Prepares a tier frame — the picture every card of one rarity wears.
 *
 *   node tools/fit-frame.js rare app/src/main/res/drawable/rare.png
 *   node tools/fit-frame.js rare <file> --check     measure, write nothing
 *
 * Writes drawable-nodpi/frame_<tier>.png, trimmed to the frame's own edge, and
 * an entry in web/art-frames.json saying where its bars are. The deck writes
 * the timing, the name, the rules and the kind into those bars, so a frame
 * carries no words and a reworded card never needs a repaint.
 *
 * Two shapes of frame both work. Some come with the middle filled — dark card
 * stock — and some come cut out, so a photograph can sit behind the frame. The
 * window is found either way: see-through where the file has alpha there, dark
 * where it does not.
 *
 * The bars are placed in fixed proportions of that window rather than hunted
 * for, because these frames arrive as one open field with nothing drawn inside
 * it. Holding every tier to the same proportions is what makes a common and a
 * rare look like the same deck when the frames themselves come out different
 * shapes — and they do, by several percent.
 */
const fs = require("fs");
const path = require("path");
const png = require("./png.js");

const root = path.join(__dirname, "..");
const TIERS = ["common", "uncommon", "rare", "epic", "legendary"];
const tier = (process.argv[2] || "").toLowerCase();
const src = process.argv[3];
const dry = process.argv.indexOf("--check") !== -1;

if (TIERS.indexOf(tier) === -1 || !src) {
  console.error("usage: node tools/fit-frame.js <" + TIERS.join("|") + "> <file.png> [--check]");
  process.exit(1);
}
if (!fs.existsSync(src)) { console.error("no such file: " + src); process.exit(1); }

/* Where each bar sits inside the window, as fractions of it. Taken off the
   first frame that arrived with its bars painted in, so every later frame is
   laid out the way that one was drawn. */
const LAYOUT = {
  timing: [0.0119, 0.0436],
  title:  [0.0574, 0.1208],
  // The ground covers the whole window: on a frame that is cut out there,
  // anything it does not cover is a hole through to the screen.
  ground: [0, 1],
  // And the rules take everything under the name. With no photograph to make
  // room for, the words are what the card is, and they are read at arm's
  // length on a tee pad — so they get the room rather than a panel's worth of
  // it. Short cards simply set larger and sit in the middle of it.
  text:   [0.20, 0.77],
};

const raw = png.read(src);
const W = raw.width, H = raw.height, CH = raw.channels, D = raw.data;
const alpha = (x, y) => (CH === 4 ? D[(y * W + x) * 4 + 3] : 255);

// ---- the frame's own edge ----
let l = W, r = -1, t = H, b = -1;
for (let y = 0; y < H; y++) {
  for (let x = 0; x < W; x++) {
    if (alpha(x, y) <= 140) continue;
    if (x < l) l = x;
    if (x > r) r = x;
    if (y < t) t = y;
    if (y > b) b = y;
  }
}
const cut = png.crop(raw, l, t, r - l + 1, b - t + 1);
const FW = cut.width, FH = cut.height;
const a2 = (x, y) => (cut.channels === 4 ? cut.data[(y * FW + x) * 4 + 3] : 255);
const lum = (x, y) => {
  const i = (y * FW + x) * cut.channels;
  return 0.299 * cut.data[i] + 0.587 * cut.data[i + 1] + 0.114 * cut.data[i + 2];
};

/*
 * ---- the window: cut out, or merely darker than the frame around it ----
 *
 * Backgrounds arrive at any brightness. The first frames had near-black
 * middles; a later pair came in a lit blue running to 131 where the older ones
 * sat at 20. So the edge cannot be a brightness written down here: one loose
 * enough for the blue walks straight through the dark frames' inner border,
 * and one tight enough for those stops on the blue's own glow.
 *
 * It is measured off each picture instead. Sample the middle, take the
 * brightest tenth of it as what the background gets up to, and call the frame
 * anything well past that. The metal is far brighter than any background —
 * 190 and up against 135 at the very most — so the gap is wide either way.
 *
 * And it has to be a run of bright pixels, not one. These backgrounds carry
 * glints and hot spots, and a single bright pixel is a speck in the artwork,
 * not the edge of the card.
*/
const probeX = Math.round(FW / 2), probeY = Math.round(FH * 0.32);
const seeThrough = a2(probeX, probeY) < 60;

/** How bright this background gets: the 90th percentile of a patch of it. */
function backgroundLevel(x0, x1, y0, y1) {
  const seen = [];
  for (let y = y0; y < y1; y += 3) for (let x = x0; x < x1; x += 3) seen.push(lum(x, y));
  seen.sort((a, b) => a - b);
  return seen[Math.floor(seen.length * 0.9)] || 0;
}
const bgWindow = backgroundLevel(
  Math.round(FW * 0.35), Math.round(FW * 0.65),
  Math.round(FH * 0.22), Math.round(FH * 0.45));
// Clear of the background, and still clear of the metal.
const windowEdge = Math.max(75, Math.round(bgWindow + 45));
const inside = seeThrough
  ? (x, y) => a2(x, y) < 60
  : (x, y) => lum(x, y) < windowEdge;
const RUN = 3;
/* Walks out from [from] until the frame proper starts, and answers with the
   last pixel that was still inside the window. */
function edgeFrom(from, step, fixed, along, test) {
  const within = test || inside;
  const limit = along === "x" ? FW : FH;
  let at = from, last = from, run = 0;
  while (true) {
    const next = at + step;
    if (next < 0 || next >= limit) break;
    const isIn = along === "x" ? within(next, fixed) : within(fixed, next);
    if (isIn) { run = 0; last = next; }
    else if (++run >= RUN) break;
    at = next;
  }
  return last;
}
const wl = edgeFrom(probeX, -1, probeY, "x");
const wr = edgeFrom(probeX, 1, probeY, "x");
const midX = Math.round((wl + wr) / 2);
const wt = edgeFrom(probeY, -1, midX, "y");
const wb = edgeFrom(probeY, 1, midX, "y");
const WW = wr - wl + 1, WH2 = wb - wt + 1;

// ---- the empty bar at the foot, where the kind goes ----
// Measured on its own: the bar's ground is a different brightness from the
// window's, and on some frames it is the darker of the two.
const barY = Math.round(FH * 0.90), barX = Math.round(FW * 0.18);
const bgBar = backgroundLevel(
  Math.round(FW * 0.13), Math.round(FW * 0.23),
  Math.round(FH * 0.875), Math.round(FH * 0.925));
const barEdge = Math.max(75, Math.round(bgBar + 45));
const insideBar = (x, y) => lum(x, y) < barEdge;
const bl = edgeFrom(barX, -1, barY, "x", insideBar);
const br = edgeFrom(barX, 1, barY, "x", insideBar);
const barMid = Math.round((bl + br) / 2);
const bt = edgeFrom(barY, -1, barMid, "y", insideBar);
const bb = edgeFrom(barY, 1, barMid, "y", insideBar);
const BW = br - bl + 1, BH = bb - bt + 1;

const box = (x, y, w, h) => ({
  x: +(x / FW).toFixed(4), y: +(y / FH).toFixed(4),
  w: +(w / FW).toFixed(4), h: +(h / FH).toFixed(4),
});
const band = (which, insetX) => {
  const [fy, fh] = LAYOUT[which];
  return box(wl + insetX, Math.round(wt + WH2 * fy), WW - insetX * 2, Math.round(WH2 * fh));
};
const entry = {
  ar: +(FW / FH).toFixed(4),
  timing: band("timing", 10),
  title: band("title", 10),
  ground: band("ground", 0),
  text: band("text", 44),
  // Held to the middle of its bar: given the whole thing the kind grows past
  // the card's own name, which reads as the card being called ATTACK.
  kind: box(bl + Math.round(BW * 0.075), bt + Math.round(BH * 0.22),
            Math.round(BW * 0.84), Math.round(BH * 0.56)),
};

console.log(tier + " frame");
console.log("  file     " + W + " x " + H);
console.log("  trimmed  " + FW + " x " + FH + "   ratio " + entry.ar + "   (the drawn faces are 0.720)");
console.log("  window   " + WW + " x " + WH2 + " at " + wl + "," + wt +
  "   (" + (seeThrough ? "cut out" : "filled") + ")");
console.log("  kind bar " + BW + " x " + BH + " at " + bl + "," + bt);
console.log("  " + JSON.stringify(entry));

if (dry) { console.log("\n--check: nothing written."); process.exit(0); }

png.write(path.join(root, "app", "src", "main", "res", "drawable-nodpi", `frame_${tier}.png`), cut);
const P = path.join(root, "web", "art-frames.json");
const all = fs.existsSync(P) ? JSON.parse(fs.readFileSync(P, "utf8")) : {};
all[tier] = entry;
fs.writeFileSync(P, JSON.stringify(all, null, 1) + "\n");
console.log("\n  wrote    app/src/main/res/drawable-nodpi/frame_" + tier + ".png");
console.log("  wrote    web/art-frames.json");
console.log("\nNow run: node web/build.js");
