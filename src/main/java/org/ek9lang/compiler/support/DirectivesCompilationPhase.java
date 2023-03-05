package org.ek9lang.compiler.support;

import java.util.Arrays;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.main.phases.CompilationPhase;

/**
 * Just extracts the compilation phase or throws an illegal argument exception.
 */
public class DirectivesCompilationPhase implements Function<EK9Parser.DirectiveContext, CompilationPhase> {

  @Override
  public CompilationPhase apply(EK9Parser.DirectiveContext ctx) {
    try {
      return CompilationPhase.valueOf(ctx.directivePart(0).getText());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Expecting one of: " + Arrays.toString(CompilationPhase.values()));
    }
  }
}
