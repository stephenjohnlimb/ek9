package org.ek9lang.core.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.ek9lang.core.exception.CompilerException;
import org.junit.jupiter.api.Test;

class ExceptionConverterTest {

  @Test
  void testWithException() {
    ExceptionConverter underTest = new ExceptionConverter();

    Exception exception = assertThrows(CompilerException.class,
        () -> underTest.apply(() -> { throw new Exception("A test");}));
  }
}
