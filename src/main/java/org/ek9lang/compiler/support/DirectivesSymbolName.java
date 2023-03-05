package org.ek9lang.compiler.support;

import java.util.Arrays;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Just extracts the symbol name or throws an illegal argument exception.
 */
public class DirectivesSymbolName implements Function<EK9Parser.DirectiveContext, String> {

  @Override
  public String apply(EK9Parser.DirectiveContext ctx) {

    var symbolName = ctx.directivePart(2).getText();
    if (!symbolName.startsWith("\"")) {
      throw new IllegalArgumentException("Expecting one of: " + Arrays.toString(ISymbol.SymbolCategory.values()));
    }
    return symbolName.substring(1, symbolName.length() - 1);
  }
}
