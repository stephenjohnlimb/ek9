package org.ek9lang.compiler;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.search.SymbolSearch;

/**
 * This is used to simulate the same method in the CompilableProgram.
 * All this does is record the possibleGenericSymbol in the symbol table if it is not present.
 */
public class ParametricResolveOrDefine implements Function<PossibleGenericSymbol, ResolvedOrDefineResult> {
  private final IScope scope;

  public ParametricResolveOrDefine(final IScope symbolTable) {
    this.scope = symbolTable;
  }

  @Override
  public ResolvedOrDefineResult apply(PossibleGenericSymbol possibleGenericSymbol) {
    var search = new SymbolSearch(possibleGenericSymbol);
    var resolved = scope.resolve(search);
    if (resolved.isEmpty()) {
      scope.define(possibleGenericSymbol);
      return new ResolvedOrDefineResult(Optional.of(possibleGenericSymbol), true);
    }
    return new ResolvedOrDefineResult(Optional.of((PossibleGenericSymbol) resolved.get()), false);
  }
}
