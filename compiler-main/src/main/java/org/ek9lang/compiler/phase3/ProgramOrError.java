package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Checks if there is any application associated with the program.
 * Then links it to the program.
 */
final class ProgramOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.MethodDeclarationContext> {

  ProgramOrError(final SymbolsAndScopes symbolsAndScopes,
                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.MethodDeclarationContext ctx) {
    final var program = getRecordedAndTypedSymbol(ctx);

    AssertValue.checkNotNull("Program must have been defined by this phase", program);

    if (ctx.APPLICATION() != null && ctx.identifierReference() != null) {
      final var application = getRecordedAndTypedSymbol(ctx.identifierReference());
      AssertValue.checkNotNull("Application must have been defined by this phase", application);

      if (program instanceof AggregateSymbol programAsAggregate
          && application instanceof AggregateSymbol applicationAsAggregate) {
        programAsAggregate.setApplication(applicationAsAggregate);
      } else {
        throw new CompilerException("Programs and Application must be aggregates");
      }
    }
  }
}
