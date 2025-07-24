package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Test Result of (JSON, String) parameterized type.
 * This tests the delegation pattern and JSON/String-specific type safety.
 * Result encapsulates either an OK JSON value or an ERROR String value.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456Test extends Common {

  // Type alias for the long parameterized Result class name
  private static final Class<_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456> RESULT_JSON_STRING = 
      _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456.class;
  
  // Test data setup - JSON values
  private final JSON testJsonString = JSON._of("\"SuccessValue\"");
  private final JSON testJsonNumber = JSON._of("42");
  private final JSON testJsonObject = JSON._of("{\"key\": \"value\"}");
  private final JSON testJsonArray = JSON._of("[1, 2, 3]");
  
  // Test data setup - String error values
  private final String testErrorValue = String._of("ErrorValue");
  private final String testAnotherError = String._of("AnotherError");
  
  // Factory methods for cleaner test code
  private static _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 resultJsonString() {
    return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456();
  }
  
  // Helper methods for reducing assertion duplication
  
  /**
   * Helper method to assert that a Result&lt;JSON, String&gt; is unset and verify all related state.
   */
  private void assertResultUnset(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 result) {
    assertUnset.accept(result);
    assertFalse.accept(result._isSet());
    assertTrue.accept(result._empty());
    assertFalse.accept(result.isOk());
    assertFalse.accept(result.isError());
  }
  
  /**
   * Helper method to assert that a Result&lt;JSON, String&gt; is OK and verify all related state.
   */
  private void assertResultOk(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 result) {
    assertSet.accept(result);
    assertTrue.accept(result._isSet());
    assertFalse.accept(result._empty());
    assertTrue.accept(result.isOk());
    assertFalse.accept(result.isError());
  }
  
  /**
   * Helper method to assert that a Result&lt;JSON, String&gt; is ERROR and verify all related state.
   */
  private void assertResultError(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 result) {
    assertNotNull(result);
    assertFalse.accept(result._isSet()); // _isSet() returns isOk()
    assertFalse.accept(result._empty());
    assertFalse.accept(result.isOk());
    assertTrue.accept(result.isError());
  }
  
  /**
   * Mock Acceptor for JSON testing.
   */
  private static class MockJsonAcceptor extends _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 {
    private JSON calledWith;

    @Override
    public void _call(JSON t) {
      super._call(t);
      this.calledWith = t;
    }

    public boolean verifyNotCalled() {
      return calledWith == null;
    }

    public boolean verifyCalledWith(JSON t) {
      if (calledWith == null) {
        return false;
      }
      return calledWith == t;
    }
  }
  
  /**
   * Mock Consumer for JSON testing.
   */
  private static class MockJsonConsumer extends _Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 {
    private JSON calledWith;

    @Override
    public void _call(JSON t) {
      super._call(t);
      this.calledWith = t;
    }

    public boolean verifyNotCalled() {
      return calledWith == null;
    }

    public boolean verifyCalledWith(JSON t) {
      if (calledWith == null) {
        return false;
      }
      return calledWith == t;
    }
  }
  
  /**
   * Mock Acceptor for String testing.
   */
  private static class MockStringAcceptor extends _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA {
    private String calledWith;

    @Override
    public void _call(String t) {
      super._call(t);
      this.calledWith = t;
    }

    public boolean verifyNotCalled() {
      return calledWith == null;
    }

    public boolean verifyCalledWith(String t) {
      if (calledWith == null) {
        return false;
      }
      return calledWith == t;
    }
  }
  
  /**
   * Mock Consumer for String testing.
   */
  private static class MockStringConsumer extends _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 {
    private String calledWith;

    @Override
    public void _call(String t) {
      super._call(t);
      this.calledWith = t;
    }

    public boolean verifyNotCalled() {
      return calledWith == null;
    }

    public boolean verifyCalledWith(String t) {
      if (calledWith == null) {
        return false;
      }
      return calledWith == t;
    }
  }
  
  /**
   * Helper method to test whenOk behavior with both Acceptor and Consumer.
   * Tests that the callbacks are called with the expected JSON value.
   */
  private void assertWhenOkCalled(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 result, JSON expectedValue) {
    final var acceptor = new MockJsonAcceptor();
    result.whenOk(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyCalledWith(expectedValue)));
    
    final var consumer = new MockJsonConsumer();
    result.whenOk(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyCalledWith(expectedValue)));
  }
  
  /**
   * Helper method to test whenOk behavior with both Acceptor and Consumer.
   * Tests that the callbacks are NOT called.
   */
  private void assertWhenOkNotCalled(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 result) {
    final var acceptor = new MockJsonAcceptor();
    result.whenOk(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyNotCalled()));
    
    final var consumer = new MockJsonConsumer();
    result.whenOk(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyNotCalled()));
  }
  
  /**
   * Helper method to test whenError behavior with both Acceptor and Consumer.
   * Tests that the callbacks are called with the expected String value.
   */
  private void assertWhenErrorCalled(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 result, String expectedValue) {
    final var acceptor = new MockStringAcceptor();
    result.whenError(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyCalledWith(expectedValue)));
    
    final var consumer = new MockStringConsumer();
    result.whenError(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyCalledWith(expectedValue)));
  }
  
  /**
   * Helper method to test whenError behavior with both Acceptor and Consumer.
   * Tests that the callbacks are NOT called.
   */
  private void assertWhenErrorNotCalled(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 result) {
    final var acceptor = new MockStringAcceptor();
    result.whenError(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyNotCalled()));
    
    final var consumer = new MockStringConsumer();
    result.whenError(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyNotCalled()));
  }

  @Test
  void testConstructionAndFactoryMethods() {
    // Test default constructor
    final var defaultResult = resultJsonString();
    assertResultUnset(defaultResult);

    // Test factory method with no arguments
    final var factoryEmpty = _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456._of();
    assertResultUnset(factoryEmpty);

    // Test factory method with base Result
    final var baseResult = Result._ofOk(testJsonString);
    final var factoryFromResult = _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456._of(baseResult);
    assertResultOk(factoryFromResult);
    assertEquals(testJsonString, factoryFromResult.ok());

    // Test factory method with null Result
    final var factoryFromNull = _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456._of(null);
    assertResultUnset(factoryFromNull);
    
    // Test factory method with JSON and String values
    final var factoryWithValues = _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456._of(testJsonString, testErrorValue);
    assertNotNull(factoryWithValues);
    assertTrue.accept(factoryWithValues.isOk());
    assertTrue.accept(factoryWithValues.isError());
  }

  @Test
  void testAsMethodFactories() {
    final var result = resultJsonString();

    // asEmpty should return empty Result
    final var empty = result.asEmpty();
    assertResultUnset(empty);

    // asOk should return OK Result with JSON value
    final var ok = result.asOk(testJsonString);
    assertResultOk(ok);
    assertEquals(testJsonString, ok.ok());

    // asError should return ERROR Result with String value
    final var error = result.asError(testErrorValue);
    assertResultError(error);
    assertEquals(testErrorValue, error.error());

    // asOk with null should return empty Result
    final var okNull = result.asOk(null);
    assertResultUnset(okNull);

    // asError with null should return empty Result
    final var errorNull = result.asError(null);
    assertResultUnset(errorNull);
  }

  @Test
  void testJsonTypeSpecificOperations() {
    // Test with different JSON value types
    final var stringResult = resultJsonString().asOk(testJsonString);
    final var numberResult = resultJsonString().asOk(testJsonNumber);
    final var objectResult = resultJsonString().asOk(testJsonObject);
    final var arrayResult = resultJsonString().asOk(testJsonArray);

    // Verify all different JSON types work correctly
    assertResultOk(stringResult);
    assertEquals(testJsonString, stringResult.ok());
    
    assertResultOk(numberResult);
    assertEquals(testJsonNumber, numberResult.ok());
    
    assertResultOk(objectResult);
    assertEquals(testJsonObject, objectResult.ok());
    
    assertResultOk(arrayResult);
    assertEquals(testJsonArray, arrayResult.ok());

    // Test getOrDefault with JSON values
    final var errorResult = resultJsonString().asError(testErrorValue);
    assertEquals(testJsonString, errorResult.getOrDefault(testJsonString));
    assertEquals(testJsonObject, errorResult.okOrDefault(testJsonObject));
  }

  @Test
  void testStateManagement() {
    final var unsetResult = resultJsonString();
    assertNotNull(unsetResult);
    final var okResult = resultJsonString().asOk(testJsonString);
    final var errorResult = resultJsonString().asError(testErrorValue);

    // Unset Result state
    assertTrue.accept(unsetResult._empty());
    assertFalse.accept(unsetResult.isOk());
    assertFalse.accept(unsetResult.isError());
    assertFalse.accept(unsetResult._isSet());

    // OK Result state
    assertFalse.accept(okResult._empty());
    assertTrue.accept(okResult.isOk());
    assertFalse.accept(okResult.isError());
    assertTrue.accept(okResult._isSet()); // _isSet() returns isOk()

    // ERROR Result state
    assertFalse.accept(errorResult._empty());
    assertFalse.accept(errorResult.isOk());
    assertTrue.accept(errorResult.isError());
    assertFalse.accept(errorResult._isSet()); // _isSet() returns isOk()
  }

  @Test
  void testValueAccess() {
    final var okResult = resultJsonString().asOk(testJsonString);
    final var errorResult = resultJsonString().asError(testErrorValue);
    final var unsetResult = resultJsonString();

    // OK value access - JSON specific
    assertEquals(testJsonString, okResult.ok());
    assertEquals(testJsonString, okResult.okOrDefault(testJsonObject));
    assertEquals(testJsonString, okResult.getOrDefault(testJsonArray));

    // ERROR value access - String specific
    assertEquals(testErrorValue, errorResult.error());

    // Default value access with JSON types
    assertEquals(testJsonNumber, unsetResult.okOrDefault(testJsonNumber));
    assertEquals(testJsonObject, unsetResult.getOrDefault(testJsonObject));
    assertEquals(testJsonArray, errorResult.okOrDefault(testJsonArray));

    // Exception on wrong access
    assertThrows(Exception.class, okResult::error);
    assertThrows(Exception.class, errorResult::ok);
    assertThrows(Exception.class, unsetResult::ok);
    assertThrows(Exception.class, unsetResult::error);
  }

  @Test
  void testWhenOkWithJsonTypes() {
    final var okResult = resultJsonString().asOk(testJsonString);
    final var errorResult = resultJsonString().asError(testErrorValue);
    final var unsetResult = resultJsonString();

    // OK Result should call acceptor/consumer with JSON value
    assertWhenOkCalled(okResult, testJsonString);

    // ERROR Result should not call acceptor/consumer
    assertWhenOkNotCalled(errorResult);

    // unset Result should not call acceptor/consumer
    assertWhenOkNotCalled(unsetResult);
  }

  @Test
  void testWhenErrorWithStringTypes() {
    final var okResult = resultJsonString().asOk(testJsonString);
    final var errorResult = resultJsonString().asError(testErrorValue);
    final var unsetResult = resultJsonString();

    // ERROR Result should call acceptor/consumer with String value
    assertWhenErrorCalled(errorResult, testErrorValue);

    // OK Result should not call acceptor/consumer
    assertWhenErrorNotCalled(okResult);

    // unset Result should not call acceptor/consumer
    assertWhenErrorNotCalled(unsetResult);
  }

  @Test
  void testIteratorWithJsonTypes() {
    final var okResult = resultJsonString().asOk(testJsonString);
    final var errorResult = resultJsonString().asError(testErrorValue);
    final var unsetResult = resultJsonString();

    // OK Result should have iterator with JSON value
    final var okIterator = okResult.iterator();
    assertSet.accept(okIterator);
    assertTrue.accept(okIterator.hasNext());
    assertEquals(testJsonString, okIterator.next());

    // ERROR Result should have unset iterator
    final var errorIterator = errorResult.iterator();
    assertUnset.accept(errorIterator);

    // unset Result should have unset iterator
    final var emptyIterator = unsetResult.iterator();
    assertUnset.accept(emptyIterator);
  }

  @Test
  void testEqualityOperators() {
    final var okResult1 = resultJsonString().asOk(testJsonString);
    final var okResult2 = resultJsonString().asOk(testJsonString);
    final var okResult3 = resultJsonString().asOk(testJsonNumber);
    final var errorResult1 = resultJsonString().asError(testErrorValue);
    final var errorResult2 = resultJsonString().asError(testErrorValue);
    final var errorResult3 = resultJsonString().asError(testAnotherError);
    final var unsetResult1 = resultJsonString();
    final var unsetResult2 = resultJsonString();

    // Same OK JSON values should be equal
    assertTrue.accept(okResult1._eq(okResult2));
    assertFalse.accept(okResult1._neq(okResult2));

    // Different OK JSON values should not be equal
    assertFalse.accept(okResult1._eq(okResult3));
    assertTrue.accept(okResult1._neq(okResult3));

    // Same ERROR String values should be equal
    assertTrue.accept(errorResult1._eq(errorResult2));
    assertFalse.accept(errorResult1._neq(errorResult2));

    // Different ERROR String values should not be equal
    assertFalse.accept(errorResult1._eq(errorResult3));
    assertTrue.accept(errorResult1._neq(errorResult3));

    // unset Results should result in unset value
    assertUnset.accept(unsetResult1._eq(unsetResult2));
    assertUnset.accept(unsetResult1._neq(unsetResult2));

    // OK and ERROR should not be equal
    assertFalse.accept(okResult1._eq(errorResult1));
    assertTrue.accept(okResult1._neq(errorResult1));

    // Test _eq with Any (polymorphic)
    assertNotNull(okResult1._eq((Any) okResult2));
    assertUnset.accept(okResult1._eq((Any) null));
    assertUnset.accept(okResult1._eq(testJsonString));
  }

  @Test
  void testOperatorConsistencyAndTypeSafety() {
    final var result = resultJsonString().asOk(testJsonString);

    // Test operator consistency
    assertNotNull(result._isSet());
    assertNotNull(result._string());
    assertNotNull(result._hashcode());

    // Verify class type is correct
    assertEquals(RESULT_JSON_STRING, result.getClass());

    // Test contains operations with JSON and String types
    assertTrue.accept(result._contains(testJsonString));
    assertFalse.accept(result._contains(testJsonNumber));
    assertFalse.accept(result._containsError(testErrorValue));

    final var errorResult = resultJsonString().asError(testErrorValue);
    assertFalse.accept(errorResult._contains(testJsonString));
    assertTrue.accept(errorResult._containsError(testErrorValue));
    assertFalse.accept(errorResult._containsError(testAnotherError));
  }

  @Test
  void testAssignmentOperatorsWithJsonTypes() {
    final var result1 = resultJsonString();
    final var okResult = resultJsonString().asOk(testJsonString);
    final var errorResult = resultJsonString().asError(testErrorValue);

    // Copy operation with OK Result containing JSON
    result1._copy(okResult);
    assertTrue.accept(result1.isOk());
    assertEquals(testJsonString, result1.ok());

    // Copy operation with ERROR Result containing String
    result1._copy(errorResult);
    assertTrue.accept(result1.isError());
    assertEquals(testErrorValue, result1.error());

    // Copy operation with null should clear
    result1._copy(null);
    assertResultUnset(result1);

    // Replace operation (same as copy)
    result1._replace(okResult);
    assertTrue.accept(result1.isOk());
    assertEquals(testJsonString, result1.ok());

    // Merge operation
    final var result2 = resultJsonString();
    result2._merge(okResult);
    assertTrue.accept(result2.isOk());
    assertEquals(testJsonString, result2.ok());
  }

  @Test
  void testPipeOperatorWithJsonTypes() {
    final var result = resultJsonString();

    // Pipe JSON value
    result._pipe(testJsonString);
    assertTrue.accept(result.isOk());
    assertEquals(testJsonString, result.ok());

    // Pipe different JSON types
    final var result2 = resultJsonString();
    result2._pipe(testJsonObject);
    assertTrue.accept(result2.isOk());
    assertEquals(testJsonObject, result2.ok());

    // Pipe null values should not change state
    final var result3 = resultJsonString();
    result3._pipe(null);
    assertTrue.accept(result3._empty());
  }

  @Test
  void testStringAndHashOperations() {
    final var okResult = resultJsonString().asOk(testJsonString);
    final var errorResult = resultJsonString().asError(testErrorValue);
    final var unsetResult = resultJsonString();

    // String operations should work with JSON/String content
    final var okString = okResult._string();
    assertSet.accept(okString);
    assertNotNull(okString.state);

    final var errorString = errorResult._string();
    assertSet.accept(errorString);
    assertNotNull(errorString.state);

    final var emptyString = unsetResult._string();
    assertUnset.accept(emptyString);

    // Hash code operations
    final var okHash = okResult._hashcode();
    assertSet.accept(okHash);

    final var errorHash = errorResult._hashcode();
    assertSet.accept(errorHash);

    final var emptyHash = unsetResult._hashcode();
    assertUnset.accept(emptyHash);

    // Different Results should likely have different hash codes
    assertNotEquals(okHash, errorHash);
  }

  @Test
  void testEdgeCasesWithJsonTypes() {
    // Test with various JSON natures
    final var nullResult = resultJsonString().asOk(JSON._of("null"));
    final var boolResult = resultJsonString().asOk(JSON._of("true"));
    final var arrayResult = resultJsonString().asOk(JSON._of("[]"));
    final var objectResult = resultJsonString().asOk(JSON._of("{}"));

    // Verify different JSON natures work correctly
    assertResultOk(nullResult);
    assertResultOk(boolResult);
    assertResultOk(arrayResult);
    assertResultOk(objectResult);

    // Test toString method
    assertNotNull(nullResult.toString());
    assertNotNull(boolResult.toString());
    assertNotNull(arrayResult.toString());
    assertNotNull(objectResult.toString());

    // Test empty operator
    final var unsetResult = resultJsonString();
    assertTrue.accept(unsetResult._empty());
    assertFalse.accept(nullResult._empty());
  }

}