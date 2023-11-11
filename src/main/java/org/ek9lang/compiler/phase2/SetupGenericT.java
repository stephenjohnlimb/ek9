package org.ek9lang.compiler.phase2;

import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.core.CompilerException;

/**
 * The Generic T used in parametric types will exist but won't have any methods on yet.
 * This is because it is only when we get to this stage can the possible constraining type
 * be resolved.
 * But also need to consider if it is a Function rather than an Aggregate.
 */
final class SetupGenericT implements Consumer<EK9Parser.ParameterisedDetailContext> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final AggregateFactory aggregateFactory;
  private final ErrorListener errorListener;

  SetupGenericT(final SymbolAndScopeManagement symbolAndScopeManagement,
                final AggregateFactory aggregateFactory,
                final ErrorListener errorListener) {

    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.aggregateFactory = aggregateFactory;
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ParameterisedDetailContext ctx) {
    var t = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (t instanceof IAggregateSymbol aggregateT) {

      //So it is not constrained an is just a 'T'
      if (ctx.typeDef() == null) {
        aggregateFactory.addAllSyntheticOperators(aggregateT);
      } else {
        getConstrainingTypeOrError(ctx.typeDef())
            .ifPresent(constrainingType -> aggregateFactory.updateToConstrainBy(aggregateT, constrainingType));
      }
    } else {
      //What about if 't' is actually a Function.
      throw new CompilerException("Failed to get [" + ctx.Identifier().getText() + "] not expecting this");
    }
  }

  private Optional<IAggregateSymbol> getConstrainingTypeOrError(final EK9Parser.TypeDefContext ctx) {
    var theConstrainingType = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (theConstrainingType instanceof IAggregateSymbol aggregate) {
      return Optional.of(aggregate);
    }
    errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    return Optional.empty();
  }
}
