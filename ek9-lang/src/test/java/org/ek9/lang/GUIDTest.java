package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the GUID class.
 * Note: GUID has no unset behavior - it always contains a valid UUID.
 */
class GUIDTest extends Common {

  // Constructor Tests

  @Test
  void testDefaultConstructor() {
    GUID guid = new GUID();
    assertNotNull(guid);

    // GUID is always set
    assertTrue(guid._isSet().state);

    // Should have a valid UUID string representation
    String guidStr = guid._string();
    assertSet.accept(guidStr);
    assertFalse(guidStr.state.isEmpty());

    // Should be parseable as UUID
    assertDoesNotThrow(() -> UUID.fromString(guidStr.state));
  }

  @Test
  void testCopyConstructor() {
    GUID original = new GUID();
    GUID copy = new GUID(original);

    assertNotNull(copy);
    assertTrue(copy._isSet().state);

    // Should have same UUID value
    assertEquals(original._string().state, copy._string().state);

    // But should be different objects
    assertNotSame(original, copy);
  }

  @Test
  void testStringConstructorWithValidUUID() {
    java.lang.String validUuid = "550e8400-e29b-41d4-a716-446655440000";
    GUID guid = new GUID(String._of(validUuid));

    assertNotNull(guid);
    assertTrue(guid._isSet().state);
    assertEquals(validUuid, guid._string().state);
  }

  @Test
  void testStringConstructorWithInvalidUUID() {
    java.lang.String invalidUuid = "not-a-valid-uuid";
    GUID guid = new GUID(String._of(invalidUuid));

    assertNotNull(guid);
    assertTrue(guid._isSet().state); // Should still be set with random UUID

    // Should have a valid UUID string (not the invalid input)
    String guidStr = guid._string();
    assertSet.accept(guidStr);
    assertNotEquals(invalidUuid, guidStr.state);
    assertDoesNotThrow(() -> UUID.fromString(guidStr.state));
  }

  @Test
  void testStringConstructorWithUnsetString() {
    String unsetString = new String();
    GUID guid = new GUID(unsetString);

    assertNotNull(guid);
    assertTrue(guid._isSet().state); // Should still be set with random UUID

    // Should have a valid UUID string
    String guidStr = guid._string();
    assertSet.accept(guidStr);
    assertDoesNotThrow(() -> UUID.fromString(guidStr.state));
  }

  // Factory Method Tests

  @Test
  void testFactoryMethodFromString() {
    java.lang.String validUuid = "550e8400-e29b-41d4-a716-446655440000";
    GUID guid = GUID._of(validUuid);

    assertNotNull(guid);
    assertTrue(guid._isSet().state);
    assertEquals(validUuid, guid._string().state);
  }

  @Test
  void testFactoryMethodFromJavaUUID() {
    UUID javaUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    GUID guid = GUID._of(javaUuid);

    assertNotNull(guid);
    assertTrue(guid._isSet().state);
    assertEquals(javaUuid.toString(), guid._string().state);
  }

  @Test
  void testFactoryMethodFromNullUUID() {
    GUID guid = GUID._of((UUID) null);

    assertNotNull(guid);
    assertTrue(guid._isSet().state);

    // Should have a valid UUID string
    String guidStr = guid._string();
    assertSet.accept(guidStr);
    assertDoesNotThrow(() -> UUID.fromString(guidStr.state));
  }

  @Test
  void testOfMethod() {
    GUID guid = GUID._of();

    assertNotNull(guid);
    assertTrue(guid._isSet().state);

    // Should have a valid UUID string
    String guidStr = guid._string();
    assertSet.accept(guidStr);
    assertDoesNotThrow(() -> UUID.fromString(guidStr.state));
  }

  @Test
  void testOfUniqueness() {
    GUID guid1 = GUID._of();
    GUID guid2 = GUID._of();

    // Should be different UUIDs
    assertNotEquals(guid1._string().state, guid2._string().state);
    assertFalse(guid1._eq(guid2).state);
  }

  // Equality Operator Tests

  @Test
  void testEqualityOperatorWithSameGUID() {
    java.lang.String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
    GUID guid1 = GUID._of(uuidStr);
    GUID guid2 = GUID._of(uuidStr);

    assertTrue(guid1._eq(guid2).state);
    assertFalse(guid1._neq(guid2).state);
  }

