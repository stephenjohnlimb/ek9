package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.ICanBeGeneric;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Checks for the containing construct type and whether it is open for extension or not.
 */
public class CheckIfExtendableByContext extends RuleSupport implements BiConsumer<MethodSymbol, ParserRuleContext> {

  protected CheckIfExtendableByContext(final SymbolAndScopeManagement symbolAndScopeManagement,
                                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(MethodSymbol methodSymbol, ParserRuleContext ctx) {
    var containingConstructCtx = ctx.getParent().getParent();
    if (containingConstructCtx instanceof EK9Parser.ClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.DynamicClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.ComponentDeclarationContext) {
      var parent = symbolAndScopeManagement.getRecordedSymbol(containingConstructCtx);
      var constructLine = containingConstructCtx.start.getLine();
      if (parent instanceof ICanBeGeneric possibleGeneric
          && !possibleGeneric.isOpenForExtension() && methodSymbol.isMarkedAbstract()) {
        var message = "containing construct, on line " + constructLine + ", is not open for extension, '"
            + methodSymbol.getFriendlyName() + "':";
        errorListener.semanticError(ctx.start, message,
            ErrorListener.SemanticClassification.CANNOT_BE_ABSTRACT);
      }
    }
  }
}
