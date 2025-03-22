package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.Statement;
import org.ek9lang.core.AssertValue;

/**
 * Creates the appropriate IR representation of a statement/expression that can be used within a block.
 * TODO this now need to delegate to additional Creators to process each of the specific types of statement/expression.
 */
public class BlockStatementCreator implements Function<EK9Parser.BlockStatementContext, Statement> {

  private final ParsedModule parsedModule;

  public BlockStatementCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;

  }

  @Override
  public Statement apply(final EK9Parser.BlockStatementContext ctx) {
    if (ctx.variableDeclaration() != null) {
      final var varDecl = parsedModule.getRecordedSymbol(ctx.variableDeclaration());
      AssertValue.checkNotNull("Variable declaration cannot be null", varDecl);
      return new Statement(ctx.variableDeclaration().getText());
    } else if (ctx.variableOnlyDeclaration() != null) {
      final var varOnlyDecl = parsedModule.getRecordedSymbol(ctx.variableOnlyDeclaration());
      AssertValue.checkNotNull("Variable declaration cannot be null", varOnlyDecl);
      return new Statement(ctx.variableOnlyDeclaration().getText());
    } else if (ctx.statement() != null) {
      return new Statement(ctx.statement().getText());
    }
    return null;
  }
}
