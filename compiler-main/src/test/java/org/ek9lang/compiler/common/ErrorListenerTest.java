package org.ek9lang.compiler.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.junit.jupiter.api.Test;

/**
 * Most of the use of the ErrorListener will be driven by examples of
 * EK9 source code that is designed to 'fail' to compile.
 * <p>
 * These examples of EK9 that fail to compile should be stored in 'badExamples' in the
 * resources' directory. Ideally these bad example will be grouped in to subdirectories.
 * <p>
 * So this unit test is just designed to test the basic mechanisms.
 */
final class ErrorListenerTest {

  @Test
  void testErrorSourceName() {
    ErrorListener underTest = new ErrorListener("test");
    assertEquals("test", underTest.getGeneralIdentifierOfSource());
  }

  @Test
  void testReturningRequired() {
    ErrorListener underTest = new ErrorListener("test");
    underTest.raiseReturningRequired(createSyntheticToken(), "_EK9 Test");
    assertInError(underTest);
  }

  @Test
  void testReturningRedundant() {
    ErrorListener underTest = new ErrorListener("test");
    underTest.raiseReturningNotRequired(createSyntheticToken(), "_EK9 Test");
    assertInError(underTest);
  }


  @Test
  void testSemanticErrorCreation() {
    ErrorListener underTest = new ErrorListener("test");
    underTest.semanticError(createSyntheticToken(), "_EK9 Test",
        ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
    assertInError(underTest);
  }

  @Test
  void testDuplicateSemanticErrorCreation() {
    ErrorListener underTest = new ErrorListener("test");

    underTest.semanticError(createSyntheticToken(), "_EK9 Test",
        ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);

    //Add same again and check deduplicated.
    underTest.semanticError(createSyntheticToken(), "_EK9 Test",
        ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);

    assertInError(underTest);
  }

  @Test
  void testSemanticErrorCreationNoToken() {
    ErrorListener underTest = new ErrorListener("test");
    IToken token = createSyntheticToken();
    underTest.semanticError(token, "Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
    assertInError(underTest);
  }

  @Test
  void testSemanticWarningCreationNoToken() {
    ErrorListener underTest = new ErrorListener("test");
    underTest.semanticWarning(null, "Test", ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
    assertJustWarnings(underTest);
  }

  @Test
  void testSyntaxErrorWithEK9Message() {
    ErrorListener underTest = new ErrorListener("test");
    var msg = "_EK9 Some sort of general syntax error";
    underTest.syntaxError(null, "Anything", 0, 0, msg, null);
    assertInError(underTest);
  }

  @Test
  void testSyntaxErrorWithGeneralMessage() {
    ErrorListener underTest = new ErrorListener("test");
    var msg = "Some sort of general syntax error";
    underTest.syntaxError(null, "Anything", 0, 0, msg, null);
    assertInError(underTest);
  }

  private void assertJustWarnings(ErrorListener underTest) {
    assertTrue(underTest.isErrorFree());
    assertFalse(underTest.isWarningFree());
    assertErrorDetails(underTest.getWarnings().next());
  }

  private void assertInError(ErrorListener underTest) {
    assertFalse(underTest.isErrorFree());
    assertTrue(underTest.isWarningFree());
    assertErrorDetails(underTest.getErrors().next());
  }

  private void assertErrorDetails(ErrorListener.ErrorDetails details) {
    assertNotNull(details);
    assertNotNull(details.getTypeOfError());
    assertEquals(0, details.getPosition());
    assertEquals(0, details.getLineNumber());
    assertNotNull(details.getLikelyOffendingSymbol());
    assertNotNull(details.toString());
  }

  @Test
  void testConstructionAndSetup() {
    ErrorListener underTest = new ErrorListener("test");

    //Setup check defaults and ensure switches work.
    assertTrue(underTest.isErrorFree());
    assertTrue(underTest.isWarningFree());

    //These are the three main flags that indicate there might be an issue with the grammar.
    assertFalse(underTest.isExceptionOnAmbiguity());
    underTest.setExceptionOnAmbiguity(true);
    assertTrue(underTest.isExceptionOnAmbiguity());

    assertFalse(underTest.isExceptionOnContextSensitive());
    underTest.setExceptionOnContextSensitive(true);
    assertTrue(underTest.isExceptionOnContextSensitive());

    assertFalse(underTest.isExceptionOnFullContext());
    underTest.setExceptionOnFullContext(true);
    assertTrue(underTest.isExceptionOnFullContext());

    IToken token = createSyntheticToken();
    underTest.raiseReturningNotRequired(token, "Test Message");

    //Should now be in error
    assertFalse(underTest.isErrorFree());
    ErrorListener.ErrorDetails details = underTest.getErrors().next();
    assertNotNull(details);
    assertNotNull(details.getClassification());

    assertNotNull(details.toString());
    assertEquals(ErrorListener.SemanticClassification.RETURNING_NOT_REQUIRED,
        details.getSemanticClassification());

    assertTrue(underTest.isWarningFree());

    //Now create a warning
    underTest.semanticWarning(createSyntheticToken(), "Test Warning Message",
        ErrorListener.SemanticClassification.METHOD_AMBIGUOUS);
    assertFalse(underTest.isWarningFree());

    details = underTest.getWarnings().next();
    assertNotNull(details.toString());
    assertEquals(ErrorListener.SemanticClassification.METHOD_AMBIGUOUS,
        details.getSemanticClassification());

  }

  private IToken createSyntheticToken() {
    return new Ek9Token("test", 0);
  }
}
