package org.ek9lang.compiler.tokenizer;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.ek9lang.core.Ari;
import org.ek9lang.core.Logger;

/**
 * But also to aid in listing out tokens so that when building grammars out of tokens
 * it is possible to see what is being produced.
 * Note the use of the LexerPlugin, this is so we can alter the underlying Lexer to get the right
 * stream of tokens.
 * As an aside - aids in assessing the readability of source code.
 */
public class TokenStreamAssessment {
  private String getSymbolicContent(final String symbolicName, final String literalContent) {

    return symbolicName == null ? literalContent : symbolicName;
  }

  /**
   * Assess the reading complexity of the tokens from the lexer.
   */
  public String assess(final LexerPlugin lexer, final boolean printTokens) {

    if (printTokens) {
      Logger.debug("\n[TOKENS]");
    }

    return doAssessment(lexer, printTokens);
  }

  private String doAssessment(final LexerPlugin lexer, final boolean printTokens) {

    final var tokens = new CommonTokenStream(lexer);
    tokens.fill();

    final var counters = new Counters();

    for (Token t : tokens.getTokens()) {
      String literalContent = tidyLiteralContent(t.getText());
      final var symbolicContent = getSymbolicContent(lexer.getSymbolicName(t.getType()), literalContent);
      if (printTokens) {
        Logger.debugf("  %-20s '%s'%n", symbolicContent, literalContent);
      }

      doAssessTokenContent(symbolicContent, literalContent, counters);
    }

    return assessReadability(printTokens, counters);
  }

  private void doAssessTokenContent(final String symbolicContent,
                                    final String literalContent,
                                    final Counters counters) {

    if (!assessFormOfNewLine(symbolicContent, counters)) {
      if (!Pattern.matches("\\p{Punct}", literalContent) && !literalContent.equals("<-")
          && !literalContent.equals("->") && !literalContent.equals(":=")) {
        if (!assessQuotedContent(literalContent, counters)) {
          counters.numLetters += literalContent.length();
          counters.numWords++;
        }
      } else {
        assessConsideredAWholeWord(literalContent, counters);
        counters.numLetters += literalContent.length();
      }
    }
  }

  private boolean assessQuotedContent(final String literalContent, final Counters counters) {

    final var consideredQuotedContent = literalContent.startsWith("\"");
    if (consideredQuotedContent) {
      final var words = literalContent.split("\\s+");
      counters.numWords += words.length;
      counters.numLetters += Arrays.stream(words).map(String::length).reduce(0, Integer::sum);
    }

    return consideredQuotedContent;
  }

  private String assessReadability(boolean printTokens, Counters counters) {

    //We have to approximate sentences in source code
    final var readability = new Ari().getScore(counters.numLetters, counters.numWords,
        counters.numIndents, counters.numNewLines);

    if (printTokens) {
      Logger.log("Readability [" + readability + "]");
    }

    return readability;
  }

  private void assessConsideredAWholeWord(final String literalContent, final Counters counters) {

    final var formOfWholeWord = literalContent.length() != 1 && !literalContent.equals("<-")
        && !literalContent.equals("->") && !literalContent.equals(":=");

    if (formOfWholeWord) {
      counters.numWords++;
    }

  }

  private boolean assessFormOfNewLine(final String symbolicContent, final Counters counters) {
    var formOfNewLine = symbolicContent.equals("NL") || symbolicContent.equals("newline")
        || symbolicContent.equals("indent") || symbolicContent.equals("dedent");

    if (formOfNewLine) {
      if (symbolicContent.equals("NL")) {
        counters.numNewLines++;
      }
      if (symbolicContent.equals("indent")) {
        counters.numIndents++;
      }
    }

    return formOfNewLine;
  }

  private String tidyLiteralContent(final String content) {

    return content.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
  }

  @SuppressWarnings("java:S1104")
  private static class Counters {
    public int numWords = 0;
    public int numLetters = 0;
    public int numIndents = 0;
    public int numNewLines = 0;
  }
}
