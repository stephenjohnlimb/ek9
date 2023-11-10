package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks that any recursive type constraints on a type are valid.
 * Note that this consumer does have side effects in terms of recording expressions against nodes.
 */
final class ProcessTypeConstraint extends TypedSymbolAccess
    implements BiConsumer<AggregateSymbol, EK9Parser.ConstrainDeclarationContext> {
  private final SymbolFactory symbolFactory;

  ProcessTypeConstraint(final SymbolAndScopeManagement symbolAndScopeManagement,
                        final SymbolFactory symbolFactory,
                        final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol,
                     final EK9Parser.ConstrainDeclarationContext ctx) {

    var possibleExpression = processConstrainType(aggregateSymbol, ctx.constrainType());
    possibleExpression.ifPresent(expression -> recordATypedSymbol(expression, ctx));
  }

  //Note this can be called recursively.

  /**
   * We are processing this.
   * <pre>
   *   constrainType
   *     : op=(GT | GE | LT | LE | EQUAL | NOTEQUAL | NOTEQUAL2 | MATCHES | CONTAINS)? literal
   *     | constrainType op=(AND | OR) constrainType
   *     | LPAREN constrainType RPAREN
   *     ;
   * </pre>
   */
  private Optional<ISymbol> processConstrainType(final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.ConstrainTypeContext ctx) {
    //At this point we do not expect any expression to have been recorded against this node.
    Optional<ISymbol> rtn;
    if (ctx.literal() == null) {
      if (ctx.constrainType().size() == 1) {
        rtn = processConstrainType(aggregateSymbol, ctx.constrainType(0));
      } else {
        rtn = processConstrainType(aggregateSymbol, ctx.constrainType(0), new Ek9Token(ctx.op), ctx.constrainType(1));
      }
      //Then it is either an 'and,or' or a grouped version
    } else {
      rtn = processLiteralConstrainType(aggregateSymbol, ctx);
    }
    rtn.ifPresent(expression -> recordATypedSymbol(expression, ctx));
    return rtn;
  }

  private Optional<ISymbol> processConstrainType(final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.ConstrainTypeContext ctx1,
                                                 final IToken operationToken,
                                                 final EK9Parser.ConstrainTypeContext ctx2) {
    var first = processConstrainType(aggregateSymbol, ctx1);
    var second = processConstrainType(aggregateSymbol, ctx2);
    if (first.isPresent() && second.isPresent()) {
      var name = ctx1.getText() + operationToken.getText() + ctx2.getText();
      var expression = symbolFactory.newExpressionSymbol(operationToken, name, first.get().getType());
      return Optional.of(expression);
    }
    return Optional.empty();
  }

  private Optional<ISymbol> processLiteralConstrainType(final AggregateSymbol aggregateSymbol,
                                                        final EK9Parser.ConstrainTypeContext ctx) {
    //If operator is not specified we assume ==
    var operator = "==";

    //But if it is specified we use it but have to accommodate != and <> meaning not equals.
    if (ctx.op != null) {
      operator = ctx.op.getText();
      if ("!=".equals(operator)) {
        operator = "<>";
      }
    }
    var literalSymbol = getRecordedAndTypedSymbol(ctx.literal());
    if (literalSymbol != null) {
      var search = new MethodSymbolSearch(operator).addTypeParameter(literalSymbol.getType());
      return resolveOrError(aggregateSymbol, search, literalSymbol.getSourceToken());
    }
    return Optional.empty();
  }

  private Optional<ISymbol> resolveOrError(final AggregateSymbol aggregateSymbol,
                                           final MethodSymbolSearch search,
                                           final IToken token) {

    var msgStart = "Looking on '" + aggregateSymbol.getFriendlyName() + "' for operator ";
    var results = aggregateSymbol.resolveMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult());
    if (results.isAmbiguous()) {
      var msg = msgStart + "'"
          + search.toString()
          + "' resolved: "
          + results.getAmbiguousMethodParameters();
      errorListener.semanticError(token, msg, ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
    } else if (results.isEmpty()) {
      var msg = msgStart + "'" + search.toString() + "':";
      errorListener.semanticError(token, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);
    } else if (results.getSingleBestMatchSymbol().isPresent()) {
      return Optional.of(symbolFactory.newExpressionSymbol(results.getSingleBestMatchSymbol().get()));
    }
    return Optional.empty();
  }
}
