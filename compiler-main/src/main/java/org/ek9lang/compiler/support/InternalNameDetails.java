package org.ek9lang.compiler.support;

import java.util.List;

/**
 * Holds the details for creating and internal 'decorated name' for a generic type that has been
 * parameterised.
 *
 * @param primaryName This is the simple name of the new parameterised type.
 * @param genericName This is the name of the generic type that was parameterised.
 * @param parameters  These are the parameterizing type arguments.
 */
public record InternalNameDetails(String primaryName, String genericName, List<String> parameters) {
}
