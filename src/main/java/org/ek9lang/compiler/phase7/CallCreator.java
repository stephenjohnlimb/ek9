package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.Call;
import org.ek9lang.compiler.ir.ConstructorCall;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates a call node (including appropriate call arguments).
 */
public final class CallCreator implements Function<EK9Parser.CallContext, INode> {

  private final ParsedModule parsedModule;
  private final ArgumentsCreator argumentsCreator;

  public CallCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.argumentsCreator = new ArgumentsCreator(parsedModule);
  }

  @Override
  public INode apply(final EK9Parser.CallContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Call symbol should not be null", symbol);
    final var callSymbol = (CallSymbol) symbol;
    final var toBeCalled = callSymbol.getResolvedSymbolToCall();
    AssertValue.checkNotNull("Symbol to be called should not be null", toBeCalled);

    final var callArguments = argumentsCreator.apply(ctx.paramExpression());
    if (toBeCalled instanceof MethodSymbol methodSymbol) {
      if (methodSymbol.isConstructor()) {
        return new ConstructorCall(callSymbol, methodSymbol, callArguments);
      }
      return new Call(callSymbol, methodSymbol, callArguments);
    }
    //In the fullness of time will deal with Functions as well.

    throw new CompilerException("Calls not fully implemented yet");
  }

}
