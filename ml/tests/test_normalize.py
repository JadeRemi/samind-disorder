from samind_ml.features import DIM, featurize
from samind_ml.normalize import normalize


def test_leet_substitutions():
    assert normalize("st4rv1ng") == "starving"
    assert normalize("f4$t") == "fast"


def test_real_numbers_survive():
    assert normalize("cw 52 gw 44") == "cw 52 gw 44"
    assert normalize("only 300 kcal today") == "only 300 kcal today"


def test_separators_inside_words():
    assert normalize("s.t.a.r.v.i.n.g") == "starving"
    assert normalize("t-h-i-n") == "thin"


def test_homoglyphs_folded_to_latin():
    assert normalize("рurgе") == "purge"
    assert normalize("сalоriеs") == "calories"


def test_cyrillic_words_stay_cyrillic():
    assert normalize("привет") == "привет"
    assert normalize("пpивет") == "привет"
    assert normalize("г0лодовка") == "голодовка"
    assert normalize("4то ты ешь") == "что ты ешь"


def test_mixed_language_line():
    assert normalize("голодовка is a lifestyle") == "голодовка is a lifestyle"


def test_stretched_letters_collapsed():
    assert normalize("sooooo skinnyyyy") == "soo skinnyy"


def test_case_and_whitespace():
    assert normalize("  Skip   MEALS ") == "skip meals"


def test_safe_text_untouched():
    assert normalize("what a lovely sunset") == "what a lovely sunset"


def test_emoji_word_substitutes():
    assert normalize("only ⭐ will give you the body") == "only star will give you the body"
    assert normalize("💧 fast day 3") == "water fast day 3"
    assert normalize("(star)⭐️🪽(wing)") == "(star) star wing (wing)"


def test_unmapped_emoji_still_stripped():
    assert normalize("great workout 🎉 today") == "great workout today"


def test_featurizer_shape_and_determinism():
    a = featurize("skip meals")
    b = featurize("skip meals")
    assert a.shape == (DIM,)
    assert (a == b).all()
    assert a.sum() > 0
