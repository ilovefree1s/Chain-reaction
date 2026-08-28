/* Serves docs/ — the built app — on http://localhost:5173 with caching off,
   so the page in a test tab is always the file build.js just wrote. */
const http = require("http");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..", "docs");
const types = {
  ".html": "text/html", ".js": "text/javascript", ".css": "text/css",
  ".png": "image/png", ".webp": "image/webp", ".jpg": "image/jpeg",
  ".svg": "image/svg+xml", ".mp3": "audio/mpeg",
  ".webmanifest": "application/manifest+json",
};

http.createServer((req, res) => {
  let p = decodeURIComponent(req.url.split("?")[0]);
  if (p === "/") p = "/index.html";
  const f = path.normalize(path.join(root, p));
  if (!f.startsWith(root) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) {
    res.writeHead(404); res.end("not found"); return;
  }
  res.writeHead(200, {
    "Content-Type": types[path.extname(f)] || "application/octet-stream",
    "Cache-Control": "no-store",
  });
  fs.createReadStream(f).pipe(res);
}).listen(5173, () => console.log("serving docs/ on http://localhost:5173"));
