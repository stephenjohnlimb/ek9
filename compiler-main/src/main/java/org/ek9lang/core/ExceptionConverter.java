package org.ek9lang.core;


import java.util.function.Function;

/**
 * Just wraps any exceptions in a runtime CompilerException.
 */
public class ExceptionConverter<T> implements Function<Processor<T>, T> {

  @Override
  public T apply(final Processor<T> processor) {

    try {
      return processor.process();
    } catch (Exception e) {
      throw new CompilerException("Processor processing failure", e);
    }

  }
}
