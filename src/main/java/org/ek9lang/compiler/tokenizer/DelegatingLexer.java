package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;

/**
 * A Lexer, but one that delegates to the actual lexer that in plugged in.
 */
public class DelegatingLexer implements LexerPlugin {
  private final LexerPlugin delegateTo;

  public DelegatingLexer(LexerPlugin delegateTo) {
    this.delegateTo = delegateTo;
  }

  public int getIndentToken() {
    return delegateTo.getIndentToken();
  }

  public int getDedentToken() {
    return delegateTo.getDedentToken();
  }

  @Override
  public String getSymbolicName(int tokenType) {
    if (tokenType == getIndentToken()) {
      return "indent";
    } else if (tokenType == getDedentToken()) {
      return "dedent";
    }
    return delegateTo.getSymbolicName(tokenType);
  }

  @Override
  public Token nextToken() {
    return delegateTo.nextToken();
  }

  @Override
  public int getLine() {
    return delegateTo.getLine();
  }

  @Override
  public int getCharPositionInLine() {
    return delegateTo.getCharPositionInLine();
  }

  @Override
  public CharStream getInputStream() {
    return delegateTo.getInputStream();
  }

  @Override
  public String getSourceName() {
    return delegateTo.getSourceName();
  }

  @Override
  public TokenFactory<?> getTokenFactory() {
    return delegateTo.getTokenFactory();
  }

  @Override
  public void setTokenFactory(TokenFactory<?> factory) {
    delegateTo.setTokenFactory(factory);
  }

  @Override
  public void removeErrorListeners() {
    delegateTo.removeErrorListeners();
  }

  @Override
  public void addErrorListener(ANTLRErrorListener listener) {
    delegateTo.addErrorListener(listener);
  }
}
