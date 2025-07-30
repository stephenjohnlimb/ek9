package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class Ek9BuiltinIntrospectionSupplierTest {

  private final Ek9BuiltinIntrospectionSupplier underTest = new Ek9BuiltinIntrospectionSupplier();

  @Test
  void testIntrospectionLoading() {
    final var compilableSources = underTest.get();
    assertNotNull(compilableSources);
    assertFalse(compilableSources.isEmpty());

    //No need to fully check the contents - see Ek9IntrospectedBootStrapTest
    //That actually uses the supplier and compiles (checks the contents).
    //This is just a simple check to make sure the content is at least not empty.
    //The Ek9 compiler does allow for empty input, hence the need for this test - we are not expecting empty.

    compilableSources.forEach(source -> assertFalse(source.getSourceAsStringForDebugging().isEmpty()));
  }
}
