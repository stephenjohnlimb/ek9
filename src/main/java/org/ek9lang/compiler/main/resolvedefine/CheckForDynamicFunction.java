package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.support.LocationExtractor;

/**
 * Checks if a dynamic function body is needed and provided.
 */
public class CheckForDynamicFunction extends RuleSupport
    implements Consumer<EK9Parser.DynamicFunctionDeclarationContext> {

  private final CheckPureModifier checkPureModifier;

  private final LocationExtractor locationExtractor = new LocationExtractor();

  /**
   * Create a new function to check dynamic functions.
   */
  public CheckForDynamicFunction(final SymbolAndScopeManagement symbolAndScopeManagement,
                                 final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.checkPureModifier = new CheckPureModifier(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.DynamicFunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    var noBodyProvided = ctx.dynamicFunctionBody() == null;

    symbol.getSuperFunctionSymbol().ifPresent(superFunction -> {
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
