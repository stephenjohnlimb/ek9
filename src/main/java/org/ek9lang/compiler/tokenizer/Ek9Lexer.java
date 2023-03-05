package org.ek9lang.compiler.tokenizer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.ek9lang.antlr.EK9LexerRules;
import org.ek9lang.core.utils.Logger;

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Robert Einhorn (originally)
 * Copyright (c) 2021 Modified S J Limb for EK9
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Origin
 * Project      : Python3 Indent/Dedent handler for ANTLR4 grammars
 * https://github.com/antlr/grammars-v4/tree/master/python/tiny-python/tiny-grammar-without-actions
 * Developed by : Robert Einhorn, robert.einhorn.hu@gmail.com
 */

/**
 * Original good for python but too pythonesque for EK9 -
 * needed reworking to remove # comment and tabs for indenting.
 * EK9 only allows spaces for indents.
 */
public class Ek9Lexer extends EK9LexerRules implements LexerPlugin {
  public static final String TEXT_INSERTED_INDENT = "INDENT";
  private static final String NEW_LINE = "<NEWLINE>";
  private static final String DEDENT = "<DEDENT, ";
  private final int indentToken;
  private final int dedentToken;
  // A linked list where extra tokens are pushed on
  private final LinkedList<Token> pendingTokens = new LinkedList<>();
  private boolean printTokensAsSupplied = false;
  // The stack that keeps track of the indentation lengths
  private Deque<Integer> indentLengths = new ArrayDeque<>();
  //Whatever the first indent length all others must be the same.
  private int firstIndentLength = 0;
  // An int that stores the last pending token type (including the inserted
  // INDENT/DEDENT/NEWLINE token types also)
  private Token lastToken = null;
  private int lastPendingTokenType;
  // The amount of opened braces, brackets and parenthesis
  private int opened = 0;
  private String sourceName = "unknown";
  private TokenConsumptionListener tokenListener = null;

  /**
   * Create a lexer for an input with specific indent and dedent tokens.
   */
  public Ek9Lexer(CharStream input, int indentToken, int dedentToken) {
    super(input);
    this.indentToken = indentToken;
    this.dedentToken = dedentToken;
  }

  public Ek9Lexer(CharStream input, int indentToken, int dedentToken, boolean printTokens) {
    this(input, indentToken, dedentToken);
    this.printTokensAsSupplied = printTokens;
  }

  @Override
  public String getSourceName() {
    return sourceName;
  }

  public Ek9Lexer setSourceName(String sourceName) {
    this.sourceName = sourceName;
    return this;
  }

  public void setTokenListener(TokenConsumptionListener tokenListener) {
    this.tokenListener = tokenListener;
  }

  public int getIndentToken() {
    return indentToken;
  }

  public int getDedentToken() {
    return dedentToken;
  }

  @Override
  public String getSymbolicName(int tokenType) {
    return EK9LexerRules.VOCABULARY.getSymbolicName(tokenType);
  }

  @Override
  public Token nextToken() {
    if (_input.size() == 0) {
      return publishToken(new CommonToken(EOF, "<EOF>"));
    } else {
      checkNextToken();
      Token rtn = this.pendingTokens.pollFirst();

      if (printTokensAsSupplied && rtn != null) {
        Logger.log("Supplying token [" + rtn.getText() + "]");
        if (!_modeStack.isEmpty()) {
          int currentMode = _modeStack.peek();
          Logger.log("in mode [" + currentMode + "]");
        }

      }
      return publishToken(rtn);
    }
  }

  private Token publishToken(Token token) {
    if (tokenListener != null) {
      tokenListener.tokenConsumed(token);
    }
    return token;
  }

  public Ek9Lexer setPrintTokensAsSupplied(boolean printTokensAsSupplied) {
    this.printTokensAsSupplied = printTokensAsSupplied;
    return this;
  }

  @Override
  public int popMode() {
    return _modeStack.isEmpty()
        ? DEFAULT_MODE
        : super.popMode();
  }

  @Override
  protected boolean isRegexPossible() {
    if (this.lastToken == null) {
      // No token has been produced yet: at the start of the input,
      // no division is possible, so a regex literal _is_ possible.
      return true;
    }

    return switch (this.lastToken.getType()) {
      case Identifier, BooleanLiteral, IntegerLiteral, StringLiteral, FloatingPointLiteral,
          CharacterLiteral, TimeLiteral, DateLiteral, DateTimeLiteral, DurationLiteral,
          MillisecondLiteral, RBRACE, RBRACK, RPAREN ->
          // After any of the tokens above, no regex literal can follow.
          false;
      default ->
          // In all other cases, a regex literal _is_ possible.
          true;
    };
  }

  private void checkNextToken() {
    if (this.indentLengths != null) {
      final int startSize = this.pendingTokens.size();
      Token curToken;
      do {
        curToken = super.nextToken(); // get the next token from the input stream
        lastToken = curToken;
        checkStartOfInput(curToken);
        switch (curToken.getType()) {
          case LPAREN, LBRACK, LBRACE -> {
            //If we are in an interpolated string mode will be pushed we don't
            //allow multi-line braces in interpolated strings.
            if (_modeStack.isEmpty()) {
              this.opened++;
            }
            this.pendingTokens.addLast(curToken);
          }
          case RPAREN, RBRACK, RBRACE -> {
            //If we are in an interpolated string mode will be pushed we don't
            //allow multi-line braces in interpolated strings.
            if (_modeStack.isEmpty()) {
              this.opened--;
            }
            this.pendingTokens.addLast(curToken);
          }
          case NL -> handleNewLineToken(curToken);
          case EOF -> handleEofToken(curToken); // indentLengths stack will be set to null
          case TAB -> getErrorListenerDispatch().syntaxError(this,
              curToken, curToken.getLine(),
              curToken.getCharPositionInLine(),
              "Tabs not supported for indentation; use spaces", null);
          default -> this.pendingTokens.addLast(curToken);
        }
      } while (this.pendingTokens.size() == startSize);
      this.lastPendingTokenType = curToken.getType();
    }
  }

