package org.ek9lang.compiler.phase2;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

final class ProcessTypeDeclarationOrError extends RuleSupport
    implements Consumer<EK9Parser.TypeDeclarationContext> {

  private final PopulateConstrainedTypeOrError populateConstrainedTypeOrError;
  private final AggregateFactory aggregateFactory;

  ProcessTypeDeclarationOrError(final SymbolsAndScopes symbolsAndScopes,
                                final AggregateFactory aggregateFactory,
                                final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.aggregateFactory = aggregateFactory;
    this.populateConstrainedTypeOrError =
        new PopulateConstrainedTypeOrError(symbolsAndScopes, aggregateFactory, errorListener);
  }

  @Override
  public void accept(EK9Parser.TypeDeclarationContext ctx) {
    final var aggregateSymbol = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    //Might be null if name if the name is duplicated.
    if (aggregateSymbol != null) {
      aggregateFactory.addSyntheticConstructorIfRequired(aggregateSymbol);
      aggregateFactory.addConstructor(aggregateSymbol, new VariableSymbol("arg", aggregateSymbol));
      if (ctx.typeDef() == null) {
        //For an enumeration we allow creation via String.
        final var constructor =
            aggregateFactory.addConstructor(aggregateSymbol,
                new VariableSymbol("arg", aggregateFactory.getEk9Types().ek9String()));
        constructor.setMarkedPure(true);
      } else {
        final var theConstrainedType = symbolsAndScopes.getRecordedSymbol(ctx.typeDef());
        populateConstrainedTypeOrError.accept(aggregateSymbol, theConstrainedType);
        //else we should already get an error for this missing type.
      }
    }
  }
}
