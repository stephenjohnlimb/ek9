package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Processes the Object Access start, this is the start of chained method/function calls.
 * Emits errors if resolution fails or if types are not resolved.
 * <pre>
 *   objectAccessStart
 *     :  (primaryReference | identifier | call)
 * </pre>
 */
final class ProcessObjectAccessStartOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.ObjectAccessStartContext> {
  private final ProcessIdentifierOrError processIdentifierOrError;
  private final EnumerationLocator enumerationLocator;

  ProcessObjectAccessStartOrError(final SymbolsAndScopes symbolsAndScopes,
                                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.processIdentifierOrError = new ProcessIdentifierOrError(symbolsAndScopes, errorListener);
    this.enumerationLocator = new EnumerationLocator(symbolsAndScopes);

  }

  @Override
  public void accept(final EK9Parser.ObjectAccessStartContext ctx) {

    //primaryReference | identifier | call
    if (ctx.primaryReference() != null) {
      processByPrimaryReference(ctx);
    } else if (ctx.identifier() != null) {
      processByIdentifier(ctx);
    } else if (ctx.call() != null) {
      processByCall(ctx);
    } else {
      throw new CompilerException("Expecting finite set of operations 'primaryReference | identifier | call'");
    }

  }

  private void processByPrimaryReference(final EK9Parser.ObjectAccessStartContext ctx) {

    final var resolved = getRecordedAndTypedSymbol(ctx.primaryReference());
    if (resolved != null) {
      recordATypedSymbol(resolved, ctx);
    }

  }

  private void processByIdentifier(final EK9Parser.ObjectAccessStartContext ctx) {

    //In this context we can resolve the identifier and record it. - its not an identifierReference but an identifier.
    //This is done to find the compromise of reuse in the grammar. But makes this a bit more inconsistent.
    //But it means that we need some more complex logic here to check the identifier reference use.
    final var resolved = resolveByIdentifier(ctx.identifier());

    if (resolved != null) {
      //Record against both
      resolved.setReferenced(true);
      recordATypedSymbol(resolved, ctx.identifier());
      recordATypedSymbol(resolved, ctx);
    }

  }

  private void processByCall(final EK9Parser.ObjectAccessStartContext ctx) {

    final var resolved = getRecordedAndTypedSymbol(ctx.call());
    if (resolved != null) {
      recordATypedSymbol(resolved, ctx);
    }

  }

  private ISymbol resolveByIdentifier(final EK9Parser.IdentifierContext ctx) {

    final var possibleEnumeratedType = enumerationLocator.apply(ctx.getText());
    return possibleEnumeratedType.orElseGet(() -> processIdentifierOrError.apply(ctx));
  }

}
