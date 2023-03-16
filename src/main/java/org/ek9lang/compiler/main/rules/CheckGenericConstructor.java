package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Checks that a constructor method on a generic aggregate has the same number of
 * parameters as the types being used in the generic type. This enables type inference.
 */
public class CheckGenericConstructor implements BiConsumer<Token, MethodSymbol> {
  private final ErrorListener errorListener;

  public CheckGenericConstructor(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(Token token, MethodSymbol methodSymbol) {
    var numParametersInConstructor = methodSymbol.getSymbolsForThisScope().size();

    //We must check the parameters in the constructor tie up with the parameter numbers for creation
    //We tie these two together unlike java so that we can implement inference - so a bit of a cost here.
    //We allow a default constructor with no parameters, but that means no type inference it will have
    //to be defined correctly on the lhs.
    if (numParametersInConstructor > 0
        && methodSymbol.getParentScope() instanceof IAggregateSymbol aggregateSymbol
        && aggregateSymbol.getCategory().equals(ISymbol.SymbolCategory.TEMPLATE_TYPE)) {

      var numParameterizingTypes = aggregateSymbol.getParameterTypes().size();

      if (numParametersInConstructor != numParameterizingTypes) {
        errorListener.semanticError(token, "",
            ErrorListener.SemanticClassification.GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE);
      }
    }
  }
}
