package org.ek9lang.core.utils;


import java.util.function.Function;
import org.ek9lang.core.exception.CompilerException;

/**
 * Just wraps any exceptions in a runtime CompilerException.
 */
public class ExceptionConverter<T> implements Function<Processor<T>, T> {

  @Override
  public T apply(Processor<T> processor) {
    try {
      return processor.process();
    } catch (Exception e) {
      throw new CompilerException("Processor processing failure", e);
    }
  }
}
