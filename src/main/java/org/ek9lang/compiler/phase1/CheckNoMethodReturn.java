package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.support.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Typically used for Constructors, but could be used in other contexts where a method
 * is not expected to return anything. Used in the very early stages of compilation.
 * Will probably need additional checks in the IR phase.
 * But these are basic check we can do as early in the compilation as possible.
 * Fail as early as possible.
 */
final class CheckNoMethodReturn extends RuleSupport
    implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {
  CheckNoMethodReturn(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final MethodSymbol methodSymbol, final EK9Parser.MethodDeclarationContext ctx) {
    if (ctx.operationDetails() != null
        && ctx.operationDetails().returningParam() != null
        && !methodSymbol.isMarkedAbstract()) {
      //Then there is a return and that's what this rule deals with - issuing error in this case.
      errorListener.semanticError(ctx.start, "'" + methodSymbol.getFriendlyName() + "':",
          ErrorListener.SemanticClassification.RETURN_VALUE_NOT_SUPPORTED);

    }

  }
}
