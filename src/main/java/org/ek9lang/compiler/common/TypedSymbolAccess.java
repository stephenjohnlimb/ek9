package org.ek9lang.compiler.common;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.AggregateHasPureConstruction;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Designed to be used in phase3 only - FULL_RESOLUTION.
 * Because at this stage whenever a symbol is accessed via 'getRecordedSymbol'
 * in @link org.ek9lang.compiler.common.SymbolAndScopeManagement' it must be typed
 * or an error issued. Clearly the symbol could still be null at this stage (if there were
 * errors in expressions).
 * But if the symbol provided is not null then it must by now have a type.
 * So if not typed, then we need to emit an error. This means other code can just focus on
 * symbol != null &amp;&amp; symbol.getType.isPresent() then it can continue with its processing.
 */
public class TypedSymbolAccess extends RuleSupport {
  private final AggregateHasPureConstruction aggregateHasPureConstruction = new AggregateHasPureConstruction();

  /**
   * Constructor to provided typed access.
   */
  protected TypedSymbolAccess(final SymbolAndScopeManagement symbolAndScopeManagement,
                    final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  /**
   * Uses the SymbolAndScopeManagement to get the main processing scope a method or a function.
   * Then checks it that is marked as pure or not.
   *
   * @return true if pure, false if mutable.
   */
  protected boolean isProcessingScopePure() {

    final var scope = symbolAndScopeManagement.traverseBackUpStackToMethodOrFunction();
    if (scope.isEmpty()) {
      //Now this could be inside an aggregate where properties are declared.
      var possibleAggregate = symbolAndScopeManagement.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);

      //So get the enclosing scope, check and cast to IAggregate and check if it has pure construction
      //If so then we deem this scope to also be pure.
      return possibleAggregate
          .filter(IAggregateSymbol.class::isInstance)
          .map(IAggregateSymbol.class::cast)
          .filter(aggregateHasPureConstruction).isPresent();
    }

    return scope.map(IScope::isMarkedPure).orElse(false);
  }

  /**
   * Records a symbol against a node and also emits an error if its type has not been set.
   */
  public void recordATypedSymbol(final ISymbol symbol, final ParseTree node) {

    symbolAndScopeManagement.recordSymbol(symbol, node);
    if (symbol.getType().isEmpty()) {
      emitTypeNotResolvedError(symbol);
    }

  }

  /**
   * Gets any symbol associated with this node.
   * If the symbol is not null then it is marked as referenced and it's type is checked.
   * If there is no type then an error is emitted.
   */
  public ISymbol getRecordedAndTypedSymbol(final ParseTree node) {

    final var symbol = symbolAndScopeManagement.getRecordedSymbol(node);
    if (symbol != null && symbol.getType().isEmpty()) {
      emitTypeNotResolvedError(symbol);
    }

    //either way return the symbol
    return symbol;
  }

  private void emitTypeNotResolvedError(final ISymbol symbol) {

    final var msg = "'" + symbol.getName() + "':";
    errorListener.semanticError(symbol.getSourceToken(), msg, ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);

  }
}
