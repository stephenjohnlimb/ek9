package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * While not actually file access, we are tinkering with System here and need to serialize access.
 */
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
class StdoutTest extends StdCommon {

  //So it can be set back after the test
  private PrintStream originalOut = System.out;

  //So it is possible to inspect the contents, once written to.
  private ByteArrayOutputStream outContent;

  @BeforeEach
  void setUp() {
    originalOut = System.out;
    outContent = new ByteArrayOutputStream();

    //Now override where the output goes to.
    System.setOut(new PrintStream(outContent));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void testConstruction() {
    final var underTest = new Stdout();
    assertNotNull(underTest);
    assertSet.accept(underTest);
  }

  @Test
  void testPrintln() {
    assertPrintln(new Stdout(), outContent);
  }

  @Test
  void testPipe() {
    assertPipe(new Stdout(), outContent);
  }

  @Test
  void testPrint() {

    assertPrint(new Stdout(), outContent);
  }

}
