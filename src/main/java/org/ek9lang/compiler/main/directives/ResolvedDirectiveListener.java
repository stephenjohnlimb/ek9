package org.ek9lang.compiler.main.directives;

import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.DirectiveType;
import org.ek9lang.compiler.support.ResolutionDirective;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Just checks if there are any directives that relate to @Resolved in the parsed module and checks the
 * resolution.
 */
public class ResolvedDirectiveListener extends ResolutionDirectiveListener {

  @Override
  public void accept(final CompilationEvent compilationEvent) {
    if (compilationEvent.parsedModule() != null) {
      //Only interested in resolved directives for this phase.
      var directives = compilationEvent.parsedModule().getDirectives(DirectiveType.Resolved, compilationEvent.phase());
      if (!directives.isEmpty()) {
        processDirectives(compilationEvent, directives);
      }
    }
  }

  @Override
  protected void symbolMatch(ErrorListener errorListener, ResolutionDirective directive, ISymbol symbol) {
    var categoryMatch = symbol.getCategory().equals(directive.getSymbolCategory());
    if (!categoryMatch) {
      var msg = "Looking for '" + directive + "' but got category '" + symbol.getCategory() + "'";
      errorListener.directiveError(directive.getDirectiveToken(), msg,
          ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_CATEGORY_MISMATCH);
    }
  }

  @Override
  protected void noSymbolMatch(ErrorListener errorListener, ResolutionDirective directive) {
    var msg = "'" + directive + "'";
    errorListener.directiveError(directive.getDirectiveToken(), msg,
        ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_NOT_RESOLVED);
  }
}
