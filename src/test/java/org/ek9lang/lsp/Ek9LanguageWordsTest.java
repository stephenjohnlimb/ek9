package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.junit.jupiter.api.Test;

class Ek9LanguageWordsTest {

  @Test
  void testInitialisation() {
    var underTest = new Ek9LanguageWords();
    assertNotNull(underTest);
  }

  @Test
  void testExactMatch() {
    var underTest = new Ek9LanguageWords();
    TokenResult search = createSyntheticTokenResultFor("defines");
    var foundKeyWord = underTest.exactMatch(search);
    assertNotNull(foundKeyWord);
    assertEquals("Define a module or construct block, https://www.ek9.io/structure.html",
        foundKeyWord.hoverText);
  }

  @Test
  void testFuzzyMatch() {
    var underTest = new Ek9LanguageWords();
    TokenResult search = createSyntheticTokenResultFor("operator");
    var foundKeyWords = underTest.fuzzyMatch(search);
    assertNotNull(foundKeyWords);
    assertEquals(41, foundKeyWords.size());
  }

  private TokenResult createSyntheticTokenResultFor(String lookupWord)
  {
    Token synthetic = new SyntheticToken(lookupWord);
    return new TokenResult(synthetic, List.of(synthetic), 0);
  }
}
