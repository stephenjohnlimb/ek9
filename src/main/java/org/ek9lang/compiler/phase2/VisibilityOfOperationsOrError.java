package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Does a simple check (excluding any inheritance) for visibility rules on methods/operators on
 * aggregates.
 * Also limits ability to add methods other than constructors and operators to records.
 * Classes are not missing, here they just support public, protected and private.
 * Whereas the other constructs support different variations.
 * Also note that there is no need to check service web methods or operators as there is now way
 * in the grammar to express an access modifier.
 * <br/>
 * Note that class methods can be private, protected or public. But in the case of public we catch that
 * superfluous "public" if the EK9 developer uses it in the symbol definition phase.
 */
final class VisibilityOfOperationsOrError extends RuleSupport implements Consumer<IAggregateSymbol> {

  private final Map<ISymbol.SymbolGenus, Consumer<MethodSymbol>> genusChecks = Map.of(
      ISymbol.SymbolGenus.COMPONENT, this::componentMethodVisibilityOrError,
      ISymbol.SymbolGenus.RECORD, this::recordMethodVisibilityOrError,
      ISymbol.SymbolGenus.CLASS_TRAIT, this::traitMethodVisibilityOrError,
      ISymbol.SymbolGenus.SERVICE, this::serviceMethodVisibilityOrError
  );

  /**
   * Create a new operations checker an aggregates.
   */
  VisibilityOfOperationsOrError(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final IAggregateSymbol aggregate) {

    checkMethodVisibility(aggregate, aggregate.getAllNonAbstractMethods());
    checkMethodVisibility(aggregate, aggregate.getAllAbstractMethods());

  }

  private void checkMethodVisibility(final IAggregateSymbol aggregate,
                                     final List<MethodSymbol> methods) {

    final var genusCheck = genusChecks.get(aggregate.getGenus());
    if (genusCheck != null) {
      methods.forEach(genusCheck);
    }

  }

  private void componentMethodVisibilityOrError(final MethodSymbol method) {

    if (method.isProtected()) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.METHOD_MODIFIER_PROTECTED_IN_COMPONENT);
    }

  }

  private void recordMethodVisibilityOrError(final MethodSymbol method) {

    if (!method.isConstructor() && !method.isOperator()) {
      errorListener.semanticError(method.getSourceToken(), "'" + method.getFriendlyName() + "'",
          ErrorListener.SemanticClassification.RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS);
    }

  }

  private void traitMethodVisibilityOrError(final MethodSymbol method) {

    if (!method.isPublic()) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT);
    }

  }

  private void serviceMethodVisibilityOrError(final MethodSymbol method) {

    if (method.isProtected()) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.METHOD_MODIFIER_PROTECTED_IN_SERVICE);
    }

  }
}
