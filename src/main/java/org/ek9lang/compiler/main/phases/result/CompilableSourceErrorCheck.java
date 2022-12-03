package org.ek9lang.compiler.main.phases.result;

import java.util.Collection;
import java.util.function.Predicate;
import org.ek9lang.compiler.files.CompilableSource;

/**
 * Check to see if any of the compilable sources have errors.
 */
public class CompilableSourceErrorCheck implements Predicate<Collection<CompilableSource>> {
  @Override
  public boolean test(Collection<CompilableSource> compilableSources) {
    return compilableSources.parallelStream().map(source -> source.getErrorListener().hasErrors())
        .findFirst().orElse(false);
  }
}
