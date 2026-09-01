// Measures the phone frame on every tab and screenshots each screen.
// Run against a dev/preview server: node scripts/visual-check.mjs <url> <outdir>
import { chromium } from "playwright";

const url = process.argv[2] ?? "http://localhost:4173";
const outdir = process.argv[3] ?? "/tmp/visual";

const browser = await chromium.launch({ channel: "chrome" });
const page = await browser.newPage({ viewport: { width: 900, height: 950 } });
await page.goto(url, { waitUntil: "networkidle" });

const tabs = ["home", "feed", "chat", "ground", "stats"];
let previous = null;
let stable = true;

for (let i = 0; i < tabs.length; i++) {
  await page.locator(".tab").nth(i).click();
  await page.waitForTimeout(350);
  const box = await page.locator(".phone").boundingBox();
  const rect = `${Math.round(box.x)},${Math.round(box.y)} ${Math.round(box.width)}x${Math.round(box.height)}`;
  const drift = previous && rect !== previous ? "  <-- MOVED" : "";
  if (drift) stable = false;
  console.log(`${tabs[i].padEnd(7)} phone at ${rect}${drift}`);
  previous = rect;
  await page.screenshot({ path: `${outdir}/${i}_${tabs[i]}.png` });
}

console.log(stable ? "FRAME STABLE across all tabs" : "FRAME UNSTABLE");
await browser.close();
