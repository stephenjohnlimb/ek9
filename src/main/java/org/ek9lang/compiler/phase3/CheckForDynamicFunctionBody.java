package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Checks if a dynamic function body is needed and provided.
 */
final class CheckForDynamicFunctionBody extends TypedSymbolAccess
    implements Consumer<EK9Parser.DynamicFunctionDeclarationContext> {
  private final CheckPureModifier checkPureModifier;
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();

  /**
   * Create a new function to check dynamic functions.
   */
  CheckForDynamicFunctionBody(final SymbolsAndScopes symbolsAndScopes,
                              final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.checkPureModifier = new CheckPureModifier(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var symbol = (FunctionSymbol) getRecordedAndTypedSymbol(ctx);
    final var noBodyProvided = ctx.dynamicFunctionBody() == null;

    symbol.getSuperFunction().ifPresent(superFunction -> {
      if (superFunction.isMarkedAbstract() && noBodyProvided) {
        var errorMessage = "function defined "
            + locationExtractorFromSymbol.apply(symbol)
            + " is abstract:";
        errorListener.semanticError(symbol.getSourceToken(), errorMessage,
            ErrorListener.SemanticClassification.GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED);
      }
      checkPureModifier.accept(new PureCheckData("", superFunction, symbol));
    });

  }
}