  private void checkStartOfInput(Token curToken) {
    if (indentLengths.isEmpty()) {
      indentLengths.push(0); // initialize the stack with default 0 indentation length
      if (_input.getText(new Interval(0, 0)).trim().length() == 0) {
        this.insertLeadingTokens(curToken.getType(), curToken.getStartIndex());
      }
    }
  }

  private void handleNewLineToken(Token curToken) {
    if (this.opened == 0) {
      int toCheck = _input.LA(1);
      if (toCheck == '\r' || toCheck == '\n' || toCheck == '\f' || toCheck == EOF) {
        return;
      }


      this.pendingTokens.addLast(curToken);
      int indentLength = getIndentationLength(curToken.getText());
      if (firstIndentLength == 0) {
        firstIndentLength = indentLength;
      }

      this.insertIndentDedentTokens(indentLength);
      if (indentLength % 2 != 0) {
        getErrorListenerDispatch().syntaxError(this,
            curToken, curToken.getLine(),
            curToken.getCharPositionInLine(),
            "Odd number of spaces for indentation", null);
      }
    }
  }

  private void handleEofToken(Token curToken) {
    this.insertTrailingTokens(this.lastPendingTokenType); // indentLengths stack will be null!
    this.pendingTokens.addLast(curToken); // insert the current EOF token
  }

  private void insertLeadingTokens(int type, int startIndex) {
    if (type != NL && type != EOF) {
      // (after a whitespace) The first token is visible, so We insert a NEWLINE
      // and an INDENT token before it to raise an 'unexpected indent' error
      // later by the parser
      this.insertToken(0, startIndex - 1, NEW_LINE + " ".repeat(startIndex), NL, 1, 0);
      this.insertToken(startIndex, startIndex - 1,
          "<" + TEXT_INSERTED_INDENT + ", " + this.getIndentationDescription(startIndex) + ">",
          indentToken, 1, startIndex);
      this.indentLengths.push(startIndex);
    }
  }

  private void insertIndentDedentTokens(int curIndentLength) {
    int prevIndentLength = this.indentLengths.peek();
    if (curIndentLength > prevIndentLength) {
      this.insertToken(
          "<" + TEXT_INSERTED_INDENT + ", " + this.getIndentationDescription(curIndentLength) + ">",
          indentToken);
      this.indentLengths.push(curIndentLength);
    } else {
      while (curIndentLength < prevIndentLength) { // More than 1 DEDENT token may be inserted
        this.indentLengths.pop();
        prevIndentLength = this.indentLengths.peek();
        if (curIndentLength <= prevIndentLength) {
          this.insertToken(DEDENT + this.getIndentationDescription(prevIndentLength) + ">",
              dedentToken);
          //Always add a spare new line make grammar more consistent after a dedent
        } else {
          this.insertToken(DEDENT + "length=" + curIndentLength + ">",
              dedentToken);
          //Always add a spare new line make grammar more consistent after a dedent
          //Think about raising an error here as indentation level is not right.
        }
        this.insertToken(NEW_LINE, NL);
      }
    }
  }

  private void insertTrailingTokens(int type) {
    if (type != NL && type != dedentToken) {
      // If the last pending token was not a NEWLINE and not a DEDENT then
      this.insertToken(NEW_LINE, NL);
      // insert an extra trailing NEWLINE token that serves as the end of the statement
    }

    while (this.indentLengths.size() > 1) {
      // Now insert as much trailing DEDENT tokens as needed
      this.insertToken(
          DEDENT + this.getIndentationDescription(this.indentLengths.pop()) + ">",
          dedentToken);
      //Always add a spare new line make grammar more consistent after a dedent
      this.insertToken(NEW_LINE, NL);
    }
    this.indentLengths = null; // there will be no more token read from the input stream
  }

  private String getIndentationDescription(int lengthOfIndent) {
    return "length=" + lengthOfIndent + ", level=" + this.indentLengths.size();
  }

  private void insertToken(String text, int type) {
    final int startIndex = _tokenStartCharIndex + getText().length();
    this.insertToken(startIndex, startIndex - 1, text, type, getLine(), getCharPositionInLine());
  }

  private void insertToken(int startIndex, int stopIndex, String text, int type, int line,
                           int charPositionInLine) {
    CommonToken token =
        new CommonToken(_tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, startIndex,
            stopIndex);
    token.setText(text);
    token.setLine(line);
    token.setCharPositionInLine(charPositionInLine);
    this.pendingTokens.addLast(token);
  }

  // Calculates the indentation of the provided spaces.
  private int getIndentationLength(String textOfMatchedNewLine) {
    int count = 0;
    for (char ch : textOfMatchedNewLine.toCharArray()) {
      if (ch == ' ') {
        count++;
      }
    }
    return count;
  }
}