package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.CheckForBody;
import org.ek9lang.compiler.support.ExternallyImplemented;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.support.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Checks if the body of a function is appropriate.
 * i.e has it correctly been marked as abstract.
 */
final class CheckInappropriateFunctionBody extends RuleSupport implements
    BiConsumer<FunctionSymbol, EK9Parser.OperationDetailsContext> {

  private final CheckForBody checkForBody = new CheckForBody();
  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();

  CheckInappropriateFunctionBody(final SymbolAndScopeManagement symbolAndScopeManagement,
                                 final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final FunctionSymbol functionSymbol,
                     final EK9Parser.OperationDetailsContext ctx) {
    final var hasBody = checkForBody.test(ctx);
    var isAbstract = functionSymbol.isMarkedAbstract();
    var isExternallyImplemented = externallyImplemented.test(functionSymbol);

    if (hasBody && isAbstract) {
      errorListener.semanticError(functionSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.ABSTRACT_BUT_BODY_PROVIDED);
    } else if (!hasBody && !(isAbstract || isExternallyImplemented)) {
      errorListener.semanticError(functionSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.NOT_ABSTRACT_AND_NO_BODY_PROVIDED);
    }
  }
}
