package org.ek9lang.compiler.symbol.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * get the appropriate return type from functions, methods and function delegates and constructors.
 */
public class ReturnTypeExtractor implements Function<ISymbol, Optional<ISymbol>> {

  private final boolean formOfDeclaration;

  public ReturnTypeExtractor() {
    this(false);
  }

  public ReturnTypeExtractor(final boolean formOfDeclaration) {
    this.formOfDeclaration = formOfDeclaration;
  }

  @Override
  public Optional<ISymbol> apply(ISymbol symbol) {
    if (symbol == null) {
      return Optional.empty();
    }

    if (symbol instanceof MethodSymbol method) {
      if (method.isReturningSymbolPresent()) {
        return method.getReturningSymbol().getType();
      }
    } else if (symbol instanceof FunctionSymbol function) {
      if (formOfDeclaration) {
        return function.getType();
      } else if (function.isReturningSymbolPresent()) {
        return function.getReturningSymbol().getType();
      }
    } else {
      if (symbol.getType().isPresent()
          && symbol.getType().get() instanceof FunctionSymbol delegateFunction
          && delegateFunction.isReturningSymbolPresent()) {
        return delegateFunction.getReturningSymbol().getType();
      }
    }
    return symbol.getType();
  }
}
