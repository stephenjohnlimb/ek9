package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.compiler.ir.Statement;

/**
 * Creates the appropriate node structure for object access. Now this can be chained and
 * may involve chained calls or even a mix of chained calls property access and function delegate calls.
 * <pre>
 *   objectAccessExpression
 *     : objectAccessStart objectAccess
 *     ;
 *
 * objectAccessStart
 *     :  (primaryReference | identifier | call)
 *     ;
 *
 * objectAccess
 *     : objectAccessType objectAccess?
 *     ;
 *
 * objectAccessType
 *     : DOT (identifier | operationCall)
 *     ;
 *
 * operationCall
 *     : identifier paramExpression
 *     | operator paramExpression
 *     ;
 * </pre>
 */
public final class ObjectAccessCreator implements Function<EK9Parser.ObjectAccessExpressionContext, INode> {
  private final ParsedModule parsedModule;

  public ObjectAccessCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
  }

  @Override
  public INode apply(final EK9Parser.ObjectAccessExpressionContext ctx) {

    //TODO traverse the structure creating the appropriate nodes along the way in the right form
    //TODO linking back to the symbols.
    //Needs to be in a final form that could be converted to chained calls in an implementation.
    return new Statement(ctx.getText());
  }
}
