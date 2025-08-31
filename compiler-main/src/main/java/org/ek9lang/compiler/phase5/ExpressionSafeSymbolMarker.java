package org.ek9lang.compiler.phase5;

import java.util.Map;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Once an expression has been assessed as being simple enough to process, this
 * consumer is called to mark the appropriate symbol(s) used in the expression as safe if the
 * appropriate methods are called.
 */
class ExpressionSafeSymbolMarker extends AbstractSafeSymbolMarker
    implements BiConsumer<EK9Parser.ExpressionContext, IScope> {

  private final Map<String, CommonValues> resultMethodNameLookup =
      Map.of("?", CommonValues.OK_ACCESS_REQUIRES_SAFE_ACCESS,
          "isOk", CommonValues.OK_ACCESS_REQUIRES_SAFE_ACCESS,
          "isError", CommonValues.ERROR_ACCESS_REQUIRES_SAFE_ACCESS);

  private final Map<CommonValues, BiConsumer<ISymbol, IScope>> resultMethodLookup;

  private final Map<String, CommonValues> optionalMethodNameLookup =
      Map.of("?", CommonValues.GET_ACCESS_REQUIRES_SAFE_ACCESS);

  private final Map<String, CommonValues> iteratorMethodNameLookup =
      Map.of("?", CommonValues.NEXT_ACCESS_REQUIRES_SAFE_ACCESS,
          "hasNext", CommonValues.NEXT_ACCESS_REQUIRES_SAFE_ACCESS);

  /**
   * Constructor to provided typed access.
   */
  protected ExpressionSafeSymbolMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);

    //For Result it's a bit more complex.
    resultMethodLookup =
        Map.of(CommonValues.OK_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::markOkResultAccessSafe,
            CommonValues.ERROR_ACCESS_REQUIRES_SAFE_ACCESS, symbolsAndScopes::markErrorResultAccessSafe);
  }

  @Override
  public void accept(final EK9Parser.ExpressionContext ctx, final IScope scopeMadeSafe) {
    assessExpression(ctx, scopeMadeSafe);
  }

  private void assessExpression(final EK9Parser.ExpressionContext ctx, final IScope scopeMadeSafe) {

    //Then it is just this expression and not linked in any way
    if (ctx.expression().isEmpty()) {
      //primary can be '(' expression ')'
      if (ctx.primary() != null && ctx.primary().expression() != null) {
        assessExpression(ctx.primary().expression(), scopeMadeSafe);
      } else if (ctx.objectAccessExpression() != null) {
        assessObjectAccessExpression(ctx.objectAccessExpression(), scopeMadeSafe);
      }
    } else if (ctx.expression().size() == 2 && ctx.AND() != null) {
      //If there are two expressions with an 'and' then assess both sides.
      assessExpression(ctx.expression(0), scopeMadeSafe);
      assessExpression(ctx.expression(1), scopeMadeSafe);

    } else if (ctx.QUESTION() != null) {
      //Just a single expression but with an operator of '?'
      final var toBeAssessed = symbolsAndScopes.getRecordedSymbol(ctx.expression(0));
      assessIsSetCall(toBeAssessed, scopeMadeSafe);

    }
    //Otherwise it's too complex with 'not' / 'or' etc. so nothing to assess.

  }

  private void assessObjectAccessExpression(final EK9Parser.ObjectAccessExpressionContext ctx,
                                            final IScope scopeMadeSafe) {

    final var objectAccessStartSymbol = getRecordedAndTypedSymbol(ctx.objectAccessStart());

    if (objectAccessStartSymbol != null) {
      checkObjectAccessContext(objectAccessStartSymbol, ctx.objectAccess(), scopeMadeSafe);
    }

  }

  //This also has to be recursive through the object access structure
  private void checkObjectAccessContext(final ISymbol calledFromSymbol,
                                        final EK9Parser.ObjectAccessContext ctx,
                                        final IScope scopeMadeSafe) {
    final var possibleCallSymbol = getRecordedAndTypedSymbol(ctx);
    //Should now be valid and typed by the time we get to this phase.
    if (possibleCallSymbol != null) {
      processPossibleSafeMakingCall(calledFromSymbol, possibleCallSymbol, scopeMadeSafe);

      //Now make the recursive call. So this is the chaining and on we go until then end of the call chain.
      if (ctx.objectAccess() != null) {
        checkObjectAccessContext(possibleCallSymbol, ctx.objectAccess(), scopeMadeSafe);
      }
    }
  }

  /**
   * Now the possibleCallSymbol might be the call that makes other method calls on
   * calledFromSymbol safe. This method checks that and updates the appropriate code flow map in symbolsAndScopes.
   */
  private void processPossibleSafeMakingCall(final ISymbol calledFromSymbol,
                                             final ISymbol possibleCallSymbol,
                                             final IScope scopeMadeSafe) {

    if (possibleCallSymbol instanceof CallSymbol asCallSymbol) {
      final var resolvedToCall = asCallSymbol.getResolvedSymbolToCall();
      //At this point we know for sure that the method name is valid on the calledFromSymbol - because it has
      //been resolved. The question is, can it just be called without a check call before.
      final var methodName = resolvedToCall.getName();

      if (resultTypeCheck.test(calledFromSymbol)) {
        final var squirrelledLookupValue = resultMethodNameLookup.get(methodName);
        if (squirrelledLookupValue != null) {
          final var toCall = resultMethodLookup.get(squirrelledLookupValue);
          toCall.accept(calledFromSymbol, scopeMadeSafe);
        }
      } else if (optionalTypeCheck.test(calledFromSymbol) && optionalMethodNameLookup.get(methodName) != null) {
        symbolsAndScopes.markGetOptionalAccessSafe(calledFromSymbol, scopeMadeSafe);
      } else if (iteratorTypeCheck.test(calledFromSymbol) && iteratorMethodNameLookup.get(methodName) != null) {
        symbolsAndScopes.markNextIteratorAccessSafe(calledFromSymbol, scopeMadeSafe);
      }

    }
  }
}
