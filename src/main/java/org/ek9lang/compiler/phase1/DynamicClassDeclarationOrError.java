package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks that a dynamic class does not used naming when used inside a generic type/function.
 */
final class DynamicClassDeclarationOrError implements Consumer<EK9Parser.DynamicClassDeclarationContext> {

  private final OuterGenericsUseOrError outerGenericsUseOrError;

  DynamicClassDeclarationOrError(final SymbolsAndScopes symbolsAndScopes,
                                 final ErrorListener errorListener) {

    outerGenericsUseOrError = new OuterGenericsUseOrError(symbolsAndScopes, errorListener,
        ErrorListener.SemanticClassification.GENERIC_WITH_NAMED_DYNAMIC_CLASS);
  }

  @Override
  public void accept(final EK9Parser.DynamicClassDeclarationContext ctx) {

    if (ctx.Identifier() != null) {
      outerGenericsUseOrError.accept(new Ek9Token(ctx.start));
    }

  }
}