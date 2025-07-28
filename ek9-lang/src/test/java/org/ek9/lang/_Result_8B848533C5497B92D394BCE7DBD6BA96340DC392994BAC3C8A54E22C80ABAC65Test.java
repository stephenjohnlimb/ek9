package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Test Result of (String, Boolean) parameterized type.
 * This tests the delegation pattern and String/Boolean-specific type safety.
 * Result encapsulates either an OK String value or an ERROR Boolean value.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65Test extends Common {

  // Type alias for the long parameterized Result class name
  private static final Class<_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65> RESULT_STRING_BOOLEAN = 
      _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65.class;
  
  // Test data setup - String OK values
  private final String testOkValue = String._of("SuccessValue");
  private final String testAnotherOk = String._of("AnotherSuccess");
  private final String emptyStringOk = String._of("");
  
  // Test data setup - Boolean error values
  private final Boolean testErrorValue = Boolean._of(true);
  private final Boolean testAnotherError = Boolean._of(false);
  
  // Factory methods for cleaner test code
  private static _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 resultStringBoolean() {
    return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65();
  }
  
  // Helper methods for reducing assertion duplication
  
  /**
   * Helper method to assert that a Result&lt;String, Boolean&gt; is unset and verify all related state.
   */
  private void assertResultUnset(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 result) {
    assertUnset.accept(result);
    assertFalse.accept(result._isSet());
    assertTrue.accept(result._empty());
    assertFalse.accept(result.isOk());
    assertFalse.accept(result.isError());
  }
  
  /**
   * Helper method to assert that a Result&lt;String, Boolean&gt; is set as OK and verify state.
   */
  private void assertResultOk(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 result, String expectedValue) {
    assertSet.accept(result);
    assertTrue.accept(result._isSet());
    assertFalse.accept(result._empty());
    assertTrue.accept(result.isOk());
    assertFalse.accept(result.isError());
    assertEquals(expectedValue, result.ok());
  }
  
  /**
   * Helper method to assert that a Result&lt;String, Boolean&gt; is set as ERROR and verify state.
   */
  private void assertResultError(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 result, Boolean expectedError) {
    // Note: In EK9 Result semantics, error results are not considered "set" in terms of _isSet()
    // _isSet() only returns true for OK results
    assertNotNull(result);
    assertFalse.accept(result._isSet()); // _isSet() returns isOk()
    assertFalse.accept(result._empty());
    assertFalse.accept(result.isOk());
    assertTrue.accept(result.isError());
    assertEquals(expectedError, result.error());
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
   * Mock Acceptor for Boolean testing.
   */
  private static class MockBooleanAcceptor extends _Acceptor_2C592A33B214C92F1431194345F7C7534F5C064A0710DC28946BDF0EC13E6817 {
    private Boolean calledWith;

    @Override
    public void _call(Boolean t) {
      super._call(t);
      this.calledWith = t;
    }

    public boolean verifyNotCalled() {
      return calledWith == null;
    }

    public boolean verifyCalledWith(Boolean t) {
      if (calledWith == null) {
        return false;
      }
      return calledWith == t;
    }
  }
  
  /**
   * Mock Consumer for Boolean testing.
   */
  private static class MockBooleanConsumer extends _Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 {
    private Boolean calledWith;

    @Override
    public void _call(Boolean t) {
      super._call(t);
      this.calledWith = t;
    }

    public boolean verifyNotCalled() {
      return calledWith == null;
    }

    public boolean verifyCalledWith(Boolean t) {
      if (calledWith == null) {
        return false;
      }
      return calledWith == t;
    }
  }
  
  /**
   * Helper method to test whenOk behavior with both Acceptor and Consumer.
   * Tests that the callbacks are called with the expected String value.
   */
  private void assertWhenOkCalled(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 result, String expectedValue) {
    final var acceptor = new MockStringAcceptor();
    result.whenOk(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyCalledWith(expectedValue)));
    
    final var consumer = new MockStringConsumer();
    result.whenOk(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyCalledWith(expectedValue)));
  }
  
  /**
   * Helper method to test whenOk behavior with both Acceptor and Consumer.
   * Tests that the callbacks are NOT called.
   */
  private void assertWhenOkNotCalled(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 result) {
    final var acceptor = new MockStringAcceptor();
    result.whenOk(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyNotCalled()));
    
    final var consumer = new MockStringConsumer();
    result.whenOk(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyNotCalled()));
  }
  
  /**
   * Helper method to test whenError behavior with both Acceptor and Consumer.
   * Tests that the callbacks are called with the expected Boolean value.
   */
  private void assertWhenErrorCalled(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 result, Boolean expectedValue) {
    final var acceptor = new MockBooleanAcceptor();
    result.whenError(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyCalledWith(expectedValue)));
    
    final var consumer = new MockBooleanConsumer();
    result.whenError(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyCalledWith(expectedValue)));
  }
  
  /**
   * Helper method to test whenError behavior with both Acceptor and Consumer.
   * Tests that the callbacks are NOT called.
   */
  private void assertWhenErrorNotCalled(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 result) {
    final var acceptor = new MockBooleanAcceptor();
    result.whenError(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyNotCalled()));
    
    final var consumer = new MockBooleanConsumer();
    result.whenError(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyNotCalled()));
  }

  @Test
  void testConstructionAndFactoryMethods() {
    // Test default constructor
    final var defaultResult = resultStringBoolean();
    assertResultUnset(defaultResult);

    // Test factory method with no arguments
    final var factoryEmpty = _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65._of();
    assertResultUnset(factoryEmpty);

    // Test factory method with base Result
    final var baseResult = Result._ofOk(testOkValue);
    final var factoryFromResult = _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65._of(baseResult);
    assertResultOk(factoryFromResult, testOkValue);

    // Test factory method with null Result
    final var factoryFromNull = _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65._of(null);
    assertResultUnset(factoryFromNull);
    
    // Test factory method with String and Boolean values
    final var factoryWithValues = _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65._of(testOkValue, testErrorValue);
    assertNotNull(factoryWithValues);
    assertTrue.accept(factoryWithValues.isOk());
    assertTrue.accept(factoryWithValues.isError());
  }

  @Test
  void testAsMethodFactories() {
    final var result = resultStringBoolean();

    // asEmpty should return empty Result
    final var empty = result.asEmpty();
    assertResultUnset(empty);

    // asOk should return OK Result with String value
    final var ok = result.asOk(testOkValue);
    assertResultOk(ok, testOkValue);

    // asError should return ERROR Result with Boolean value
    final var error = result.asError(testErrorValue);
    assertResultError(error, testErrorValue);

    // asOk with null should return empty Result
    final var okNull = result.asOk(null);
    assertResultUnset(okNull);

    // asError with null should return empty Result
    final var errorNull = result.asError(null);
    assertResultUnset(errorNull);
  }

  @Test
  void testStringBooleanTypeSpecificOperations() {
    // Test with different String value types
    final var standardResult = resultStringBoolean().asOk(testOkValue);
    final var emptyStringResult = resultStringBoolean().asOk(emptyStringOk);
    final var anotherResult = resultStringBoolean().asOk(testAnotherOk);

    // Verify all different String types work correctly
    assertResultOk(standardResult, testOkValue);
    assertResultOk(emptyStringResult, emptyStringOk);
    assertResultOk(anotherResult, testAnotherOk);

    // Test with different Boolean error types
    final var trueErrorResult = resultStringBoolean().asError(testErrorValue); // true
    final var falseErrorResult = resultStringBoolean().asError(testAnotherError); // false
    
    assertResultError(trueErrorResult, testErrorValue);
    assertResultError(falseErrorResult, testAnotherError);

    // Test getOrDefault with String values
    final var errorResult = resultStringBoolean().asError(testErrorValue);
    assertEquals(testAnotherOk, errorResult.getOrDefault(testAnotherOk));
    assertEquals(emptyStringOk, errorResult.okOrDefault(emptyStringOk));
  }

  @Test
  void testStateManagement() {
    final var unsetResult = resultStringBoolean();
    assertNotNull(unsetResult);
    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var errorResult = resultStringBoolean().asError(testErrorValue);

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
    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var errorResult = resultStringBoolean().asError(testErrorValue);
    final var unsetResult = resultStringBoolean();

    // OK value access - String specific
    assertEquals(testOkValue, okResult.ok());
    assertEquals(testOkValue, okResult.okOrDefault(testAnotherOk));
    assertEquals(testOkValue, okResult.getOrDefault(emptyStringOk));

    // ERROR value access - Boolean specific
    assertEquals(testErrorValue, errorResult.error());

    // Default value access with String/Boolean types
    assertEquals(testAnotherOk, unsetResult.okOrDefault(testAnotherOk));
    assertEquals(emptyStringOk, unsetResult.getOrDefault(emptyStringOk));
    assertEquals(testAnotherOk, errorResult.okOrDefault(testAnotherOk));
    
    // Error default access
    assertEquals(testAnotherError, unsetResult.errorOrDefault(testAnotherError));
    assertEquals(testAnotherError, okResult.errorOrDefault(testAnotherError));

    // Exception on wrong access
    assertThrows(Exception.class, okResult::error);
    assertThrows(Exception.class, errorResult::ok);
    assertThrows(Exception.class, unsetResult::ok);
    assertThrows(Exception.class, unsetResult::error);
  }

  @Test
  void testWhenOkWithStringTypes() {
    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var errorResult = resultStringBoolean().asError(testErrorValue);
    final var unsetResult = resultStringBoolean();

    // OK Result should call acceptor/consumer with String value
    assertWhenOkCalled(okResult, testOkValue);

    // ERROR Result should not call acceptor/consumer
    assertWhenOkNotCalled(errorResult);

    // unset Result should not call acceptor/consumer
    assertWhenOkNotCalled(unsetResult);
  }

  @Test
  void testWhenErrorWithBooleanTypes() {
    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var errorResult = resultStringBoolean().asError(testErrorValue);
    final var unsetResult = resultStringBoolean();

    // ERROR Result should call acceptor/consumer with Boolean value
    assertWhenErrorCalled(errorResult, testErrorValue);

    // OK Result should not call acceptor/consumer
    assertWhenErrorNotCalled(okResult);

    // unset Result should not call acceptor/consumer
    assertWhenErrorNotCalled(unsetResult);
  }

  @Test
  void testIteratorWithStringTypes() {
    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var errorResult = resultStringBoolean().asError(testErrorValue);
    final var unsetResult = resultStringBoolean();

    // OK Result should have iterator with String value
    final var okIterator = okResult.iterator();
    assertSet.accept(okIterator);
    assertTrue.accept(okIterator.hasNext());
    assertEquals(testOkValue, okIterator.next());

    // ERROR Result should have unset iterator
    final var errorIterator = errorResult.iterator();
    assertUnset.accept(errorIterator);

    // unset Result should have unset iterator
    final var emptyIterator = unsetResult.iterator();
    assertUnset.accept(emptyIterator);
  }

  @Test
  void testEqualityOperators() {
    final var okResult1 = resultStringBoolean().asOk(testOkValue);
    final var okResult2 = resultStringBoolean().asOk(testOkValue);
    final var okResult3 = resultStringBoolean().asOk(testAnotherOk);
    final var errorResult1 = resultStringBoolean().asError(testErrorValue);
    final var errorResult2 = resultStringBoolean().asError(testErrorValue);
    final var errorResult3 = resultStringBoolean().asError(testAnotherError);
    final var unsetResult1 = resultStringBoolean();
    final var unsetResult2 = resultStringBoolean();

    // Same OK String values should be equal
    assertTrue.accept(okResult1._eq(okResult2));
    assertFalse.accept(okResult1._neq(okResult2));

    // Different OK String values should not be equal
    assertFalse.accept(okResult1._eq(okResult3));
    assertTrue.accept(okResult1._neq(okResult3));

    // Same ERROR Boolean values should be equal
    assertTrue.accept(errorResult1._eq(errorResult2));
    assertFalse.accept(errorResult1._neq(errorResult2));

    // Different ERROR Boolean values should not be equal
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
    assertUnset.accept(okResult1._eq(testOkValue));
  }

  @Test
  void testOperatorConsistencyAndTypeSafety() {
    final var result = resultStringBoolean().asOk(testOkValue);

    // Test operator consistency
    assertNotNull(result._isSet());
    assertNotNull(result._string());
    assertNotNull(result._hashcode());

    // Verify class type is correct
    assertEquals(RESULT_STRING_BOOLEAN, result.getClass());

    // Test contains operations with String and Boolean types
    assertTrue.accept(result._contains(testOkValue));
    assertFalse.accept(result._contains(testAnotherOk));
    assertFalse.accept(result._containsError(testErrorValue));

    final var errorResult = resultStringBoolean().asError(testErrorValue);
    assertFalse.accept(errorResult._contains(testOkValue));
    assertTrue.accept(errorResult._containsError(testErrorValue));
    assertFalse.accept(errorResult._containsError(testAnotherError));
  }

  @Test
  void testAssignmentOperatorsWithStringBooleanTypes() {
    final var result1 = resultStringBoolean();
    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var errorResult = resultStringBoolean().asError(testErrorValue);

    // Copy operation with OK Result containing String
    result1._copy(okResult);
    assertTrue.accept(result1.isOk());
    assertEquals(testOkValue, result1.ok());

    // Copy operation with ERROR Result containing Boolean
    result1._copy(errorResult);
    assertTrue.accept(result1.isError());
    assertEquals(testErrorValue, result1.error());

    // Copy operation with null should clear
    result1._copy(null);
    assertResultUnset(result1);

    // Replace operation (same as copy)
    result1._replace(okResult);
    assertTrue.accept(result1.isOk());
    assertEquals(testOkValue, result1.ok());

    // Merge operation
    final var result2 = resultStringBoolean();
    result2._merge(okResult);
    assertTrue.accept(result2.isOk());
    assertEquals(testOkValue, result2.ok());
  }

  @Test
  void testPipeOperatorWithStringTypes() {
    final var result = resultStringBoolean();

    // Pipe String value
    result._pipe(testOkValue);
    assertTrue.accept(result.isOk());
    assertEquals(testOkValue, result.ok());

    // Pipe different String types
    final var result2 = resultStringBoolean();
    result2._pipe(emptyStringOk);
    assertTrue.accept(result2.isOk());
    assertEquals(emptyStringOk, result2.ok());

    // Pipe null values should not change state
    final var result3 = resultStringBoolean();
    result3._pipe((String)null);
    assertTrue.accept(result3._empty());
  }

  @Test
  void testStringAndHashOperations() {
    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var errorResult = resultStringBoolean().asError(testErrorValue);
    final var unsetResult = resultStringBoolean();

    // String operations should work with String/Boolean content
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
  void testEdgeCasesWithStringBooleanTypes() {
    // Test with empty String OK value
    final var emptyResult = resultStringBoolean().asOk(emptyStringOk);
    assertResultOk(emptyResult, emptyStringOk);
    
    // Test with both Boolean error values
    final var trueErrorResult = resultStringBoolean().asError(Boolean._of(true));
    final var falseErrorResult = resultStringBoolean().asError(Boolean._of(false));
    
    assertResultError(trueErrorResult, Boolean._of(true));
    assertResultError(falseErrorResult, Boolean._of(false));

    // Test toString method
    assertNotNull(emptyResult.toString());
    assertNotNull(trueErrorResult.toString());
    assertNotNull(falseErrorResult.toString());

    // Test empty operator
    final var unsetResult = resultStringBoolean();
    assertTrue.accept(unsetResult._empty());
    assertFalse.accept(emptyResult._empty());
    assertFalse.accept(trueErrorResult._empty());
  }

  @Test
  void testAsJson() {
    // Test unset Result of (String, Boolean)
    final var unsetResult = resultStringBoolean();
    assertNotNull(unsetResult);
    final var unsetJson = unsetResult._json();
    assertUnset.accept(unsetJson);

    final var okResult = resultStringBoolean().asOk(testOkValue);
    final var okJson = okResult._json();
    assertSet.accept(okJson);
    assertTrue.accept(okJson.objectNature());

    // Verify the wrapper structure
    final var resultProperty = okJson.get(String._of("result"));
    assertSet.accept(resultProperty);
    assertTrue.accept(resultProperty.objectNature());

    // Verify the inner OK value
    final var okProperty = resultProperty.get(String._of("ok"));
    assertSet.accept(okProperty);
    final var expectedOkJson = testOkValue._json();
    assertTrue.accept(okProperty._eq(expectedOkJson));

    // Verify the inner ERROR value is null/unset (since this is an OK result)
    final var errorProperty = resultProperty.get(String._of("error"));

    assertUnset.accept(errorProperty);

    // Test ERROR Result
    final var errorResult = resultStringBoolean().asError(testErrorValue);
    final var errorJson = errorResult._json();
    assertSet.accept(errorJson);
    assertTrue.accept(errorJson.objectNature());

    // Verify the wrapper structure
    final var errorResultProperty = errorJson.get(String._of("result"));
    assertSet.accept(errorResultProperty);
    assertTrue.accept(errorResultProperty.objectNature());

    // Verify the inner ERROR value
    final var errorInnerProperty = errorResultProperty.get(String._of("error"));
    assertSet.accept(errorInnerProperty);
    final var expectedErrorJson = testErrorValue._json();
    assertTrue.accept(errorInnerProperty._eq(expectedErrorJson));

    // Verify the inner OK value is null/unset (since this is an ERROR result)
    final var okInnerProperty = errorResultProperty.get(String._of("ok"));
    assertUnset.accept(okInnerProperty);

    // Test Result with both values (simulating mixed state for coverage)
    final var mixedResult = _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65._of(testOkValue, testErrorValue);
    final var mixedJson = mixedResult._json();
    assertSet.accept(mixedJson);
    assertTrue.accept(mixedJson.objectNature());

    final var mixedResultProperty = mixedJson.get(String._of("result"));
    assertSet.accept(mixedResultProperty);
    assertTrue.accept(mixedResultProperty.objectNature());

    // Both OK and ERROR should have their JSON representations
    final var mixedOkProperty = mixedResultProperty.get(String._of("ok"));
    final var mixedErrorProperty = mixedResultProperty.get(String._of("error"));
    
    assertSet.accept(mixedOkProperty);
    assertSet.accept(mixedErrorProperty);
    
    assertTrue.accept(mixedOkProperty._eq(testOkValue._json()));
    assertTrue.accept(mixedErrorProperty._eq(testErrorValue._json()));
  }
}