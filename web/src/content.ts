import type { Lang } from "./i18n";

export const QUESTIONS: Record<Lang, string[]> = {
  en: [
    "What kind of weather do you like most?",
    "If you could teleport anywhere right now, where would you go?",
    "What song has been stuck in your head lately?",
    "What smell instantly reminds you of childhood?",
    "If animals could talk, which would be the rudest?",
    "If your week had a color, what would it be?",
    "What would you ask a time traveler from 3026?",
    "If clouds had flavors, what would today's taste like?",
    "What's the softest thing you've ever touched?",
    "If you opened a tiny shop, what would it sell?",
  ],
  ru: [
    "Какую погоду ты любишь больше всего?",
    "Если бы можно было телепортироваться прямо сейчас — куда?",
    "Какая песня застряла у тебя в голове на этой неделе?",
    "Какой запах мгновенно возвращает тебя в детство?",
    "Если бы животные умели говорить, кто был бы самым дерзким?",
    "Если бы у твоей недели был цвет — какой?",
    "О чём бы ты спросил путешественника во времени из 3026 года?",
    "Если бы у облаков был вкус, каким было бы сегодняшнее?",
    "Какая самая мягкая вещь, к которой ты прикасался?",
    "Если бы ты открыл крошечный магазинчик, что бы он продавал?",
  ],
};

export interface Technique {
  title: string;
  summary: string;
  steps: string[];
}

export const TECHNIQUES: Record<Lang, Technique[]> = {
  en: [
    {
      title: "5-4-3-2-1 senses",
      summary: "Anchor yourself through your five senses.",
      steps: [
        "Name 5 things you can see around you.",
        "Name 4 things you can physically feel.",
        "Name 3 things you can hear right now.",
        "Name 2 things you can smell.",
        "Name 1 thing you can taste.",
      ],
    },
    {
      title: "Box breathing",
      summary: "Slow, even breaths to settle your body.",
      steps: [
        "Breathe in through your nose for 4 counts.",
        "Hold your breath for 4 counts.",
        "Breathe out slowly for 4 counts.",
        "Hold empty for 4 counts.",
        "Repeat the square 4 more times.",
      ],
    },
    {
      title: "Cool reset",
      summary: "Temperature shift interrupts the stress loop.",
      steps: [
        "Go to the nearest sink.",
        "Run cool water over your wrists for 30 seconds.",
        "Notice the temperature changing on your skin.",
        "Pat your face gently with cool hands.",
      ],
    },
    {
      title: "Category sprint",
      summary: "Give the racing mind a neutral job.",
      steps: [
        "Pick a category: cities, animals, or films.",
        "Name one item for every letter from A to J.",
        "Stuck on a letter? Skip it, keep moving.",
        "Notice how your breathing slowed down.",
      ],
    },
  ],
  ru: [
    {
      title: "5-4-3-2-1: органы чувств",
      summary: "Заземлись через пять чувств.",
      steps: [
        "Назови 5 вещей, которые видишь вокруг.",
        "Назови 4 вещи, которые физически ощущаешь.",
        "Назови 3 звука, которые слышишь прямо сейчас.",
        "Назови 2 запаха.",
        "Назови 1 вкус.",
      ],
    },
    {
      title: "Дыхание по квадрату",
      summary: "Медленные ровные вдохи успокаивают тело.",
      steps: [
        "Вдохни через нос на 4 счёта.",
        "Задержи дыхание на 4 счёта.",
        "Медленно выдохни на 4 счёта.",
        "Пауза на 4 счёта.",
        "Повтори квадрат ещё 4 раза.",
      ],
    },
    {
      title: "Прохладный перезапуск",
      summary: "Смена температуры прерывает петлю стресса.",
      steps: [
        "Подойди к ближайшей раковине.",
        "Подержи запястья под прохладной водой 30 секунд.",
        "Заметь, как меняется ощущение на коже.",
        "Мягко коснись лица прохладными ладонями.",
      ],
    },
    {
      title: "Спринт по категориям",
      summary: "Дай мечущимся мыслям нейтральную задачу.",
      steps: [
        "Выбери категорию: города, животные или фильмы.",
        "Назови по одному слову на каждую букву от А до К.",
        "Застрял на букве? Пропусти и двигайся дальше.",
        "Заметь, как замедлилось дыхание.",
      ],
    },
  ],
};

interface ChatRule {
  patterns: RegExp[];
  replies: string[];
}

