package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just checks if there are any directives that relate to @Resolved in the parsed module and checks the
 * non-resolution.
 */
public class NotResolvedDirectiveListener extends ResolutionDirectiveListener {
  @Override
  public void accept(final CompilationEvent compilationEvent) {

    if (compilationEvent.parsedModule() != null) {
      //Only interested in resolved directives for this phase.
      final var directives = compilationEvent
          .parsedModule()
          .getDirectives(DirectiveType.NotResolved, compilationEvent.phase());

      if (!directives.isEmpty()) {
        processDirectives(compilationEvent, directives);
      }
    }

  }

  @Override
  protected void symbolMatch(final CompilationEvent compilationEvent,
                             final ResolutionDirective directive,
                             final ISymbol symbol) {

    final var msg = "Directive '" + directive + "' but found '" + symbol.getFriendlyName() + "':";
    compilationEvent.source().getErrorListener().directiveError(directive.getDirectiveToken(), msg,
        ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_FOUND_UNEXPECTED_SYMBOL);

  }

  @Override
  protected void noSymbolMatch(final CompilationEvent compilationEvent,
                               final ResolutionDirective directive) {

    //This is a 'no-op' this listener is expecting not to find the symbol!

  }
}
