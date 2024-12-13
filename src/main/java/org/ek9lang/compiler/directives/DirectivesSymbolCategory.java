package org.ek9lang.compiler.directives;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.SymbolCategory;

/**
 * Just extracts the symbol category or throws an illegal argument exception.
 */
public class DirectivesSymbolCategory implements Function<EK9Parser.DirectiveContext, SymbolCategory> {

  @Override
  public SymbolCategory apply(final EK9Parser.DirectiveContext ctx) {

    try {
      return SymbolCategory.valueOf(ctx.directivePart(1).getText());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Expecting one of: " + applicableSymbolCategories());
    }
  }

  private List<String> applicableSymbolCategories() {

    final Predicate<SymbolCategory> acceptableValues = symbolCategory
        -> SymbolCategory.METHOD != symbolCategory && SymbolCategory.CONTROL != symbolCategory;

    return Arrays.stream(SymbolCategory.values()).filter(acceptableValues).map(Enum::toString).toList();
  }
}
