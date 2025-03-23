package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.compiler.ir.Statement;
import org.ek9lang.compiler.ir.VariableDecl;
import org.ek9lang.core.AssertValue;

/**
 * Creates the appropriate IR representation of a statement/expression that can be used within a block.
 * TODO this now need to delegate to additional Creators to process each of the specific types of statement/expression.
 */
public class BlockStatementCreator implements Function<EK9Parser.BlockStatementContext, INode> {

  private final ParsedModule parsedModule;
  private final VariableDeclCreator variableDeclCreator;

  public BlockStatementCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.variableDeclCreator = new VariableDeclCreator(parsedModule);

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
      return new Statement(ctx.statement().getText());
    }
    return null;
  }

}
