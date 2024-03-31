package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.ParameterisedLocator;
import org.ek9lang.compiler.support.ParameterisedTypeData;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Creates/Updates an enumerated type adding methods as appropriate.
 */
final class ProcessEnumeratedType extends TypedSymbolAccess implements Consumer<EK9Parser.TypeDeclarationContext> {
  private final ParameterisedLocator parameterisedLocator;

  ProcessEnumeratedType(final SymbolAndScopeManagement symbolAndScopeManagement, final SymbolFactory symbolFactory,
                        final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.parameterisedLocator = new ParameterisedLocator(symbolAndScopeManagement, symbolFactory, errorListener, true);
  }

  @Override
  public void accept(final EK9Parser.TypeDeclarationContext ctx) {
    var startToken = new Ek9Token(ctx.start);

    final var enumeratedTypeSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);

    final var iteratorType = symbolAndScopeManagement.getEk9Types().ek9Iterator();

    final var typeData = new ParameterisedTypeData(startToken, iteratorType, List.of(enumeratedTypeSymbol));
    final var resolvedNewType = parameterisedLocator.resolveOrDefine(typeData);

    //TODO make iterator() method that return this type and add it to the enumeratedTypeSymbol
    System.out.println("Found [" + resolvedNewType + "]");
  }

}
