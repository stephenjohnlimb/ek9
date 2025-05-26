package org.ek9lang.compiler.symbols;

/**
 * For symbols that are both a pure symbol but can also define a scope.
 */
public interface IScopedSymbol extends IScope, ISymbol {

  IScopedSymbol clone(final IScope withParentAsAppropriate);
}
