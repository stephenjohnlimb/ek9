package org.ek9lang.compiler.phase1;

import static org.ek9lang.compiler.support.AggregateManipulator.PUBLIC;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.AppropriateBodyOrError;
import org.ek9lang.compiler.common.Defaulted;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OverrideOrAbstractOrError;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Processes and checks Methods that apply in all method contexts for basic correctness.
 */
final class ProcessCommonMethodsOrError extends RuleSupport
    implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final Defaulted defaulted = new Defaulted();
  private final AppropriateBodyOrError appropriateBodyOrError;

  private final OverrideOrAbstractOrError overrideOrAbstractOrError;

  ProcessCommonMethodsOrError(final SymbolsAndScopes symbolsAndScopes,
                              final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    appropriateBodyOrError = new AppropriateBodyOrError(symbolsAndScopes, errorListener);
    overrideOrAbstractOrError = new OverrideOrAbstractOrError(symbolsAndScopes, errorListener);

  }


  @Override
  public void accept(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    appropriateBodyOrError.accept(method, ctx.operationDetails());
    overrideOrAbstractOrError.accept(method);

    final var message = "for method '" + ctx.identifier().getText() + "':";
    checkAccessModifier(method, ctx, message);
    checkConstructor(method, message);
    checkDefaulted(method, ctx, message);

  }

  private void checkAccessModifier(final MethodSymbol method,
                                   final EK9Parser.MethodDeclarationContext ctx,
                                   final String errorMessage) {

    if (ctx.accessModifier() != null) {

      if (!"private".equals(method.getAccessModifier())) {
        //non-private are really part of an interface, so we allow these to be unreferenced
        //by marking them as referenced, that way later check do not issue errors.
        method.setReferenced(true);
      }

      //In EK9 while we define public methods and the like it is not needed
      //So the grammar has public but EK9 issues an error stating that it is not needed.
      //This only because developer are likely to use public due to C# and Java experience.
      if (PUBLIC.equals(ctx.accessModifier().getText())) {
        //use the ctx to see if developer specified public, the 'method symbol' will default to public.
        errorListener.semanticError(ctx.accessModifier().start, errorMessage,
            ErrorListener.SemanticClassification.METHOD_ACCESS_MODIFIER_DEFAULT);
      }

    }

    if ("private".equals(method.getAccessModifier()) && method.isOverride()) {
      //It is private, so no logic in allowing override as that makes no sense.
      errorListener.semanticError(ctx.OVERRIDE().getSymbol(), errorMessage,
          ErrorListener.SemanticClassification.METHOD_ACCESS_MODIFIER_PRIVATE_OVERRIDE);
    }

  }

  private void checkConstructor(final MethodSymbol method,
                                final String errorMessage) {

    if (method.isConstructor()) {
      if (method.isMarkedAbstract()) {
        //Makes no sense
        errorListener.semanticError(method.getSourceToken(), errorMessage,
            ErrorListener.SemanticClassification.ABSTRACT_CONSTRUCTOR);
      }

      if (method.isOverride()) {
        //Makes no sense for a constructor to be declared like this.
        errorListener.semanticError(method.getSourceToken(), errorMessage,
            ErrorListener.SemanticClassification.OVERRIDE_CONSTRUCTOR);
      }
    }

  }

  private void checkDefaulted(final MethodSymbol method,
                              final EK9Parser.MethodDeclarationContext ctx,
                              final String errorMessage) {

    if (defaulted.test(method) && method.isConstructor() && !method.getCallParameters().isEmpty()) {
      errorListener.semanticError(ctx.DEFAULT().getSymbol(), errorMessage,
          ErrorListener.SemanticClassification.INVALID_DEFAULT_CONSTRUCTOR);
    }
  }

}
