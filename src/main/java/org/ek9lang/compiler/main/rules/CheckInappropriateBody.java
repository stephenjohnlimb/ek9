package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.DEFAULTED;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Checks if the body of a method/operator is appropriate.
 * i.e. has it been marked as abstract or default - neither of which need a body.
 */
public class CheckInappropriateBody extends RuleSupport implements
    BiConsumer<MethodSymbol, EK9Parser.OperationDetailsContext> {

  private final CheckForBody checkForBody = new CheckForBody();

  protected CheckInappropriateBody(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final MethodSymbol methodSymbol,
                     final EK9Parser.OperationDetailsContext ctx) {
    final var hasBody = checkForBody.test(ctx);
    var defaulted = "TRUE".equals(methodSymbol.getSquirrelledData(DEFAULTED));
    var isAbstract = methodSymbol.isMarkedAbstract();
    if (hasBody && (defaulted || isAbstract)) {
      errorListener.semanticError(methodSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.ABSTRACT_BUT_BODY_PROVIDED);
    }
  }
}
