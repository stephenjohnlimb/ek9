package org.ek9lang.compiler.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Create a new parameterised symbol from a generic type and a set of type arguments.
 */
public final class ParameterisedLocator extends ResolverOrDefiner
    implements Function<ParameterisedTypeData, Optional<ISymbol>> {

  /**
   * Create a new Function that can define or resolve a specific generic type with a single type parameter.
   */
  public ParameterisedLocator(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final SymbolFactory symbolFactory, final ErrorListener errorListener,
                              final boolean errorIfNotDefinedOrResolved) {
    super(symbolAndScopeManagement, symbolFactory, errorListener, errorIfNotDefinedOrResolved);
  }

  @Override
  public Optional<ISymbol> apply(final ParameterisedTypeData details) {
    return super.resolveOrDefine(details);
  }
}
