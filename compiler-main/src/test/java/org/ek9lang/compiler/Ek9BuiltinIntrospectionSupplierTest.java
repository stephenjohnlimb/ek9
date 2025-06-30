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
    //To do - add a few more assertions in to check the contents are reasonable.
    compilableSources.forEach(source -> System.out.println(source.getSourceAsStringForDebugging()));

  }
}
