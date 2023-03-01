package org.ek9lang.compiler.errors;

import java.util.function.Consumer;

/**
 * Register this event listener with the compiler to pick up on
 * any warnings of errors during compilation.
 * But this will be called at various phases during compilation.
 * There may not always be errors or warnings.
 * Use CompilableSource.getErrorListener() to check for errors/warnings.
 */
public interface CompilationPhaseListener extends Consumer<CompilationEvent> {

  /**
   * Once the compiler has processed (or attempted to process) a source file it will issue
   * this event.
   */
  void accept(final CompilationEvent compilationEvent);
}
