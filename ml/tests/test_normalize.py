from samind_ml.features import DIM, featurize
from samind_ml.normalize import normalize


def test_leet_substitutions():
    assert normalize("st4rv1ng") == "starving"
    assert normalize("f4$t") == "fast"


def test_separators_inside_words():
    assert normalize("s.t.a.r.v.i.n.g") == "starving"
    assert normalize("t-h-i-n") == "thin"


def test_homoglyphs_folded_to_latin():
    assert normalize("рurgе") == "purge"
    assert normalize("сalоriеs") == "calories"


def test_stretched_letters_collapsed():
    assert normalize("sooooo skinnyyyy") == "soo skinnyy"


def test_case_and_whitespace():
    assert normalize("  Skip   MEALS ") == "skip meals"


def test_safe_text_untouched():
    assert normalize("what a lovely sunset") == "what a lovely sunset"


def test_featurizer_shape_and_determinism():
    a = featurize("skip meals")
    b = featurize("skip meals")
    assert a.shape == (DIM,)
    assert (a == b).all()
    assert a.sum() > 0
