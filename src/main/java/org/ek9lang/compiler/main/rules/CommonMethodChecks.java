package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Check Methods that apply in all method contexts.
 */
public class CommonMethodChecks implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final ErrorListener errorListener;

  public CommonMethodChecks(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(MethodSymbol method, EK9Parser.MethodDeclarationContext ctx) {

    var message = "for method '" + method.getName() + "':";
    if (ctx.accessModifier() != null) {
      if (!"private".equals(method.getAccessModifier())) {
        //non-private are really part of an interface, so we allow these to be unreferenced.
        //by marking them as referenced.
        method.setReferenced(true);
      }

      //In EK9 while we define public methods and the like it is not needed
      //So the grammar has public but EK9 issues an error stating that it is not needed.
      //This only because developer are likely to use public due to C# and Java experience.
      if ("public".equals(ctx.accessModifier().getText())) {
        //use the ctx to see if developer specified public, the 'method symbol' will default to public.
        errorListener.semanticError(ctx.accessModifier().start, message,
            ErrorListener.SemanticClassification.METHOD_ACCESS_MODIFIER_DEFAULT);
      }
    }

    if ("private".equals(method.getAccessModifier()) && method.isOverride()) {
      //It is private, so no logic in allowing override as that makes no sense.
      errorListener.semanticError(ctx.OVERRIDE().getSymbol(), message,
          ErrorListener.SemanticClassification.METHOD_ACCESS_MODIFIER_PRIVATE_OVERRIDE);
    }

    if (method.isMarkedAbstract() && method.isOverride()) {
      //This makes no sense to define a method abstract and also say it overrides something.
      errorListener.semanticError(ctx.OVERRIDE().getSymbol(), message,
          ErrorListener.SemanticClassification.ABSTRACT_METHOD_AND_OVERRIDE);
    }

    if (method.isConstructor() && method.isMarkedAbstract()) {
      //Makes no sense
      errorListener.semanticError(ctx.ABSTRACT().getSymbol(), message,
          ErrorListener.SemanticClassification.ABSTRACT_CONSTRUCTOR);
    }

    if (method.isConstructor() && method.isOverride()) {
      //Makes no sense for a constructor to be declared like this.
      errorListener.semanticError(ctx.OVERRIDE().getSymbol(), message,
          ErrorListener.SemanticClassification.OVERRIDE_CONSTRUCTOR);
    }
  }
}
