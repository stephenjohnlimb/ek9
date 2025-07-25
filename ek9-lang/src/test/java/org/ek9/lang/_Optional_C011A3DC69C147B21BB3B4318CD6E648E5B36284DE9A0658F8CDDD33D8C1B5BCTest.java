package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BCTest extends Common {

  // Type alias for the long parameterized Optional class name
  private static final Class<_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC> OPT_STRING =
      _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC.class;

  // Factory methods for cleaner test code
  private static _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC optString() {
    return _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of();
  }

  private static _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC optString(String value) {
    return _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(value);
  }

  private static _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC optOptional(Optional value) {
    return _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(value);
  }

  // Test data setup - String-specific
  private final _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC unset = optString();
  private final String testValue = String._of("TestValue");
  private final _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC setOptional =
      optString(testValue);

  // Helper methods for reducing assertion duplication

  /**
   * Helper method to assert that an Optional&lt;String&gt; is unset and verify all related state.
   */
  private void assertOptionalUnset(
      _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC optional) {
    assertUnset.accept(optional);
    assertFalse.accept(optional._isSet());
    assertTrue.accept(optional._empty());
  }

  /**
   * Helper method to assert that an Optional&lt;String&gt; is set and verify all related state.
   */
  private void assertOptionalSet(_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC optional) {
    assertSet.accept(optional);
    assertTrue.accept(optional._isSet());
    assertFalse.accept(optional._empty());
  }

  /**
   * Helper method to verify String type consistency across operations.
   */
  private void assertStringType(Object obj) {
    assertEquals(String.class, obj.getClass());
  }

  @Test
  void testConstructionAndFactoryMethods() {
    // Default constructor should create unset Optional
    assertOptionalUnset(optString());

    // Value constructor should create set Optional
    final var valueConstructor = optString(String._of("Hello"));
    assertOptionalSet(valueConstructor);
    assertEquals("Hello", valueConstructor.get().state);
    assertStringType(valueConstructor.get());

    // Factory method with no args should create unset Optional
    assertOptionalUnset(optString());

    // Factory method with String should create set Optional
    final var factoryWithValue = optString(String._of("test"));
    assertOptionalSet(factoryWithValue);
    assertEquals(String._of("test"), factoryWithValue.get());

    // Null String should create unset Optional
    assertOptionalUnset(optString(null));

    // Factory method with base Optional
    final var fromBase = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(
        Optional._of(String._of("BaseTest")));
    assertOptionalSet(fromBase);
    assertEquals(String._of("BaseTest"), fromBase.get());
  }

  @Test
  void testTypeSpecificMethods() {
    // Test get() returns String, not Any
    assertEquals(testValue, setOptional.get());
    assertStringType(setOptional.get());

    // Test getOrDefault with String parameters
    assertEquals(testValue, setOptional.getOrDefault(String._of("default")));
    assertStringType(setOptional.getOrDefault(String._of("default")));

    assertEquals(String._of("default"), unset.getOrDefault(String._of("default")));
    assertStringType(unset.getOrDefault(String._of("default")));

    // Test asEmpty returns parameterized type
    final var emptyFromSet = setOptional.asEmpty();
    assertEquals(OPT_STRING, emptyFromSet.getClass());
    assertOptionalUnset(emptyFromSet);
  }

  @Test
  void testIteratorIntegration() {
    // Test iterator() returns correct parameterized type on set Optional
    final var setIter = setOptional.iterator();
    assertEquals(_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2.class,
        setIter.getClass());
    assertSet.accept(setIter);
    assertTrue.accept(setIter.hasNext());

    // Verify iterator returns String, not Any
    final var iterResult = setIter.next();
    assertEquals(testValue, iterResult);
    assertStringType(iterResult);
    assertFalse.accept(setIter.hasNext());

    // Test iterator() on unset Optional returns empty Iterator of String
    final var unsetIter = unset.iterator();
    assertEquals(_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2.class,
        unsetIter.getClass());
    assertUnset.accept(unsetIter);
    assertFalse.accept(unsetIter.hasNext());

    // Test complete iteration workflow
    final var testString = String._of("IteratorTest");
    final var optional = optString(testString);

    int iterationCount = 0;
    final var iter = optional.iterator();
    while (iter.hasNext().state) {
      final var value = iter.next();
      assertEquals(testString, value);
      assertStringType(value);
      iterationCount++;
    }
    assertEquals(1, iterationCount); // Optional should iterate exactly once when set

    // Test empty Optional iteration - should not iterate
    int emptyIterationCount = 0;
    final var emptyIter = unset.iterator();
    while (emptyIter.hasNext().state) {
      emptyIter.next();
      emptyIterationCount++;
    }
    assertEquals(0, emptyIterationCount);
  }

  @Test
  void testStringOperations() {
    // Test _contains with String parameter
    assertTrue.accept(setOptional._contains(testValue));
    assertFalse.accept(setOptional._contains(String._of("Different")));
    assertUnset.accept(unset._contains(testValue));

    // Test _contains with different String values
    final var hello = String._of("Hello");
    final var optionalHello = optString(hello);
    assertTrue.accept(optionalHello._contains(hello));
    assertTrue.accept(optionalHello._contains(String._of("Hello"))); // Same content
    assertFalse.accept(optionalHello._contains(String._of("hello"))); // Different case
    assertFalse.accept(optionalHello._contains(String._of("World")));

    // Test _pipe with String
    final var pipeTarget = optString();
    pipeTarget._pipe(String._of("Piped"));
    assertOptionalSet(pipeTarget);
    assertEquals(String._of("Piped"), pipeTarget.get());
  }

  @Test
  void testAssignmentOperators() {
    // Test _copy with Optional of String
    final var copyTarget = optString();
    copyTarget._copy(optString(String._of("CopyTest")));
    assertOptionalSet(copyTarget);
    assertEquals(String._of("CopyTest"), copyTarget.get());

    // Test _replace with Optional of String
    final var replaceTarget = optString(String._of("Old"));
    replaceTarget._replace(optString(String._of("New")));
    assertOptionalSet(replaceTarget);
    assertEquals(String._of("New"), replaceTarget.get());

    // Test _merge with Optional of String
    final var mergeTarget = optString();
    mergeTarget._merge(optString(String._of("MergeTest")));
    assertOptionalSet(mergeTarget);
    assertEquals(String._of("MergeTest"), mergeTarget.get());
  }

  @Test
  void testEqualityOperators() {
    // Test _eq with same Optional of String type
    final var same1 = optString(String._of("Same"));
    assertNotNull(same1);
    final var same2 = optString(String._of("Same"));
    final var different = optString(String._of("Different"));

    assertTrue.accept(same1._eq(same2));
    assertFalse.accept(same1._eq(different));
    assertUnset.accept(unset._eq(same1));
    assertUnset.accept(same1._eq(unset));

    // Test _eq with Any (polymorphic)
    assertTrue.accept(same1._eq((Any) same2));
    assertFalse.accept(same1._eq((Any) different));
  }


  // Consolidated mock classes for testing whenPresent behavior

  private static class MockAcceptor extends _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA {
    private boolean wasCalled = false;
    private java.lang.String calledWith = null;

    @Override
    public void _call(String t) {
      super._call(t);
      this.wasCalled = true;
      this.calledWith = t.state;
    }

    public boolean verifyCalledWith(String expected) {
      return wasCalled && expected.state.equals(calledWith);
    }

    public boolean verifyNotCalled() {
      return !wasCalled;
    }
  }

  private static class MockConsumer extends _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 {
    private boolean wasCalled = false;
    private java.lang.String calledWith = null;

    @Override
    public void _call(String t) {
      super._call(t);
      this.wasCalled = true;
      this.calledWith = t.state;
    }

    public boolean verifyCalledWith(String expected) {
      return wasCalled && expected.state.equals(calledWith);
    }

    public boolean verifyNotCalled() {
      return !wasCalled;
    }
  }

  // Helper methods for whenPresent testing
  private void assertWhenPresentCalled(
      _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC optional, String expectedValue) {
    final var acceptor = new MockAcceptor();
    optional.whenPresent(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyCalledWith(expectedValue)));

    final var consumer = new MockConsumer();
    optional.whenPresent(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyCalledWith(expectedValue)));
  }

  private void assertWhenPresentNotCalled(
      _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC optional) {
    final var acceptor = new MockAcceptor();
    optional.whenPresent(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyNotCalled()));

    final var consumer = new MockConsumer();
    optional.whenPresent(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyNotCalled()));
  }

  @Test
  void testWhenPresentIntegration() {
    // Test whenPresent with SET Optional (functions should be called)
    assertWhenPresentCalled(setOptional, testValue);

    // Test whenPresent with UNSET Optional (functions should NOT be called)
    assertWhenPresentNotCalled(unset);
  }

  @Test
  void testOperatorConsistency() {
    // Test _string operator
    assertSet.accept(setOptional._string());
    assertEquals(testValue, setOptional._string());

    // Test _hashcode operator  
    assertSet.accept(setOptional._hashcode());
    assertUnset.accept(unset._hashcode());

    // Verify set/unset state consistency (already covered by helper methods)
    assertOptionalSet(setOptional);
    assertOptionalUnset(unset);
  }

  @Test
  void testExceptionsAndEdgeCases() {
    // Test get() throws exception on unset Optional
    assertThrows(Exception.class, unset::get);
    // Test exception does not occur on set Optional
    assertEquals(testValue, setOptional.get());

    // Test null handling in assignment operators
    final var nullTarget = optString(String._of("Initial"));
    nullTarget._copy(null);
    assertOptionalUnset(nullTarget); // Should become unset when copying null

    final var replaceTarget = optString(String._of("Initial"));
    replaceTarget._replace(null);
    assertOptionalUnset(replaceTarget); // Should become unset when replacing with null

    final var mergeTarget = optString(String._of("Initial"));
    mergeTarget._merge(null);
    assertOptionalSet(mergeTarget); // Should remain set when merging null

    // Test with various String edge cases
    final var emptyOptional = optString(String._of(""));
    assertOptionalSet(emptyOptional);
    assertEquals(String._of(""), emptyOptional.get());

    final var longOptional = optString(String._of("This is a very long string to test edge cases"));
    assertOptionalSet(longOptional);
    assertEquals(String._of("This is a very long string to test edge cases"), longOptional.get());
  }

  @Test
  void testTypeConsistencyWithBase() {
    // Verify delegation to base Optional works correctly
    final var baseOptional = Optional._of(testValue);
    final var paramOptional = optOptional(baseOptional);

    // Both should have same set/unset state
    assertEquals(baseOptional._isSet().state, paramOptional._isSet().state);
    assertEquals(baseOptional._empty().state, paramOptional._empty().state);

    // Parameterized version should provide String-typed access
    assertEquals(testValue, paramOptional.get());
    assertStringType(paramOptional.get());

    // String operations should work consistently
    assertTrue.accept(paramOptional._contains(testValue));
    assertEquals(testValue, paramOptional.getOrDefault(String._of("default")));
  }

  @Test
  void testAsJson() {
    // Test unset Optional of String
    final var unsetJson = unset._json();
    assertNotNull(unsetJson);
    assertUnset.accept(unsetJson);

    final var setJson = setOptional._json();
    assertSet.accept(setJson);
    assertTrue.accept(setJson.objectNature());

    // Verify the wrapper structure
    final var optionalProperty = setJson.get(String._of("optional"));
    assertSet.accept(optionalProperty);
    assertTrue.accept(optionalProperty.valueNature());

    // Verify the inner value matches the original string's JSON representation
    final var expectedInnerJson = testValue._json();
    assertTrue.accept(optionalProperty._eq(expectedInnerJson));

    // Test with different string value
    final var anotherValue = String._of("AnotherValue");
    final var anotherOptional = optString(anotherValue);
    final var anotherJson = anotherOptional._json();
    
    assertSet.accept(anotherJson);
    assertTrue.accept(anotherJson.objectNature());
    
    final var anotherOptionalProperty = anotherJson.get(String._of("optional"));
    assertSet.accept(anotherOptionalProperty);
    final var expectedAnotherJson = anotherValue._json();
    assertTrue.accept(anotherOptionalProperty._eq(expectedAnotherJson));
  }

}