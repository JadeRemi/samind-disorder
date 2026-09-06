import { chromium } from "playwright";
import { readdirSync, statSync, writeFileSync, unlinkSync } from "fs";
import { join, basename, extname, dirname } from "path";

const [,, inDir, outDir, maxDimArg] = process.argv;
const MAX = Number(maxDimArg ?? 1800);
const browser = await chromium.launch({ channel: "chrome" });

const files = [];
const walk = (d) => readdirSync(d).forEach((f) => {
  const p = join(d, f);
  statSync(p).isDirectory() ? walk(p) : extname(p) === ".svg" && files.push(p);
});
walk(inDir);

for (const file of files) {
  const name = basename(file, ".svg").replace(/\s+/g, "_");
  // wrap in html so the SVG can be scaled like an image
  const wrapper = join(dirname(file), `.render_${name}.html`);
  writeFileSync(wrapper, `<style>html,body{margin:0;background:#fff}img{display:block;width:100vw}</style><img src="${encodeURIComponent(basename(file))}">`);
  const page = await browser.newPage({ viewport: { width: 1000, height: 1000 } });
  await page.goto("file://" + wrapper, { waitUntil: "load", timeout: 180000 });
  const box = await page.evaluate(async () => {
    const img = document.querySelector("img");
    if (!img.complete) await img.decode().catch(() => {});
    return { w: img.naturalWidth, h: img.naturalHeight };
  });
  const scale = Math.min(1, MAX / Math.max(box.w || 1, box.h || 1));
  const width = Math.max(320, Math.round((box.w || 800) * scale));
  const height = Math.max(320, Math.round((box.h || 600) * scale));
  await page.setViewportSize({ width, height });
  await page.waitForTimeout(2500);
  await page.screenshot({ path: join(outDir, name + ".png") });
  await page.close();
  unlinkSync(wrapper);
  console.log(`${name}: ${box.w}x${box.h} -> ${width}x${height}`);
}
await browser.close();
