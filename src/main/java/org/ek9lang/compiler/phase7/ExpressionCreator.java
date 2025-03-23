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
 * Creates an Expression node from the AST passed in.
 *
 * <p>
 * This is the backbone of the language really.
 * </p>
 * <pre>
 *   expression
 *     : expression op=(INC | DEC | BANG)
 *     | op=SUB expression
 *     | expression op=QUESTION
 *     | op=TOJSON expression
 *     | op=DOLLAR expression
 *     | op=PROMOTE expression
 *     | op=LENGTH OF? expression
 *     | op=PREFIX expression
 *     | op=SUFFIX expression
 *     | op=HASHCODE expression
 *     | op=ABS OF? expression
 *     | op=SQRT OF? expression
 *     | &lt;assoc=right&gt; left=expression coalescing=(CHECK | ELVIS) right=expression
 *     | primary
 *     | call
 *     | objectAccessExpression
 *     | list
 *     | dict
 *     | expression IS? neg=NOT? op=EMPTY
 *     | op=(NOT | TILDE) expression
 *     | left=expression op=CARET right=expression
 *     | left=expression op=(DIV | MUL | MOD | REM ) NL? right=expression
 *     | left=expression op=(ADD | SUB) NL? right=expression
 *     | left=expression op=(SHFTL | SHFTR) right=expression
 *     | left=expression op=(CMP | FUZ) NL? right=expression
 *     | left=expression op=(LE | GE | GT | LT) NL? right=expression
 *     | left=expression op=(EQUAL | NOTEQUAL | NOTEQUAL2) NL? right=expression
 *     | &lt;assoc=right&gt; left=expression coalescing_equality=(COALESCE_LE | COALESCE_GE | COALESCE_GT | COALESCE_LT) right=expression
 *     | left=expression neg=NOT? op=MATCHES right=expression
 *     | left=expression neg=NOT? op=CONTAINS right=expression
 *     | left=expression IS? neg=NOT? IN right=expression
 *     | left=expression op=(AND | XOR | OR) NL? right=expression
 *     | expression IS? neg=NOT? IN range
 *     | &lt;assoc=right&gt; control=expression LEFT_ARROW ternaryPart ternary=(COLON|ELSE) ternaryPart
 *     ;
 * </pre>
 */
public final class ExpressionCreator implements Function<EK9Parser.ExpressionContext, INode> {

  private final ParsedModule parsedModule;

  public ExpressionCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;

  }

  @Override
  public INode apply(final EK9Parser.ExpressionContext ctx) {

    if (ctx.call() != null) {

      //TODO use a specific function for all this as call can be very complex
      //Also relocate SymbolsFromParamExpression to common - as it will be needed to
      //locate the arguments for the call.
      final var symbol = parsedModule.getRecordedSymbol(ctx.call());
      AssertValue.checkNotNull("Call symbol should not be null", symbol);
      final var callSymbol = (CallSymbol) symbol;
      final var toBeCalled = callSymbol.getResolvedSymbolToCall();
      AssertValue.checkNotNull("Symbol to be called should not be null", toBeCalled);

      //I would have thought I'd have the arguments to pass in here somewhere.
      //But cannot see them at present.
      //TODO investigate as these will be needed for general calls and consructor calls.
      if (toBeCalled instanceof MethodSymbol methodSymbol) {
        if (methodSymbol.isConstructor()) {
          return new ConstructorCall();
        }
        return new Call();
      }
      //In the fullness of time will deal with Functions as well.
    }
    throw new CompilerException("Expression not fully implemented yet");
  }
}
