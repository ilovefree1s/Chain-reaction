/*
 * Takes a painted card, trims it to its own edge, finds the empty panel the
 * painting left for the card's words, and records both.
 *
 *   node tools/fit-art.js 57 app/src/main/res/drawable/lonewolf.png
 *
 * Writes app/src/main/res/drawable-nodpi/card_NN.png — the cropped picture the
 * build copies — and an entry in web/art-boxes.json saying where the panel is,
 * as fractions of that picture, plus its aspect ratio so the page can lay the
 * card out before the image has loaded.
 *
 * Two things are measured rather than assumed:
 *
 *   The edge. Card art arrives with a soft outer glow or a transparent
 *   surround, and the tier glow the app draws is a shadow cast from the
 *   picture's own shape — so it has to sit against the painted edge, not
 *   against padding. Cropped on alpha where the file has it, on brightness
 *   where it does not.
 *
 *   The panel. Found as the largest unbroken run of dim rows in the lower half:
 *   the panel interior is flat and dark, and its own lit border stops the run.
 *   Its left and right come from scanning outward from the middle of it, so the
 *   dark of the frame further out is never mistaken for more panel.
 *
 * Pass --check to measure and print without writing anything.
 */
const fs = require("fs");
const path = require("path");
const png = require("./png.js");

const root = path.join(__dirname, "..");
const id = Number(process.argv[2]);
const src = process.argv[3];
const dry = process.argv.indexOf("--check") !== -1;

if (!id || !src) {
  console.error("usage: node tools/fit-art.js <card id> <source.png> [--check]");
  process.exit(1);
}
if (!fs.existsSync(src)) { console.error("no such file: " + src); process.exit(1); }

const spec = fs.readFileSync(path.join(root, "BUILD_SPEC.md"), "utf8");
const card = JSON.parse(spec.match(/```json\s*([\s\S]*?)```/)[1]).cards.find((c) => c.id === id);
if (!card) { console.error("no card " + id); process.exit(1); }

const img = png.read(src);
const W = img.width, H = img.height, CH = img.channels, D = img.data;
const lum = (x, y) => {
  const i = (y * W + x) * CH;
  return 0.299 * D[i] + 0.587 * D[i + 1] + 0.114 * D[i + 2];
};
const alpha = (x, y) => (CH === 4 ? D[(y * W + x) * 4 + 3] : 255);

// ---- the painted edge ----
// Half-transparent pixels are the glow around the card, not the card.
let l = W, r = -1, t = H, b = -1;
const solid = CH === 4
  ? (x, y) => alpha(x, y) > 140
  : (x, y) => lum(x, y) > 128;
for (let y = 0; y < H; y++) {
  for (let x = 0; x < W; x++) {
    if (!solid(x, y)) continue;
    if (x < l) l = x;
    if (x > r) r = x;
    if (y < t) t = y;
    if (y > b) b = y;
  }
}
const cw = r - l + 1, chh = b - t + 1;
const cut = png.crop(img, l, t, cw, chh);

// ---- the empty panel, in the cropped picture's own coordinates ----
const cl = (x, y) => {
  const i = (y * cut.width + x) * cut.channels;
  return 0.299 * cut.data[i] + 0.587 * cut.data[i + 1] + 0.114 * cut.data[i + 2];
};
function rowBright(y) {
  let m = 0;
  for (let x = Math.round(cut.width * 0.25); x < cut.width * 0.75; x++) m = Math.max(m, cl(x, y));
  return m;
}
let best = [0, -1], run = -1;
for (let y = Math.round(cut.height * 0.45); y < cut.height; y++) {
  if (rowBright(y) < 60) { if (run < 0) run = y; }
  else { if (run >= 0 && y - 1 - run > best[1] - best[0]) best = [run, y - 1]; run = -1; }
}
if (run >= 0 && cut.height - 1 - run > best[1] - best[0]) best = [run, cut.height - 1];
const [top, bot] = best;
if (bot <= top) { console.error("no empty panel found — is the box painted in?"); process.exit(1); }

const midY = Math.round((top + bot) / 2), midX = Math.round(cut.width / 2);
let left = midX, right = midX;
while (left > 0 && cl(left - 1, midY) < 60) left--;
while (right < cut.width - 1 && cl(right + 1, midY) < 60) right++;

// A little air inside the painted border, so the words never touch it.
const padX = Math.round((right - left + 1) * 0.04), padY = Math.round((bot - top + 1) * 0.08);
const box = {
  x: +((left + padX) / cut.width).toFixed(4),
  y: +((top + padY) / cut.height).toFixed(4),
  w: +((right - left + 1 - padX * 2) / cut.width).toFixed(4),
  h: +((bot - top + 1 - padY * 2) / cut.height).toFixed(4),
  ar: +(cut.width / cut.height).toFixed(4),
};

console.log("#" + id + "  " + card.name);
console.log("  file     " + W + " x " + H);
console.log("  trimmed  " + cw + " x " + chh + "   (left " + l + ", top " + t +
  ", right " + (W - 1 - r) + ", bottom " + (H - 1 - b) + ")");
console.log("  ratio    " + box.ar + "   (the drawn faces are 0.720)");
console.log("  panel    " + (right - left + 1) + " x " + (bot - top + 1) + " px at " + left + "," + top);
console.log("  box      " + JSON.stringify(box));

if (dry) { console.log("\n--check: nothing written."); process.exit(0); }

const two = String(id).padStart(2, "0");
const dest = path.join(root, "app", "src", "main", "res", "drawable-nodpi", `card_${two}.png`);
png.write(dest, cut);

const BOXES = path.join(root, "web", "art-boxes.json");
const boxes = fs.existsSync(BOXES) ? JSON.parse(fs.readFileSync(BOXES, "utf8")) : {};
boxes[String(id)] = box;
fs.writeFileSync(BOXES, JSON.stringify(boxes, null, 1) + "\n");

console.log("\n  wrote    " + path.relative(root, dest) + "  " +
  (fs.statSync(dest).size / 1048576).toFixed(2) + " MB");
console.log("  wrote    web/art-boxes.json");
console.log("\nNow run: node web/build.js");
