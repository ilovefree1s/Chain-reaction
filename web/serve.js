/*
 * Serves docs/ over http for local testing — the same folder GitHub Pages publishes.
 *
 *   node web/serve.js          # http://localhost:5173
 *   node web/serve.js 8080     # different port
 *
 * A service worker only registers on https or localhost, so opening docs/index.html
 * as a file will never exercise the offline path — use this instead.
 */
const http = require("http");
const fs = require("fs");
const path = require("path");

const dist = path.join(__dirname, "..", "docs");
const port = Number(process.argv[2]) || 5173;

const TYPES = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".webmanifest": "application/manifest+json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".webp": "image/webp",
  ".svg": "image/svg+xml",
};

http
  .createServer((req, res) => {
    const url = decodeURIComponent(req.url.split("?")[0]);
    const rel = url === "/" ? "index.html" : url.replace(/^\/+/, "");
    // Stay inside dist/ whatever the request asks for.
    const file = path.join(dist, rel);
    if (!file.startsWith(dist)) {
      res.writeHead(403).end("forbidden");
      return;
    }
    fs.readFile(file, (err, body) => {
      if (err) {
        res.writeHead(404, { "content-type": "text/plain" }).end("not found");
        return;
      }
      res.writeHead(200, {
        "content-type": TYPES[path.extname(file).toLowerCase()] || "application/octet-stream",
        // No caching from the server: the service worker owns caching, and a stale
        // sw.js is the one thing that makes a redeploy invisible.
        "cache-control": "no-cache",
      });
      res.end(body);
    });
  })
  .listen(port, () => console.log(`serving docs/ on http://localhost:${port}`));
