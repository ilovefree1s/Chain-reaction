// After a card is removed, the SETTLED map's keys still point at the old ids.
//   node tools/shift-settled.js <removed-id>
const fs = require("fs");
const P = __dirname + "/deck-audit.js";
const GONE = Number(process.argv[2]);
let s = fs.readFileSync(P, "utf8");
const start = s.indexOf("const SETTLED = {");
const end = s.indexOf("\n};", start);
const body = s.slice(start, end);
let dropped = 0, moved = 0;
const next = body.replace(/^( *)(\d+):/gm, (line, pad, n) => {
  const id = Number(n);
  if (id === GONE) { dropped++; return `${pad}__DROP__:`; }
  if (id > GONE) { moved++; return `${pad}${id - 1}:`; }
  return line;
}).replace(/^ *__DROP__:.*(\r?\n)?/gm, "");
fs.writeFileSync(P, s.slice(0, start) + next + s.slice(end));
console.log(`shifted ${moved} keys down, dropped ${dropped}`);
