package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;

/**
 * Checks that variable declarations have a typedef when using in generic types/functions.
 * This is an initial limitation of using generics. It makes dependent generic use very hard to
 * determine - because you need expression results and types - so a catch 22 (for now).
 */
final class CheckVariableDeclaration implements Consumer<EK9Parser.VariableDeclarationContext> {

  private final CheckOuterGenericsUse checkOuterGenericsUse;

  /**
   * If the variable has been declared within any sort of generic type/function and not been
   * used with a 'typedef' i.e. it has been inferred - this will generate an error.
   */
  CheckVariableDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {

    checkOuterGenericsUse = new CheckOuterGenericsUse(symbolAndScopeManagement, errorListener,
        ErrorListener.SemanticClassification.TYPE_INFERENCE_NOT_SUPPORTED);
  }

  @Override
  public void accept(final EK9Parser.VariableDeclarationContext ctx) {
    if (ctx.typeDef() == null) {
      checkOuterGenericsUse.accept(ctx.start);
    }
  }
}