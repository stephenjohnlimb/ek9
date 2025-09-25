package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_GENUS_CONSTRUCTOR;
import static org.ek9lang.compiler.symbols.SymbolGenus.GENERAL_APPLICATION;
import static org.ek9lang.compiler.symbols.SymbolGenus.PROGRAM;
import static org.ek9lang.compiler.symbols.SymbolGenus.SERVICE_APPLICATION;

import java.util.Set;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Checks that a specific constructor can be called on a particular type of Aggregate.
 */
final class ConstructorCallOrError extends TypedSymbolAccess
    implements Consumer<AggregateMethodData> {

  private final Set<SymbolGenus> disallowedConstructorGenus = Set.of(PROGRAM,
      GENERAL_APPLICATION,
      SERVICE_APPLICATION);


  ConstructorCallOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final AggregateMethodData data) {
    if (data.methodSymbol().isConstructor() && disallowedConstructorGenus.contains(data.aggregate().getGenus())) {
      errorListener.semanticError(data.location(), "", INCOMPATIBLE_GENUS_CONSTRUCTOR);
    }
  }
}