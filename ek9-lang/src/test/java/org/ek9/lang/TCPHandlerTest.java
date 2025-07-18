package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class TCPHandlerTest extends Common {

  @Test
  void testConstruction() {
    final var underTest = new TCPHandler();
    assertNotNull(underTest);
    //Always set.
    assertSet.accept(underTest._isSet());
  }

  @Test
  void testDefaultCallImplementation() {
    //handler to test
    final var underTest = new TCPHandler();
    assertNotNull(underTest);

    //set up of dummy data and also a way to capture responses.
    final var inputText = "line1\nline2\nlast\n";
    final var is = new ByteArrayInputStream(inputText.getBytes());
    final var response = new ByteArrayOutputStream();
    final var ps = new PrintStream(response);

    //wire the input and output to components.
    final var input = new StringInputImpl(is);
    final var output = new StringOutputImpl(ps);

    //Now call the function.
    underTest._call(input, output);

    //Just expect the input to be consumed and the output created.

    //input and output should now be closed and unset.
    assertFalse.accept(input._isSet());
    assertFalse.accept(output._isSet());

    //and the output should match the input for this default implementation.
    assertEquals(inputText, response.toString());
  }

  @Test
  void testUnsetOrNullInput() {
    final var underTest = new TCPHandler();
    assertNotNull(underTest);
    final var response = new ByteArrayOutputStream();
    final var ps = new PrintStream(response);

    underTest._call(new StringInput() {
    }, new StringOutputImpl(ps));
    assertEquals("", response.toString());

    underTest._call(null, new StringOutputImpl(ps));
    assertEquals("", response.toString());
  }


  @Test
  void testUnsetOrNullOutput() {
    final var underTest = new TCPHandler();
    assertNotNull(underTest);

    final var inputText = "line1\nline2\nlast\n";
    final var is = new ByteArrayInputStream(inputText.getBytes());
    final var input = new StringInputImpl(is);

    underTest._call(input, new StringOutput() {
    });
    //Now check input has not been consumed.
    assertTrue.accept(input._isSet());

    underTest._call(input, null);
    //Now check input has not been consumed.
    assertTrue.accept(input._isSet());
  }
}
