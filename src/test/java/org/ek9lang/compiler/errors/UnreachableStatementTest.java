package org.ek9lang.compiler.errors;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.jupiter.api.Test;

class UnreachableStatementTest {

  @Test
  void testUnreachableStatement() {
    var errorListener = new ErrorListener("dummy.ek9");

    var underTest = new UnreachableStatement(errorListener);

    underTest.accept(new SyntheticToken("Cause", 5), new SyntheticToken("Target", 10));

    assertTrue(errorListener.hasErrors());
    var errorDetails = errorListener.getErrors().next();
    assertEquals("Error   : File SyntheticTokenSource 'Cause' on line 5 position 0: Because 'Target' on line 10 makes 'Cause' an unreachable statement",
        errorDetails.toString());

  }
}
