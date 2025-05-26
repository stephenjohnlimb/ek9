package org.ek9lang.core;


import java.util.function.Function;

/**
 * Just catches exceptions and ignores them.
 * Use carefully, typically this is for low level issues on closing io operations.
 * Where there is little that can actually be done in terms of dealing with the exception.
 */
public class ExceptionIgnore<T> implements Function<Processor<T>, T> {

  @Override
  public T apply(final Processor<T> processor) {

    try {
      return processor.process();
    } catch (Exception e) {
      //Ignore the exception.
    }
    return null;
  }
}
