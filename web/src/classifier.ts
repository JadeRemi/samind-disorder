// Lexicon tier of the classifier, matching TriggerClassifier.kt plus Russian
// community slang from the trigger corpus. Model inference can slot in later;
// the demo runs on the same rules the app falls back to.
import { normalize } from "./normalize";

export interface Classification {
  risky: boolean;
  score: number;
}

const EN_LEXICON: RegExp[] = [
  /\bthinspo\b/, /\bmeanspo\b/, /\bpro ?ana\b/, /\bpro ?mia\b/, /\bugw\b/,
  /\bcw\b.{0,12}\bgw\b/, /\bstarv\w*/, /\bpurg\w*/,
  /skip\w* (meals?|dinner|breakfast|lunch)/, /(water|liquid) ?fast/,
  /body ?check/, /(collarbones?|ribs) .*(goals?|progress|visible)/,
  /(burn|earn) .*(everything|every)? ?you (ate|eat)/,
  /nothing tastes as good as/, /low restriction/,
  /(only|just) \d{2,3} (kcal|cal(orie)?s?)( today| a day)?/,
];

const RU_LEXICON: RegExp[] = [
  /(?<![а-яё])рхп(?![а-яё])/u,
  /(?<![а-яё])жб(?![а-яё])/u,
  /(?<![а-яё])кп(?![а-яё])/u,
  /голодовк/u,
  /анобабочк/u,
  /мама ана/u,
  /(?<![а-яё])фуро(?![а-яё])/u,
  /(?<![а-яё])бисак(?![а-яё])/u,
  /жуй.{0,3}блюй/u,
  /напарник\w* по похудению/u,
  /(только|всего) \d{2,3} (ккал|кк|калори)/u,
];

const MIN_LENGTH = 12;

export function classify(rawText: string): Classification {
  const text = normalize(rawText);
  if (text.length < MIN_LENGTH) return { risky: false, score: 0 };
  const hits = [...EN_LEXICON, ...RU_LEXICON].filter((re) => re.test(text)).length;
  return { risky: hits > 0, score: Math.min(hits / 2, 1) };
}
