package org.ek9lang.compiler.main.directives;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationEvent;

/**
 * Provides the listeners for directive checks.
 */
public class DirectiveListenerSupplier implements Supplier<Consumer<CompilationEvent>> {
  @Override
  public Consumer<CompilationEvent> get() {
    return new ErrorDirectiveListener()
        .andThen(new ResolvedDirectiveListener())
        .andThen(new NotResolvedDirectiveListener());
  }
}
