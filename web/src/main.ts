import { classify } from "./classifier";
import { CHAT_RULES, FEED_POSTS, QUESTIONS, TECHNIQUES } from "./content";
import { currentLang, setLang, t } from "./i18n";

type Tab = "home" | "feed" | "chat" | "ground" | "stats";

const app = document.getElementById("app")!;
let tab: Tab = "home";
let monitoring = localStorage.getItem("samind.monitoring") === "1";
const intercepted = new Set<string>();

function statsCount(): number {
  return Number(localStorage.getItem("samind.stats.count") ?? "0");
}

function recordTrigger(author: string): void {
  localStorage.setItem("samind.stats.count", String(statsCount() + 1));
  const log: string[] = JSON.parse(localStorage.getItem("samind.stats.log") ?? "[]");
  log.unshift(`${author} — ${new Date().toLocaleTimeString()}`);
  localStorage.setItem("samind.stats.log", JSON.stringify(log.slice(0, 20)));
}

function el(html: string): HTMLElement {
  const div = document.createElement("div");
  div.innerHTML = html.trim();
  return div.firstElementChild as HTMLElement;
}

function mascotSvg(size = 48): string {
  return `<svg width="${size}" height="${size}" viewBox="0 0 48 48" aria-hidden="true">
    <circle cx="24" cy="24" r="20" fill="var(--sage)"/>
    <path d="M23 37V22" stroke="var(--mist)" stroke-width="2" stroke-linecap="round"/>
    <path d="M23 24c-6-1-9-5-9-11 6 0 10 3 10 9z" fill="var(--mist)"/>
    <path d="M25 28c5-.8 8-4 8-9-5 0-9 2.5-9 7.5z" fill="var(--mist)"/>
  </svg>`;
}

function render(): void {
  app.innerHTML = "";
  const phone = el(`<div class="phone"></div>`);
  const body = el(`<div class="screen"></div>`);
  phone.append(body, tabbar());
  app.append(phone);
  const views: Record<Tab, (root: HTMLElement) => void> = {
    home: renderHome, feed: renderFeed, chat: renderChat, ground: renderGround, stats: renderStats,
  };
  views[tab](body);
}

function tabbar(): HTMLElement {
  const bar = el(`<nav class="tabbar"></nav>`);
  const items: Array<[Tab, string]> = [
    ["home", t("tabHome")], ["feed", t("tabFeed")], ["chat", t("tabChat")],
    ["ground", t("tabGround")], ["stats", t("tabStats")],
  ];
  for (const [id, label] of items) {
    const btn = el(`<button class="tab ${tab === id ? "active" : ""}">${label}</button>`);
    btn.onclick = () => { tab = id; render(); };
    bar.append(btn);
  }
  return bar;
}

function renderHome(root: HTMLElement): void {
  const lang = currentLang();
  root.append(
    el(`<div class="center">${mascotSvg(96)}</div>`),
    el(`<h1>${t("homeTitle")}</h1>`),
    el(`<p class="sub">${t("homeSubtitle")}</p>`),
    el(`<p class="status">${monitoring ? t("monitoringOn") : t("monitoringOff")}</p>`),
  );
  const toggle = el(`<button class="primary">${monitoring ? t("disable") : t("enable")}</button>`);
  toggle.onclick = () => {
    monitoring = !monitoring;
    localStorage.setItem("samind.monitoring", monitoring ? "1" : "0");
    render();
  };
  const langRow = el(`<div class="lang-row"><span>${t("language")}</span></div>`);
  for (const code of ["ru", "en"] as const) {
    const b = el(`<button class="chip ${lang === code ? "active" : ""}">${code.toUpperCase()}</button>`);
    b.onclick = () => { setLang(code); render(); };
    langRow.append(b);
  }
  root.append(toggle, langRow, el(`<p class="note">${t("webNote")}</p>`), el(`<p class="fine">${t("disclaimer")}</p>`));
}

function renderFeed(root: HTMLElement): void {
  root.append(el(`<p class="note">${t("feedHint")}</p>`));
  const lang = currentLang();
  for (const post of FEED_POSTS[lang]) {
    const card = el(
      `<article class="post"><header>@${post.author}</header><p>${post.text}</p></article>`,
    );
    root.append(card);
    const key = `${lang}:${post.author}`;
    if (monitoring && !intercepted.has(key) && classify(post.text).risky) {
      card.classList.add("dimmed");
      card.append(interception(key));
    }
  }
}