export const CHAT_RULES: Record<Lang, { rules: ChatRule[]; fallback: string[]; crisis: ChatRule }> = {
  en: {
    crisis: {
      patterns: [/(hurt|harm|kill)\w* (myself|me)/, /\bsuicid\w*/, /don'?t want to (live|be here)/],
      replies: [
        "I'm really glad you told me. This is bigger than what I can hold, and you deserve real support. Please reach out right now to someone you trust or a crisis line — you can find one at findahelpline.com. I'll stay here with you meanwhile.",
      ],
    },
    rules: [
      {
        patterns: [/anxi\w*/, /panic/, /overwhelm\w*/, /scared/],
        replies: [
          "That sounds really heavy. Let's slow things down together — try breathing in for 4 counts and out for 6. Want a grounding exercise?",
          "Anxiety lies about how urgent everything is. You're safe in this moment. Can you name 3 things you can see right now?",
        ],
      },
      {
        patterns: [/trigger\w*/, /saw a post/, /that content/],
        replies: [
          "It makes sense that it shook you — that content is designed to hook. You noticed it, and that's the skill that matters. Shall we do a quick reset?",
        ],
      },
      {
        patterns: [/\burge\b/, /want to (restrict|skip|purge)/],
        replies: [
          "Thank you for saying it out loud — urges lose power when they're named. They peak and pass, usually within 20-30 minutes. Can we ride this one out together?",
        ],
      },
      {
        patterns: [/\b(hi|hello|hey)\b/],
        replies: ["Hey, good to see you. How are you feeling right now, honestly?"],
      },
    ],
    fallback: [
      "I hear you. Tell me a bit more about what that feels like?",
      "That matters. What do you think your body is asking for right now — rest, warmth, company?",
      "I'm listening. Would a grounding exercise help while we talk?",
    ],
  },
  ru: {
    crisis: {
      patterns: [/(повреди|навреди|убь)\w* себ/u, /суицид\w*/u, /не хочу (жить|быть здесь)/u],
      replies: [
        "Спасибо, что сказал это мне. Это больше, чем я могу удержать, и ты заслуживаешь настоящей поддержки. Пожалуйста, обратись прямо сейчас к тому, кому доверяешь, или на линию помощи. Я останусь рядом.",
      ],
    },
    rules: [
      {
        patterns: [/тревог\w*/u, /паник\w*/u, /страшно/u, /накрыва/u],
        replies: [
          "Звучит очень тяжело. Давай замедлимся вместе — вдох на 4 счёта, выдох на 6. Хочешь упражнение на заземление?",
          "Тревога врёт о том, насколько всё срочно. В этот момент ты в безопасности. Назови 3 вещи, которые видишь прямо сейчас?",
        ],
      },
      {
        patterns: [/триггер\w*/u, /увидел\w* пост/u, /этот контент/u],
        replies: [
          "Понятно, что это задело — такой контент специально сделан, чтобы цеплять. Ты это заметил, и это главный навык. Сделаем быстрый перезапуск?",
        ],
      },
      {
        patterns: [/тян[ае]т/u, /хочу (ограничить|пропустить|очистить)/u, /срыв/u],
        replies: [
          "Спасибо, что назвал это вслух — порывы теряют силу, когда их называют. Они нарастают и проходят, обычно за 20–30 минут. Переждём этот вместе?",
        ],
      },
      {
        patterns: [/\b(привет|здравствуй|хай)\b/u],
        replies: ["Привет, рада тебя видеть. Как ты сейчас, честно?"],
      },
    ],
    fallback: [
      "Я слышу тебя. Расскажи чуть больше, на что это похоже?",
      "Это важно. Как думаешь, чего сейчас просит твоё тело — отдыха, тепла, компании?",
      "Я рядом. Помогло бы упражнение на заземление, пока мы говорим?",
    ],
  },
};

export interface FeedPost {
  author: string;
  text: string;
}

// mixed demo feed; risky ones are intentionally the obfuscated kind
export const FEED_POSTS: Record<Lang, FeedPost[]> = {
  en: [
    { author: "sunny.trails", text: "Finished a 5k run this morning, legs are jelly but the sunrise was worth it" },
    { author: "plantmomdaily", text: "My monstera FINALLY unfurled the new leaf. Three weeks of waiting for this" },
    { author: "wintergrl", text: "nothing t@stes as good as sk1nny feels. keep going girls" },
    { author: "bookedandbusy", text: "Started a book club with coworkers. First pick: something cozy for autumn" },
    { author: "mealprepmax", text: "Sunday prep done: chicken, rice, roasted veggies. Future me says thanks" },
    { author: "angel.wings.x", text: "cw 52 gw 44 ugw 40. who wants to be accountability partners" },
    { author: "citywalks", text: "Found a tiny courtyard cafe behind the library. Best hot chocolate in town" },
    { author: "liftlog", text: "Rest days are part of training too. Body rebuilds while you sleep" },
    { author: "st4rlight", text: "water f4st day 3, st0mach applauding not grumbling. join us" },
    { author: "doodle.dan", text: "Learned to crochet a scarf this month. Crooked but mine" },
  ],
  ru: [
    { author: "утро.в.городе", text: "Пробежала 5 км на рассвете, ноги гудят, но вид того стоил" },
    { author: "дом.и.листья", text: "Монстера наконец развернула новый лист. Три недели ждала этого момента" },
    { author: "тихий.час", text: "г0лодовка это стиль жизни а не этап. держимся девочки" },
    { author: "книжный.клуб", text: "Собрали книжный клуб с коллегами. Первая книга — что-то уютное на осень" },
    { author: "спорт.дневник", text: "Дни отдыха — тоже часть тренировок. Тело восстанавливается, пока ты спишь" },
    { author: "бабочка.ана", text: "ищу напарника по похудению, кто со мной? кп больше не хочу" },
    { author: "рецепты.дома", text: "Бабушкин суп по её рецепту — никогда не подводит" },
    { author: "лёгкость.х", text: "только 300 кк сегодня и мама ана похвалит" },
    { author: "прогулки.спб", text: "Нашла крошечное кафе во дворике за библиотекой. Лучший какао в городе" },
    { author: "вязание.вечер", text: "Научилась вязать шарф за месяц. Кривой, но мой" },
  ],
};
