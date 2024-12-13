package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Just checks if there are any directives that relate to @Genus in the parsed module and checks the
 * resolution through the symbol and its genus.
 */
public class GenusDirectiveListener extends ResolvedDirectiveListener {

  @Override
  public void accept(final CompilationEvent compilationEvent) {

    if (compilationEvent.parsedModule() != null) {
      //Only interested in extends directives for this phase.
      final var directives =
          compilationEvent.parsedModule().getDirectives(DirectiveType.Genus, compilationEvent.phase());
      if (!directives.isEmpty()) {
        processDirectives(compilationEvent, directives);
      }
    }

  }

  @Override
  protected void symbolMatch(final CompilationEvent compilationEvent,
                             final ResolutionDirective resolutionDirective,
                             final ISymbol symbol) {

    //Check the types and the like.
    super.symbolMatch(compilationEvent, resolutionDirective, symbol);

    try {

      final var genus = SymbolGenus.valueOf(resolutionDirective.getAdditionalName());
      final var genusMatch = symbol.getGenus().equals(genus);

      if (!genusMatch) {
        final var msg = "Looking for '" + resolutionDirective + "' but got genus '" + symbol.getGenus() + "':";
        compilationEvent.source().getErrorListener().directiveError(resolutionDirective.getDirectiveToken(), msg,
            ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_GENUS_MISMATCH);
      }

    } catch (IllegalArgumentException badGenus) {
      compilationEvent.source().getErrorListener().directiveError(resolutionDirective.getDirectiveToken(),
          "'" + resolutionDirective.getAdditionalName() + "':",
          ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_NO_SUCH_GENUS);
    }

  }
}
