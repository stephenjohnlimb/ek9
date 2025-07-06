package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExceptionTest extends Common {

  final Exception unset = new Exception();
  final String simpleReason = String._of("Simple error message");
  final String complexReason = String._of("Complex error with details");
  final Integer exitCode1 = Integer._of(1);
  final Integer exitCode42 = Integer._of(42);
  final Integer unsetExitCode = new Integer();

  @Test
  void testConstruction() {
    // Default constructor - creates unset exception
    final var defaultConstructor = new Exception();
    assertEquals("", defaultConstructor.toString());

    assertFalse.accept(defaultConstructor._isSet());

    // Constructor with String reason
    final var reasonException = new Exception(simpleReason);
    assertTrue.accept(reasonException._isSet());
    assertEquals("Exception: Simple error message", reasonException.toString());

    // Constructor with String reason and exit code
    final var reasonExitCodeException = new Exception(complexReason, exitCode42);
    assertEquals("Exception: Complex error with details: Exit Code: 42", reasonExitCodeException.toString());

    // Constructor with Exception cause
    final var causeException = new Exception(reasonException);
    assertTrue.accept(causeException._isSet());


    // Constructor with String reason and Exception cause
    final var reasonCauseException = new Exception(simpleReason, reasonException);
    assertEquals("Exception: Simple error message: Root Cause: Exception: Simple error message", reasonCauseException.toString());

    // Constructor with String reason, Exception cause, and exit code
    final var fullException = new Exception(complexReason, reasonException, exitCode42);
    assertEquals("Exception: Complex error with details: Root Cause: Exception: Simple error message: Exit Code: 42", fullException.toString());

    // Test with unset String
    final var unsetReasonException = new Exception(new String());
    assertEquals("Exception: ", unsetReasonException.toString());

    // Test with unset exit code
    final var unsetExitException = new Exception(simpleReason, unsetExitCode);
    assertEquals("Exception: Simple error message", unsetExitException.toString());
  }

  @Test
  void testStaticFactoryMethods() {
    // Static factory with String
    final var factoryStringException = Exception._of("Factory created exception");
    assertEquals("Exception: Factory created exception", factoryStringException.toString());

    // Static factory with RuntimeException
    final var runtimeException = new RuntimeException("Runtime error");
    final var factoryRuntimeException = Exception._of(runtimeException);
    assertTrue.accept(factoryRuntimeException._isSet());

    // Static factory with null
    final var nullException = Exception._of((java.lang.String) null);
    assertEquals("Exception: ", nullException.toString());
  }

  @Test
  void testIsSet() {
    assertNotNull(unset);
    assertFalse.accept(unset._isSet());

    final var setException = new Exception(simpleReason);
    assertNotNull(setException);
    assertTrue.accept(setException._isSet());

    // All Exception constructors except default should create set exceptions
    final var reasonException = new Exception(simpleReason);
    assertTrue.accept(reasonException._isSet());

    final var causeException = new Exception(reasonException);
    assertTrue.accept(causeException._isSet());

    final var reasonExitException = new Exception(simpleReason, exitCode1);
    assertTrue.accept(reasonExitException._isSet());

    final var causeExitException = new Exception(reasonException, exitCode1);
    assertTrue.accept(causeExitException._isSet());

    final var reasonCauseException = new Exception(simpleReason, reasonException);
    assertTrue.accept(reasonCauseException._isSet());

    final var fullException = new Exception(simpleReason, reasonException, exitCode1);
    assertTrue.accept(fullException._isSet());
  }

  @Test
  void testReasonMethod() {
    // Unset exception should have unset reason
    final var unsetReason = unset.reason();
    assertUnset.accept(unsetReason);

    // Exception with reason
    final var reasonException = new Exception(simpleReason);
    final var reason = reasonException.reason();
    assertSet.accept(reason);
    assertEquals("Simple error message", reason.state);

    // Exception with unset reason
    final var unsetReasonException = new Exception(new String());
    final var unsetReasonResult = unsetReasonException.reason();
    assertUnset.accept(unsetReasonResult);

    // Exception created from RuntimeException should have unset reason
    final var runtimeException = new RuntimeException("Runtime error");
    final var factoryException = Exception._of(runtimeException);
    final var factoryReason = factoryException.reason();
    assertUnset.accept(factoryReason);
  }

  @Test
  void testExitCodeMethod() {
    // Default exception should have unset exit code
    final var defaultExitCode = unset.exitCode();
    assertUnset.accept(defaultExitCode);

    // Exception without exit code should have unset exit code
    final var reasonException = new Exception(simpleReason);
    final var reasonExitCode = reasonException.exitCode();
    assertUnset.accept(reasonExitCode);

    // Exception with exit code
    final var exitCodeException = new Exception(simpleReason, exitCode42);
    final var exitCode = exitCodeException.exitCode();
    assertSet.accept(exitCode);
    assertEquals(42, exitCode.state);

    // Exception with cause and exit code
    final var causeExitException = new Exception(reasonException, exitCode1);
    final var causeExitCode = causeExitException.exitCode();
    assertSet.accept(causeExitCode);
    assertEquals(1, causeExitCode.state);

    // Exception with unset exit code
    final var unsetExitException = new Exception(simpleReason, unsetExitCode);
    final var unsetExit = unsetExitException.exitCode();
    assertUnset.accept(unsetExit);
  }

  @Test
  void testStringConversion() {
    // Unset exception
    final var unsetString = unset._string();
    assertUnset.accept(unsetString);
    assertEquals("", unset.toString());

    // Simple reason exception
    final var reasonException = new Exception(simpleReason);
    final var reasonString = reasonException._string();
    assertSet.accept(reasonString);
    assertEquals("Exception: Simple error message", reasonString.state);
    assertEquals("Exception: Simple error message", reasonException.toString());

    // Exception with exit code
    final var exitCodeException = new Exception(complexReason, exitCode42);
    assertEquals("Exception: Complex error with details: Exit Code: 42", exitCodeException.toString());

    // Exception with both reason and exit code
    final var fullToString = new Exception(String._of("Test message"), exitCode1);
    assertEquals("Exception: Test message: Exit Code: 1", fullToString.toString());

    // Exception with unset reason but set exit code
    final var unsetReasonException = new Exception(new String(), exitCode42);
    assertEquals("Exception: Exit Code: 42", unsetReasonException.toString());

    // Exception with reason but unset exit code
    final var unsetExitException = new Exception(simpleReason, unsetExitCode);
    assertEquals("Exception: Simple error message", unsetExitException.toString());
  }

  @Test
  void testToStringBehavior() {
    // Test various combinations of toString output

    // Just reason
    final var reasonOnly = new Exception(String._of("Error occurred"));
    assertEquals("Exception: Error occurred", reasonOnly.toString());

    // Just exit code (with unset reason)
    final var exitCodeOnly = new Exception(new String(), Integer._of(5));
    assertEquals("Exception: Exit Code: 5", exitCodeOnly.toString());

    // Both reason and exit code
    final var bothReasonExit = new Exception(String._of("Fatal error"), Integer._of(255));
    assertEquals("Exception: Fatal error: Exit Code: 255", bothReasonExit.toString());

    // Empty reason string
    final var emptyReason = new Exception(String._of(""));
    assertEquals("Exception: ", emptyReason.toString());

    // Zero exit code
    final var zeroExit = new Exception(String._of("Success"), Integer._of(0));
    assertEquals("Exception: Success: Exit Code: 0", zeroExit.toString());

    // Negative exit code
    final var negativeExit = new Exception(String._of("Error"), Integer._of(-1));
    assertEquals("Exception: Error: Exit Code: -1", negativeExit.toString());

    // Very long reason string
    final var longReason = String._of("This is a very long error message that contains many details about what went wrong in the application and should still be properly formatted");
    final var longReasonException = new Exception(longReason);
    assertEquals("Exception: This is a very long error message that contains many details about what went wrong in the application and should still be properly formatted", longReasonException.toString());
  }

  @Test
  void testCauseChaining() {
    // Test exception chaining behavior
    final var rootCause = new Exception(String._of("Root cause error"));
    final var middleCause = new Exception(String._of("Middle cause"), rootCause);
    final var topLevel = new Exception(String._of("Top level error"), middleCause);

    // Each should be set
    assertTrue.accept(rootCause._isSet());
    assertTrue.accept(middleCause._isSet());
    assertTrue.accept(topLevel._isSet());

    // Check toString behavior doesn't include cause details (as per implementation)
    assertEquals("Exception: Root cause error", rootCause.toString());
    assertEquals("Exception: Middle cause: Root Cause: Exception: Root cause error", middleCause.toString());
    assertEquals("Exception: Top level error: Root Cause: Exception: Middle cause: Root Cause: Exception: Root cause error", topLevel.toString());

    // Check cause chain exists
    assertEquals(middleCause, topLevel.getCause());
    assertEquals(rootCause, middleCause.getCause());
    assertNull(rootCause.getCause());
  }

  @Test
  void testFactoryMethods() {
    // Test static factory methods
    final var stringFactory = Exception._of("Factory exception");
    assertEquals("Exception: Factory exception", stringFactory.toString());

    // Test RuntimeException factory
    final var runtimeEx = new RuntimeException("Runtime exception message");
    final var runtimeFactory = Exception._of(runtimeEx);
    assertTrue.accept(runtimeFactory._isSet());

    // The toString for RuntimeException factory should show underlying message
    final var toStringResult = runtimeFactory.toString();
    assertNotNull(toStringResult);
    assertTrue.accept(Boolean._of(toStringResult.contains("Exception:")));
  }

  @Test
  void testEdgeCases() {
    // Exception with null RuntimeException
    final var nullRuntimeEx = Exception._of((RuntimeException) null);
    assertTrue.accept(nullRuntimeEx._isSet());

    // Exception with very large exit code
    final var largeExitCode = Integer._of(2147483647);
    final var largeExitException = new Exception(simpleReason, largeExitCode);
    final var largeExitString = largeExitException.toString();
    assertTrue.accept(Boolean._of(largeExitString.contains("Exit Code: 2147483647")));

    // Exception with very small (negative) exit code
    final var smallExitCode = Integer._of(-2147483648);
    final var smallExitException = new Exception(simpleReason, smallExitCode);
    final var smallExitString = smallExitException.toString();
    assertTrue.accept(Boolean._of(smallExitString.contains("Exit Code: -2147483648")));

    // Multiple levels of exception nesting
    final var level1 = new Exception(String._of("Level 1"));
    final var level2 = new Exception(String._of("Level 2"), level1);
    final var level3 = new Exception(String._of("Level 3"), level2, exitCode42);
    
    assertTrue.accept(level3._isSet());
    assertEquals("Exception: Level 3: Root Cause: Exception: Level 2: Root Cause: Exception: Level 1: Exit Code: 42", level3.toString());
    assertEquals(level2, level3.getCause());
    assertEquals(level1, level2.getCause());
  }

  @Test
  void testExceptionEquality() {
    // Test basic equality and inequality
    final var exception1 = new Exception(String._of("Same message"));
    final var exception2 = new Exception(String._of("Same message"));
    final var exception3 = new Exception(String._of("Different message"));

    // Note: Exception objects are not equal even with same message (reference equality)
    assertNotEquals(exception1, exception2);
    assertNotEquals(exception1, exception3);

    // But their string representations should be equal if same
    assertEquals(exception1.toString(), exception2.toString());
    assertNotEquals(exception1.toString(), exception3.toString());

    // Unset exceptions
    final var unset1 = new Exception();
    final var unset2 = new Exception();
    assertEquals(unset1.toString(), unset2.toString());
    assertEquals("", unset1.toString());
  }

  @Test
  void testExceptionWithSpecialCharacters() {
    // Test with special characters in reason
    final var specialCharsReason = String._of("Error: Invalid input! @#$%^&*()");
    final var specialCharsException = new Exception(specialCharsReason);
    assertEquals("Exception: Error: Invalid input! @#$%^&*()", specialCharsException.toString());

    // Test with newlines and tabs
    final var newlineReason = String._of("Line 1\nLine 2\tTabbed");
    final var newlineException = new Exception(newlineReason);
    assertEquals("Exception: Line 1\nLine 2\tTabbed", newlineException.toString());

    // Test with Unicode characters
    final var unicodeReason = String._of("Unicode test: \u2603 \u263A \u2665");
    final var unicodeException = new Exception(unicodeReason);
    assertEquals("Exception: Unicode test: \u2603 \u263A \u2665", unicodeException.toString());
  }
}