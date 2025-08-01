package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Only triggers the call the retrieve the appropriate symbol for the node.
 * But the call it uses expects a symbol to exist for that not, and it also expects it to have a type.
 * This is exactly what we want. So this can be used with any node that should have a type associated with it.
 * The obvious ones are variables, but also expressions need to have a type, as do methods and functions.
 * Even if those types are 'Void'.
 * <p>
 * Actually by this stage in the compilation process any failure here really indicates a defect in the compiler
 * but lets see if there some situations I've not yet catered for - before throwing exceptions.
 * </p>
 */
final class SymbolHasTypeOrError extends TypedSymbolAccess implements Consumer<ParseTree> {

  SymbolHasTypeOrError(final SymbolsAndScopes symbolsAndScopes,
                       final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final ParseTree node) {
    super.getRecordedAndTypedSymbol(node);
  }
}
