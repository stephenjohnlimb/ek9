package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExceptionTest extends Common {

  final Exception unsetException = new Exception();
  final String simpleReason = String._of("Simple error message");
  final String complexReason = String._of("Complex error with details");
  final Integer exitCode1 = Integer._of(1);
  final Integer exitCode42 = INT_42;
  final Integer unsetExitCode = new Integer();

  @Test
  void testConstruction() {
    // Default constructor - creates unset exception
    final var defaultConstructor = new Exception();
    assertFalse.accept(defaultConstructor._isSet());

    // Constructor with String reason
    final var reasonException = new Exception(simpleReason);
    assertTrue.accept(reasonException._isSet());
    assertEquals(String._of("Exception: Simple error message"), reasonException._string());

    // Constructor with String reason and exit code
    final var reasonExitCodeException = new Exception(complexReason, exitCode42);
    assertEquals(String._of("Exception: Complex error with details: Exit Code: 42"), reasonExitCodeException._string());

    // Constructor with Exception cause
    final var causeException = new Exception(reasonException);
    assertTrue.accept(causeException._isSet());

    // Constructor with String reason and Exception cause
    final var reasonCauseException = new Exception(simpleReason, reasonException);
    assertEquals(String._of("Exception: Simple error message: Root Cause: Exception: Simple error message"),
        reasonCauseException._string());

    // Constructor with String reason, Exception cause, and exit code
    final var fullException = new Exception(complexReason, reasonException, exitCode42);
    assertEquals(
        String._of("Exception: Complex error with details: Root Cause: Exception: Simple error message: Exit Code: 42"),
        fullException._string());

    // Test with unset String
    final var unsetReasonException = new Exception(new String());
    assertUnset.accept(unsetReasonException._string());

    // Test with unset exit code
    final var unsetExitException = new Exception(simpleReason, unsetExitCode);
    assertEquals(String._of("Exception: Simple error message"), unsetExitException._string());
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
    assertNotNull(unsetException);
    assertFalse.accept(unsetException._isSet());

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
    final var unsetReason = unsetException.reason();
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
    final var defaultExitCode = unsetException.exitCode();
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
    final var unsetString = unsetException._string();
    assertUnset.accept(unsetString);
    assertEquals("", unsetException.toString());

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
    final var fullToString = new Exception(STR_TEST_MESSAGE, exitCode1);
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
    assertEquals(String._of("Exception: Error occurred"), reasonOnly._string());

    // Just exit code (with unset reason)
    final var exitCodeOnly = new Exception(new String(), Integer._of(5));
    assertEquals(String._of("Exception: Exit Code: 5"), exitCodeOnly._string());

    // Both reason and exit code
    final var bothReasonExit = new Exception(String._of("Fatal error"), Integer._of(255));
    assertEquals(String._of("Exception: Fatal error: Exit Code: 255"), bothReasonExit._string());

    // Empty reason string
    final var emptyReason = new Exception(String._of(""));
    assertEquals(String._of("Exception: "), emptyReason._string());

    // Zero exit code
    final var zeroExit = new Exception(String._of("Success"), Integer._of(0));
    assertEquals(String._of("Exception: Success: Exit Code: 0"), zeroExit._string());

    // Negative exit code
    final var negativeExit = new Exception(String._of("Error"), Integer._of(-1));
    assertEquals(String._of("Exception: Error: Exit Code: -1"), negativeExit._string());

    // Very long reason string
    final var longReason = String._of(
        "This is a very long error message that contains many details about what went wrong in the application and should still be properly formatted");
    final var longReasonException = new Exception(longReason);
    assertEquals(String._of("Exception: " + longReason._string()), longReasonException._string());
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
    assertEquals(
        "Exception: Top level error: Root Cause: Exception: Middle cause: Root Cause: Exception: Root cause error",
        topLevel.toString());

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
  void testJavaExceptionFactory() {
    // Test static factory method with java.lang.Exception
    final var javaException = new java.lang.Exception("Java exception message");
    final var ek9FromJavaException = Exception._of(javaException);
    assertTrue.accept(ek9FromJavaException._isSet());
    
    // The toString should show the underlying Java exception message
    final var toStringResult = ek9FromJavaException.toString();
    assertNotNull(toStringResult);
    assertTrue.accept(Boolean._of(toStringResult.contains("Exception:")));
    assertTrue.accept(Boolean._of(toStringResult.contains("Java exception message")));
    
    // Test with IOException (subclass of java.lang.Exception)
    final var ioException = new java.io.IOException("IO error occurred");
    final var ek9FromIOException = Exception._of(ioException);
    assertTrue.accept(ek9FromIOException._isSet());
    final var ioToString = ek9FromIOException.toString();
    assertTrue.accept(Boolean._of(ioToString.contains("IO error occurred")));
    
    // Test with null java.lang.Exception
    final var nullJavaException = Exception._of((java.lang.Exception) null);
    assertTrue.accept(nullJavaException._isSet());
    
    // Test cause chain from Java exception
    final var javaExceptionWithCause = new java.lang.Exception("Outer", javaException);
    final var ek9WithCause = Exception._of(javaExceptionWithCause);
    assertTrue.accept(ek9WithCause._isSet());
    assertEquals(javaExceptionWithCause, ek9WithCause.getCause());
    
    // Test that the EK9 Exception wraps the Java exception properly
    final var checkedEx = new java.lang.Exception("Checked exception");
    final var wrappedEx = Exception._of(checkedEx);
    assertTrue.accept(wrappedEx._isSet());
    assertEquals(checkedEx, wrappedEx.getCause());
    
    // Test reason() method returns unset for Java exception factory (no EK9 reason set)
    final var reasonResult = ek9FromJavaException.reason();
    assertUnset.accept(reasonResult);
    
    // Test exitCode() method returns unset for Java exception factory (no exit code set)
    final var exitCodeResult = ek9FromJavaException.exitCode();
    assertUnset.accept(exitCodeResult);
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
    assertEquals("Exception: Level 3: Root Cause: Exception: Level 2: Root Cause: Exception: Level 1: Exit Code: 42",
        level3.toString());
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
    assertEquals(exception1._string(), exception2._string());
    assertNotEquals(exception1._string(), exception3._string());

    // Unset exceptions
    final var unset1 = new Exception();
    assertUnset.accept(unset1._string());
  }

  @Test
  void testExceptionWithSpecialCharacters() {
    // Test with special characters in reason
    final var specialCharsReason = String._of("Error: Invalid input! @#$%^&*()");
    final var specialCharsException = new Exception(specialCharsReason);
    assertEquals(String._of("Exception: Error: Invalid input! @#$%^&*()"), specialCharsException._string());

    // Test with newlines and tabs
    final var newlineReason = String._of("Line 1\nLine 2\tTabbed");
    final var newlineException = new Exception(newlineReason);
    assertEquals(String._of("Exception: Line 1\nLine 2\tTabbed"), newlineException._string());

    // Test with Unicode characters
    final var unicodeReason = String._of("Unicode test: \u2603 \u263A \u2665");
    final var unicodeException = new Exception(unicodeReason);
    assertEquals(String._of("Exception: Unicode test: \u2603 \u263A \u2665"), unicodeException._string());
  }

  @Test
  void testComparisonOperator() {
    // Test _cmp operator behavior
    final var exception1 = new Exception(String._of("Error A"));
    final var exception2 = new Exception(String._of("Error A"));
    final var exception3 = new Exception(String._of("Error B"));

    // Same string content should compare equal (0)
    final var compareEqual = exception1._cmp(exception2);
    assertSet.accept(compareEqual);
    assertEquals(0, compareEqual.state);

    // Different string content should compare unequal
    final var compareDifferent = exception1._cmp(exception3);
    assertSet.accept(compareDifferent);
    // "Error A" < "Error B" lexicographically
    assertTrue.accept(Boolean._of(compareDifferent.state < 0));

    // Reverse comparison
    final var compareReverse = exception3._cmp(exception1);
    assertSet.accept(compareReverse);
    // "Error B" > "Error A" lexicographically
    assertTrue.accept(Boolean._of(compareReverse.state > 0));

    // Compare unset exception with set exception
    assertUnset.accept(unsetException._cmp(exception1));
    assertUnset.accept(exception1._cmp(unsetException));

    // Compare unset exception with unset exception
    assertUnset.accept(unsetException._cmp(unsetException));

    // Compare exception with non-exception Any type
    final var nonException = new Any() {};
    assertUnset.accept(exception1._cmp(nonException));

    // Compare exception with null
    assertUnset.accept(exception1._cmp(null));
  }

  @Test
  void testHashCodeOperator() {
    // Test _hashcode operator behavior
    final var exception1 = new Exception(STR_TEST_MESSAGE);
    final var exception2 = new Exception(STR_TEST_MESSAGE);
    final var exception3 = new Exception(String._of("Different message"));

    // Set exceptions should have set hashcodes
    final var hash1 = exception1._hashcode();
    assertSet.accept(hash1);
    final var hash2 = exception2._hashcode();
    assertSet.accept(hash2);
    final var hash3 = exception3._hashcode();
    assertSet.accept(hash3);

    // Exceptions with same string content should have same hashcode
    assertEquals(hash1.state, hash2.state);

    // Exceptions with different string content should have different hashcodes
    assertNotEquals(hash1.state, hash3.state);

    // Unset exception should have unset hashcode
    final var unsetHash = unsetException._hashcode();
    assertUnset.accept(unsetHash);

    // Test hashcode consistency with string hashcode
    final var stringHash = exception1._string()._hashcode();
    assertSet.accept(stringHash);
    assertEquals(hash1.state, stringHash.state);

    // Test with various exception types
    final var exitCodeException = new Exception(String._of("Exit error"), exitCode42);
    final var exitCodeHash = exitCodeException._hashcode();
    assertSet.accept(exitCodeHash);

    final var causeException = new Exception(String._of("Cause error"), exception1);
    final var causeHash = causeException._hashcode();
    assertSet.accept(causeHash);
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var reasonException = new Exception(simpleReason);
    final var reasonJson = reasonException._json();
    assertSet.accept(reasonJson);

    final var exitCodeException = new Exception(complexReason, exitCode42);
    final var exitCodeJson = exitCodeException._json();
    assertSet.accept(exitCodeJson);

    final var causeException = new Exception(simpleReason, reasonException);
    final var causeJson = causeException._json();
    assertSet.accept(causeJson);

    // Test JSON conversion with unset value
    assertUnset.accept(unsetException._json());

    // Test that JSON content matches string representation
    final var testException = new Exception(String._of("JSON test"));
    final var testJson = testException._json();
    assertSet.accept(testJson);
    
    // Verify JSON was created from string representation
    // (We can't directly test JSON content, but we can verify it's set and non-null)
    assertNotNull(testJson);
    assertTrue.accept(testJson._isSet());
  }
}