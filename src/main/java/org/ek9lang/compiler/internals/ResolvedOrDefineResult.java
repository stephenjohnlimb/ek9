package org.ek9lang.compiler.internals;

import java.util.Optional;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Mainly used in the CompilableProgram for resolving or defining a new Parameterized type.
 * The main issue with defining a Parameterized type is that it must be done in a single operation
 * inside a lock.
 * So when we encounter the need for a new parameterized generic type, the appropriate component
 * creates one and then calls on the CompilableProgram to either 'resolve' one that matches, or use
 * the one that has been passed in to 'define' that parameterized type. This is because there can and will be
 * multiple threads running through different sources. They may each find the need for a 'List of String',
 * the first one gets to define and the rest resolve, hence the need for a lock with a single operation.
 * But the calling code needs to 'know' if the returned symbol has been newly defined. This is because the
 * initial creation of the parameterized type will only be a minimal outline (name only), there will be
 * no methods or dependent types populated.
 * It is this latter aspect that must be done - BUT only if the parameterized type is newly defined.
 */
public record ResolvedOrDefineResult(Optional<ISymbol> symbol, boolean newlyDefined) {
}
