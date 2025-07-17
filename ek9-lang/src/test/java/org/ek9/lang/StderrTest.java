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
class StderrTest extends StdCommon {

  //So it can be set back after the test
  private PrintStream originalErr = System.err;

  //So it is possible to inspect the contents, once written to.
  private ByteArrayOutputStream errContent;

  @BeforeEach
  void setUp() {
    originalErr = System.err;
    errContent = new ByteArrayOutputStream();

    //Now override where the output goes to.
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalErr);
  }

  @Test
  void testConstruction() {
    final var underTest = new Stderr();
    assertNotNull(underTest);
    assertSet.accept(underTest);
  }

  @Test
  void testPrintln() {
    assertPrintln(new Stderr(), errContent);
  }

  @Test
  void testPipe() {
    assertPipe(new Stderr(), errContent);
  }

  @Test
  void testPrint() {
    assertPrint(new Stderr(), errContent);
  }

}
