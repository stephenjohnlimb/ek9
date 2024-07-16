package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Checks for the appropriate use of the APPLICATION of xyz on a method declaration.
 * Only to be used with Programs - this is because the grammar allows this - to keep it simple.
 */
final class ApplicationOnMethodOrError extends RuleSupport
    implements Consumer<EK9Parser.MethodDeclarationContext> {

  ApplicationOnMethodOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(EK9Parser.MethodDeclarationContext ctx) {

    if (ctx.APPLICATION() != null && !(ctx.parent instanceof EK9Parser.ProgramBlockContext)) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.APPLICATION_SELECTION_INVALID);
    }

  }
}
