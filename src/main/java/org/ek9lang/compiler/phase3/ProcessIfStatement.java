package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Checks the IfStatement is valid or emits errors.
 */
final class ProcessIfStatement extends TypedSymbolAccess implements Consumer<EK9Parser.IfStatementContext> {
  private final CheckControlIsBooleanOrError checkControlIsBooleanOrError;

  ProcessIfStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                     final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.checkControlIsBooleanOrError = new CheckControlIsBooleanOrError(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.IfStatementContext ctx) {

    ctx.ifControlBlock().stream()
        .map(controlBlock -> controlBlock.preFlowAndControl().control)
        .forEach(checkControlIsBooleanOrError);

  }

}