function interception(key: string): HTMLElement {
  const lang = currentLang();
  const questions = QUESTIONS[lang];
  const question = questions[Math.floor(Math.random() * questions.length)];
  const box = el(
    `<div class="intercept">
       <div class="center">${mascotSvg(40)}</div>
       <p class="question">${question}</p>
     </div>`,
  );
  const dismiss = el(`<button class="ghost">${t("overlayDismiss")}</button>`);
  const open = el(`<button class="primary">${t("overlayOpen")}</button>`);
  const done = () => {
    intercepted.add(key);
    recordTrigger(key.split(":")[1]);
  };
  dismiss.onclick = () => { done(); render(); };
  open.onclick = () => { done(); tab = "ground"; render(); };
  box.append(open, dismiss);
  return box;
}

function renderChat(root: HTMLElement): void {
  const lang = currentLang();
  const { rules, fallback, crisis } = CHAT_RULES[lang];
  const log = el(`<div class="chat-log"></div>`);
  const add = (text: string, mine: boolean) => {
    log.append(el(`<p class="bubble ${mine ? "mine" : ""}">${text}</p>`));
    log.scrollTop = log.scrollHeight;
  };
  add(t("chatGreeting"), false);

  const row = el(`<div class="chat-row"></div>`);
  const input = el(`<input placeholder="${t("chatPlaceholder")}">`) as HTMLInputElement;
  const send = el(`<button class="primary">${t("chatSend")}</button>`);
  const reply = (text: string): string => {
    const lower = text.toLowerCase();
    if (crisis.patterns.some((re) => re.test(lower))) return crisis.replies[0];
    for (const rule of rules) {
      if (rule.patterns.some((re) => re.test(lower))) {
        return rule.replies[Math.floor(Math.random() * rule.replies.length)];
      }
    }
    return fallback[Math.floor(Math.random() * fallback.length)];
  };
  const submit = () => {
    const text = input.value.trim();
    if (!text) return;
    add(text, true);
    add(reply(text), false);
    input.value = "";
  };
  send.onclick = submit;
  input.onkeydown = (e) => { if (e.key === "Enter") submit(); };
  row.append(input, send);
  root.append(log, row);
}

function renderGround(root: HTMLElement): void {
  root.append(el(`<h1>${t("groundTitle")}</h1>`));
  for (const technique of TECHNIQUES[currentLang()]) {
    const card = el(
      `<div class="card"><h2>${technique.title}</h2><p>${technique.summary}</p></div>`,
    );
    const start = el(`<button class="primary">${t("groundStart")}</button>`);
    start.onclick = () => runTechnique(root, technique.title, technique.steps);
    card.append(start);
    root.append(card);
  }
}

function runTechnique(root: HTMLElement, title: string, steps: string[]): void {
  let step = 0;
  const show = () => {
    root.innerHTML = "";
    root.append(
      el(`<h1>${title}</h1>`),
      el(`<progress max="${steps.length}" value="${step + 1}"></progress>`),
      el(`<p class="step">${steps[step]}</p>`),
    );
    const last = step === steps.length - 1;
    const next = el(`<button class="primary">${last ? t("groundDone") : t("groundNext")}</button>`);
    next.onclick = () => { if (last) { render(); } else { step++; show(); } };
    root.append(next);
  };
  show();
}

function renderStats(root: HTMLElement): void {
  const count = statsCount();
  root.append(el(`<h1>${t("statsTitle")}</h1>`));
  if (count === 0) {
    root.append(el(`<p class="sub">${t("statsEmpty")}</p>`));
    return;
  }
  root.append(el(
    `<div class="card stat"><span class="big">${count}</span>
     <span>${t("statsIntercepted")} ${t("statsSession")}</span></div>`,
  ));
  const log: string[] = JSON.parse(localStorage.getItem("samind.stats.log") ?? "[]");
  for (const line of log) root.append(el(`<p class="fine">${line}</p>`));
}

render();
