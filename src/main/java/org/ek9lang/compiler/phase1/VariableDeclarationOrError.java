package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks that variable declarations is acceptable.
 */
final class VariableDeclarationOrError implements Consumer<EK9Parser.VariableDeclarationContext> {

  private final OuterGenericsUseOrError outerGenericsUseOrError;

  /**
   * If the variable has been declared within any sort of generic type/function and not been
   * used with a 'typedef' i.e. it has been inferred - this will generate an error.
   */
  VariableDeclarationOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {

    outerGenericsUseOrError = new OuterGenericsUseOrError(symbolsAndScopes, errorListener,
        ErrorListener.SemanticClassification.TYPE_INFERENCE_NOT_SUPPORTED);
  }

  @Override
  public void accept(final EK9Parser.VariableDeclarationContext ctx) {

    if (ctx.typeDef() == null) {
      outerGenericsUseOrError.accept(new Ek9Token(ctx.start));
    }

  }
}