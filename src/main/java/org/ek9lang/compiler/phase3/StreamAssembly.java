package org.ek9lang.compiler.phase3;

import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * The construct for a Stream statement/expression.
 *
 * @param source      This is a typed source of typed objects.
 * @param streamParts These are the optional stream parts.
 * @param termination This is the termination part and has a type, that may or may not accept piped in typed objects.
 */
public record StreamAssembly(StreamCallSymbol source,
                             List<EK9Parser.StreamPartContext> streamParts,
                             StreamCallSymbol termination) {
}
