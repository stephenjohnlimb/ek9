package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Check trait specifics on methods, Allowing missing body but marking as abstract.
 */
public class CheckTraitMethod implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final CheckForBody checkForBody = new CheckForBody();

    @Override
  public void accept(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    final var hasBody = checkForBody.test(ctx);
    final var isVirtual = !method.isMarkedAbstract() && !hasBody;

    if (isVirtual) {
      method.setMarkedAbstract(true);
    }
  }
}
