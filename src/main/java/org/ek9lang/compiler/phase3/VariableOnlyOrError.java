package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.COMPONENT_INJECTION_IN_PURE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.COMPONENT_INJECTION_NOT_POSSIBLE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.COMPONENT_INJECTION_OF_NON_ABSTRACT;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Focus on checking a variable only declaration, now that types are all known.
 * This is over and above the tests in phase 1. Which does all the basics.
 */
final class VariableOnlyOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.VariableOnlyDeclarationContext> {
  private final NoDelegateMethodClashesOrError noDelegateMethodClashesOrError;

  VariableOnlyOrError(SymbolsAndScopes symbolsAndScopes,
                      ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.noDelegateMethodClashesOrError = new NoDelegateMethodClashesOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    //This will issue errors is symbol not resolved or not typed.
    final var symbol = getRecordedAndTypedSymbol(ctx);
    if (symbol != null && symbol.getType().isPresent()) {
      if (ctx.BANG() != null) {
        validInjectionOrError(symbol, symbol.getType().get());
      }
      noDelegateMethodClashesOrError.accept(symbol);
    }

  }


  private void validInjectionOrError(final ISymbol symbol, final ISymbol symbolType) {

    if (!symbolType.getGenus().equals(ISymbol.SymbolGenus.COMPONENT)) {
      errorListener.semanticError(symbol.getSourceToken(), "is not a component but a '"
          + symbolType.getGenus() + "':", COMPONENT_INJECTION_NOT_POSSIBLE);
      return;
    }

    if (!symbolType.isMarkedAbstract()) {
      errorListener.semanticError(symbol.getSourceToken(), "", COMPONENT_INJECTION_OF_NON_ABSTRACT);
      return;
    }

    if (symbol.isIncomingParameter()) {
      errorListener.semanticError(symbol.getSourceToken(), "is an incoming parameter:",
          COMPONENT_INJECTION_NOT_POSSIBLE);
      return;
    }

    if (symbol.isReturningParameter()) {
      errorListener.semanticError(symbol.getSourceToken(), "is a returning parameter:",
          COMPONENT_INJECTION_NOT_POSSIBLE);
      return;
    }

    //So we do have an injection - now we need to check if it used in a pure context, because that's not allowed.
    if (isProcessingScopePure()) {
      errorListener.semanticError(symbol.getSourceToken(), "", COMPONENT_INJECTION_IN_PURE);
    }
  }
}
