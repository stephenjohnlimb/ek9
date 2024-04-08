package org.ek9lang.compiler.tokenizer;

import java.io.InputStream;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStreams;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.core.ExceptionConverter;
import org.ek9lang.core.Processor;

/**
 * Just wraps up the creation of the EK9Lexer from an inputStream.
 * Converts to a CharStream and wraps exceptions in an unchecked CompilerException.
 */
public class Ek9LexerForInput implements Function<InputStream, Ek9Lexer> {
  @Override
  public Ek9Lexer apply(final InputStream inputStream) {

    final Processor<Ek9Lexer> processor = () -> {
      //It is possible to set the input later with the lexer, the antlr tooling uses this.
      if (inputStream != null) {
        return new Ek9Lexer(CharStreams.fromStream(inputStream), EK9Parser.INDENT, EK9Parser.DEDENT);
      }
      return new Ek9Lexer(null, EK9Parser.INDENT, EK9Parser.DEDENT);
    };

    return new ExceptionConverter<Ek9Lexer>().apply(processor);
  }
}
