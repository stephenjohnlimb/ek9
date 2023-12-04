package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_INITIALISED_BEFORE_USE;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Given an identifier, this consumer checks if it is a property of some aggregate.
 * If so it then process the safe access methods to check that a property when not initialised
 * during declaration can be safely accessed within a method body.
 */
public class ProcessIdentifierAsProperty extends TypedSymbolAccess implements Consumer<EK9Parser.IdentifierContext> {
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();
  private final MakesIdentifierSubsequenceAccessSafe makesIdentifierSubsequenceAccessSafe
      = new MakesIdentifierSubsequenceAccessSafe();

  protected ProcessIdentifierAsProperty(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.IdentifierContext ctx) {
    var symbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (symbol != null && symbol.isPropertyField()) {
      processSymbol(ctx, symbol);
    }
  }

  private void processSymbol(final EK9Parser.IdentifierContext ctx, final ISymbol symbol) {
    var scope = symbolAndScopeManagement.getTopScope();
    var accessSafe = makesIdentifierSubsequenceAccessSafe.test(ctx);
    if (accessSafe) {
      //See if this sort of access would be safe and if so mark it as such
      symbolAndScopeManagement.markSymbolAccessSafe(symbol, scope);
    } else if (!symbolAndScopeManagement.isSymbolAccessSafe(symbol, scope)) {
      emitError(ctx, symbol);
    }
  }

  private void emitError(final EK9Parser.IdentifierContext ctx, final ISymbol symbol) {

    errorListener.semanticError(ctx.start, "'" + symbol.getFriendlyName()
        + "' " + locationExtractorFromSymbol.apply(symbol)
        + ":", NOT_INITIALISED_BEFORE_USE);
  }

}
