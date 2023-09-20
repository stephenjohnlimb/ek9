package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.Token;

/**
 * Models a token that we need to create as a synthetic token.
 * The EK9 compiler sometimes needs to deal with tokens that were not
 * in the actual source code input.
 */
public class Ek9Token implements IToken {

  private final int type;

  private final String textName;
  private final int lineNumber;

  private final String sourceName;

  private final int charPositionInLine;

  private final int tokenIndex;

  /**
   * Create a new token with default name of 'Synthetic'.
   */
  public Ek9Token() {
    this("Synthetic", 0);
  }

  /**
   * Create a new token with a specific name.
   */
  public Ek9Token(String textName) {
    this(textName, 0);
  }

  /**
   * Create a new token with name and specific line number.
   */
  public Ek9Token(final String textName, final int lineNumber) {
    this(textName, lineNumber, "SyntheticTokenSource");
  }

  /**
   * Create a new token with name, specific line number and named source file.
   */
  public Ek9Token(final String textName, final int lineNumber, final String sourceName) {
    this(0, textName, lineNumber, sourceName, 0, 0);
  }

  /**
   * Create a new token with all necessary details.
   */
  public Ek9Token(final int type, final String textName, final int lineNumber, final String sourceName,
                  final int charPositionInLine, final int tokenIndex) {
    this.type = type;
    this.textName = textName;
    this.lineNumber = lineNumber;
    this.sourceName = sourceName;
    this.charPositionInLine = charPositionInLine;
    this.tokenIndex = tokenIndex;

  }

  /**
   * Pull out all relevant details of an ANTLR Token into a minimal EK9Token.
   */
  public Ek9Token(final Token token) {
    this(token.getType(), token.getText(), token.getLine(),
        token.getTokenSource().getSourceName(),
        token.getCharPositionInLine(), token.getTokenIndex());
  }

  @Override
  public int getType() {
    return type;
  }

  @Override
  public String getText() {
    return textName;
  }

  @Override
  public String getSourceName() {
    return sourceName;
  }

  @Override
  public int getLine() {
    return lineNumber;
  }

  @Override
  public int getCharPositionInLine() {
    return charPositionInLine;
  }

  @Override
  public int getTokenIndex() {
    return tokenIndex;
  }
}
