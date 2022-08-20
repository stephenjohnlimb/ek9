package org.ek9lang.compiler.tokenizer;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;

/**
 * Typically used for locating the nearest token to some position in a source code file.
 */
public class TokenResult {
  private Token token;
  private List<Token> tokensInLine = new ArrayList<>();
  private int tokenPositionInLine = -1;

  /**
   * Create a new and invalid TokenResult.
   */
  public TokenResult() {
  }

  /**
   * Create a valid token result with the token its position and the other surrounding tokens.
   */
  public TokenResult(Token token, List<Token> tokensInLine, int positionInLine) {
    this.token = token;
    this.tokenPositionInLine = positionInLine;
    this.tokensInLine = tokensInLine;
  }

  public Token getToken() {
    return token;
  }

  public int getTokenPositionInLine() {
    return tokenPositionInLine;
  }

  public boolean isPresent() {
    return token != null;
  }

  /**
   * Checks the previous tokens in the line.
   *
   * @return true if all previous tokens are indents or this is first token.
   */
  public boolean previousTokensIndentsOrFirst() {
    boolean rtn = true;
    for (int i = 0; i < tokenPositionInLine; i++) {
      boolean isIndent = tokensInLine.get(i).getType() == EK9Parser.INDENT;
      rtn &= isIndent;
    }
    return rtn;
  }

  /**
   * Checks if the previous token was some sort of assignment.
   */
  public boolean previousTokenIsAssignment() {
    if (tokenPositionInLine == 0) {
      return false;
    }

    var previousToken = tokensInLine.get(tokenPositionInLine - 1).getType();

    return switch (previousToken) {
      case EK9Parser.ADD_ASSIGN,
          EK9Parser.SUB_ASSIGN,
          EK9Parser.MUL_ASSIGN,
          EK9Parser.DIV_ASSIGN,
          EK9Parser.ASSIGN,
          EK9Parser.ASSIGN2,
          EK9Parser.COLON,
          EK9Parser.LEFT_ARROW -> true;
      default -> false;
    };
  }

  /**
   * Check if previous token was a pipe token.
   */
  public boolean previousTokenIsPipe() {
    if (tokenPositionInLine > 0) {
      return tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.PIPE;
    }
    return false;
  }

  /**
   * Check if previous token was a 'defines' token.
   */
  public boolean previousTokenIsDefines() {
    if (tokenPositionInLine > 0) {
      return tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.DEFINES;
    }
    return false;
  }

  /**
   * Check if previous token was an overrides token.
   */
  public boolean previousTokenIsOverride() {
    if (tokenPositionInLine > 0) {
      return tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.OVERRIDE;
    }
    return false;
  }
}
