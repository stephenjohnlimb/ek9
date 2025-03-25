package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.core.CompilerException;

/**
 * Deals with the multitude of different types of statements to create the appropriate nodes.
 */
public final class StatementCreator implements Function<EK9Parser.StatementContext, INode> {

  private final ParsedModule parsedModule;
  private final ObjectAccessCreator objectAccessCreator;

  public StatementCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.objectAccessCreator = new ObjectAccessCreator(parsedModule);
  }

  @Override
  public INode apply(final EK9Parser.StatementContext ctx) {

    if (ctx.objectAccessExpression() != null) {
      return objectAccessCreator.apply(ctx.objectAccessExpression());
    }
    throw new CompilerException("Expression not fully implemented yet");
  }
}
