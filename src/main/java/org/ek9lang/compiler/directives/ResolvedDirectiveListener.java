package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks if there are any directives that relate to @Resolved in the parsed module and checks the
 * resolution.
 */
class ResolvedDirectiveListener extends ResolutionDirectiveListener {

  @Override
  public void accept(final CompilationEvent compilationEvent) {

    if (compilationEvent.parsedModule() != null) {
      //Only interested in resolved directives for this phase.
      final var directives =
          compilationEvent.parsedModule().getDirectives(DirectiveType.Resolved, compilationEvent.phase());
      if (!directives.isEmpty()) {
        processDirectives(compilationEvent, directives);
      }
    }

  }

  @Override
  protected void symbolMatch(final CompilationEvent compilationEvent,
                             final ResolutionDirective directive,
                             final ISymbol symbol) {

    final var categoryMatch = symbol.getCategory().equals(directive.getSymbolCategory());
    if (!categoryMatch) {
      final var msg = "Looking for '" + directive + "' but got category '" + symbol.getCategory() + "':";
      compilationEvent.source().getErrorListener().directiveError(directive.getDirectiveToken(), msg,
          ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_CATEGORY_MISMATCH);
    }

  }

  @Override
  protected void noSymbolMatch(final CompilationEvent compilationEvent,
                               final ResolutionDirective directive) {

    final var msg = "'" + directive + "':";
    compilationEvent.source().getErrorListener().directiveError(directive.getDirectiveToken(), msg,
        ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_NOT_RESOLVED);

  }
}
