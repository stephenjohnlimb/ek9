package org.ek9lang.compiler.ir;

import java.util.HashSet;
import java.util.function.Function;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Extracts call metadata from symbols for use in IR generation.
 * Provides purity information, complexity scoring, and side effect classification
 * that backends can use for optimization decisions.
 */
public class CallMetaDataExtractor implements Function<ISymbol, CallMetaData> {

  private final Ek9Types ek9Types;
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();

  public CallMetaDataExtractor(final Ek9Types ek9Types) {
    this.ek9Types = ek9Types;
  }

  @Override
  public CallMetaData apply(final ISymbol symbol) {
    if (symbol == null) {
      return CallMetaData.defaultMetaData();
    }

    final var isPure = symbol.isMarkedPure();

    // Extract complexity from squirrelled data
    final var complexityData = symbol.getSquirrelledData(CommonValues.COMPLEXITY);
    final var complexityScore = complexityData != null ? parseComplexityScore(complexityData) : 0;

    // Detect side effects
    final var sideEffects = new HashSet<String>();

    final var symbolType = symbolTypeOrException.apply(symbol);
    // Check for IO side effects via trait hierarchy
    if (symbolType.isAssignableTo(ek9Types.ek9IO())) {
      sideEffects.add("IO");
    }

    // Check for mutation potential (non-Void return type)
    if (!symbolType.isExactSameType(ek9Types.ek9Void())) {
      sideEffects.add("MUTATION");
    }

    return new CallMetaData(isPure, complexityScore, sideEffects);
  }

  /**
   * Parses complexity score from squirrelled data, handling potential formatting.
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  private int parseComplexityScore(final String complexityData) {
    try {
      // Handle potential quoted values like "\"5\""
      final var cleaned = complexityData.replace("\"", "");
      return Integer.parseInt(cleaned);
    } catch (NumberFormatException _) {
      // If parsing fails, default to 0
      return 0;
    }
  }
}