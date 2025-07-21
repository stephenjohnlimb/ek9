package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BCTest extends Common {

  // Test data setup - String-specific
  private final _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC unset =
      new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC();
  private final String testValue = String._of("TestValue");
  private final _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC setOptional =
      new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC(testValue);

  @Test
  void testConstruction() {
    // Default constructor should create unset Optional
    final var defaultConstructor = new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC();
    assertNotNull(defaultConstructor);
    assertUnset.accept(defaultConstructor);
    assertFalse.accept(defaultConstructor._isSet());
    assertTrue.accept(defaultConstructor._empty());

    // Value constructor should create set Optional
    final var valueConstructor =
        new _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC(String._of("Hello"));
    assertSet.accept(valueConstructor);
    assertTrue.accept(valueConstructor._isSet());
    assertFalse.accept(valueConstructor._empty());

    // String value verification
    assertEquals("Hello", valueConstructor.get().state);
    assertEquals(String.class, valueConstructor.get().getClass());
  }

  @Test
  void testFactoryMethods() {
    // Factory method with no args should create unset Optional
    final var factoryEmpty = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of();
    assertUnset.accept(factoryEmpty);
    assertFalse.accept(factoryEmpty._isSet());
    assertTrue.accept(factoryEmpty._empty());

    // Factory method with String should create set Optional
    final var factoryWithValue =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("test"));
    assertSet.accept(factoryWithValue);
    assertTrue.accept(factoryWithValue._isSet());
    assertFalse.accept(factoryWithValue._empty());
    assertEquals(String._of("test"), factoryWithValue.get());

    // Null String should create unset Optional
    final var factoryWithNull =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of((String) null);
    assertUnset.accept(factoryWithNull);
    assertFalse.accept(factoryWithNull._isSet());
    assertTrue.accept(factoryWithNull._empty());

    // Factory method with base Optional
    final var baseOptional = Optional._of(String._of("BaseTest"));
    final var fromBase = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(baseOptional);
    assertSet.accept(fromBase);
    assertEquals(String._of("BaseTest"), fromBase.get());
  }

  @Test
  void testTypeSpecificMethods() {
    // Test get() returns String, not Any
    final var result = setOptional.get();
    assertEquals(testValue, result);
    assertEquals(String.class, result.getClass());

    // Test getOrDefault with String parameters
    final var defaultValue = String._of("default");
    final var resultFromSet = setOptional.getOrDefault(defaultValue);
    assertEquals(testValue, resultFromSet);
    assertEquals(String.class, resultFromSet.getClass());

    final var resultFromUnset = unset.getOrDefault(defaultValue);
    assertEquals(defaultValue, resultFromUnset);
    assertEquals(String.class, resultFromUnset.getClass());

    // Test asEmpty returns parameterized type
    final var emptyFromSet = setOptional.asEmpty();
    assertNotNull(emptyFromSet);
    assertEquals(_Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC.class,
        emptyFromSet.getClass());
    assertUnset.accept(emptyFromSet);
  }

  @Test
  void testIteratorIntegration() {
    // CRITICAL TEST: Iterator integration with Iterator of String

    // Test iterator() returns correct parameterized type on set Optional
    final var setIter = setOptional.iterator();
    assertNotNull(setIter);
    assertEquals(_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2.class,
        setIter.getClass());
    assertSet.accept(setIter);
    assertTrue.accept(setIter.hasNext());

    // Verify iterator returns String, not Any
    final var iterResult = setIter.next();
    assertEquals(testValue, iterResult);
    assertEquals(String.class, iterResult.getClass());
    assertFalse.accept(setIter.hasNext());

    // Test iterator() on unset Optional returns empty Iterator of String
    final var unsetIter = unset.iterator();
    assertNotNull(unsetIter);
    assertEquals(_Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2.class,
        unsetIter.getClass());
    assertUnset.accept(unsetIter);
    assertFalse.accept(unsetIter.hasNext());
  }

  @Test
  void testIteratorWorkflow() {
    // Test complete iteration workflow
    final var testString = String._of("IteratorTest");
    final var optional = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(testString);

    int iterationCount = 0;
    final var iter = optional.iterator();
    while (iter.hasNext().state) {
      final var value = iter.next();
      assertEquals(testString, value);
      assertEquals(String.class, value.getClass());
      iterationCount++;
    }
    assertEquals(1, iterationCount); // Optional should iterate exactly once when set

    // Test empty Optional iteration
    int emptyIterationCount = 0;
    final var emptyIter = unset.iterator();
    while (emptyIter.hasNext().state) {
      emptyIter.next();
      emptyIterationCount++;
    }
    assertEquals(0, emptyIterationCount); // Empty Optional should not iterate
  }

  @Test
  void testStringOperations() {
    // Test _contains with String parameter
    assertTrue.accept(setOptional._contains(testValue));
    assertFalse.accept(setOptional._contains(String._of("Different")));
    assertUnset.accept(unset._contains(testValue));

    // Test _contains with different String values
    final var hello = String._of("Hello");
    final var optionalHello = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(hello);
    assertTrue.accept(optionalHello._contains(hello));
    assertTrue.accept(optionalHello._contains(String._of("Hello"))); // Same content
    assertFalse.accept(optionalHello._contains(String._of("hello"))); // Different case
    assertFalse.accept(optionalHello._contains(String._of("World")));

    // Test _pipe with String
    final var pipeTarget = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of();
    pipeTarget._pipe(String._of("Piped"));
    assertSet.accept(pipeTarget);
    assertEquals(String._of("Piped"), pipeTarget.get());
  }

  @Test
  void testAssignmentOperators() {
    // Test _copy with Optional of String
    final var copyTarget = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of();
    final var copySource =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("CopyTest"));

    copyTarget._copy(copySource);
    assertSet.accept(copyTarget);
    assertEquals(String._of("CopyTest"), copyTarget.get());

    // Test _replace with Optional of String
    final var replaceTarget =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("Old"));
    final var replaceSource =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("New"));

    replaceTarget._replace(replaceSource);
    assertSet.accept(replaceTarget);
    assertEquals(String._of("New"), replaceTarget.get());

    // Test _merge with Optional of String
    final var mergeTarget = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of();
    final var mergeSource =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("MergeTest"));

    mergeTarget._merge(mergeSource);
    assertSet.accept(mergeTarget);
    assertEquals(String._of("MergeTest"), mergeTarget.get());
  }

  @Test
  void testEqualityOperators() {
    // Test _eq with same Optional of String type
    final var same1 =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("Same"));
    final var same2 =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("Same"));
    final var different =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("Different"));
    assertNotNull(different);

    assertTrue.accept(same1._eq(same2));
    assertFalse.accept(same1._eq(different));
    assertUnset.accept(unset._eq(same1));
    assertUnset.accept(same1._eq(unset));

    // Test _eq with Any (polymorphic)
    assertTrue.accept(same1._eq((Any) same2));
    assertFalse.accept(same1._eq((Any) different));
  }

  @Test
  void testExceptionBehavior() {
    // Test get() throws exception on unset Optional
    assertThrows(Exception.class, unset::get);

    // Test exception does not occur on set Optional
    final var result = setOptional.get();
    assertNotNull(result);
    assertEquals(testValue, result);
  }

  // Helper class for testing SET Optional behavior (should be called)
  private static class TestAcceptorForSet extends _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA {
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
  }

  // Helper class for testing SET Optional behavior (should be called)
  private static class TestConsumerForSet extends _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 {
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
  }

  // Helper class for testing UNSET Optional behavior (should NOT be called)
  private static class TestAcceptorForUnset extends _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA {
    private boolean wasCalled = false;

    @Override
    public void _call(String t) {
      super._call(t);
      this.wasCalled = true;
    }

    public boolean verifyNotCalled() {
      return !wasCalled;
    }
  }

  // Helper class for testing UNSET Optional behavior (should NOT be called)
  private static class TestConsumerForUnset extends _Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 {
    private boolean wasCalled = false;

    @Override
    public void _call(String t) {
      super._call(t);
      this.wasCalled = true;
    }

    public boolean verifyNotCalled() {
      return !wasCalled;
    }
  }

  @Test
  void testWhenPresentIntegration() {
    // Test whenPresent with SET Optional (functions should be called)
    final var acceptor = new TestAcceptorForSet();
    setOptional.whenPresent(acceptor);
    assertTrue.accept(Boolean._of(acceptor.verifyCalledWith(testValue)));

    final var consumer = new TestConsumerForSet();
    setOptional.whenPresent(consumer);
    assertTrue.accept(Boolean._of(consumer.verifyCalledWith(testValue)));

    // Test whenPresent with UNSET Optional (functions should NOT be called)
    final var unsetAcceptor = new TestAcceptorForUnset();
    unset.whenPresent(unsetAcceptor);
    assertTrue.accept(Boolean._of(unsetAcceptor.verifyNotCalled()));

    final var unsetConsumer = new TestConsumerForUnset();
    unset.whenPresent(unsetConsumer);
    assertTrue.accept(Boolean._of(unsetConsumer.verifyNotCalled()));
  }

  @Test
  void testOperatorConsistency() {
    // Test _string operator
    assertSet.accept(setOptional._string());
    assertEquals(testValue, setOptional._string());

    // Test _hashcode operator  
    assertSet.accept(setOptional._hashcode());
    assertUnset.accept(unset._hashcode());

    // Test _isSet operator
    assertTrue.accept(setOptional._isSet());
    assertFalse.accept(unset._isSet());

    // Test _empty operator
    assertFalse.accept(setOptional._empty());
    assertTrue.accept(unset._empty());
  }

  @Test
  void testEdgeCasesAndNullHandling() {
    // Test null handling in assignment operators
    final var nullTarget =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("Initial"));

    nullTarget._copy(null);
    // Should become unset when copying null (following EK9 Optional semantics)
    assertUnset.accept(nullTarget);

    // Reset for replace test
    final var replaceTarget =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("Initial"));
    replaceTarget._replace(null);
    // Should become unset when replacing with null
    assertUnset.accept(replaceTarget);

    // Merge with null should not change unset Optional
    final var mergeTarget =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(String._of("Initial"));
    mergeTarget._merge(null);
    // Should remain set when merging null (merge only acts on set values)
    assertSet.accept(mergeTarget);

    // Test with various String edge cases
    final var emptyString = String._of("");
    final var emptyOptional =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(emptyString);
    assertSet.accept(emptyOptional);
    assertEquals(emptyString, emptyOptional.get());

    final var longString = String._of("This is a very long string to test edge cases");
    final var longOptional = _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(longString);
    assertSet.accept(longOptional);
    assertEquals(longString, longOptional.get());
  }

  @Test
  void testTypeConsistencyWithBase() {
    // Verify delegation to base Optional works correctly
    final var baseOptional = Optional._of(testValue);
    final var paramOptional =
        _Optional_C011A3DC69C147B21BB3B4318CD6E648E5B36284DE9A0658F8CDDD33D8C1B5BC._of(baseOptional);

    // Both should have same set/unset state
    assertEquals(baseOptional._isSet().state, paramOptional._isSet().state);
    assertEquals(baseOptional._empty().state, paramOptional._empty().state);

    // Parameterized version should provide String-typed access
    assertEquals(testValue, paramOptional.get());
    assertEquals(String.class, paramOptional.get().getClass());

    // String operations should work consistently
    assertTrue.accept(paramOptional._contains(testValue));
    assertEquals(testValue, paramOptional.getOrDefault(String._of("default")));
  }

}