  @Test
  void testEqualityOperatorWithDifferentGUID() {
    GUID guid1 = GUID._of("550e8400-e29b-41d4-a716-446655440000");
    GUID guid2 = GUID._of("550e8400-e29b-41d4-a716-446655440001");

    assertFalse(guid1._eq(guid2).state);
    assertTrue(guid1._neq(guid2).state);
  }

  // Comparison Operator Tests

  @Test
  void testComparisonOperatorWithGUID() {
    GUID guid1 = GUID._of("550e8400-e29b-41d4-a716-446655440000");
    GUID guid2 = GUID._of("550e8400-e29b-41d4-a716-446655440001");

    Integer result = guid1._cmp(guid2);
    assertTrue(result._isSet().state);
    assertTrue(result.state < 0); // guid1 < guid2

    Integer result2 = guid2._cmp(guid1);
    assertTrue(result2._isSet().state);
    assertTrue(result2.state > 0); // guid2 > guid1
  }

  @Test
  void testComparisonOperatorWithSameGUID() {
    java.lang.String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
    GUID guid1 = GUID._of(uuidStr);
    GUID guid2 = GUID._of(uuidStr);

    Integer result = guid1._cmp(guid2);
    assertTrue(result._isSet().state);
    assertEquals(0, (int) result.state); // Equal GUIDs
  }

  @Test
  void testComparisonOperatorWithAny() {
    GUID guid1 = GUID._of("550e8400-e29b-41d4-a716-446655440000");
    GUID guid2 = GUID._of("550e8400-e29b-41d4-a716-446655440001");

    Integer result = guid1._cmp((Any) guid2);
    assertTrue(result._isSet().state);
    assertTrue(result.state < 0); // guid1 < guid2
  }

  @Test
  void testComparisonOperatorWithNonGUID() {
    GUID guid = new GUID();
    String notGuid = String._of("not a guid");
    assertNotNull(guid);
    assertNotNull(notGuid);

    Integer result = guid._cmp(notGuid);
    assertUnset.accept(result); // Should return unset for non-GUID comparison
  }

  // String Operator Tests

  @Test
  void testStringOperator() {
    java.lang.String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
    GUID guid = GUID._of(uuidStr);

    String result = guid._string();
    assertTrue(result._isSet().state);
    assertEquals(uuidStr, result.state);
  }

  @Test
  void testPromoteOperator() {
    java.lang.String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
    GUID guid = GUID._of(uuidStr);

    String result = guid._promote();
    assertTrue(result._isSet().state);
    assertEquals(uuidStr, result.state);
  }

  @Test
  void testStringAndPromoteOperatorsSame() {
    GUID guid = new GUID();

    String stringResult = guid._string();
    String promoteResult = guid._promote();

    assertTrue(stringResult._isSet().state);
    assertTrue(promoteResult._isSet().state);
    assertEquals(stringResult.state, promoteResult.state);
  }

  // Assignment Operator Tests

  @Test
  void testReplaceOperator() {
    GUID guid1 = GUID._of("550e8400-e29b-41d4-a716-446655440000");
    GUID guid2 = GUID._of("550e8400-e29b-41d4-a716-446655440001");

    java.lang.String originalValue = guid1._string().state;
    java.lang.String newValue = guid2._string().state;

    guid1._replace(guid2);

    assertEquals(newValue, guid1._string().state);
    assertNotEquals(originalValue, guid1._string().state);
  }

  @Test
  void testCopyOperator() {
    GUID guid1 = GUID._of("550e8400-e29b-41d4-a716-446655440000");
    GUID guid2 = GUID._of("550e8400-e29b-41d4-a716-446655440001");

    java.lang.String originalValue = guid1._string().state;
    java.lang.String newValue = guid2._string().state;

    guid1._copy(guid2);

    assertEquals(newValue, guid1._string().state);
    assertNotEquals(originalValue, guid1._string().state);
  }

  // Set/Unset Behavior Tests

