package org.ek9lang.compiler;

/**
 * Effectively a tuple for returning if the phase was a match for the required
 * compilation phase to be run to and if this phase ran ok.
 */
public record CompilationPhaseResult(CompilationPhase phase, boolean phaseSuccess, boolean phaseMatch) {
}

