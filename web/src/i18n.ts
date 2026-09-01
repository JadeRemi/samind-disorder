// English is the project's primary language; Russian is the expected default
// for pilot users. Default comes from VITE_DEFAULT_LANG (falls back to ru) and
// the user's choice is persisted.
export type Lang = "en" | "ru";

const STRINGS = {
  en: {
    appName: "Samind",
    tabHome: "Home",
    tabFeed: "Feed",
    tabChat: "Chat",
    tabGround: "Ground",
    tabStats: "Stats",
    homeTitle: "Welcome to Samind",
    homeSubtitle: "A quiet companion that redirects attention before a trigger settles.",
    monitoringOn: "Monitoring is on",
    monitoringOff: "Monitoring is off",
    enable: "Enable monitoring",
    disable: "Pause monitoring",
    webNote: "This is the browser preview: monitoring works inside the demo feed only. On-device screen reading and overlays exist in the Android app.",
    disclaimer: "Samind is not a medical device and does not replace professional help.",
    feedHint: "Scroll the feed. With monitoring on, risky posts get intercepted the way the phone overlay would.",
    overlayDismiss: "I'm okay",
    overlayOpen: "Help me refocus",
    chatPlaceholder: "Tell me how you feel…",
    chatSend: "Send",
    chatGreeting: "Hi, I'm here for you. What's on your mind?",
    groundTitle: "Grounding techniques",
    groundStart: "Start",
    groundDone: "I feel better",
    groundNext: "Next",
    statsTitle: "Your progress",
    statsEmpty: "No triggers intercepted yet. That's a good day.",
    statsIntercepted: "triggers intercepted",
    statsSession: "this session",
    language: "Language",
  },
  ru: {
    appName: "Samind",
    tabHome: "Главная",
    tabFeed: "Лента",
    tabChat: "Чат",
    tabGround: "Опора",
    tabStats: "Статистика",
    homeTitle: "Добро пожаловать в Samind",
    homeSubtitle: "Тихий помощник, который переключает внимание до того, как триггер закрепится.",
    monitoringOn: "Мониторинг включён",
    monitoringOff: "Мониторинг выключен",
    enable: "Включить мониторинг",
    disable: "Приостановить мониторинг",
    webNote: "Это браузерная версия: мониторинг работает только внутри демо-ленты. Чтение экрана и оверлеи есть в Android-приложении.",
    disclaimer: "Samind — не медицинское устройство и не заменяет помощь специалиста.",
    feedHint: "Листай ленту. При включённом мониторинге опасные посты перехватываются так же, как это делает оверлей на телефоне.",
    overlayDismiss: "Я в порядке",
    overlayOpen: "Помоги переключиться",
    chatPlaceholder: "Расскажи, как ты…",
    chatSend: "Отправить",
    chatGreeting: "Привет, я рядом. Что у тебя на душе?",
    groundTitle: "Техники заземления",
    groundStart: "Начать",
    groundDone: "Мне лучше",
    groundNext: "Дальше",
    statsTitle: "Твой прогресс",
    statsEmpty: "Пока ни одного перехваченного триггера. Хороший день.",
    statsIntercepted: "триггеров перехвачено",
    statsSession: "за эту сессию",
    language: "Язык",
  },
} as const;

export type StringKey = keyof typeof STRINGS.en;

const KEY = "samind.lang";

export function currentLang(): Lang {
  const stored = localStorage.getItem(KEY);
  if (stored === "en" || stored === "ru") return stored;
  const fallback = (import.meta.env.VITE_DEFAULT_LANG as Lang | undefined) ?? "ru";
  return fallback === "en" ? "en" : "ru";
}

export function setLang(lang: Lang): void {
  localStorage.setItem(KEY, lang);
}

export function t(key: StringKey): string {
  return STRINGS[currentLang()][key];
}
