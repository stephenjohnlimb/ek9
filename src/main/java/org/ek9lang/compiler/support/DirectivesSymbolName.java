package org.ek9lang.compiler.support;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;

/**
 * Just extracts the symbol name or throws an illegal argument exception.
 */
public class DirectivesSymbolName implements Function<EK9Parser.DirectiveContext, String> {

  private final int position;

  public DirectivesSymbolName(final int position) {
    this.position = position;
  }

  @Override
  public String apply(EK9Parser.DirectiveContext ctx) {

    var symbolName = ctx.directivePart(position).getText();
    if (!symbolName.startsWith("\"")) {
      throw new IllegalArgumentException("Expecting quoted symbol name");
    }
    return symbolName.substring(1, symbolName.length() - 1);
  }
}
