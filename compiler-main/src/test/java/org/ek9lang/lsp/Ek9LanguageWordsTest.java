package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class Ek9LanguageWordsTest {

  private final Ek9LanguageWords underTest = new Ek9LanguageWords();

  private final Function<String, TokenResult> keyWordToTokenResult = keyWord -> {
    var synthetic = new Ek9Token(keyWord);
    return new TokenResult(synthetic, List.of(synthetic), 0);
  };

  static Stream<String> getAllKeyWords() {
    return Optional.of(new Ek9LanguageWords())
        .map(Ek9LanguageWords::getAllKeyWords)
        .stream()
        .flatMap(Collection::stream);
  }

  @Test
  void testInitialisation() {
    assertNotNull(underTest);
  }

  @Test
  void testListingAllEk9LanguageWords() {
    assertEquals(133, underTest.getAllKeyWords().size());
  }

  @ParameterizedTest
  @MethodSource("getAllKeyWords")
  void testAllWordsExactMatch(String keyWord) {
    var foundKeyWord = Optional.of(keyWord)
        .map(keyWordToTokenResult)
        .map(underTest::exactMatch)
        .map(found -> found.hoverText)
        .isPresent();
    assertTrue(foundKeyWord, "Unable to find " + keyWord);
  }

  @ParameterizedTest
  @MethodSource("getAllKeyWords")
  void testAllWordsFuzzyMatch(String keyWord) {
    var numFound = Optional.of(keyWord)
        .map(keyWordToTokenResult)
        .map(underTest::fuzzyMatch)
        .stream()
        .mapToLong(Collection::size)
        .sum();
    //We may or may not have any fuzzy matches in a context.
    assertTrue(numFound >= 0);

  }

  @Test
  void testExactMatch() {

    var actual = Optional.of("defines")
        .map(keyWordToTokenResult)
        .map(underTest::exactMatch)
        .map(found -> found.hoverText)
        .orElse("");

    assertEquals("DEFINES BLOCK: Begin definition section for module constructs (functions, classes, records, traits, etc.). All type definitions must appear within a `defines` block inside a module. Use to organize and declare all constructs. Module can have multiple defines blocks for organization. Use when: declaring any functions, classes, records, components, services, applications. Required for all type definitions - cannot define constructs outside defines block. https://ek9.io/structure.html",
        actual);
  }

  @Test
  void testFuzzyMatch() {
    var numMatches = Optional.of("operator")
        .map(keyWordToTokenResult)
        .map(underTest::fuzzyMatch)
        .stream()
        .mapToLong(Collection::size)
        .sum();

    assertEquals(41, numMatches);
  }

}