  @Test
  void testIsSetAlwaysTrue() {
    GUID guid = new GUID();
    assertTrue(guid._isSet().state);

    // Even after operations, should remain set
    guid._copy(new GUID());
    assertTrue(guid._isSet().state);

    guid._replace(new GUID());
    assertTrue(guid._isSet().state);
  }

  @Test
  void testHashcodeOperator() {
    GUID guid = GUID._of("550e8400-e29b-41d4-a716-446655440000");

    Integer hashcode = guid._hashcode();
    assertTrue(hashcode._isSet().state);

    // Same GUID should have same hashcode
    GUID guid2 = GUID._of("550e8400-e29b-41d4-a716-446655440000");
    Integer hashcode2 = guid2._hashcode();
    assertEquals(hashcode.state, hashcode2.state);
  }

  // Assign Method Tests

  @Test
  void testAssignMethod() {
    GUID guid = new GUID();
    UUID newUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    guid.assign(newUuid);

    assertTrue(guid._isSet().state);
    assertEquals(newUuid.toString(), guid._string().state);
  }

  // Edge Cases and Integration Tests

  @Test
  void testUUIDStringFormat() {
    GUID guid = new GUID();
    String uuidStr = guid._string();

    // Should be valid UUID format (36 characters with hyphens)
    assertEquals(36, uuidStr.state.length());
    assertTrue(uuidStr.state.matches(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
  }

  @Test
  void testRoundTripStringConversion() {
    GUID original = new GUID();
    java.lang.String uuidStr = original._string().state;
    GUID reconstructed = GUID._of(uuidStr);

    assertEquals(original._string().state, reconstructed._string().state);
    assertTrue(original._eq(reconstructed).state);
  }

  @Test
  void testConsistentOperatorBehavior() {
    GUID guid = GUID._of("550e8400-e29b-41d4-a716-446655440000");

    // All operations should be consistent with set state
    assertTrue(guid._isSet().state);
    assertTrue(guid._string()._isSet().state);
    assertTrue(guid._promote()._isSet().state);
    assertTrue(guid._hashcode()._isSet().state);

    // Comparison with itself should be 0
    Integer selfCompare = guid._cmp(guid);
    assertTrue(selfCompare._isSet().state);
    assertEquals(0, (int) selfCompare.state);

    // Equality with itself should be true
    assertTrue(guid._eq(guid).state);
    assertFalse(guid._neq(guid).state);
  }

  @Test
  void testWithNull() {
    final var guid1 = GUID._of((UUID) null);
    assertNotNull(guid1);
    assertSet.accept(guid1);

    final var guid2 = GUID._of((java.lang.String) null);
    assertNotNull(guid2);
    assertSet.accept(guid2);

    final var guid3 = new GUID((GUID) null);
    assertNotNull(guid3);
    assertSet.accept(guid3);

    final var guid4 = new GUID((String) null);
    assertNotNull(guid4);
    assertSet.accept(guid4);

    assertUnset.accept(guid1._eq((Any) null));
    assertUnset.accept(guid1._eq(null));

    assertUnset.accept(guid1._neq((Any) null));
    assertUnset.accept(guid1._neq(null));

    assertUnset.accept(guid1._cmp((Any) null));
    assertUnset.accept(guid1._cmp(null));

    final var beforeNullCopy = guid1._string();
    guid1._copy(null);
    assertSet.accept(guid1);
    final var afterNullCopy = guid1._string();
    //This is destructive and generates a new GUID.
    assertNotEquals(beforeNullCopy, afterNullCopy);
  }

  @Test
  void testUUIDVersionAndVariant() {
    // Test that generated UUIDs are valid version 4 (random) UUIDs
    GUID guid = new GUID();
    String uuidStr = guid._string();
    UUID uuid = UUID.fromString(uuidStr.state);

    // Version 4 UUIDs should have version bits set to 4
    assertEquals(4, uuid.version());

    // Variant should be 2 (IETF variant)
    assertEquals(2, uuid.variant());

    // Test multiple generations to ensure consistency
    for (int i = 0; i < 5; i++) {
      GUID testGuid = GUID._of();
      UUID testUuid = UUID.fromString(testGuid._string().state);
      assertEquals(4, testUuid.version());
      assertEquals(2, testUuid.variant());
    }
  }
}