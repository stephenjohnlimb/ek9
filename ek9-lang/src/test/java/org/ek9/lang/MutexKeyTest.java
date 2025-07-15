package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * While it is abstract in an EK9 context, in Java we can 'new' it and test the default behaviour.
 */
class MutexKeyTest extends Common {

  @Test
  void testSimpleDefaultInstance() {
    final var defaultImpl = new MutexKey(){};

    assertNotNull(defaultImpl);

    //Functions always report as set.
    assertSet.accept(defaultImpl._isSet());

    //The default will do nothing
    assertDoesNotThrow(() -> defaultImpl._call(new String()));
  }
}
