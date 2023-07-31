package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Does a simple check (excluding any inheritance) for visibility rules on methods/operators on
 * aggregates, such as classes, components, services, traits and dynamic classes.
 * Also limits ability to add methods other than constructors and operators to records.
 * Classes are not missing, here they just support public, protected and private.
 * Whereas the other constructs support different variations.
 * Also note that there is no need to check service web methods or operators as there is now way
 * in the grammar to express an access modifier.
 */
final class CheckVisibilityOfOperations extends RuleSupport implements Consumer<IAggregateSymbol> {

  private final Map<ISymbol.SymbolGenus, Consumer<MethodSymbol>> genusChecks = Map.of(
      ISymbol.SymbolGenus.COMPONENT, this::checkComponentMethodVisibility,
      ISymbol.SymbolGenus.RECORD, this::checkRecordMethodVisibility,
      ISymbol.SymbolGenus.CLASS_TRAIT, this::checkTraitMethodVisibility,
      ISymbol.SymbolGenus.SERVICE, this::checkServiceMethodVisibility
  );

  /**
   * Create a new operations checker an aggregates.
   */
  CheckVisibilityOfOperations(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IAggregateSymbol aggregate) {
    checkMethodVisibility(aggregate, aggregate.getAllNonAbstractMethods());
    checkMethodVisibility(aggregate, aggregate.getAllAbstractMethods());
  }

  private void checkMethodVisibility(final IAggregateSymbol aggregate,
                                     final List<MethodSymbol> methods) {

    var genusCheck = genusChecks.get(aggregate.getGenus());
    if (genusCheck != null) {
      methods.forEach(genusCheck);
    }
  }

  private void checkComponentMethodVisibility(final MethodSymbol method) {
    if (method.isProtected()) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.METHOD_MODIFIER_PROTECTED_IN_COMPONENT);
    }
  }

  private void checkRecordMethodVisibility(final MethodSymbol method) {
    if (!method.isConstructor() && !method.isOperator()) {
      errorListener.semanticError(method.getSourceToken(), "'" + method.getFriendlyName() + "'",
          ErrorListener.SemanticClassification.RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS);
    }
  }

  private void checkTraitMethodVisibility(final MethodSymbol method) {
    if (!method.isPublic()) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT);
    }
  }

  private void checkServiceMethodVisibility(final MethodSymbol method) {
    if (method.isProtected()) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.METHOD_MODIFIER_PROTECTED_IN_SERVICE);
    }
  }
}
