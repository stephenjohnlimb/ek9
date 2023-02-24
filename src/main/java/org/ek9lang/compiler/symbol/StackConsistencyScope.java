package org.ek9lang.compiler.symbol;

import java.util.Optional;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Used when duplicate symbols are found, where we need to add a placeholder.
 * So the result of the listening and processing can take place, symbols can still be
 * put into some sort of scope; so they don't mess up other valid scopes.
 * The phase will fail compilation with duplicate errors, but we will have ensured that
 * other symbols and processing has been put somewhere.
 */
public class StackConsistencyScope extends LocalScope implements ICanCaptureVariables {

  private Optional<LocalScope> capturedVariables = Optional.empty();

  public StackConsistencyScope(IScope enclosingScope) {
    super("StackConsistencyScope", enclosingScope);
  }

  @Override
  public Optional<LocalScope> getCapturedVariables() {
    return capturedVariables;
  }

  @Override
  public void setCapturedVariables(LocalScope capturedVariables) {
    this.capturedVariables = Optional.ofNullable(capturedVariables);
  }

  @Override
  public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search) {
    return super.resolveInThisScopeOnly(search);
  }
}
