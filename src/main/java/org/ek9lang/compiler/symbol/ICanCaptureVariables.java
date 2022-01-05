package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

import java.util.Optional;

/**
 * Interface for dynamic functions and classes so that dynamic variables can be captured.
 */
public interface ICanCaptureVariables
{

	/**
	 * So that a number of variables can be captured when a new dynamic type is defined.
	 * @param capturedVariables The variables to capture.
	 */
	void setCapturedVariables(LocalScope capturedVariables);

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
