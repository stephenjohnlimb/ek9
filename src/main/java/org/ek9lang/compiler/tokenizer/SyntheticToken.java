package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;

/**
 * Models a token that we need to create as a synthetic token.
 * The EK9 compiler sometimes needs to deal with tokens that were not
 * in the actual source code input.
 */
public class SyntheticToken implements Token {

  private final String textName;
  private final int lineNumber;

  private final TokenSource tokenSource;

  /**
   * Create a new token with default name of 'Synthetic'.
   */
  public SyntheticToken() {
    this("Synthetic", 0);
  }

  /**
   * Create a new token with a specific name.
   */
  public SyntheticToken(String textName) {
    this(textName, 0);
  }

  public SyntheticToken(final String textName, final int lineNumber) {
    tokenSource = new TokenSource() {

      @Override
      public Token nextToken() {
        return null;
      }

      @Override
      public int getLine() {
        return lineNumber;
      }

      @Override
      public int getCharPositionInLine() {
        return 0;
      }

      @Override
      public CharStream getInputStream() {
        return null;
      }

      @Override
      public String getSourceName() {
        return "SyntheticTokenSource";
      }

      @Override
      public TokenFactory<?> getTokenFactory() {
        return null;
      }

      @Override
      public void setTokenFactory(TokenFactory<?> factory) {
        //No op in a synthetic token.
      }
    };

    this.textName = textName;
    this.lineNumber = lineNumber;
  }

  @Override
  public String getText() {
    return textName;
  }

  @Override
  public int getType() {
    return 0;
  }

  @Override
  public int getLine() {
    return lineNumber;
  }

  @Override
  public int getCharPositionInLine() {
    return 0;
  }

  @Override
  public int getChannel() {
    return 0;
  }

  @Override
  public int getTokenIndex() {
    return 0;
  }

  @Override
  public int getStartIndex() {
    return 0;
  }

  @Override
  public int getStopIndex() {
    return 0;
  }

  @Override
  public TokenSource getTokenSource() {
    return tokenSource;
  }

  @Override
  public CharStream getInputStream() {
    return null;
  }
}
