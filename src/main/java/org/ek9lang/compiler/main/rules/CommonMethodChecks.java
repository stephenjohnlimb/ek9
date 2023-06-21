package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.DEFAULTED;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Check Methods that apply in all method contexts.
 */
public class CommonMethodChecks implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final ErrorListener errorListener;

  private final CheckForBody checkForBody = new CheckForBody();

  public CommonMethodChecks(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    final var message = "for method '" + ctx.identifier().getText() + "':";
    final var hasBody = checkForBody.test(ctx.operationDetails());

    checkAbstract(method, ctx, hasBody, message);
    checkAccessModifier(method, ctx, message);
    checkConstructor(method, message);
    checkDefaulted(method, ctx, hasBody, message);

  }

  private void checkAbstract(final MethodSymbol method,
                             final EK9Parser.MethodDeclarationContext ctx,
                             final boolean hasBody, final String errorMessage) {
    //Now we don't check the converse in these common checks.
    //i.e. not marked as abstract, but without a body provided.
    //This is done elsewhere because for trait methods we accommodate this.

    if (method.isMarkedAbstract() && hasBody) {
      errorListener.semanticError(ctx.ABSTRACT().getSymbol(), errorMessage,
          ErrorListener.SemanticClassification.ABSTRACT_BUT_BODY_PROVIDED);
    }
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
      if ("public".equals(ctx.accessModifier().getText())) {
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

    if (method.isMarkedAbstract() && method.isOverride()) {
      //This makes no sense to define a method abstract and also say it overrides something.
      errorListener.semanticError(ctx.OVERRIDE().getSymbol(), errorMessage,
          ErrorListener.SemanticClassification.ABSTRACT_METHOD_AND_OVERRIDE);
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
                              final boolean hasBody, final String errorMessage) {
    if ("TRUE".equals(method.getSquirrelledData(DEFAULTED))) {
      if (method.isConstructor() && !method.getMethodParameters().isEmpty()) {
        errorListener.semanticError(ctx.DEFAULT().getSymbol(), errorMessage,
            ErrorListener.SemanticClassification.INVALID_DEFAULT_CONSTRUCTOR);
      }

      if (hasBody) {
        errorListener.semanticError(method.getSourceToken(), errorMessage,
            ErrorListener.SemanticClassification.ABSTRACT_BUT_BODY_PROVIDED);
      }
    }
  }
}
