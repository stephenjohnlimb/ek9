package org.ek9lang.compiler.directives;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
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
      throw new IllegalArgumentException("Expecting one of: " + applicableSymbolCategories());
    }
  }

  private List<String> applicableSymbolCategories() {
    Predicate<ISymbol.SymbolCategory> acceptableValues = symbolCategory
        -> ISymbol.SymbolCategory.METHOD != symbolCategory && ISymbol.SymbolCategory.CONTROL != symbolCategory;
    return Arrays.stream(ISymbol.SymbolCategory.values()).filter(acceptableValues).map(Enum::toString).toList();
  }
}
