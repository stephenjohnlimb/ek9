package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.junit.jupiter.api.Test;

class UnreachableStatementTest {

  @Test
  void testUnreachableStatement() {
    var errorListener = new ErrorListener("dummy.ek9");

    var underTest = new EmitUnreachableStatementError(errorListener);

    underTest.accept(new Ek9Token("Cause", 5), new Ek9Token("Target", 10));

    assertTrue(errorListener.hasErrors());
    var errorDetails = errorListener.getErrors().next();
    assertEquals(
        "Error   : 'Cause' on line 5 position 0: Unreachable, because of 'Target' on line 10: all paths lead to an Exception",
        errorDetails.toString());

  }
}
