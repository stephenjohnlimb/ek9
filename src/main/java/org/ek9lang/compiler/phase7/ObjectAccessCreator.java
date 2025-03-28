package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.Call;
import org.ek9lang.compiler.ir.ChainedAccess;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.compiler.ir.VariableRef;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

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

  private final ArgumentsCreator argumentsCreator;

  public ObjectAccessCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.argumentsCreator = new ArgumentsCreator(parsedModule);
  }

  @Override
  public INode apply(final EK9Parser.ObjectAccessExpressionContext ctx) {

    //Needs to be in a final form that could be converted to chained calls in an implementation.
    final var rtn = new ChainedAccess();
    rtn.add(getObjectAccessStart(ctx.objectAccessStart()));

    processObjectAccess(rtn, ctx.objectAccess());
    return rtn;
  }

  private INode getObjectAccessStart(final EK9Parser.ObjectAccessStartContext ctx) {
    if (ctx.identifier() != null) {
      var symbol = parsedModule.getRecordedSymbol(ctx.identifier());
      return new VariableRef(symbol);
    }

    throw new CompilerException("ObjectAccessStart not fully implemented yet");
  }

  /**
   * Handles the recursive nature of chained access.
   *
   * @param rtn The Chained access to add the appropriate node to.
   * @param ctx The context to be processed.
   */
  private void processObjectAccess(final ChainedAccess rtn,
                                   final EK9Parser.ObjectAccessContext ctx) {

    processObjectAccessType(rtn, ctx.objectAccessType());

    //Make recursive call as appropriate.
    if (ctx.objectAccess() != null) {
      processObjectAccess(rtn, ctx.objectAccess());
    }

  }

  private void processObjectAccessType(final ChainedAccess rtn,
                                       final EK9Parser.ObjectAccessTypeContext ctx) {
    if (ctx.operationCall() != null) {
      final var symbol = parsedModule.getRecordedSymbol(ctx.operationCall());
      AssertValue.checkNotNull("OperationCall symbol should not be null", symbol);
      final var callSymbol = (CallSymbol) symbol;
      final var toBeCalled = callSymbol.getResolvedSymbolToCall();
      AssertValue.checkNotNull("Symbol to be called should not be null", toBeCalled);

      final var callArguments = argumentsCreator.apply(ctx.operationCall().paramExpression());
      if (toBeCalled instanceof MethodSymbol methodSymbol) {
        rtn.add(new Call(callSymbol, methodSymbol, callArguments));
      }
    } else if (ctx.identifier() != null) {
      var symbol = parsedModule.getRecordedSymbol(ctx.identifier());
      rtn.add(new VariableRef(symbol));
    } else {
      throw new CompilerException("Object Access Type unexpected structure");
    }
  }
}
