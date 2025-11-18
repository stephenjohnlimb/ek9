package org.ek9lang.compiler.common;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks if the body of a method/operator is appropriate.
 * i.e. has it been marked as abstract or default - neither of which should have a body.
 * Emits compiler error if inappropriate.
 * Note: DEFAULT_ONLY_FOR_CONSTRUCTORS checks for 'default' on non-constructors (runs first).
 * This checks for 'default' constructors with bodies (which should use default implementation).
 */
public class AppropriateBodyOrError extends RuleSupport implements
    BiConsumer<MethodSymbol, EK9Parser.OperationDetailsContext> {

  private final ProcessingBodyPresent processingBodyPresent = new ProcessingBodyPresent();
  private final Defaulted defaulted = new Defaulted();

  /**
   * Create new checker.
   */
  public AppropriateBodyOrError(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final MethodSymbol methodSymbol,
                     final EK9Parser.OperationDetailsContext ctx) {

    // Check for abstract OR default constructors with body provided
    // Note: default on non-constructors is caught earlier by DEFAULT_ONLY_FOR_CONSTRUCTORS
    if ((methodSymbol.isMarkedAbstract() || (defaulted.test(methodSymbol) && methodSymbol.isConstructor()))
        && processingBodyPresent.test(ctx)) {
      errorListener.semanticError(methodSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.ABSTRACT_BUT_BODY_PROVIDED);
    }

  }
}
