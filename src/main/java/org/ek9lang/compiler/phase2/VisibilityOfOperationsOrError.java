package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Does a simple check (excluding any inheritance) for visibility rules on methods/operators on
 * aggregates.
 * <p>
 * Also limits ability to add methods other than constructors and operators to records.
 * Classes are not missing, here they just support public, protected and private.
 * Whereas the other constructs support different variations.
 * </p>
 * <p>
 * Note that there is no need to check service web methods or operators as this is now
 * in the grammar to express an access modifier.
 * </p>
 * <p>
 * Note that class methods can be private, protected or public. But in the case of public we catch that
 * superfluous "public" if the EK9 developer uses it in the symbol definition phase.
 * </p>
 */
final class VisibilityOfOperationsOrError extends RuleSupport implements Consumer<IAggregateSymbol> {

  private final Map<SymbolGenus, Consumer<MethodSymbol>> genusChecks = Map.of(
      SymbolGenus.COMPONENT, this::componentMethodVisibilityOrError,
      SymbolGenus.RECORD, this::recordMethodVisibilityOrError,
      SymbolGenus.CLASS_TRAIT, this::traitMethodVisibilityOrError,
      SymbolGenus.CLASS, this::classMethodVisibilityOrError,
      SymbolGenus.SERVICE, this::serviceMethodVisibilityOrError
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

  private void classMethodVisibilityOrError(final MethodSymbol method) {

    //So can be marked as protected in a closed class but only if overriding method in super.
    if (method.isProtected() && !method.isOverride()
        && method.getParentScope() instanceof IAggregateSymbol aggregate
        && !aggregate.isOpenForExtension()) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS);
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
