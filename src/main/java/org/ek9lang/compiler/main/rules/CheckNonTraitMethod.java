package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.DEFAULTED;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.EXTERN;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Check non-trait specifics on methods/operators.
 */
public class CheckNonTraitMethod implements BiConsumer<MethodSymbol, EK9Parser.OperationDetailsContext> {

  private final CheckForBody checkForBody = new CheckForBody();

  private final ErrorListener errorListener;

  public CheckNonTraitMethod(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.OperationDetailsContext ctx) {
    final var hasBody = checkForBody.test(ctx);
    final var isVirtual = !method.isMarkedAbstract() && !hasBody;

    //So for general methods if they are not marked as abstract and have no supplied body
    //Then we must check if it is marked as default or is externally provided.
    var isDefaultedMethod = "TRUE".equals(method.getSquirrelledData(DEFAULTED));
    var isExternallyImplemented = "TRUE".equals(method.getSquirrelledData(EXTERN));

    if (isVirtual && !isDefaultedMethod && !isExternallyImplemented) {
      errorListener.semanticError(method.getSourceToken(), "",
          ErrorListener.SemanticClassification.NOT_ABSTRACT_AND_NO_BODY_PROVIDED);
    }
  }
}