package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.CommonTypeSuperOrTraitOrError;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Ensures that 'expression' is now resolved and 'typed' or a not resolved error.
 * This is a beast and will have to delegate parts, as there are just so many types of expression.
 */
final class ExpressionOrError extends TypedSymbolAccess implements Consumer<EK9Parser.ExpressionContext> {
  private final SymbolFactory symbolFactory;
  private final IsSetPresentOrError isSetPresentOrError;
  private final ComparatorPresentOrError comparatorPresentOrError;
  private final RequiredOperatorPresentOrError requiredOperatorPresentOrError;
  private final MethodSymbolSearchForExpression methodSymbolSearchForExpression;
  private final ControlIsBooleanOrError controlIsBooleanOrError;
  private final CommonTypeSuperOrTraitOrError commonTypeSuperOrTraitOrError;
  private final AccessLeftAndRightOrError accessLeftAndRightOrError;
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final IsConvertableToStringOrError isConvertableToStringOrError;

  private final Function<Optional<ExprLeftAndRightData>, List<ISymbol>> toList =
      exprLeftAndRightData -> exprLeftAndRightData.map(data -> List.of(data.left(), data.right())).orElse(List.of());

  /**
   * Check Primary resolves and attempt to 'type' it.
   */
  ExpressionOrError(final SymbolsAndScopes symbolsAndScopes, final SymbolFactory symbolFactory,
                    final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFactory = symbolFactory;
    this.isSetPresentOrError = new IsSetPresentOrError(symbolsAndScopes, errorListener);
    this.comparatorPresentOrError = new ComparatorPresentOrError(symbolsAndScopes, errorListener);
    this.requiredOperatorPresentOrError = new RequiredOperatorPresentOrError(symbolsAndScopes, errorListener);
    this.methodSymbolSearchForExpression = new MethodSymbolSearchForExpression(symbolsAndScopes, errorListener);
    this.controlIsBooleanOrError = new ControlIsBooleanOrError(symbolsAndScopes, errorListener);
    this.commonTypeSuperOrTraitOrError = new CommonTypeSuperOrTraitOrError(symbolsAndScopes, errorListener);
    this.accessLeftAndRightOrError = new AccessLeftAndRightOrError(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError = new SymbolFromContextOrError(symbolsAndScopes, errorListener);
    this.isConvertableToStringOrError = new IsConvertableToStringOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ExpressionContext ctx) {

    final var symbol = processExpressionOrError(ctx);
    if (symbol != null) {
      symbolsAndScopes.recordSymbol(symbol, ctx);
      final var errorLocation = new Ek9Token(ctx.start);
      if (symbol.getType().isEmpty()) {
        emitTypeNotResolvedError(errorLocation, symbol);
      } else {
        //Now if the expression is used as part of an interpolated String need to
        //Check that the type has $ operator or promotion to String
        if (ctx.getParent() instanceof EK9Parser.StringPartContext) {
          isConvertableToStringOrError.test(errorLocation, symbol.getType().get());
        }
      }
    }

  }

  /**
   * Accepts the expression context and validates it, checks the parts needed (depending on the type of expression).
   * Then either returns null if it cannot be converted to an expression system,
   * or it returns a valid ExpressionSymbol with its 'Type' correctly set
   * (this is effectively the return type of the EK9 expression).
   */
  private ISymbol processExpressionOrError(final EK9Parser.ExpressionContext ctx) {
    //The idea here is that rather than have a giant 'if else' combo, the process is grouped.
    //So we get a big bang just by using 'op' as these are all just operators on types.
    //I've tried to get these in some reasonable order. With a combination of code coverage and
    //real examples we should be sure to get all the combinations. So some of these methods deal with several
    //parts of the 'expression' grammar.

    if (ctx.op != null) {
      return processOperationOrError(ctx);
    } else if (ctx.coalescing != null) {
      return processCoalescingOrError(ctx);
    } else if (ctx.coalescing_equality != null) {
      return processCoalescingEquality(ctx);
    } else if (ctx.primary() != null) {
      return processPrimary(ctx);
    } else if (ctx.call() != null) {
      return symbolFromContextOrError.apply(ctx.call());
    } else if (ctx.objectAccessExpression() != null) {
      return processObjectAccessExpression(ctx);
    }

    return processControlsOrStructures(ctx);

  }

  private ISymbol processControlsOrStructures(final EK9Parser.ExpressionContext ctx) {

    if (ctx.list() != null) {
      return symbolFromContextOrError.apply(ctx.list());
    } else if (ctx.dict() != null) {
      return symbolFromContextOrError.apply(ctx.dict());
    } else if (ctx.IN() != null) {
      return processInCollectionOrRangeOrError(ctx);
    } else if (ctx.control != null) {
      return processTernaryOrError(ctx);
    } else {
      AssertValue.fail(
          "Expecting finite set of operations for expression [" + ctx.getText() + "] line: " + ctx.start.getLine());
    }
    return null;
  }

  private ISymbol processPrimary(final EK9Parser.ExpressionContext ctx) {

    //Note it can be used in calls (but again within constraints), but this is use of 'super' like this.
    if (ctx.primary().primaryReference() != null && ctx.primary().primaryReference().SUPER() != null) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_SUPER);
    }

