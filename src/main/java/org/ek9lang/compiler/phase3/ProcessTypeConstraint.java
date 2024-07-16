package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
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

  ProcessTypeConstraint(final SymbolsAndScopes symbolsAndScopes,
                        final SymbolFactory symbolFactory,
                        final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFactory = symbolFactory;

  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol,
                     final EK9Parser.ConstrainDeclarationContext ctx) {

    final var possibleExpression = processConstrainType(aggregateSymbol, ctx.constrainType());
    possibleExpression.ifPresent(expression -> recordATypedSymbol(expression, ctx));

  }

  //Note this can be called recursively.

  private Optional<ISymbol> getConstrainType(final AggregateSymbol aggregateSymbol,
                                             final EK9Parser.ConstrainTypeContext ctx) {

    if (ctx.literal() != null) {
      return processLiteralConstrainType(aggregateSymbol, ctx);
    }

    if (ctx.constrainType().size() == 1) {
      return processConstrainType(aggregateSymbol, ctx.constrainType(0));
    }

    return processConstrainType(aggregateSymbol, ctx.constrainType(0), new Ek9Token(ctx.op), ctx.constrainType(1));
  }

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
    final Optional<ISymbol> rtn = getConstrainType(aggregateSymbol, ctx);
    rtn.ifPresent(expression -> recordATypedSymbol(expression, ctx));

    return rtn;
  }


  private Optional<ISymbol> processConstrainType(final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.ConstrainTypeContext ctx1,
                                                 final IToken operationToken,
                                                 final EK9Parser.ConstrainTypeContext ctx2) {

    final var first = processConstrainType(aggregateSymbol, ctx1);
    final var second = processConstrainType(aggregateSymbol, ctx2);

    if (first.isPresent() && second.isPresent()) {
      final var name = ctx1.getText() + operationToken.getText() + ctx2.getText();
      final var expression = symbolFactory.newExpressionSymbol(operationToken, name, first.get().getType());

      return Optional.of(expression);
    }

    return Optional.empty();
  }

  private Optional<ISymbol> processLiteralConstrainType(final AggregateSymbol aggregateSymbol,
                                                        final EK9Parser.ConstrainTypeContext ctx) {

    final var operator = getOrAssumeOperator(ctx.op);
    final var literalSymbol = getRecordedAndTypedSymbol(ctx.literal());

    if (literalSymbol != null) {
      final var search = new MethodSymbolSearch(operator).addTypeParameter(literalSymbol.getType());
      return resolveOrError(aggregateSymbol, search, literalSymbol.getSourceToken());
    }

    return Optional.empty();
  }

  /**
   * If not provided then we assume '==', but it the EK9 developer has used '!=' then we must adapt to
   * '&lt;&gt;' otherwise use the operator the developer supplied.
   */
  private String getOrAssumeOperator(final Token token) {

    if (token == null) {
      return "==";
    }
    if ("!=".equals(token.getText())) {
      return "<>";
    }

    return token.getText();
  }

  private Optional<ISymbol> resolveOrError(final AggregateSymbol aggregateSymbol,
                                           final MethodSymbolSearch search,
                                           final IToken errorLocation) {

    final var results = aggregateSymbol.resolveMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult());

    if (results.getSingleBestMatchSymbol().isPresent()) {
      return Optional.of(symbolFactory.newExpressionSymbol(results.getSingleBestMatchSymbol().get()));
    }

    final var msgStart = "Looking on '" + aggregateSymbol.getFriendlyName() + "' for operator ";
    if (results.isAmbiguous()) {
      emitAmbiguousResolutionError(errorLocation, msgStart, search, results);
    } else if (results.isEmpty()) {
      emitMethodNotResolvedError(errorLocation, msgStart, search);
    }

    return Optional.empty();
  }

  private void emitAmbiguousResolutionError(final IToken errorLocation,
                                            final String msgStart,
                                            final MethodSymbolSearch search,
                                            final MethodSymbolSearchResult results) {

    final var msg = msgStart + "'"
        + search.toString()
        + "' resolved: "
        + results.getAmbiguousMethodParameters();
    errorListener.semanticError(errorLocation, msg, ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);

  }

  private void emitMethodNotResolvedError(final IToken errorLocation,
                                          final String msgStart,
                                          final MethodSymbolSearch search) {

    var msg = msgStart + "'" + search.toString() + "':";
    errorListener.semanticError(errorLocation, msg, ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED);

  }
}
