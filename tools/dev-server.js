/* Serves docs/ — the built app — with caching off, so the page in a test tab is
   always the file build.js just wrote. The port comes from PORT when the harness
   assigns one, so a second chat's server does not collide with the first.

   /tools/ is served too, for the deck audit sheet. It is a real page with real
   scripting, and a browser opening it off the filesystem gets a static snapshot
   instead — served over http it works, without putting a review sheet inside the
   folder that publishes to Pages. */
const http = require("http");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..", "docs");
const tools = __dirname;
const port = Number(process.env.PORT) || 5173;
// Charset spelled out on everything with words in it: the app and the audit
// sheet are full of em dashes and the group's own punctuation, and a browser
// left to guess renders every one of them as mojibake.
const types = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".png": "image/png", ".webp": "image/webp", ".jpg": "image/jpeg",
  ".svg": "image/svg+xml", ".mp3": "audio/mpeg",
  ".webmanifest": "application/manifest+json",
};

http.createServer((req, res) => {
  let p = decodeURIComponent(req.url.split("?")[0]);
  if (p === "/") p = "/index.html";
  const under = p.startsWith("/tools/") ? tools : root;
  const f = path.normalize(path.join(under, p.replace(/^\/tools/, "")));
  if (!f.startsWith(under) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) {
    res.writeHead(404); res.end("not found"); return;
  }
  res.writeHead(200, {
    "Content-Type": types[path.extname(f)] || "application/octet-stream",
    "Cache-Control": "no-store",
  });
  fs.createReadStream(f).pipe(res);
}).listen(port, () => console.log("serving docs/ on http://localhost:" + port));
