package org.ek9lang.compiler.tokenizer;

import java.util.function.Function;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;

/**
 * Creates the lexer and parser and plugs in the listners.
 */
public class ParserCreator implements Function<ParserSpec, EK9Parser> {
  private final Ek9LexerForInput ek9LexerForInput = new Ek9LexerForInput();

  @Override
  public EK9Parser apply(ParserSpec spec) {
    Ek9Lexer lexer = ek9LexerForInput.apply(spec.inputStream());
    //we will set the parsed module once parsed.
    lexer.setSourceName(spec.src().getFileName());
    lexer.setTokenListener(spec.listener());
    lexer.removeErrorListeners();
    lexer.addErrorListener(spec.errorListener());

    EK9Parser parser = new EK9Parser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(spec.errorListener());
    return parser;
  }
}
