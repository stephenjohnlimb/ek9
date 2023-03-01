package org.ek9lang.compiler.errors;

import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.phases.CompilationPhase;

/**
 * As and when sources are processed during each phase of the compilation.
 * One of these events will be emitted.
 *
 * @param phase        - The phase of the compilation that has just been completes for a source file.
 * @param parsedModule - The parsedModule where that parsed sources symbols will have been recorded.
 * @param source       The related source that has just been processed.
 */
public record CompilationEvent(CompilationPhase phase, ParsedModule parsedModule, CompilableSource source) {
}
