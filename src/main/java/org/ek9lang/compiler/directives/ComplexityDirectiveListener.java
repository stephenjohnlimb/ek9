package org.ek9lang.compiler.directives;

import static org.ek9lang.compiler.support.CommonValues.COMPLEXITY;

import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just checks if there are any directives that relate to complexity in the parsed module.
 */
public class ComplexityDirectiveListener extends ResolvedDirectiveListener {
  @Override
  public void accept(final CompilationEvent compilationEvent) {

    //Will be null until fully parsed.
    if (compilationEvent.parsedModule() != null) {
      //Only interested in error directives for this phase.
      final var directives =
          compilationEvent.parsedModule().getDirectives(DirectiveType.Complexity, compilationEvent.phase());
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

    final var expectedComplexityValue = Integer.parseInt(resolutionDirective.getAdditionalName());

    final var symbolComplexityValue = Integer.parseInt(symbol.getSquirrelledData(COMPLEXITY));

    if (expectedComplexityValue != symbolComplexityValue) {
      final var msg = String.format("expected complexity defined as %d, but calculated complexity is %d:",
          expectedComplexityValue, symbolComplexityValue);

      compilationEvent.source().getErrorListener().directiveError(resolutionDirective.getDirectiveToken(), msg,
          ErrorListener.SemanticClassification.DIRECTIVE_SYMBOL_COMPLEXITY);
    }

  }
}
