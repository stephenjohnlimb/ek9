package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.EXPLICIT_CONSTRUCTOR_REQUIRED;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Used with aggregates to check that is they have any properties that are not initialised at declaration
 * there are no default or implicit constructors being used.
 * This is to make the use of uninitialised properties 'more painful' and to draw attention to the fact that
 * there are sometimes (but not always) better ways.
 * This should cause the EK9 developer to consider alternatives and if they still believe this to be the best
 * solution to consider initialisation inside a Constructor.
 * This is not mandatory, but is very likely to lead to defects, so this is an attempt at making this issue more
 * obvious.
 */
class SuitablePropertyInitialisationOrError extends TypedSymbolAccess implements Consumer<ParseTree> {

  SuitablePropertyInitialisationOrError(final SymbolsAndScopes symbolsAndScopes,
                                        final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final ParseTree node) {
    if (getRecordedAndTypedSymbol(node) instanceof IAggregateSymbol asAggregate) {
      checkAggregate(asAggregate);
    }
  }

  private void checkAggregate(final IAggregateSymbol asAggregate) {

    final var hasOneOrMoreUninitialisedAtDeclaration = asAggregate.getProperties()
        .stream()
        .anyMatch(prop -> "TRUE".equals(prop.getSquirrelledData(CommonValues.UNINITIALISED_AT_DECLARATION)));

    final var hasOneOrMoreSyntheticConstructors = asAggregate.getConstructors()
        .stream()
        .anyMatch(MethodSymbol::isSynthetic);

    if (hasOneOrMoreSyntheticConstructors && hasOneOrMoreUninitialisedAtDeclaration) {
      errorListener.semanticError(asAggregate.getSourceToken(), "", EXPLICIT_CONSTRUCTOR_REQUIRED);
    }
  }
}
