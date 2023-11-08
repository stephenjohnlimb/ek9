package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Checks if a dynamic function body is needed and provided.
 */
final class CheckForDynamicFunctionBody extends TypedSymbolAccess
    implements Consumer<EK9Parser.DynamicFunctionDeclarationContext> {
  private final CheckPureModifier checkPureModifier;
  private final LocationExtractor locationExtractor = new LocationExtractor();

  /**
   * Create a new function to check dynamic functions.
   */
  CheckForDynamicFunctionBody(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.checkPureModifier = new CheckPureModifier(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.DynamicFunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) getRecordedAndTypedSymbol(ctx);
    var noBodyProvided = ctx.dynamicFunctionBody() == null;

    symbol.getSuperFunction().ifPresent(superFunction -> {
      if (superFunction.isMarkedAbstract() && noBodyProvided) {
        var errorMessage = "function defined "
            + locationExtractor.apply(symbol)
            + " is abstract:";
        errorListener.semanticError(symbol.getSourceToken(), errorMessage,
            ErrorListener.SemanticClassification.GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED);
      }
      checkPureModifier.accept(new PureCheckData("", superFunction, symbol));
    });
  }
}
