package org.ek9lang.core.utils;


import java.util.function.Function;
import org.ek9lang.core.exception.CompilerException;

public class ExceptionConverter implements Function<Processor, Boolean> {

  @Override
  public Boolean apply(Processor processor) {
    try {
      return processor.process();
    } catch (Exception e) {
      throw new CompilerException("Processor processing failure", e);
    }
  }
}
