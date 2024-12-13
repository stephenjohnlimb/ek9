package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Creates/Updates an enumerated type adding methods as appropriate.
 */
final class EnumeratedTypeOrError extends TypedSymbolAccess implements Consumer<EK9Parser.TypeDeclarationContext> {
  private final ParameterisedLocator parameterisedLocator;
  private final AggregateFactory aggregateFactory;

  EnumeratedTypeOrError(final SymbolsAndScopes symbolsAndScopes,
                        final SymbolFactory symbolFactory,
                        final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.parameterisedLocator = new ParameterisedLocator(symbolsAndScopes, symbolFactory, errorListener, true);
    this.aggregateFactory = symbolFactory.getAggregateFactory();

  }

  @Override
  public void accept(final EK9Parser.TypeDeclarationContext ctx) {

    //This only deals with enumerations.
    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateSymbol enumeration
        && enumeration.getGenus().equals(SymbolGenus.CLASS_ENUMERATION)) {

      final var startToken = new Ek9Token(ctx.start);
      final var iteratorType = symbolsAndScopes.getEk9Types().ek9Iterator();
      final var typeData = new ParameterisedTypeData(startToken, iteratorType, List.of(enumeration));
      final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);
      aggregateFactory.addPublicMethod(enumeration, "iterator", List.of(), resolvedNewType);
    }
  }

}
