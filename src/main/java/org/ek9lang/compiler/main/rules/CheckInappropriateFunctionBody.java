package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.FunctionSymbol;

/**
 * Checks if the body of a function is appropriate.
 * i.e has it correctly been marked as abstract.
 */
public class CheckInappropriateFunctionBody extends RuleSupport implements
    BiConsumer<FunctionSymbol, EK9Parser.OperationDetailsContext> {

  private final CheckForBody checkForBody = new CheckForBody();
  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();
  public CheckInappropriateFunctionBody(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
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
