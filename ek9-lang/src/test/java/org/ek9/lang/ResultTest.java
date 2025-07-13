package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for Result class following EK9 patterns.
 * Tests all constructors, methods, operators, and edge cases.
 */
class ResultTest extends Common {

  // Test data setup
  private final String testOkValue = String._of("SuccessValue");
  private final String testErrorValue = String._of("ErrorValue");
  private final Integer testOkInt = Integer._of(42);
  private final Integer testErrorInt = Integer._of(-1);
  private final Boolean testOkBool = Boolean._of(true);
  private final Boolean testErrorBool = Boolean._of(false);

  @Test
  void testConstruction() {
    // Default constructor should create unset Result
    final var defaultConstructor = new Result();
    assertNotNull(defaultConstructor);

    assertUnset.accept(defaultConstructor);
    assertFalse.accept(defaultConstructor._isSet());
    assertTrue.accept(defaultConstructor._empty());
    assertFalse.accept(defaultConstructor.isOk());
    assertFalse.accept(defaultConstructor.isError());

    // Two-parameter constructor should not be empty
    final var twoParamConstructor = new Result(testOkValue, testErrorValue);
    assertNotNull(twoParamConstructor);
    assertSet.accept(twoParamConstructor);
    assertTrue.accept(twoParamConstructor._isSet());
    assertFalse.accept(twoParamConstructor._empty());

    // Factory method with no args should create empty Result
    final var factoryEmpty = Result._of();
    assertUnset.accept(factoryEmpty);
    assertFalse.accept(factoryEmpty._isSet());
    assertTrue.accept(factoryEmpty._empty());

    // Factory method with OK value should create OK Result and not be empty.
    final var factoryOk = Result._ofOk(testOkValue);
    assertSet.accept(factoryOk);
    assertTrue.accept(factoryOk._isSet());
    assertFalse.accept(factoryOk._empty());
    assertTrue.accept(factoryOk.isOk());
    assertFalse.accept(factoryOk.isError());

    // Factory method with error value should create ERROR Result
    final var factoryError = Result._ofError(testErrorValue);
    assertNotNull(factoryError);
    assertFalse.accept(factoryError._isSet()); // _isSet() returns isOk()
    assertFalse.accept(factoryError._empty());
    assertFalse.accept(factoryError.isOk());
    assertTrue.accept(factoryError.isError());
  }

  @Test
  void testAsMethodsFactories() {
    final var result = new Result();

    // asEmpty should return empty Result
    final var empty = result.asEmpty();
    assertUnset.accept(empty);
    assertTrue.accept(empty._empty());

    // asOk should return OK Result
    final var ok = result.asOk(testOkValue);
    assertSet.accept(ok);
    assertTrue.accept(ok.isOk());
    assertFalse.accept(ok.isError());
    assertEquals(testOkValue, ok.ok());

    // asError should return ERROR Result
    final var error = result.asError(testErrorValue);
    assertNotNull(error);
    assertFalse.accept(error.isOk());
    assertTrue.accept(error.isError());
    assertEquals(testErrorValue, error.error());

    // asOk with null should return empty Result
    final var okNull = result.asOk(null);
    assertUnset.accept(okNull);
    assertTrue.accept(okNull._empty());

    // asError with null should return empty Result
    final var errorNull = result.asError(null);
    assertUnset.accept(errorNull);
    assertTrue.accept(errorNull._empty());
  }

  @Test
  void testStateManagement() {
    final var unsetResult = new Result();
    assertNotNull(unsetResult);

    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);

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
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // OK value access
    assertEquals(testOkValue, okResult.ok());
    assertEquals(testOkValue, okResult.okOrDefault(String._of("default")));
    assertEquals(testOkValue, okResult.getOrDefault(String._of("default")));

    // ERROR value access
    assertEquals(testErrorValue, errorResult.error());

    // Default value access
    final var defaultValue = String._of("default");
    assertEquals(defaultValue, unsetResult.okOrDefault(defaultValue));
    assertEquals(defaultValue, unsetResult.getOrDefault(defaultValue));
    assertEquals(defaultValue, errorResult.okOrDefault(defaultValue));
    assertEquals(defaultValue, errorResult.getOrDefault(defaultValue));

