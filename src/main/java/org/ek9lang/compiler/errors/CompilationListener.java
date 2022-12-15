package org.ek9lang.compiler.errors;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.main.phases.CompilationPhase;

/**
 * Register this event listener with the compiler to pick up on
 * any warnings of errors during compilation.
 * But this will be called at various phases during compilation.
 * There may not always be errors or warnings.
 * Use CompilableSource.getErrorListener() to check for errors/warnings.
 */
public interface CompilationListener extends BiConsumer<CompilationPhase, CompilableSource> {

  /**
   * Once the compiler has processed (or attempted to process) a source file it will issue
   * this event.
   *
   * @param phase            The phase of compilation being undertaken.
   * @param compilableSource The source that was processed.
   */
  void accept(CompilationPhase phase, CompilableSource compilableSource);

}
