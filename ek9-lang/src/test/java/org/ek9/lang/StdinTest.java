package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the Ek9 version of Stdin by replacing the Java input stream with known values.
 */
class StdinTest extends Common {

  private InputStream originalIn;

  @BeforeEach
  void setUp() {
    originalIn = System.in;
    java.lang.String input = """
        line1
        line2
        line3
        check line
        """;
    //Now alter the 'in' so that we can test out Stdin class.
    System.setIn(new ByteArrayInputStream(input.getBytes()));
  }

  @AfterEach
  void tearDown() {
    System.setIn(originalIn);
  }

  @Test
  void testMultipleInputs() {

    final var underTest = new Stdin();
    assertNotNull(underTest);

    for (int i = 0; i < 3; i++) {
      assertTrue.accept(underTest.hasNext());
      final var line = underTest.next();
      assertSet.accept(line);

    }
    assertTrue.accept(underTest.hasNext());
    //Now test an actual value.
    final var line = underTest.next();
    assertSet.accept(line);
    assertEquals("check line", line.state);

    //Now check there is no more.
    assertFalse.accept(underTest.hasNext());
  }

  @Test
  void testEarlyClose() {

    final var underTest = new Stdin();
    assertNotNull(underTest);

    //Read first two then close and check that when try to read after get unset.
    for (int i = 0; i < 4; i++) {
      if(i < 2) {
        assertTrue.accept(underTest._isSet());
        assertTrue.accept(underTest.hasNext());
        final var line = underTest.next();
        assertSet.accept(line);
      }
      if(i == 2) {
        underTest._close();
        //try closing twice to ensure no exceptions
        underTest._close();
      }
      if(i > 2) {
        //Also check that is set now changes.
        assertFalse.accept(underTest._isSet());
        assertFalse.accept(underTest.hasNext());
        assertUnset.accept(underTest.next());
      }
    }
  }
}
