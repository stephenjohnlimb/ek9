package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.EXCESSIVE_COMPLEXITY;
import static org.ek9lang.compiler.support.CommonValues.COMPLEXITY;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.core.AssertValue;

/**
 * Looks for the complexity value in the complexityCounter and adds the value to the
 * appropriate symbol. But also if there is a valid stack above the current value adds
 * the complexity to that. This is so that classes have the total of all the method complexity.
 * In this way if there is a very large number of methods (even with moderate complexity) the
 * total complexity should trigger an error and cause the developer to break the class up.
 */
class AcceptableConstructComplexityOrError implements Consumer<ParseTree> {
  private final SymbolsAndScopes symbolsAndScopes;
  private final ErrorListener errorListener;
  private final ComplexityCounter complexityCounter;

  AcceptableConstructComplexityOrError(final SymbolsAndScopes symbolsAndScopes,
                                       final ErrorListener errorListener,
                                       final ComplexityCounter complexityCounter) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.errorListener = errorListener;
    this.complexityCounter = complexityCounter;
  }

  @SuppressWarnings("checkstyle:Indentation")
  @Override
  public void accept(final ParseTree node) {
    final var symbol = symbolsAndScopes.getRecordedSymbol(node);
    AssertValue.checkNotNull("Expecting symbol for node to be present", node);

    final var complexityValue = complexityCounter.pop();

    symbol.putSquirrelledData(COMPLEXITY, complexityValue.toString());
    if (SymbolCategory.TYPE.equals(symbol.getCategory())
        || SymbolCategory.TEMPLATE_TYPE.equals(symbol.getCategory())) {
      errorIfTooComplex(symbol, complexityValue, 500);
    } else {
      errorIfTooComplex(symbol, complexityValue, 50);
    }

    if (!complexityCounter.isEmpty()) {
      complexityCounter.incrementComplexity(complexityValue);
    }
  }

  private void errorIfTooComplex(final ISymbol symbol, AtomicInteger complexityValue, int maxComplexityValue) {
    final var theCalculatedComplexityValue = complexityValue.get();
    if (theCalculatedComplexityValue > maxComplexityValue) {
      final var msg = String.format("Calculated complexity of %d is too high, max allowed on a %s is %d:",
          theCalculatedComplexityValue, symbol.getCategory(), maxComplexityValue);
      errorListener.semanticError(symbol.getSourceToken(), msg, EXCESSIVE_COMPLEXITY);
    }
  }
}