    final var symbol = symbolFromContextOrError.apply(ctx.primary());
    if (symbol != null) {
      checkSuitableCategoryAndGenusOrError(ctx, symbol);
    }

    return symbol;

  }

  private void checkSuitableCategoryAndGenusOrError(final EK9Parser.ExpressionContext ctx, final ISymbol symbol) {

    if (symbol.getCategory().equals(ISymbol.SymbolCategory.VARIABLE)
        || symbol.getCategory().equals(ISymbol.SymbolCategory.FUNCTION)
        || symbol.getCategory().equals(ISymbol.SymbolCategory.TEMPLATE_FUNCTION)) {
      return;
    }

    //Also allow enumerations to be referenced directly.
    if (symbol.getGenus().equals(ISymbol.SymbolGenus.CLASS_ENUMERATION)) {
      return;
    }

    emitTypeNotAppropriateError(new Ek9Token(ctx.start), symbol);

  }

  private ISymbol processObjectAccessExpression(final EK9Parser.ExpressionContext ctx) {

    final var startToken = new Ek9Token(ctx.start);
    final var maybeResolved = symbolFromContextOrError.apply(ctx.objectAccessExpression());

    if (maybeResolved != null && maybeResolved.getType().isPresent()) {
      return symbolFactory.newExpressionSymbol(startToken, ctx.getText()).setType(maybeResolved.getType());
    }

    return null;
  }

  private ISymbol processOperationOrError(final EK9Parser.ExpressionContext ctx) {

    //Special case for isSet because it can be used against a function delegate as well.
    if (ctx.QUESTION() != null) {
      return isSetOrError(ctx);
    } else if (!ctx.expression().isEmpty()) {

      //Could be one expression (unary) or have two expressions.
      //This case only looks for operators on some form of aggregate.
      final var opToken = new Ek9Token(ctx.op);
      final var search = methodSymbolSearchForExpression.apply(ctx);
      final var symbol = symbolFromContextOrError.apply(ctx.expression(0));
      if (symbol != null && symbol.getType().isPresent()) {
        return expressionFromOperatorDataOrError(ctx, new CheckOperatorData(symbol, opToken, search));
      }
    } else {
      throw new CompilerException("Operation must have at least one expression.");
    }

    return null;
  }

  /**
   * Just deals with the three expressions involved in the ternary.
   * The first is the 'control' and the other two are the 'if/else' values.
   * <br/>
   * The control must result in a Boolean.
   * The other two return type must have some super type in common.
   * This is what we are processing here.
   * <pre>
   *   &lt;assoc=right&gt; control=expression LEFT_ARROW left=expression (COLON|ELSE) right=expression
   * </pre>
   */
  private ISymbol processTernaryOrError(final EK9Parser.ExpressionContext ctx) {

    final var start = new Ek9Token(ctx.start);
    //First lets gather the 'expressions' because if they are not there then there's little we can do here.
    final var control = symbolFromContextOrError.apply(ctx.control);
    final var leftAndRight = toList.apply(accessLeftAndRightOrError.apply(ctx));

    if (control != null && !leftAndRight.isEmpty()) {
      //So do the checks, this will result in errors being emitted if the values are not acceptable.
      controlIsBooleanOrError.accept(ctx.control);
      final var commonType = commonTypeSuperOrTraitOrError.apply(new Ek9Token(ctx.LEFT_ARROW().getSymbol()),
          leftAndRight);
      if (commonType.isPresent()) {
        //We can make an expression that models this and the correct return type.
        return symbolFactory.newExpressionSymbol(start, ctx.getText(), commonType);
      }
    }

    return null;
  }

  /**
   * This covers the null coalescing and the 'Elvis' operator.
   * Basically just need to ensure types are compatible and that the is-set '?'
   * operator exists on that type.
   * <br/>
   * It will be up to the code generation part to generate the null check (for ??) and the
   * isSet code for both '??' and '?:'.
   */
  private ISymbol processCoalescingOrError(final EK9Parser.ExpressionContext ctx) {

    final var opToken = new Ek9Token(ctx.coalescing);

    return expressionForOperationOrError(opToken, isSetPresentOrError, ctx);
  }

  /**
   * This is the case where we have null safe equality coalescing test operators.
   * &lt;?, &lt;=?, &gt;? and &gt;=? - This idea of these is to be able to
   * check if one or both of the expressions is unset. If both are 'set' then
   * it is just a normal operation. But if either is unset then the set value is the
   * one used. This is useful because with normal equality operators
   * (&lt;, &lt;=, &gt; and &gt;=) if either side is unset then the resulting Boolean is unset.
   * <br/>
   * So this case is very useful when you want to accept just the set one or the one that
   * meets the test.
   * <br/> So the result here is NOT a Boolean but the value that is
   * 'less than', 'less than or equal to', 'greater than' or 'greater than or equal to'.
   * <br/>
   * In effect this is like an assignment, combined if/else, null check and equality check all in one.
   */
  private ISymbol processCoalescingEquality(final EK9Parser.ExpressionContext ctx) {

    final var opToken = new Ek9Token(ctx.coalescing_equality);

    return expressionForOperationOrError(opToken, comparatorPresentOrError, ctx);
  }

  private ISymbol expressionForOperationOrError(final IToken opToken, final BiPredicate<IToken, ISymbol> predicate,
                                                final EK9Parser.ExpressionContext ctx) {

    final var leftAndRight = toList.apply(accessLeftAndRightOrError.apply(ctx));


    if (!leftAndRight.isEmpty()) {
      final var commonType = commonTypeSuperOrTraitOrError.apply(opToken, leftAndRight);
      if (commonType.isPresent() && predicate.test(opToken, commonType.get())) {
        return symbolFactory.newExpressionSymbol(opToken, ctx.getText(), commonType);
      }
    }

    return null;
  }

  /**
   * Deals with both of these expressions.
   *
   * <pre>
   *   identifier neg=NOT? IN range
   *   left=expression IS? neg=NOT? IN right=expression
   * </pre>
   */
  private ISymbol processInCollectionOrRangeOrError(final EK9Parser.ExpressionContext ctx) {

    if (ctx.range() != null) {
      return withinRangeOrError(ctx);
    }

    return validContainsOrError(ctx);
  }

  private ISymbol withinRangeOrError(final EK9Parser.ExpressionContext ctx) {

    final var opToken = new Ek9Token(ctx.IN().getSymbol());
    final var expr = symbolFromContextOrError.apply(ctx.expression(0));
    final var range = symbolFromContextOrError.apply(ctx.range());

    if (expr != null && range != null && expr.getType().isPresent() && range.getType().isPresent()) {
      //This just comes down to the range type having a comparator that accepts the expr type
      final var search = new MethodSymbolSearch("<=>").addTypeParameter(expr.getType())
          .setOfTypeOrReturn(symbolsAndScopes.getEk9Types().ek9Integer());
      final var data = new CheckOperatorData(range, opToken, search);
      final var locatedReturningType = requiredOperatorPresentOrError.apply(data);
      if (locatedReturningType.isPresent()) {
        final var rtnType = Optional.of(symbolsAndScopes.getEk9Types().ek9Boolean());
        final var returnExpr =
            symbolFactory.newExpressionSymbol(data.operatorUseToken(), data.symbol().getName(), rtnType);
        return negationIfRequiredOrError(ctx, returnExpr);
      }
    }

    return null;
  }

  private ISymbol validContainsOrError(final EK9Parser.ExpressionContext ctx) {

    final var opToken = new Ek9Token(ctx.IN().getSymbol());
    final var leftAndRight = accessLeftAndRightOrError.apply(ctx);

    if (leftAndRight.isPresent()) {
      //This just comes down to right having the 'contains' that accepts the left type
      final var search = new MethodSymbolSearch("contains").addTypeParameter(leftAndRight.get().left().getType())
          .setOfTypeOrReturn(symbolsAndScopes.getEk9Types().ek9Boolean());
      return expressionFromOperatorDataOrError(ctx, new CheckOperatorData(leftAndRight.get().right(), opToken, search));
    }

    return null;
  }

  private ISymbol expressionFromOperatorDataOrError(final EK9Parser.ExpressionContext ctx,
                                                    final CheckOperatorData data) {

    final var locatedReturningType = requiredOperatorPresentOrError.apply(data);

    if (locatedReturningType.isPresent()) {
      var expr =
          symbolFactory.newExpressionSymbol(data.operatorUseToken(), data.symbol().getName(), locatedReturningType);
      return negationIfRequiredOrError(ctx, expr);
    }

    return null;
  }

  private ISymbol negationIfRequiredOrError(final EK9Parser.ExpressionContext ctx, final ISymbol expr) {

    //Mow there could be a negation inside the expression (to make the syntax nicer)
    if (ctx.neg != null) {
      return notOperationOrError(new Ek9Token(ctx.neg), expr);
    }

    return expr;
  }

  private ISymbol notOperationOrError(final IToken notOpToken, final ISymbol exprSymbol) {

    final var search =
        new MethodSymbolSearch("~").setOfTypeOrReturn(symbolsAndScopes.getEk9Types().ek9Boolean());
    final var located = requiredOperatorPresentOrError.apply(new CheckOperatorData(exprSymbol, notOpToken, search));

    if (located.isPresent()) {
      return symbolFactory.newExpressionSymbol(notOpToken, exprSymbol.getName(), located);
    }

    return null;
  }

  private ISymbol isSetOrError(final EK9Parser.ExpressionContext ctx) {

    final var expressionInQuestion = symbolFromContextOrError.apply(ctx.expression(0));
    final var opToken = new Ek9Token(ctx.op);

    if (isSetPresentOrError.test(opToken, expressionInQuestion)) {
      return symbolFactory.newExpressionSymbol(opToken, expressionInQuestion.getName())
          .setType(symbolsAndScopes.getEk9Types().ek9Boolean());
    }

    return null;
  }

  private void emitTypeNotResolvedError(final IToken lineToken, final ISymbol argument) {

    final var msg = "'" + argument.getName() + "' :";
    errorListener.semanticError(lineToken, msg, ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);

  }

  private void emitTypeNotAppropriateError(final IToken lineToken, final ISymbol argument) {

    final var msg = "'" + argument.getFriendlyName() + "' as '" + argument.getCategory() + "':";
    errorListener.semanticError(lineToken, msg, ErrorListener.SemanticClassification.INAPPROPRIATE_USE);

  }

}