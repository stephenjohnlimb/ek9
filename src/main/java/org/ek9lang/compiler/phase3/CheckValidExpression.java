package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.CommonTypeSuperOrTrait;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Ensures that 'expression' is now resolved and 'typed' or a not resolved error.
 * This is a beast and will have to delegate parts, as there are just some many types of expression.
 * TODO lot's of splitting of this logic up, because this is the 'beast'.
 */
final class CheckValidExpression extends TypedSymbolAccess implements Consumer<EK9Parser.ExpressionContext> {

  private final SymbolFactory symbolFactory;
  private final CheckIsSet checkIsSet;
  private final CheckForComparator checkForComparator;
  private final CheckForOperator checkForOperator;
  private final MethodSymbolSearchForExpression methodSymbolSearchForExpression;
  private final CheckTypeIsBoolean checkTypeIsBoolean;
  private final CommonTypeSuperOrTrait commonTypeSuperOrTrait;

  /**
   * Check Primary resolves and attempt to 'type' it.
   */
  CheckValidExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final SymbolFactory symbolFactory,
                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;
    this.checkIsSet = new CheckIsSet(symbolAndScopeManagement, errorListener);
    this.checkForComparator = new CheckForComparator(symbolAndScopeManagement, errorListener);
    this.checkForOperator = new CheckForOperator(symbolAndScopeManagement, errorListener);
    this.methodSymbolSearchForExpression = new MethodSymbolSearchForExpression(symbolAndScopeManagement, errorListener);
    this.checkTypeIsBoolean = new CheckTypeIsBoolean(symbolAndScopeManagement, errorListener);
    this.commonTypeSuperOrTrait = new CommonTypeSuperOrTrait(errorListener);
  }

  @Override
  public void accept(final EK9Parser.ExpressionContext ctx) {
    var symbol = determineSymbolToRecord(ctx);
    if (symbol != null) {
      symbolAndScopeManagement.recordSymbol(symbol, ctx);
      if (symbol.getType().isEmpty()) {
        emitTypeNotResolvedError(new Ek9Token(ctx.start), symbol);
      }
    }
  }

  /**
   * TODO this will need to be pulled out to separate methods because it will be too much to grok in one go.
   */
  private ISymbol determineSymbolToRecord(final EK9Parser.ExpressionContext ctx) {
    var startToken = new Ek9Token(ctx.start);

    //The idea here is that rather than have a giant if else combo, the process is grouped.
    //So we get a big bang just by using 'op' as these are all just operators on types.
    //I've tried to get these in some reasonable order. With a combination of code coverage and
    //real examples we should be sure to get all the combinations.

    if (ctx.op != null) {
      //This deals with quite a few states - basically anything with an operator with 1 or two expressions.
      return checkOperation(ctx);
    } else if (ctx.ONLY() != null) {
      throw new CompilerException("TODO: implement 'only' expression " + ctx.start + " text [" + ctx.getText() + "]");
    } else if (ctx.coalescing != null) {
      return checkCoalescing(ctx);
    } else if (ctx.coalescing_equality != null) {
      return checkCoalescingEquality(ctx);
    } else if (ctx.primary() != null) {
      return getRecordedAndTypedSymbol(ctx.primary());
    } else if (ctx.call() != null) {
      return getRecordedAndTypedSymbol(ctx.call());
    } else if (ctx.objectAccessExpression() != null) {
      var maybeResolved = getRecordedAndTypedSymbol(ctx.objectAccessExpression());
      if (maybeResolved != null && maybeResolved.getType().isPresent()) {
        return symbolFactory.newExpressionSymbol(startToken, ctx.getText()).setType(maybeResolved.getType());
      }
      return symbolFactory.newExpressionSymbol(startToken, ctx.getText());
    } else if (ctx.list() != null) {
      return getRecordedAndTypedSymbol(ctx.list());
    } else if (ctx.dict() != null) {
      return getRecordedAndTypedSymbol(ctx.dict());
    } else if (ctx.IN() != null) {
      throw new CompilerException("TODO: implement both in expressions " + ctx.start + " text [" + ctx.getText() + "]");
    } else if (ctx.control != null) {
      return checkTernary(ctx);
    } else if (ctx.expression() != null && !ctx.expression().isEmpty()) {
      throw new CompilerException("Expecting to remove this line " + ctx.start + " text [" + ctx.getText() + "]");
    } else {
      AssertValue.fail(
          "Expecting finite set of operations for expression [" + ctx.getText() + "] line: " + ctx.start.getLine());
    }
    return null;
  }

  private ISymbol checkOperation(final EK9Parser.ExpressionContext ctx) {
    //Special case for isSet because it can be used against a function delegate as well.
    if (ctx.QUESTION() != null) {
      return checkAndProcessIsSet(ctx);
    } else if (!ctx.expression().isEmpty()) {
      //Could be one expression (unary) or have two expressions.
      //This case only looks for operators on some form of aggregate.
      var search = methodSymbolSearchForExpression.apply(ctx);
      var symbol = getRecordedAndTypedSymbol(ctx.expression(0));
      var opToken = new Ek9Token(ctx.op);
      var located = checkForOperator.apply(new CheckOperatorData(symbol, opToken, search));
      if (located.isPresent()) {
        var expr = symbolFactory.newExpressionSymbol(opToken, symbol.getName(), located);
        //Mow there could be a negation inside the expression (to make the syntax nicer)
        if (ctx.neg != null) {
          return checkAndProcessNotOperation(new Ek9Token(ctx.neg), expr);
        }
        return expr;
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
  private ISymbol checkTernary(final EK9Parser.ExpressionContext ctx) {
    var start = new Ek9Token(ctx.start);

    //First lets gather the 'expressions' because if they are not there then there's little we can do here.
    var control = getRecordedAndTypedSymbol(ctx.control);
    var left = getRecordedAndTypedSymbol(ctx.left);
    var right = getRecordedAndTypedSymbol(ctx.right);

    if (control != null && left != null && right != null) {
      //So do the checks, this will result in errors being emitted if the values are not acceptable.
      checkTypeIsBoolean.accept(start, control);
      var commonType = commonTypeSuperOrTrait.apply(new Ek9Token(ctx.LEFT_ARROW().getSymbol()), List.of(left, right));
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
  private ISymbol checkCoalescing(final EK9Parser.ExpressionContext ctx) {
    var opToken = new Ek9Token(ctx.coalescing);

    var left = getRecordedAndTypedSymbol(ctx.left);
    var right = getRecordedAndTypedSymbol(ctx.right);
    if (left != null && right != null) {
      var commonType = commonTypeSuperOrTrait.apply(opToken, List.of(left, right));
      if (commonType.isPresent() && checkIsSet.test(opToken, commonType.get())) {
        return symbolFactory.newExpressionSymbol(opToken, ctx.getText(), commonType);
      }
    }
    return null;
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
  private ISymbol checkCoalescingEquality(EK9Parser.ExpressionContext ctx) {
    var opToken = new Ek9Token(ctx.coalescing_equality);
    var left = getRecordedAndTypedSymbol(ctx.left);
    var right = getRecordedAndTypedSymbol(ctx.right);
    if (left != null && right != null) {
      var commonType = commonTypeSuperOrTrait.apply(opToken, List.of(left, right));
      if (commonType.isPresent() && checkForComparator.test(opToken, commonType.get())) {
        return symbolFactory.newExpressionSymbol(opToken, ctx.getText(), commonType);
      }
    }
    return null;
  }

  private ISymbol checkAndProcessNotOperation(final IToken notOpToken, final ISymbol exprSymbol) {
    var search = new MethodSymbolSearch("~")
        .setOfTypeOrReturn(symbolAndScopeManagement.getEk9Types().ek9Boolean());
    var located = checkForOperator.apply(new CheckOperatorData(exprSymbol, notOpToken, search));
    if (located.isPresent()) {
      return symbolFactory.newExpressionSymbol(notOpToken, exprSymbol.getName(), located);
    }
    return null;
  }

  private ISymbol checkAndProcessIsSet(final EK9Parser.ExpressionContext ctx) {
    var expressionInQuestion = getRecordedAndTypedSymbol(ctx.expression(0));
    var opToken = new Ek9Token(ctx.op);
    if (checkIsSet.test(opToken, expressionInQuestion)) {
      return symbolFactory.newExpressionSymbol(opToken, expressionInQuestion.getName())
          .setType(symbolAndScopeManagement.getEk9Types().ek9Boolean());
    }

    return null;
  }

  private void emitTypeNotResolvedError(final IToken lineToken, final ISymbol argument) {
    var msg = "'" + argument.getName() + "' :";
    errorListener.semanticError(lineToken, msg,
        ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
  }
}