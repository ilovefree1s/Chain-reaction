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

// ---- the window, cut out or merely dark ----
const probeX = Math.round(FW / 2), probeY = Math.round(FH * 0.32);
const seeThrough = a2(probeX, probeY) < 60;
const inside = seeThrough
  ? (x, y) => a2(x, y) < 60
  : (x, y) => lum(x, y) < 75;
let wl = probeX, wr = probeX, wt = probeY, wb = probeY;
while (wl > 0 && inside(wl - 1, probeY)) wl--;
while (wr < FW - 1 && inside(wr + 1, probeY)) wr++;
const midX = Math.round((wl + wr) / 2);
while (wt > 0 && inside(midX, wt - 1)) wt--;
while (wb < FH - 1 && inside(midX, wb + 1)) wb++;
const WW = wr - wl + 1, WH2 = wb - wt + 1;

// ---- the empty bar at the foot, where the kind goes ----
const barY = Math.round(FH * 0.90), barX = Math.round(FW * 0.18);
const dark = (x, y) => lum(x, y) < 75;
let bl = barX, br = barX, bt = barY, bb = barY;
while (bl > 0 && dark(bl - 1, barY)) bl--;
while (br < FW - 1 && dark(br + 1, barY)) br++;
const barMid = Math.round((bl + br) / 2);
while (bt > 0 && dark(barMid, bt - 1)) bt--;
while (bb < FH - 1 && dark(barMid, bb + 1)) bb++;
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
