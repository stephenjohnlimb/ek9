package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test Iterator of Integer parameterized type.
 * Tests delegation pattern and Integer-specific type safety.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4Test extends Common {

  @Test
  void testConstruction() {
    // Test default constructor
    final var defaultConstructor = new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4();
    assertNotNull(defaultConstructor);
    assertUnset.accept(defaultConstructor);
    assertFalse.accept(defaultConstructor.hasNext());

    // Test value constructor
    final var valueConstructor = new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4(Integer._of(42));
    assertNotNull(valueConstructor);
    assertSet.accept(valueConstructor);
    assertTrue.accept(valueConstructor.hasNext());
  }

  @Test
  void testFactoryMethods() {
    // Test _of() - empty iterator
    final var emptyIterator = _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of();
    assertNotNull(emptyIterator);
    assertUnset.accept(emptyIterator);
    assertFalse.accept(emptyIterator.hasNext());

    // Test _of(Iterator) - from base Iterator
    final var baseIterator = Iterator._of(Integer._of(100));
    final var fromBase = _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of(baseIterator);
    assertNotNull(fromBase);
    assertSet.accept(fromBase);
    assertTrue.accept(fromBase.hasNext());

    // Test _of(null Iterator)
    final var fromNull = _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of(null);
    assertNotNull(fromNull);
    assertUnset.accept(fromNull);
    assertFalse.accept(fromNull.hasNext());
  }

  @Test
  void testEqualityAndOperators() {
    // Test _eq with same parameterized type
    final var iterator1 = _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of();
    final var iterator2 = _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of();
    final var iteratorWithValue = new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4(Integer._of(42));

    // Test equality functionality
    assertNotNull(iterator1._eq(iterator2));
    assertNotNull(iterator1._eq(iterator1));


    // Test _isSet operator (should return hasNext result)
    assertEquals(iteratorWithValue.hasNext(), iteratorWithValue._isSet());

    // Test _hashcode operator
    assertNotNull(iteratorWithValue._hashcode());
  }

  @Test
  void testIntegerTypeSafety() {
    // Test iteration with Integer values and type safety
    final var testValue = Integer._of(123);
    final var iterator = new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4(testValue);

    // Verify hasNext and next behavior
    assertTrue.accept(iterator.hasNext());
    final var result = iterator.next();
    assertEquals(testValue, result);
    assertEquals(Integer.class, result.getClass());

    // After consuming, should be empty
    assertFalse.accept(iterator.hasNext());
  }
}