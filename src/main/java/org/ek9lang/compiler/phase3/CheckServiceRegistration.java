package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks the use of registering a service is valid and then ensures that the 'Application'
 * is altered from being a general application to a service application.
 * This has then saved the EK9 developer declaring this - as it is obvious if they have registered a service
 * that the application is a service application.
 */
final class CheckServiceRegistration extends TypedSymbolAccess implements Consumer<EK9Parser.RegisterStatementContext> {
  CheckServiceRegistration(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.RegisterStatementContext ctx) {

    if (ctx.identifierReference() != null) {
      return;
    }

    //If there are any services registered then the application genus is modified from
    //being a GENERAL_APPLICATION to a SERVICE_APPLICATION.
    final var application = getRecordedAndTypedSymbol(ctx.parent);
    final var call = getRecordedAndTypedSymbol(ctx.call());

    //So only checking here for a service, previous phase will have checked other identifierReference category.
    if (application != null && call != null && call.getType().isPresent()) {

      final var genusOfPossibleService = call.getType().get().getGenus();

      if (genusOfPossibleService.equals(ISymbol.SymbolGenus.SERVICE)) {
        application.setGenus(ISymbol.SymbolGenus.SERVICE_APPLICATION);
      } else {
        final var msg = "'" + call.getFriendlyName() + "':";
        errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INCOMPATIBLE_GENUS);
      }
    }

  }
}
