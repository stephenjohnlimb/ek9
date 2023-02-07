package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Checks for the appropriate use of the APPLICATION of xyz on a method declaration.
 * Only to be used with Programs - this is because the grammar allows this - to keep it simple.
 */
public class CheckApplicationUseOnMethodDeclaration implements Consumer<EK9Parser.MethodDeclarationContext> {
  private final ErrorListener errorListener;

  public CheckApplicationUseOnMethodDeclaration(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.MethodDeclarationContext ctx) {
    if (ctx.APPLICATION() != null && !(ctx.parent instanceof EK9Parser.ProgramBlockContext)) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.APPLICATION_SELECTION_INVALID);
    }
  }
}
