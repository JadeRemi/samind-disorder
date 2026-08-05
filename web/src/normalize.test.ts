import { describe, expect, it } from "vitest";

import { normalize } from "./normalize";

// same cases as ml/tests/test_normalize.py and TextNormalizerTest.kt
describe("normalize", () => {
  it("folds leet inside words", () => {
    expect(normalize("st4rv1ng")).toBe("starving");
    expect(normalize("f4$t")).toBe("fast");
  });

  it("keeps real numbers", () => {
    expect(normalize("cw 52 gw 44")).toBe("cw 52 gw 44");
    expect(normalize("only 300 kcal today")).toBe("only 300 kcal today");
  });

  it("removes separators inside words", () => {
    expect(normalize("s.t.a.r.v.i.n.g")).toBe("starving");
    expect(normalize("t-h-i-n")).toBe("thin");
  });

  it("folds homoglyphs toward the word's script", () => {
    expect(normalize("рurgе")).toBe("purge");
    expect(normalize("пpивет")).toBe("привет");
    expect(normalize("г0лодовка")).toBe("голодовка");
    expect(normalize("4то ты ешь")).toBe("что ты ешь");
  });

  it("collapses stretched letters", () => {
    expect(normalize("sooooo skinnyyyy")).toBe("soo skinnyy");
  });

  it("reads emoji word substitutes", () => {
    expect(normalize("only ⭐ will give you the body")).toBe("only star will give you the body");
    expect(normalize("(star)⭐️🪽(wing)")).toBe("(star) star wing (wing)");
  });

  it("leaves safe text alone", () => {
    expect(normalize("what a lovely sunset")).toBe("what a lovely sunset");
  });
});
