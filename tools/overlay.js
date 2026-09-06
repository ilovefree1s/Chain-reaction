/*
 * Lays one PNG over another, scaled and centred, straight alpha.
 *
 *   node tools/overlay.js <base.png> <over.png> <cx> <cy> <size> [out.png]
 *
 * Used to swap the badge on a painted card — the wolf medallion on
 * DESTINATION FUCKED! for the 4 THE BOYS logo. Write to a separate out.png
 * first and look at it; overwriting the card in place is hard to undo.
 *
 * Bilinear on the way down so a big logo shrunk onto a badge keeps its edges.
 */
const png = require("./png.js");

const [basePath, overPath, cxs, cys, ds, outPath] = process.argv.slice(2);
const cx = Number(cxs), cy = Number(cys), D = Number(ds);
const out = outPath || basePath;

const base = png.read(basePath);
const over = png.read(overPath);
if (base.channels !== 4) throw new Error("base has no alpha channel");

const oa = (x, y, c) => {
  const i = (y * over.width + x) * over.channels;
  return c === 3 && over.channels < 4 ? 255 : over.data[i + c];
};
/* One channel of the overlay at a fractional position, mixed from the four
   pixels around it — nearest-neighbour would leave the logo's ring jagged. */
function sample(fx, fy, c) {
  const x0 = Math.floor(fx), y0 = Math.floor(fy);
  const x1 = Math.min(over.width - 1, x0 + 1), y1 = Math.min(over.height - 1, y0 + 1);
  if (x0 < 0 || y0 < 0 || x0 >= over.width || y0 >= over.height) return 0;
  const tx = fx - x0, ty = fy - y0;
  const top = oa(x0, y0, c) * (1 - tx) + oa(x1, y0, c) * tx;
  const bot = oa(x0, y1, c) * (1 - tx) + oa(x1, y1, c) * tx;
  return top * (1 - ty) + bot * ty;
}

const left = Math.round(cx - D / 2), top = Math.round(cy - D / 2);
const scale = over.width / D;
let touched = 0;
for (let y = 0; y < D; y++) {
  const by = top + y;
  if (by < 0 || by >= base.height) continue;
  for (let x = 0; x < D; x++) {
    const bx = left + x;
    if (bx < 0 || bx >= base.width) continue;
    const fx = (x + 0.5) * scale - 0.5, fy = (y + 0.5) * (over.height / D) - 0.5;
    const a = sample(fx, fy, 3) / 255;
    if (a <= 0.002) continue;
    const i = (by * base.width + bx) * 4;
    for (let c = 0; c < 3; c++) {
      base.data[i + c] = Math.round(sample(fx, fy, c) * a + base.data[i + c] * (1 - a));
    }
    // The card underneath is solid here, so it stays solid.
    base.data[i + 3] = Math.round(255 * a + base.data[i + 3] * (1 - a));
    touched++;
  }
}
png.write(out, base);
console.log("laid " + overPath.split(/[\\/]/).pop() + " over " + basePath.split(/[\\/]/).pop() +
  " at " + cx + "," + cy + " across " + D + "px — " + touched + " pixels");
