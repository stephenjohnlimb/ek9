package org.ek9lang.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExceptionConverterTest {

  @Test
  void testWithException() {
    var underTest = new ExceptionConverter<Boolean>();

    Exception exception = assertThrows(CompilerException.class,
        () -> underTest.apply(() -> {
          throw new Exception("A test");
        }));
    assertNotNull(exception);
  }
}
