package org.ek9lang.compiler.common;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.ICanBeGeneric;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks for the containing construct type and whether it is marked as abstract or not.
 */
public class ContextSupportsAbstractMethodOrError extends RuleSupport
    implements BiConsumer<MethodSymbol, ParserRuleContext> {

  /**
   * Create new.
   */
  public ContextSupportsAbstractMethodOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                              final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final MethodSymbol methodSymbol, final ParserRuleContext ctx) {

    if (methodSymbol.isMarkedAbstract()) {
      errorIfContextIsNotMarkedAbstract(methodSymbol, ctx);
    }

  }

  private void errorIfContextIsNotMarkedAbstract(final MethodSymbol methodSymbol, final ParserRuleContext ctx) {

    //Locate the containing construct context.
    final var containingConstructCtx = ctx.getParent().getParent();

    //Only certain constructs can have methods so only check these.
    if (constructSupportsMethods(containingConstructCtx)) {

      //Now get the symbol that relates to the construct context
      final var parent = symbolAndScopeManagement.getRecordedSymbol(containingConstructCtx);

      //Finally, can check if the method is abstract and the construct is not marked abstract.
      if (parent instanceof ICanBeGeneric possibleGeneric && !possibleGeneric.isMarkedAbstract()) {

        final var constructLine = containingConstructCtx.start.getLine();
        final var message = "is abstract, but construct on line " + constructLine + " is not marked as abstract, '"
            + methodSymbol.getFriendlyName() + "':";

        errorListener.semanticError(ctx.start, message, ErrorListener.SemanticClassification.CANNOT_BE_ABSTRACT);
      }
    }

  }

  private boolean constructSupportsMethods(final ParserRuleContext containingConstructCtx) {

    return containingConstructCtx instanceof EK9Parser.ClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.DynamicClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.ComponentDeclarationContext
        || containingConstructCtx instanceof EK9Parser.RecordDeclarationContext;

  }
}
