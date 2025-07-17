package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class FileTest extends Common {

  @Test
  void testAllMethodsResultInUnset() {
    final var theDefault = new File() {
    };
    assertNotNull(theDefault);

    assertFalse.accept(theDefault._isSet());

    assertUnset.accept(theDefault.isExecutable());
    assertUnset.accept(theDefault.isReadable());
    assertUnset.accept(theDefault.isWritable());
    assertUnset.accept(theDefault.lastModified());
    assertUnset.accept(theDefault._len());
    assertUnset.accept(theDefault._string());
    assertUnset.accept(theDefault._hashcode());
  }
}