    // Exception on wrong access
    assertThrows(Exception.class, okResult::error);
    assertThrows(Exception.class, errorResult::ok);
    assertThrows(Exception.class, unsetResult::ok);
    assertThrows(Exception.class, unsetResult::error);
  }

  @Test
  void testWhenOkWithAcceptor() {
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // OK Result should call acceptor
    final var mockAcceptor1 = new MockAcceptor();
    okResult.whenOk(mockAcceptor1);
    assertTrue.accept(Boolean._of(mockAcceptor1.verifyCalledWith(testOkValue)));

    // ERROR Result should not call acceptor
    final var mockAcceptor2 = new MockAcceptor();
    errorResult.whenOk(mockAcceptor2);
    assertTrue.accept(Boolean._of(mockAcceptor2.verifyNotCalled()));

    // unset Result should not call acceptor
    final var mockAcceptor3 = new MockAcceptor();
    unsetResult.whenOk(mockAcceptor3);
    assertTrue.accept(Boolean._of(mockAcceptor3.verifyNotCalled()));
  }

  @Test
  void testWhenOkWithConsumer() {
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // OK Result should call consumer
    final var mockConsumer1 = new MockConsumer();
    okResult.whenOk(mockConsumer1);
    assertTrue.accept(Boolean._of(mockConsumer1.verifyCalledWith(testOkValue)));

    // ERROR Result should not call consumer
    final var mockConsumer2 = new MockConsumer();
    errorResult.whenOk(mockConsumer2);
    assertTrue.accept(Boolean._of(mockConsumer2.verifyNotCalled()));

    // unset Result should not call consumer
    final var mockConsumer3 = new MockConsumer();
    unsetResult.whenOk(mockConsumer3);
    assertTrue.accept(Boolean._of(mockConsumer3.verifyNotCalled()));
  }

  @Test
  void testWhenErrorWithAcceptor() {
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // ERROR Result should call acceptor
    final var mockAcceptor1 = new MockAcceptor();
    errorResult.whenError(mockAcceptor1);
    assertTrue.accept(Boolean._of(mockAcceptor1.verifyCalledWith(testErrorValue)));

    // OK Result should not call acceptor
    final var mockAcceptor2 = new MockAcceptor();
    okResult.whenError(mockAcceptor2);
    assertTrue.accept(Boolean._of(mockAcceptor2.verifyNotCalled()));

    // unset Result should not call acceptor
    final var mockAcceptor3 = new MockAcceptor();
    unsetResult.whenError(mockAcceptor3);
    assertTrue.accept(Boolean._of(mockAcceptor3.verifyNotCalled()));
  }

  @Test
  void testWhenErrorWithConsumer() {
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // ERROR Result should call consumer
    final var mockConsumer1 = new MockConsumer();
    errorResult.whenError(mockConsumer1);
    assertTrue.accept(Boolean._of(mockConsumer1.verifyCalledWith(testErrorValue)));

    // OK Result should not call consumer
    final var mockConsumer2 = new MockConsumer();
    okResult.whenError(mockConsumer2);
    assertTrue.accept(Boolean._of(mockConsumer2.verifyNotCalled()));

    // unset Result should not call consumer
    final var mockConsumer3 = new MockConsumer();
    unsetResult.whenError(mockConsumer3);
    assertTrue.accept(Boolean._of(mockConsumer3.verifyNotCalled()));
  }

  @Test
  void testIterator() {
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // OK Result should have iterator with value
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
    final var okResult1 = Result._ofOk(testOkValue);
    assertNotNull(okResult1);
    final var okResult2 = Result._ofOk(testOkValue);
    final var okResult3 = Result._ofOk(String._of("DifferentValue"));
    final var errorResult1 = Result._ofError(testErrorValue);
    final var errorResult2 = Result._ofError(testErrorValue);
    final var errorResult3 = Result._ofError(String._of("DifferentError"));
    final var unsetResult1 = new Result();
    final var unsetResult2 = new Result();

    // Same OK values should be equal
    assertTrue.accept(okResult1._eq(okResult2));
    assertFalse.accept(okResult1._neq(okResult2));

    // Different OK values should not be equal
    assertFalse.accept(okResult1._eq(okResult3));
    assertTrue.accept(okResult1._neq(okResult3));

    // Same ERROR values should be equal
    assertTrue.accept(errorResult1._eq(errorResult2));
    assertFalse.accept(errorResult1._neq(errorResult2));

    // Different ERROR values should not be equal
    assertFalse.accept(errorResult1._eq(errorResult3));
    assertTrue.accept(errorResult1._neq(errorResult3));

    // unset Results should result in unset value
    assertUnset.accept(unsetResult1._eq(unsetResult2));
    assertUnset.accept(unsetResult1._neq(unsetResult2));

    // OK and ERROR should not be equal
    assertFalse.accept(okResult1._eq(errorResult1));
    assertTrue.accept(okResult1._neq(errorResult1));

    // OK and unset should result in unset value
    assertUnset.accept(okResult1._eq(unsetResult1));
    assertUnset.accept(okResult1._neq(unsetResult1));

    // ERROR and unset should result in unset
    assertUnset.accept(errorResult1._eq(unsetResult1));
    assertUnset.accept(errorResult1._neq(unsetResult1));
  }

  @Test
  void testStringOperators() {
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var okWithErrorResult = Result._of(testOkValue, testErrorValue);
    final var unsetResult = new Result();

    // OK Result string representation
    final var okString = okResult._string();
    assertSet.accept(okString);
    assertEquals(String._of("{SuccessValue}"), okString);

    // ERROR Result string representation
    final var errorString = errorResult._string();
    assertSet.accept(errorString);
    assertEquals(String._of("{ErrorValue}"), errorString);

    final var mixedString = okWithErrorResult._string();
    assertSet.accept(mixedString);
    assertEquals(String._of("{SuccessValue, ErrorValue}"), mixedString);

    // Empty Result string representation
    final var emptyString = unsetResult._string();
    assertUnset.accept(emptyString);
  }

  @Test
  void testHashCodeOperator() {
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // OK Result should have hash code
    final var okHash = okResult._hashcode();
    assertSet.accept(okHash);

    // ERROR Result should have hash code
    final var errorHash = errorResult._hashcode();
    assertSet.accept(errorHash);

    // Empty Result should not have hash code
    final var emptyHash = unsetResult._hashcode();
    assertUnset.accept(emptyHash);

    // Different Results should likely have different hash codes
    assertNotEquals(okHash, errorHash);
  }

  @Test
  void testContainsOperators() {
    final var okResult = Result._ofOk(testOkValue);
    assertNotNull(okResult);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    // OK Result should contain its value
    assertTrue.accept(okResult._contains(testOkValue));
    assertFalse.accept(okResult._contains(String._of("DifferentValue")));

    // ERROR Result should not contain any OK value
    assertFalse.accept(errorResult._contains(testErrorValue));
    assertFalse.accept(errorResult._contains(String._of("AnyValue")));

    // ERROR Result should contain its error value
    assertTrue.accept(errorResult._containsError(testErrorValue));
    assertFalse.accept(errorResult._containsError(String._of("DifferentError")));

    // OK Result should not contain any ERROR value
    assertFalse.accept(okResult._containsError(testOkValue));
    assertFalse.accept(okResult._containsError(String._of("AnyError")));

    // Empty Result should not contain anything
    assertFalse.accept(unsetResult._contains(testOkValue));
    assertFalse.accept(unsetResult._containsError(testErrorValue));
  }

  @Test
  void testAssignmentOperators() {
    final var result1 = new Result();
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);

    // Copy operation with OK Result
    result1._copy(okResult);
    assertTrue.accept(result1.isOk());
    assertEquals(testOkValue, result1.ok());

    // Copy operation with ERROR Result
    result1._copy(errorResult);
    assertTrue.accept(result1.isError());
    assertEquals(testErrorValue, result1.error());

    // Copy operation with null should clear
    result1._copy(null);
    assertUnset.accept(result1);
    assertTrue.accept(result1._empty());

    // Replace operation (same as copy)
    result1._replace(okResult);
    assertTrue.accept(result1.isOk());
    assertEquals(testOkValue, result1.ok());

    // Merge operation (same as copy)
    result1._merge(errorResult);
    assertTrue.accept(result1.isError());
    assertEquals(testErrorValue, result1.error());
  }

  @Test
  void testPipeOperators() {
    final var result = new Result();

    // Pipe OK value
    result._pipe(testOkValue);
    assertTrue.accept(result.isOk());
    assertEquals(testOkValue, result.ok());

    // Pipe null values should not change state
    final var result2 = new Result();
    result2._pipe(null);
    assertTrue.accept(result2._empty());


  }

  @Test
  void testJavaEquals() {
    final var okResult1 = Result._ofOk(testOkValue);
    final var okResult2 = Result._ofOk(testOkValue);
    final var okResult3 = Result._ofOk(String._of("DifferentValue"));
    final var errorResult1 = Result._ofError(testErrorValue);
    final var errorResult2 = Result._ofError(testErrorValue);
    final var errorResult3 = Result._ofError(String._of("DifferentError"));
    final var unsetResult1 = new Result();
    final var unsetResult2 = new Result();
    final var mixedResult1 = Result._of(testOkValue, testErrorValue);
    final var mixedResult2 = Result._of(testOkValue, testErrorValue);

    // Test reflexivity: x.equals(x) should return true
    assertEquals(okResult1, okResult1);
    assertEquals(errorResult1, errorResult1);
    assertUnset.accept(unsetResult1);
    assertEquals(mixedResult1, mixedResult1);

    // Test symmetry: x.equals(y) should return the same as y.equals(x)
    assertEquals(okResult1, okResult2);
    assertEquals(okResult2, okResult1);
    assertEquals(errorResult1, errorResult2);
    assertEquals(errorResult2, errorResult1);
    assertUnset.accept(unsetResult2);
    assertEquals(mixedResult1, mixedResult2);
    assertEquals(mixedResult2, mixedResult1);

    // Test inequality
    assertNotEquals(okResult1, okResult3);
    assertNotEquals(okResult1, errorResult1);
    assertNotEquals(okResult1, unsetResult1);
    assertNotEquals(errorResult1, errorResult3);
    assertNotEquals(errorResult1, unsetResult1);

    // Test with null
    assertNotEquals(null, okResult1);
    assertNotEquals(null, errorResult1);

    // Test with different types
    assertNotEquals("not a Result", okResult1);
    assertNotEquals(okResult1, testOkValue);

    // Test hash code consistency: equal objects must have equal hash codes
    assertEquals(okResult1.hashCode(), okResult2.hashCode());
    assertEquals(errorResult1.hashCode(), errorResult2.hashCode());
    assertEquals(unsetResult1.hashCode(), unsetResult2.hashCode());
    assertEquals(mixedResult1.hashCode(), mixedResult2.hashCode());
  }

  @Test
  void testEdgeCases() {
    // Test with different types
    final var intOkResult = Result._ofOk(testOkInt);
    final var intErrorResult = Result._ofError(testErrorInt);
    final var boolOkResult = Result._ofOk(testOkBool);
    final var boolErrorResult = Result._ofError(testErrorBool);

    // Verify different types work correctly
    assertTrue.accept(intOkResult.isOk());
    assertEquals(testOkInt, intOkResult.ok());
    assertTrue.accept(intErrorResult.isError());
    assertEquals(testErrorInt, intErrorResult.error());

    assertTrue.accept(boolOkResult.isOk());
    assertEquals(testOkBool, boolOkResult.ok());
    assertTrue.accept(boolErrorResult.isError());
    assertEquals(testErrorBool, boolErrorResult.error());

    // Test toString method
    final var okResult = Result._ofOk(testOkValue);
    final var errorResult = Result._ofError(testErrorValue);
    final var unsetResult = new Result();

    assertNotNull(okResult.toString());
    assertNotNull(errorResult.toString());
    assertNotNull(unsetResult.toString());
  }

}