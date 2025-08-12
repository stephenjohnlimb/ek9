package org.ek9lang.compiler.directives;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.ek9lang.compiler.common.CompilationEvent;

/**
 * Provides the listeners for directive checks.
 */
public class DirectiveListenerSupplier implements Supplier<Consumer<CompilationEvent>> {
  @Override
  public Consumer<CompilationEvent> get() {

    return new ErrorDirectiveListener()
        .andThen(new ResolvedDirectiveListener())
        .andThen(new NotResolvedDirectiveListener())
        .andThen(new ImplementsDirectiveListener())
        .andThen(new GenusDirectiveListener())
        .andThen(new ComplexityDirectiveListener())
        .andThen(new IRDirectiveListener());
  }
}
