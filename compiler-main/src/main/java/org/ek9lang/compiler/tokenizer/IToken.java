package org.ek9lang.compiler.tokenizer;

import java.io.Serializable;

/**
 * The internal model of a parse token - typically created.
 */
public interface IToken extends Serializable {

  /**
   * The name of the source (if known).
   */
  String getSourceName();

  /**
   * The actual text of the token itself.
   */
  String getText();

  /**
   * The type of token.
   */
  int getType();

  /**
   * The line number on which the 1st character of this token was matched, line=1..n
   */
  int getLine();

  /**
   * The index of the first character of this token relative to the
   * beginning of the line at which it occurs, 0..n-1
   */
  int getCharPositionInLine();

  /**
   * An index from 0..n-1 of the token object in the input stream.
   * This must be valid in order to print token streams and
   * use TokenRewriteStream.
   * <br/>
   * Return -1 to indicate that this token was conjured up since
   * it doesn't have a valid index.
   */
  int getTokenIndex();
}
