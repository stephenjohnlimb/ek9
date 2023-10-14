package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
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
final class CheckValidExpression extends RuleSupport implements Consumer<EK9Parser.ExpressionContext> {

  private final SymbolFactory symbolFactory;
  private final CheckIsSet checkIsSet;
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
      throw new CompilerException(
          "TODO: implement 'coalescing' expression " + ctx.start + " text [" + ctx.getText() + "]");
    } else if (ctx.coalescing_equality != null) {
      throw new CompilerException(
          "TODO: implement 'coalescing_equality' expression " + ctx.start + " text [" + ctx.getText() + "]");
    } else if (ctx.primary() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.primary());
    } else if (ctx.call() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.call());
    } else if (ctx.objectAccessExpression() != null) {
      var maybeResolved = symbolAndScopeManagement.getRecordedSymbol(ctx.objectAccessExpression());
      if (maybeResolved != null && maybeResolved.getType().isPresent()) {
        return symbolFactory.newExpressionSymbol(startToken, ctx.getText()).setType(maybeResolved.getType());
      }
      return symbolFactory.newExpressionSymbol(startToken, ctx.getText());
    } else if (ctx.list() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.list());
    } else if (ctx.dict() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.dict());
    } else if (ctx.IN() != null) {
      throw new CompilerException("TODO: implement both in expressions " + ctx.start + " text [" + ctx.getText() + "]");
    } else if (ctx.control != null) {
      return checkTernary(ctx);
    } else if (ctx.expression() != null && !ctx.expression().isEmpty()) {
      throw new CompilerException("Expecting to remove this line " + ctx.start + " text [" + ctx.getText() + "]");
    }
    else {
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
      var symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.expression(0));
      var located = checkForOperator.apply(new CheckOperatorData(symbol, new Ek9Token(ctx.op), search));
      if (located.isPresent()) {
        var expr = symbolFactory.newExpressionSymbol(symbol).setType(located);
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
    var control = symbolAndScopeManagement.getRecordedSymbol(ctx.control);
    var left = symbolAndScopeManagement.getRecordedSymbol(ctx.left);
    var right = symbolAndScopeManagement.getRecordedSymbol(ctx.right);

    //So do the checks, this will result in errors being emitted if the values are not acceptable.
    checkTypeIsBoolean.accept(start, control);
    var commonType = commonTypeSuperOrTrait.apply(new Ek9Token(ctx.LEFT_ARROW().getSymbol()), List.of(left, right));
    if (commonType.isPresent()) {
      //We can make an expression that models this and the correct return type.
      return symbolFactory.newExpressionSymbol(start, ctx.getText(), commonType);
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
    var expressionInQuestion = symbolAndScopeManagement.getRecordedSymbol(ctx.expression(0));
    if (checkIsSet.test(new Ek9Token(ctx.op), expressionInQuestion)) {
      return symbolFactory.newExpressionSymbol(expressionInQuestion)
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