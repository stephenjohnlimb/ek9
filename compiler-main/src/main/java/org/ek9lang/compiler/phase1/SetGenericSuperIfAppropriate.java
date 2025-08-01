package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;

final class SetGenericSuperIfAppropriate implements Consumer<ISymbol> {

  @Override
  public void accept(final ISymbol symbol) {

    if (symbol instanceof IAggregateSymbol aggregate
        && aggregate.getCategory().equals(SymbolCategory.TEMPLATE_TYPE)) {

      final var any = aggregate.resolve(new SymbolSearch("org.ek9.lang::Any").setSearchType(SymbolCategory.ANY));
      any.ifPresent(anyType -> {
        if (anyType instanceof IAggregateSymbol asAggregate) {
          aggregate.setSuperAggregate(asAggregate);
        }
      });
    }
  }
}
