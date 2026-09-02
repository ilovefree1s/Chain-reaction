// After a card is removed, the audit's decision maps still key the old ids.
//   node tools/shift-settled.js <removed-id>
//
// Both maps, not one. WHEEL_SETTLED was added later and sat out two removals,
// which put every wheel decision above #46 on the wrong card. The sheet reads as
// a record of choices already made, so a note on the wrong card is worse than no
// note at all — you re-decide something that was settled, or leave something
// settled that never was.
const fs = require("fs");
const P = __dirname + "/deck-audit.js";
const GONE = Number(process.argv[2]);
if (!Number.isInteger(GONE)) {
  console.error("usage: node tools/shift-settled.js <removed-id>");
  process.exit(1);
}

function shift(name) {
  const s = fs.readFileSync(P, "utf8");
  const start = s.indexOf(`const ${name} = {`);
  if (start === -1) throw new Error(`${name} not found in deck-audit.js`);
  const end = s.indexOf("\n};", start);
  let dropped = 0, moved = 0;
  const next = s.slice(start, end).replace(/^( *)(\d+):/gm, (line, pad, n) => {
    const id = Number(n);
    if (id === GONE) { dropped++; return `${pad}__DROP__:`; }
    if (id > GONE) { moved++; return `${pad}${id - 1}:`; }
    return line;
  }).replace(/^ *__DROP__:.*(\r?\n)?/gm, "");
  fs.writeFileSync(P, s.slice(0, start) + next + s.slice(end));
  console.log(`${name}: shifted ${moved} keys down, dropped ${dropped}`);
}

["SETTLED", "WHEEL_SETTLED"].forEach(shift);
