package org.ek9lang.compiler.common;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.Source;
import org.ek9lang.compiler.tokenizer.ParserCreator;
import org.ek9lang.compiler.tokenizer.ParserSpec;
import org.ek9lang.core.ExceptionConverter;
import org.ek9lang.core.Processor;

/**
 * Just processes a bit of some EK9 source for a simple or generic type def, to return the context.
 */
public class PartialEk9StringToTypeDef implements Function<String, EK9Parser.TypeDefContext> {

  private final ParserCreator parserCreator = new ParserCreator();

  @Override
  public EK9Parser.TypeDefContext apply(String typeDefinition) {
    Source src = () -> "in-memory.ek9";
    Processor<EK9Parser.TypeDefContext> processor = () -> {
      try (var inputStream = new ByteArrayInputStream(typeDefinition.getBytes(StandardCharsets.UTF_8))) {
        ErrorListener errorListener = new ErrorListener(src.getFileName());
        var spec = new ParserSpec(src, inputStream, errorListener, null);
        var parser = parserCreator.apply(spec);
        return parser.typeDef();
      }
    };

    return new ExceptionConverter<EK9Parser.TypeDefContext>().apply(processor);
  }
}
