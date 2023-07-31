package org.ek9lang.compiler.common;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbols.ICanBeGeneric;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks for the containing construct type and whether it is marked as abstract or not.
 */
public class CheckIfContextSupportsAbstractMethod extends RuleSupport
    implements BiConsumer<MethodSymbol, ParserRuleContext> {

  public CheckIfContextSupportsAbstractMethod(final SymbolAndScopeManagement symbolAndScopeManagement,
                                              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(MethodSymbol methodSymbol, ParserRuleContext ctx) {
    var containingConstructCtx = ctx.getParent().getParent();
    if (containingConstructCtx instanceof EK9Parser.ClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.DynamicClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.ComponentDeclarationContext
        || containingConstructCtx instanceof EK9Parser.RecordDeclarationContext) {
      var parent = symbolAndScopeManagement.getRecordedSymbol(containingConstructCtx);
      var constructLine = containingConstructCtx.start.getLine();
      if (parent instanceof ICanBeGeneric possibleGeneric
          && !possibleGeneric.isMarkedAbstract() && methodSymbol.isMarkedAbstract()) {
        var message = "containing construct, on line " + constructLine + ", is not marked as abstract, '"
            + methodSymbol.getFriendlyName() + "':";
        errorListener.semanticError(ctx.start, message,
            ErrorListener.SemanticClassification.CANNOT_BE_ABSTRACT);
      }
    }
  }
}
