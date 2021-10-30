package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.SymbolSearch;

import java.util.Optional;

/**
 * Interface for dynamic functions and classes so that dynamic variables can be captured.
 */
public interface ICanCaptureVariables
{
	void setCapturedVariables(LocalScope capturedVariables);
	
	void setCapturedVariables(Optional<LocalScope> capturedVariables);
	
	/**
	 * Make the scope that holds the captured variables (if any accessible).
	 * @return The scope holding the captured variables.
	 */
	Optional<LocalScope> getCapturedVariables();
	
	/**
	 * Try and resolve a symbol but exclude looking in captured variables.
	 */
	Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search);
}
