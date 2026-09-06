/*
 * A small PNG reader/writer, so card art can be measured and cropped without
 * an image tool on the box. (`convert` here is Windows' filesystem converter,
 * not ImageMagick.) Handles 8-bit RGB and RGBA, which is what these are.
 */
const fs = require("fs");
const zlib = require("zlib");

const SIG = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

let TABLE = null;
function crcTable() {
  if (TABLE) return TABLE;
  TABLE = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    TABLE[n] = c;
  }
  return TABLE;
}
function crc32(buf) {
  const t = crcTable();
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = t[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function paeth(a, b, c) {
  const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
  return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
}

/** -> { width, height, channels, data } with data as raw rows, no filter bytes. */
function read(file) {
  const buf = fs.readFileSync(file);
  if (!buf.slice(0, 8).equals(SIG)) throw new Error("not a png: " + file);
  let at = 8, ihdr = null;
  const idat = [];
  while (at < buf.length) {
    const len = buf.readUInt32BE(at);
    const type = buf.toString("ascii", at + 4, at + 8);
    const body = buf.slice(at + 8, at + 8 + len);
    if (type === "IHDR") {
      ihdr = { width: body.readUInt32BE(0), height: body.readUInt32BE(4),
               depth: body[8], color: body[9], interlace: body[12] };
    } else if (type === "IDAT") idat.push(body);
    else if (type === "IEND") break;
    at += 12 + len;
  }
  if (!ihdr) throw new Error("no IHDR");
  if (ihdr.depth !== 8) throw new Error("only 8-bit is handled, got " + ihdr.depth);
  if (ihdr.interlace) throw new Error("interlaced pngs are not handled");
  const channels = ihdr.color === 6 ? 4 : ihdr.color === 2 ? 3 : 0;
  if (!channels) throw new Error("only RGB and RGBA are handled, got colour type " + ihdr.color);

  const raw = zlib.inflateSync(Buffer.concat(idat));
  const { width, height } = ihdr;
  const stride = width * channels;
  const out = Buffer.alloc(stride * height);
  let p = 0;
  for (let y = 0; y < height; y++) {
    const filter = raw[p++];
    const row = out.slice(y * stride, (y + 1) * stride);
    raw.copy(row, 0, p, p + stride);
    p += stride;
    const prev = y ? out.slice((y - 1) * stride, y * stride) : null;
    for (let i = 0; i < stride; i++) {
      const a = i >= channels ? row[i - channels] : 0;
      const b = prev ? prev[i] : 0;
      const c = prev && i >= channels ? prev[i - channels] : 0;
      if (filter === 1) row[i] = (row[i] + a) & 255;
      else if (filter === 2) row[i] = (row[i] + b) & 255;
      else if (filter === 3) row[i] = (row[i] + ((a + b) >> 1)) & 255;
      else if (filter === 4) row[i] = (row[i] + paeth(a, b, c)) & 255;
    }
  }
  return { width, height, channels, data: out };
}

function chunk(type, body) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(body.length, 0);
  const head = Buffer.concat([Buffer.from(type, "ascii"), body]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(head), 0);
  return Buffer.concat([len, head, crc]);
}

function write(file, img) {
  const { width, height, channels, data } = img;
  const stride = width * channels;
  /*
   * Every row is filtered five ways and the flattest one kept — the standard
   * minimum-sum-of-absolute-differences pick. Writing rows unfiltered instead
   * cost a quarter again in size on a photograph: deflate can only compress
   * what the filter has already made repetitive.
   */
  const raw = Buffer.alloc((stride + 1) * height);
  const attempt = Buffer.alloc(stride);
  for (let y = 0; y < height; y++) {
    const row = data.slice(y * stride, (y + 1) * stride);
    const prev = y ? data.slice((y - 1) * stride, y * stride) : null;
    let bestF = 0, bestScore = Infinity, best = null;
    for (let f = 0; f <= 4; f++) {
      if (f === 2 && !prev) continue;
      let score = 0;
      for (let i = 0; i < stride; i++) {
        const a = i >= channels ? row[i - channels] : 0;
        const b = prev ? prev[i] : 0;
        const c = prev && i >= channels ? prev[i - channels] : 0;
        const v = f === 0 ? row[i]
          : f === 1 ? row[i] - a
          : f === 2 ? row[i] - b
          : f === 3 ? row[i] - ((a + b) >> 1)
          : row[i] - paeth(a, b, c);
        attempt[i] = v & 255;
        // Signed distance from zero: a row of small deltas beats a row of
        // large ones, whichever direction they run.
        score += attempt[i] < 128 ? attempt[i] : 256 - attempt[i];
      }
      if (score < bestScore) { bestScore = score; bestF = f; best = Buffer.from(attempt); }
    }
    raw[y * (stride + 1)] = bestF;
    best.copy(raw, y * (stride + 1) + 1);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = channels === 4 ? 6 : 2;
  fs.writeFileSync(file, Buffer.concat([
    SIG,
    chunk("IHDR", ihdr),
    chunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
  ]));
}

function crop(img, x0, y0, w, h) {
  const { channels, width, data } = img;
  const out = Buffer.alloc(w * h * channels);
  for (let y = 0; y < h; y++) {
    const from = ((y0 + y) * width + x0) * channels;
    data.copy(out, y * w * channels, from, from + w * channels);
  }
  return { width: w, height: h, channels, data: out };
}

module.exports = { read, write, crop };
