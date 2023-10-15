package org.ek9lang.compiler.phase3;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
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
  protected TypedSymbolAccess(SymbolAndScopeManagement symbolAndScopeManagement,
                              ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
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
    var symbol = symbolAndScopeManagement.getRecordedSymbol(node);
    if (symbol != null && symbol.getType().isEmpty()) {
      emitTypeNotResolvedError(symbol);
    }
    //either way return the symbol
    return symbol;
  }

  private void emitTypeNotResolvedError(final ISymbol symbol) {
    var msg = "'" + symbol.getName() + "':";
    errorListener.semanticError(symbol.getSourceToken(), msg, ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
  }
}
