package org.ek9lang.compiler.main.phases;

import java.util.Collection;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.CompilableSource;

/**
 * Determines which CompilableSources in a collection need to be parsed.
 */
public class RequiredCompilableSourcesForParsing implements
    UnaryOperator<Collection<CompilableSource>> {
  @Override
  public Collection<CompilableSource> apply(Collection<CompilableSource> compilableSources) {
    return compilableSources.stream()
        .filter(source -> source.hasNotBeenSuccessfullyParsed() || source.isModified())
        .toList();
  }
}
