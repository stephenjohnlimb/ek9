package org.ek9lang.compiler.directives;

import java.util.List;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.TypeDefResolver;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks for resolutions directives.
 */
abstract class ResolutionDirectiveListener implements CompilationPhaseListener {

  protected abstract void symbolMatch(final CompilationEvent compilationEvent,
                                      final ResolutionDirective directive,
                                      final ISymbol symbol);

  protected abstract void noSymbolMatch(final CompilationEvent compilationEvent,
                                        final ResolutionDirective directive);

  /**
   * Process each of the directives and check for resolution or otherwise and call the
   * symbolMatch or noSymbolMatch methods.
   */
  void processDirectives(final CompilationEvent compilationEvent,
                         final List<Directive> directives) {

    final var errorListener = compilationEvent.source().getErrorListener();
    final var scope = compilationEvent.parsedModule().getModuleScope();
    final var resolver = new TypeDefResolver(scope);

    for (var directive : directives) {
      final var resolutionDirective = (ResolutionDirective) directive;

      try {
        final var resolved = resolutionDirective.isForVariable()
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
