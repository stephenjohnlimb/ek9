package org.ek9lang.compiler.directives;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;

/**
 * Just extracts the symbol name or throws an illegal argument exception.
 */
public class DirectivesSymbolName implements Function<EK9Parser.DirectiveContext, String> {

  private final int position;

  /**
   * New extractor of symbol name for a position in the source.
   */
  public DirectivesSymbolName(final int position) {

    this.position = position;

  }

  @Override
  public String apply(final EK9Parser.DirectiveContext ctx) {

    final var symbolName = ctx.directivePart(position).getText();
    if (!symbolName.startsWith("\"")) {
      throw new IllegalArgumentException("Expecting quoted symbol name");
    }

    final var result = symbolName.substring(1, symbolName.length() - 1);
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Symbol name is empty");
    }

    return result;
  }

}
