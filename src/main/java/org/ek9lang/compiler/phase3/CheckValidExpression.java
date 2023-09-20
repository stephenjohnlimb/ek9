package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Ensures that 'expression' is now resolved and 'typed' or a not resolved error.
 * This is a beast and will have to delegate parts, as there are just some many types of expression.
 * TODO lot's of splitting of this logic up, because this is the 'beast'.
 */
final class CheckValidExpression extends RuleSupport implements Consumer<EK9Parser.ExpressionContext> {

  private final SymbolFactory symbolFactory;

  private final CheckIsSet checkIsSet;

  /**
   * Check Primary resolves and attempt to 'type' it.
   */
  CheckValidExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final SymbolFactory symbolFactory,
                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;
    this.checkIsSet = new CheckIsSet(symbolAndScopeManagement, errorListener);
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
    if (ctx.QUESTION() != null) {
      return checkAndProcessIsSet(ctx);
    } else if (ctx.primary() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.primary());
    } else if (ctx.call() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.call());
    } else if (ctx.list() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.list());
    } else if (ctx.dict() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.dict());
    } else if (ctx.expression() != null && !ctx.expression().isEmpty()) {
      //TODO implement this.
      System.out.println("Expression to expression(s) next - but need to work out the type");
      return symbolFactory.newExpressionSymbol(startToken, ctx.getText(),
          symbolAndScopeManagement.getTopScope().resolve(
              new TypeSymbolSearch("Boolean")));
    } else if (ctx.objectAccessExpression() != null) {
      var maybeResolved = symbolAndScopeManagement.getRecordedSymbol(ctx.objectAccessExpression());
      if (maybeResolved != null && maybeResolved.getType().isPresent()) {
        return symbolFactory.newExpressionSymbol(startToken, ctx.getText()).setType(maybeResolved.getType());
      }
      return symbolFactory.newExpressionSymbol(startToken, ctx.getText());
    } else {
      AssertValue.fail(
          "Expecting finite set of operations for expression [" + ctx.getText() + "] line: " + ctx.start.getLine());
    }
    return null;
  }

  private ISymbol checkAndProcessIsSet(final EK9Parser.ExpressionContext ctx) {
    var expressionInQuestion = symbolAndScopeManagement.getRecordedSymbol(ctx.expression(0));
    if (checkIsSet.test(expressionInQuestion)) {
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