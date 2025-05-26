package org.ek9lang.compiler.support;

import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Used to model the scenario where a generic type has another dependent generic type used within it.
 * Note that the dependent may or may not require the same conceptual type arguments as the parent.
 *
 * @param parent    - This is the outermost generic type
 * @param dependent - This is the generic type that is directly contained within the parent.
 */
public record GenericInGenericData(PossibleGenericSymbol parent, PossibleGenericSymbol dependent) {
}
