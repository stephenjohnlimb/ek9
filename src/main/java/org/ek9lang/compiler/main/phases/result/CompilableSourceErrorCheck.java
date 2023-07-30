package org.ek9lang.compiler.main.phases.result;

import java.util.Collection;
import java.util.function.Predicate;
import org.ek9lang.compiler.CompilableSource;

/**
 * Check to see if any of the compilable sources have errors.
 */
public class CompilableSourceErrorCheck implements Predicate<Collection<CompilableSource>> {
  @Override
  public boolean test(final Collection<CompilableSource> compilableSources) {

    return hasErrors(compilableSources);
  }

  private boolean hasErrors(final Collection<CompilableSource> compilableSources) {
    return compilableSources.stream()
        .anyMatch(source -> source.getErrorListener().hasErrors() || source.getErrorListener().hasDirectiveErrors());
  }
}
