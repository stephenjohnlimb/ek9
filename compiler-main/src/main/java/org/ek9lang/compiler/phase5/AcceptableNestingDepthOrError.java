package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.EXCESSIVE_NESTING;
import static org.ek9lang.compiler.support.CommonValues.NESTING_DEPTH;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.core.AssertValue;

/**
 * Checks nesting depth at function/method/operator exit and emits error if too deep.
 * Deep nesting makes code hard to read and maintain - this enforces a reasonable limit.
 * The limit applies to control structures: if, while, for, switch, try/catch/finally.
 */
class AcceptableNestingDepthOrError implements Consumer<ParseTree> {

  /**
   * Maximum allowed nesting depth for control structures.
   * This is a reasonable limit that forces developers to extract functions
   * rather than creating deeply nested code.
   */
  private static final int MAX_NESTING_DEPTH = 10;

  private final SymbolsAndScopes symbolsAndScopes;
  private final ErrorListener errorListener;
  private final NestingDepthCounter nestingCounter;

  AcceptableNestingDepthOrError(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener,
                                final NestingDepthCounter nestingCounter) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.errorListener = errorListener;
    this.nestingCounter = nestingCounter;
  }

  @Override
  public void accept(final ParseTree node) {
    final var symbol = symbolsAndScopes.getRecordedSymbol(node);
    AssertValue.checkNotNull("Expecting symbol for node to be present", symbol);

    final var maxDepth = nestingCounter.popScope();

    // Store for potential tooling/IDE use
    symbol.putSquirrelledData(NESTING_DEPTH, String.valueOf(maxDepth));

    if (maxDepth > MAX_NESTING_DEPTH) {
      final var msg = String.format(
          "Nesting depth of %d exceeds maximum of %d:",
          maxDepth, MAX_NESTING_DEPTH);
      errorListener.semanticError(symbol.getSourceToken(), msg, EXCESSIVE_NESTING);
    }
  }
}
