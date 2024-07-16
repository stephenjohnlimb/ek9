package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks that a constructor method on a generic aggregate has the same number of
 * parameters as the types being used in the generic type. This enables type inference.
 */
final class GenericConstructorOrError implements BiConsumer<IToken, MethodSymbol> {
  private final ErrorListener errorListener;

  GenericConstructorOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken token, final MethodSymbol methodSymbol) {

    final var numParametersInConstructor = methodSymbol.getSymbolsForThisScope().size();

    //We must check the parameters in the constructor tie up with the parameter numbers for creation
    //We tie these two together unlike java so that we can implement inference - so a bit of a cost here.
    //We allow a default constructor with no parameters, but that means no type inference it will have
    //to be defined correctly on the lhs.
    if (numParametersInConstructor > 0
        && methodSymbol.getParentScope() instanceof IAggregateSymbol aggregateSymbol
        && aggregateSymbol.getCategory().equals(ISymbol.SymbolCategory.TEMPLATE_TYPE)) {

      final var numParameterizingTypes = aggregateSymbol.getTypeParameterOrArguments().size();

      if (numParametersInConstructor != numParameterizingTypes) {
        errorListener.semanticError(token, "",
            ErrorListener.SemanticClassification.GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE);
      }

    }
  }
}
