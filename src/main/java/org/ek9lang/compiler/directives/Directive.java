package org.ek9lang.compiler.directives;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.CompilationPhase;

/**
 * Provides basic interface for the EK9 internal @ type directives.
 * The idea is to accept this sort of thing.
 * //@Error: SYMBOL_DEFINITION: PARENTHESIS_NOT_REQUIRED
 * //@Error: REFERENCE_CHECKS: NOT_RESOLVED
 * //@Resolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TEMPLATE_TYPE: "List"
 * //@Resolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "List of (Integer)"
 * //@Instrument: CODE_GENERATION_PREPARATION: DEBUG
 * //@Symbols: SYMBOL_DEFINITION: ALL: 21
 * //@Symbols: SYMBOL_DEFINITION: TYPE: 11
 * //@Symbols: SYMBOL_DEFINITION: TEMPLATE_FUNCTION: 2
 */
public interface Directive {
  /**
   * What s the type of the directive.
   */
  DirectiveType type();

  /**
   * Is it for the particular compiler phase.
   */
  boolean isForPhase(final CompilationPhase phase);

  /**
   * Which line number does this directive apply to.
   */
  int getAppliesToLineNumber();

  /**
   * Provide a token from the source, by default if not possible supply synthetic.
   */
  Token getDirectiveToken();
}
