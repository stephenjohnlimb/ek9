package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.core.CompilerException;

/**
 * Processes the Object Access start, this is the start of chained method/function calls.
 * Emits errors if resolution fails or if types are not resolved.
 */
final class ProcessObjectAccessStartOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.ObjectAccessStartContext> {
  private final ProcessIdentifierOrError processIdentifierOrError;

  ProcessObjectAccessStartOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.processIdentifierOrError = new ProcessIdentifierOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.ObjectAccessStartContext ctx) {
    //primaryReference | identifier | call
    if (ctx.primaryReference() != null) {
      var resolved = getRecordedAndTypedSymbol(ctx.primaryReference());
      if (resolved != null) {
        recordATypedSymbol(resolved, ctx);
      }
    } else if (ctx.identifier() != null) {
      //In this context we can resolve the identifier and record it. - its not an identifierReference but an identifier.
      //This is done to find the compromise of reuse in the grammar. But makes this a bit more inconsistent.

      var resolved = processIdentifierOrError.apply(ctx.identifier());

      if (resolved != null) {
        //Record against both
        resolved.setReferenced(true);
        recordATypedSymbol(resolved, ctx.identifier());
        recordATypedSymbol(resolved, ctx);
      }
    } else if (ctx.call() != null) {
      var resolved = getRecordedAndTypedSymbol(ctx.call());
      if (resolved != null) {
        recordATypedSymbol(resolved, ctx);
      } else {
        throw new CompilerException("ctx call is not recorded " + ctx.call().getText());
      }
    }
  }
}
