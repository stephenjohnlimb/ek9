package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void testEmptyInput() {
    final var underTest = new TCPHandler();
    assertNotNull(underTest);

    //Empty input stream
    final var is = new ByteArrayInputStream(new byte[0]);
    final var response = new ByteArrayOutputStream();
    final var ps = new PrintStream(response);

    final var input = new StringInputImpl(is);
    final var output = new StringOutputImpl(ps);

    underTest._call(input, output);

    //Verify no output produced
    assertEquals("", response.toString());

    //Verify streams are closed/unset
    assertFalse.accept(input._isSet());
    assertTrue.accept(output._isSet());
  }

  @Test
  void testLargeInput() {
    final var underTest = new TCPHandler();
    assertNotNull(underTest);

    //Generate large input (1000 lines)
    final var inputBuilder = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      inputBuilder.append("line").append(i).append("\n");
    }
    final var inputText = inputBuilder.toString();

    final var is = new ByteArrayInputStream(inputText.getBytes());
    final var response = new ByteArrayOutputStream();
    final var ps = new PrintStream(response);

    final var input = new StringInputImpl(is);
    final var output = new StringOutputImpl(ps);

    underTest._call(input, output);

    //Verify complete output matches input
    assertEquals(inputText, response.toString());

    //Verify streams are closed/unset
    assertFalse.accept(input._isSet());
    assertFalse.accept(output._isSet());
  }

  @Test
  void testWhitespaceInput() {
    final var underTest = new TCPHandler();
    assertNotNull(underTest);

    //Test input with various whitespace
    final var inputText = "   \n\t\t\n\n  spaces  \n\n";
    final var is = new ByteArrayInputStream(inputText.getBytes());
    final var response = new ByteArrayOutputStream();
    final var ps = new PrintStream(response);

    final var input = new StringInputImpl(is);
    final var output = new StringOutputImpl(ps);

    underTest._call(input, output);

    //Only check that real content is present.
    assertTrue(response.toString().contains("spaces"));

    //Verify streams are closed/unset
    assertFalse.accept(input._isSet());
    assertFalse.accept(output._isSet());
  }

}
