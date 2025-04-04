package org.ek9lang.compiler.directives;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.CompilationPhase;

/**
 * Extracts the compilation phase or throws an illegal argument exception.
 */
public class DirectivesCompilationPhase implements Function<EK9Parser.DirectiveContext, CompilationPhase> {

  @Override
  public CompilationPhase apply(final EK9Parser.DirectiveContext ctx) {

    try {
      return CompilationPhase.valueOf(ctx.directivePart(0).getText());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Expecting one of: " + applicableCompilationPhases());
    }

  }

  private List<String> applicableCompilationPhases() {

    final Predicate<CompilationPhase> acceptableValues = compilationPhase
        -> CompilationPhase.PARSING != compilationPhase;

    return Arrays.stream(CompilationPhase.values()).filter(acceptableValues).map(Enum::toString).toList();
  }
}
