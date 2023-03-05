package org.ek9lang.compiler.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Lexer;

/**
 * Just processes a bit of some EK9 source for a simple or generic type def, to return the context.
 */
public class PartialEk9StringToTypeDef implements Function<String, EK9Parser.TypeDefContext> {
  @Override
  public EK9Parser.TypeDefContext apply(String typeDefinition) {
    final var sourceName = "in-memory.ek9";
    try (var inputStream = new ByteArrayInputStream(typeDefinition.getBytes(StandardCharsets.UTF_8))) {
      Ek9Lexer lex =
          new Ek9Lexer(CharStreams.fromStream(inputStream), EK9Parser.INDENT, EK9Parser.DEDENT, false);
      lex.setSourceName(sourceName);
      lex.removeErrorListeners();

      EK9Parser parser = new EK9Parser(new CommonTokenStream(lex));
      parser.removeErrorListeners();

      ErrorListener errorListener = new ErrorListener(sourceName);
      lex.addErrorListener(errorListener);
      parser.addErrorListener(errorListener);

      return parser.typeDef();
    } catch (IOException e) {
      return null;
    }
  }
}
