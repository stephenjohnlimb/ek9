package org.ek9lang.compiler.support;

import java.util.Arrays;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Just extracts the symbol category or throws an illegal argument exception.
 */
public class DirectivesSymbolCategory implements Function<EK9Parser.DirectiveContext, ISymbol.SymbolCategory> {

  @Override
  public ISymbol.SymbolCategory apply(EK9Parser.DirectiveContext ctx) {
    try {
      return ISymbol.SymbolCategory.valueOf(ctx.directivePart(1).getText());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Expecting one of: " + Arrays.toString(ISymbol.SymbolCategory.values()));
    }
  }
}
