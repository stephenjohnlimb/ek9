package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Check that an application definition is configured in an acceptable form.
 */
class ApplicationBodyOrError implements Consumer<EK9Parser.ApplicationDeclarationContext> {

  private final SymbolsAndScopes symbolsAndScopes;
  private final ErrorListener errorListener;

  public ApplicationBodyOrError(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ApplicationDeclarationContext ctx) {

    final var externallyImplemented = symbolsAndScopes.isExternallyImplemented();

    final var blockStatements = ctx.blockStatement();
    final var registerStatements = ctx.registerStatement();

    if (blockStatements.isEmpty()
        && registerStatements.isEmpty()
        && !externallyImplemented) {

      errorListener.semanticError(new Ek9Token(ctx.start),
          "definition of application:",
          ErrorListener.SemanticClassification.IMPLEMENTATION_MUST_BE_PROVIDED);
    }

  }
}
