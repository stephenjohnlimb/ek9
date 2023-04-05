package org.ek9lang.compiler.main.directives;

import java.util.List;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.Directive;
import org.ek9lang.compiler.support.ResolutionDirective;
import org.ek9lang.compiler.support.TypeDefResolver;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * For resolved and notResolved directives.
 */
public abstract class ResolutionDirectiveListener implements CompilationPhaseListener {

  protected abstract void symbolMatch(final CompilationEvent compilationEvent, final ResolutionDirective directive,
                                      final ISymbol symbol);

  protected abstract void noSymbolMatch(final CompilationEvent compilationEvent, final ResolutionDirective directive);

  /**
   * Process each of the directives and check for resolution or otherwise and call the
   * symbolMatch or noSymbolMatch methods.
   */
  protected void processDirectives(final CompilationEvent compilationEvent, List<Directive> directives) {
    var errorListener = compilationEvent.source().getErrorListener();
    var scope = compilationEvent.parsedModule().getModuleScope();
    TypeDefResolver resolver = new TypeDefResolver(scope);

    for (var directive : directives) {
      var resolutionDirective = (ResolutionDirective) directive;
      try {
        var resolved = resolutionDirective.isForVariable()
            ? scope.resolve(resolutionDirective.getSymbolSearch())
            : resolver.typeDefToSymbol(resolutionDirective.getSymbolName());

        resolved.ifPresentOrElse(symbol -> symbolMatch(compilationEvent, resolutionDirective, symbol),
            () -> noSymbolMatch(compilationEvent, resolutionDirective));

      } catch (IllegalArgumentException exception) {
        //In the case of directives (as a debugging tool) we may get an exception if the developer
        //incorrectly uses @Resolve @NoResolve with generic types
        errorListener.directiveError(resolutionDirective.getDirectiveToken(), exception.getMessage(),
            ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT);
      }
    }
  }
}
