package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DEFAULT_AND_TRAIT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DEFAULT_WITH_OPERATOR_SIGNATURE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.OPERATOR_DEFAULT_NOT_SUPPORTED;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Deals with normal operators (i.e. fully defined) and also 'default' operators.
 * The use of 'default' maybe inappropriate by the EK9 developer this code will check for that
 * and issue errors as appropriate.
 * <br/>
 * Note, we allow duplicate operators to be defined, but later phase will detect those.
 */
class CheckAndPopulateOperator
    implements BiFunction<EK9Parser.OperatorDeclarationContext, IAggregateSymbol, MethodSymbol> {

  final AggregateManipulator aggregateManipulator;
  final ErrorListener errorListener;

  final Function<IAggregateSymbol, String> messageFor
      = aggregate -> "wrt to type: '" + aggregate.getFriendlyName() + "':";

  CheckAndPopulateOperator(final AggregateManipulator aggregateManipulator,
                           final ErrorListener errorListener) {

    this.aggregateManipulator = aggregateManipulator;
    this.errorListener = errorListener;

  }

  @Override
  public MethodSymbol apply(final EK9Parser.OperatorDeclarationContext ctx, final IAggregateSymbol aggregate) {

    final var startToken = new Ek9Token(ctx.operator().start);

    return (ctx.DEFAULT() != null)
        ? createDefaultOperator(ctx, startToken, aggregate)
        : createNormalOperator(ctx, startToken, aggregate);
  }

  private MethodSymbol createDefaultOperator(final EK9Parser.OperatorDeclarationContext ctx,
                                             final IToken startToken,
                                             final IAggregateSymbol aggregate) {

    if (aggregate.getGenus().equals(SymbolGenus.CLASS_TRAIT)) {
      emitDefaultAndTraitError(startToken, aggregate);
      return null;
    }

    if (ctx.AS() != null || ctx.PURE() != null || ctx.ABSTRACT() != null || ctx.operationDetails() != null) {
      //Then some form of signature has been supplied, demonstration a lack of understanding of the use
      //of default in terms of operators. So this is an error.
      emitDefaultOperatorWithSignature(startToken, aggregate);
      return null;
    }

    //Now this can come back empty - meaning it is not an operator that can be defaulted.
    final var defaultOperator = aggregateManipulator.getDefaultOperator(aggregate, ctx.operator().getText());
    if (defaultOperator.isEmpty()) {
      emitOperatorDoesNotSupportDefault(startToken, aggregate);
      return null;
    }

    //Looks like all is going to be OK.
    final var operator = defaultOperator.get();
    operator.setSourceToken(startToken);
    operator.setInitialisedBy(startToken);

    return operator;

  }


  private MethodSymbol createNormalOperator(final EK9Parser.OperatorDeclarationContext ctx,
                                            final IToken startToken,
                                            final IAggregateSymbol aggregate) {

    //For operators with arguments, i.e. <, >, <>, etc. the developer really wants to test based on the
    //actual type being provided not some super. So we use the same dispatcher mechanism in classes that the ek9
    //developer can express. But here we do it behind the scenes.
    final var hasArguments = ctx.operationDetails() != null && ctx.operationDetails().argumentParam() != null;
    final var operator = new MethodSymbol(ctx.operator().getText(), aggregate);

    operator.setSourceToken(startToken);
    operator.setInitialisedBy(startToken);
    operator.setMarkedAsDispatcher(hasArguments);
    operator.setOverride(ctx.OVERRIDE() != null);
    operator.setMarkedPure(ctx.PURE() != null);
    operator.setMarkedAbstract(ctx.ABSTRACT() != null);
    operator.setOperator(true);
    //Set as Void, unless we have a returning section - processed later.
    operator.setType(aggregateManipulator.resolveVoid(aggregate));

    return operator;
  }

  private void emitDefaultAndTraitError(final IToken startToken,
                                        final IAggregateSymbol aggregate) {

    errorListener.semanticError(startToken, messageFor.apply(aggregate), DEFAULT_AND_TRAIT);

  }

  private void emitOperatorDoesNotSupportDefault(final IToken startToken,
                                                 final IAggregateSymbol aggregate) {

    errorListener.semanticError(startToken, messageFor.apply(aggregate), OPERATOR_DEFAULT_NOT_SUPPORTED);

  }

  private void emitDefaultOperatorWithSignature(final IToken startToken,
                                                final IAggregateSymbol aggregate) {

    errorListener.semanticError(startToken, messageFor.apply(aggregate), DEFAULT_WITH_OPERATOR_SIGNATURE);

  }
}
