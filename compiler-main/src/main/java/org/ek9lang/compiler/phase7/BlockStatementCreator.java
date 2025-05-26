package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.compiler.ir.VariableDecl;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR representation of a statement/expression that can be used within a block.
 */
public final class BlockStatementCreator implements Function<EK9Parser.BlockStatementContext, INode> {

  private final ParsedModule parsedModule;
  private final VariableDeclCreator variableDeclCreator;
  private final StatementCreator statementCreator;

  public BlockStatementCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.variableDeclCreator = new VariableDeclCreator(parsedModule);
    this.statementCreator = new StatementCreator(parsedModule);

  }

  @Override
  public INode apply(final EK9Parser.BlockStatementContext ctx) {
    if (ctx.variableDeclaration() != null) {
      return variableDeclCreator.apply(ctx.variableDeclaration());
    } else if (ctx.variableOnlyDeclaration() != null) {
      final var varOnlyDecl = parsedModule.getRecordedSymbol(ctx.variableOnlyDeclaration());
      AssertValue.checkNotNull("Variable declaration cannot be null", varOnlyDecl);
      return new VariableDecl(varOnlyDecl);
    } else if (ctx.statement() != null) {
      return statementCreator.apply(ctx.statement());
    }
    throw new CompilerException("Not expecting any other block statement");
  }

}
