package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.support.CommonValues.COMPLEXITY;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.core.AssertValue;

/**
 * Looks for the complexity value in the complexityCounter and adds the value to the
 * appropriate symbol. But also if there is a valid stack above the current value adds
 * the complexity to that. This is so that classes have the total of all the method complexity.
 * In this way if there is a very large number of methods (even with moderate complexity) the
 * total complexity should trigger an error and cause the developer to break the class up.
 */
class ComplexityAcceptableOrError implements Consumer<ParseTree> {
  private final SymbolsAndScopes symbolsAndScopes;
  private final ComplexityCounter complexityCounter;

  ComplexityAcceptableOrError(final SymbolsAndScopes symbolsAndScopes,
                                     final ComplexityCounter complexityCounter) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.complexityCounter = complexityCounter;
  }

  @Override
  public void accept(final ParseTree node) {
    final var symbol = symbolsAndScopes.getRecordedSymbol(node);
    AssertValue.checkNotNull("Expecting symbol for node to be present", node);

    final var complexityValue = complexityCounter.pop();
    symbol.putSquirrelledData(COMPLEXITY, complexityValue.toString());

    if (!complexityCounter.isEmpty()) {
      complexityCounter.incrementComplexity(complexityValue);
    }
  }
}
