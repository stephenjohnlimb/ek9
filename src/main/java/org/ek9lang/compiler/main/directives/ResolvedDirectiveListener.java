package org.ek9lang.compiler.main.directives;

import java.util.List;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.Directive;
import org.ek9lang.compiler.support.DirectiveType;
import org.ek9lang.compiler.support.ResolutionDirective;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;

/**
 * Just checks if there are any directives that relate to @Resolved in the parsed module and checks the
 * resolution.
 */
public class ResolvedDirectiveListener implements CompilationPhaseListener {
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

  private void processDirectives(final CompilationEvent compilationEvent, List<Directive> directives) {

    var errorListener = compilationEvent.source().getErrorListener();
    var scope = compilationEvent.parsedModule().getModuleScope();

    for (var directive : directives) {
      var resolvedDirective = (ResolutionDirective) directive;
      var resolved = scope.resolve(new AnySymbolSearch(resolvedDirective.getSymbolName()));
      resolved.ifPresentOrElse(symbol -> {
        var categoryMatch = symbol.getCategory().equals(((ResolutionDirective) directive).getSymbolCategory());
        if (!categoryMatch) {
          var msg = "Looking for '" + directive + "' but got category '" + symbol.getCategory() + "'";
          errorListener.directiveError(directive.getDirectiveToken(), msg,
              ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_CATEGORY_MISMATCH);
        }
      }, () -> {
        var msg = "'" + directive + "'";
        errorListener.directiveError(directive.getDirectiveToken(), msg,
            ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_NOT_RESOLVED);
      });
    }
  }
}
