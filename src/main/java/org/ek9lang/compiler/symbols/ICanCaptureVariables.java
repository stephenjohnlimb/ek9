package org.ek9lang.compiler.symbols;

import java.util.Optional;
import org.ek9lang.compiler.search.SymbolSearch;

/**
 * Interface for dynamic functions and classes so that dynamic variables can be captured.
 */
public interface ICanCaptureVariables {

  /**
   * Make the scope that holds the captured variables (if any accessible).
   *
   * @return The scope holding the captured variables.
   */
  Optional<CaptureScope> getCapturedVariables();

  /**
   * So that a number of variables can be captured when a new dynamic type is defined.
   *
   * @param capturedVariables The variables to capture.
   */
  void setCapturedVariables(CaptureScope capturedVariables);

  /**
   * Try and resolve a symbol but exclude looking in captured variables.
   */
  Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search);
}
