// Mirror of ml/samind_ml/normalize.py and TextNormalizer.kt — keep all three in sync.

const LEET: Record<string, string> = {
  "0": "o", "1": "i", "3": "e", "4": "a", "5": "s",
  "6": "g", "7": "t", "8": "b", "9": "g",
  "@": "a", "$": "s", "!": "i", "+": "t",
  "€": "e", "£": "l", "¡": "i",
};

const HOMOGLYPHS: Record<string, string> = {
  "а": "a", "е": "e", "о": "o", "р": "p", "с": "c",
  "у": "y", "х": "x", "к": "k", "в": "b", "м": "m",
  "н": "h", "т": "t", "і": "i", "ѕ": "s",
  "α": "a", "β": "b", "ε": "e", "ο": "o", "ρ": "p",
  "τ": "t", "υ": "u",
};

const LATIN_TO_CYRILLIC: Record<string, string> = {
  a: "а", e: "е", o: "о", p: "р", c: "с",
  y: "у", x: "х", k: "к", m: "м", t: "т",
  b: "в", h: "н",
};

const CYRILLIC_LEET: Record<string, string> = {
  "0": "о", "3": "з", "4": "ч", "6": "б",
};

const EMOJI_WORDS: Array<[string, string]> = [
  ["⭐", " star "], ["🌟", " star "], ["🪽", " wing "], ["🦋", " butterfly "],
  ["🪶", " feather "], ["👼", " angel "], ["💧", " water "], ["🍽", " meal "],
  ["🔥", " burn "], ["⚖", " weight "], ["🦴", " bones "], ["🚫", " no "],
];

const ZERO_WIDTH = /[​‌‍⁠﻿]/g;
const COMBINING = /[̀-ͯ]/g;
const SEPARATED_WORD = /\b(?:\w[.\-_*·/\\|]){2,}\w\b/gu;
const SEPARATORS = /[.\-_*·/\\|]/g;
const REPEATS = /(.)\1{2,}/gu;
const EMOJI = /[\u{1F000}-\u{1FAFF}☀-➿️]/gu;
const CYRILLIC_CHARS = /[а-яё]/g;
const LATIN_CHARS = /[a-z]/g;

function isLetter(ch: string | undefined): boolean {
  return ch !== undefined && /\p{L}/u.test(ch);
}

function foldLeet(word: string, table: Record<string, string>): string {
  const chars = [...word];
  return chars
    .map((ch, i) => {
      const mapped = table[ch];
      if (!mapped) return ch;
      return isLetter(chars[i - 1]) || isLetter(chars[i + 1]) ? mapped : ch;
    })
    .join("");
}

function foldWord(word: string): string {
  const cyrillic = (word.match(CYRILLIC_CHARS) ?? []).length;
  const latin = (word.match(LATIN_CHARS) ?? []).length;
  if (cyrillic > 0 && cyrillic >= latin) {
    const swapped = [...word].map((ch) => LATIN_TO_CYRILLIC[ch] ?? ch).join("");
    return foldLeet(swapped, CYRILLIC_LEET);
  }
  const swapped = [...word].map((ch) => HOMOGLYPHS[ch] ?? ch).join("");
  return foldLeet(swapped, LEET);
}

export function normalize(text: string): string {
  let s = text.normalize("NFKD");
  s = s.replace(ZERO_WIDTH, "").replace(COMBINING, "");
  s = s.toLowerCase();
  s = s.split(/\s+/).map(foldWord).join(" ");
  s = s.replace(SEPARATED_WORD, (m) => m.replace(SEPARATORS, ""));
  for (const [emoji, word] of EMOJI_WORDS) s = s.split(emoji).join(word);
  s = s.replace(EMOJI, " ");
  s = s.replace(REPEATS, "$1$1");
  return s.replace(/\s+/g, " ").trim();
}
