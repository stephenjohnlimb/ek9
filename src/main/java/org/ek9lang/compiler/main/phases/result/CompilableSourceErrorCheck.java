package org.ek9lang.compiler.main.phases.result;

import java.util.Collection;
import java.util.function.Predicate;
import org.ek9lang.compiler.internals.CompilableSource;

/**
 * Check to see if any of the compilable sources have errors.
 */
public class CompilableSourceErrorCheck implements Predicate<Collection<CompilableSource>> {
  @Override
  public boolean test(Collection<CompilableSource> compilableSources) {
    return compilableSources
        .parallelStream()
        .anyMatch(source -> source.getErrorListener().hasErrors());

  }
}
