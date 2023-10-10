package org.ek9lang.compiler.support;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * If the aggregate is a generic parameterised type this function triggers a check for duplicated methods.
 * If it is resolved and it is a parameterised type - then we must now check for duplications.
 * It has to be done this late in the day, because with type inference, we only know the types now
 * Also the generic type could have methods that work with S, T, K, V - but those now map all to
 * a single real concrete type the methods could be duplicate.
 */
public final class CheckForDuplicateOperationsOnGeneric extends RuleSupport
    implements BiConsumer<IToken, IAggregateSymbol> {

  private final CheckForDuplicateOperations checkForDuplicateOperations;

  /**
   * Create a new operations checker for parameterised aggregates.
   */
  public CheckForDuplicateOperationsOnGeneric(final SymbolAndScopeManagement symbolAndScopeManagement,
                                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkForDuplicateOperations =
        new CheckForDuplicateOperations(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IToken errorLocationToken, final IAggregateSymbol aggregate) {
    if (aggregate != null && aggregate.isParameterisedType()) {
      checkForDuplicateOperations.accept(errorLocationToken, aggregate);
    }
  }
}
