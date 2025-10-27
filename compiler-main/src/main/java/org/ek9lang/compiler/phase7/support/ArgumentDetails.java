package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Holds argument processing results for method/function calls.
 * Used by CallInstrGenerator, FunctionCallProcessor, and ObjectAccessInstrGenerator.
 * <p>
 * This record consolidates the common pattern of collecting argument variables,
 * parameter types, and argument symbols during call parameter processing.
 * </p>
 */
public record ArgumentDetails(
    List<String> argumentVariables,
    List<String> parameterTypes,
    List<ISymbol> argumentSymbols
) {
}
