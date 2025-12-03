package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.VerboseErrorMessages;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnreachableStatementTest {

  @BeforeEach
  void setUp() {
    // Disable verbose mode for exact string comparison
    VerboseErrorMessages.setVerboseEnabled(false);
  }

  @AfterEach
  void tearDown() {
    VerboseErrorMessages.setVerboseEnabled(false);
  }

  @Test
  void testUnreachableStatement() {
    var errorListener = new ErrorListener("dummy.ek9");

    var underTest = new EmitUnreachableStatementError(errorListener);

    underTest.accept(new Ek9Token("Cause", 5), new Ek9Token("Target", 10));

    assertTrue(errorListener.hasErrors());
    var errorDetails = errorListener.getErrors().next();
    assertEquals(
        "Error   : E07370: 'Cause' on line 5 position 0: Unreachable, because of 'Target' on line 10: all paths lead to an Exception"
            + System.lineSeparator()
            + "             See: https://ek9.io/errors.html#E07370",
        errorDetails.toString());

  }
}
