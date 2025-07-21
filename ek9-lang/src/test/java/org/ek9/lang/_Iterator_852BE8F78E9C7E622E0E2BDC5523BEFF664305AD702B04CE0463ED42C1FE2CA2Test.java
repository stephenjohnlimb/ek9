package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2Test extends Common {

  @Test
  void testConstruction() {
    final var defaultConstructor = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2();
    assertUnset.accept(defaultConstructor);
    assertFalse.accept(defaultConstructor.hasNext());

    // Constructor with single element
    final var string1 = String._of("Hello");
    final var singleElementIterator =
        new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string1);
    assertSet.accept(singleElementIterator);
    assertTrue.accept(singleElementIterator.hasNext());
    assertEquals(string1, singleElementIterator.next());
    assertFalse.accept(singleElementIterator.hasNext());
  }

  @Test
  void testFactoryMethods() {
    final var unset1 = _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of();
    assertNotNull(unset1);
    assertUnset.accept(unset1);

    final var unset2 = _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(null);
    assertNotNull(unset2);
    assertUnset.accept(unset2);

    final var set1 = _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(Iterator._of(String._of("Test")));
    assertNotNull(set1);
    assertSet.accept(set1);
    assertTrue.accept(set1.hasNext());
    assertEquals(String._of("Test"), set1.next());

    assertFalse.accept(set1.hasNext());
    assertUnset.accept(set1);

  }

  @Test
  void testIteratorEquality() {

    final var string1 = String._of("Test");
    final var iter1 = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string1);
    final var iter2 = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string1);
    assertFalse.accept(iter1._eq(iter2));
    assertNotEquals(iter1, iter2);
    assertNotEquals(iter1.hashCode(), iter2.hashCode());
  }

  @Test
  void testStringTypeSpecificBehavior() {
    // Test with empty string
    final var emptyString = String._of("");
    final var iterEmpty = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(emptyString);
    assertSet.accept(iterEmpty);
    assertTrue.accept(iterEmpty.hasNext());
    assertEquals(emptyString, iterEmpty.next());
    assertFalse.accept(iterEmpty.hasNext());
    
    // Test with longer string
    final var longString = String._of("This is a longer string");
    final var iterLong = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(longString);
    assertSet.accept(iterLong);
    assertTrue.accept(iterLong.hasNext());
    assertEquals(longString, iterLong.next());
    assertFalse.accept(iterLong.hasNext());
    
    // Test with string containing special characters
    final var specialString = String._of("Hello, World! @#$%^&*()");
    final var iterSpecial = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(specialString);
    assertSet.accept(iterSpecial);
    assertTrue.accept(iterSpecial.hasNext());
    assertEquals(specialString, iterSpecial.next());
    assertFalse.accept(iterSpecial.hasNext());
  }

  @Test
  void testTypeConsistencyWithBase() {
    // Verify that delegation to base Iterator works correctly
    final var testString = String._of("TestValue");
    final var baseIterator = Iterator._of(testString);
    final var parameterizedIterator = _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(baseIterator);
    
    assertNotNull(parameterizedIterator);
    assertSet.accept(parameterizedIterator);
    assertTrue.accept(parameterizedIterator.hasNext());
    
    final var result = parameterizedIterator.next();
    assertEquals(testString, result);
    // Verify the type is String, not Any
    assertEquals(String.class, result.getClass());
    
    assertFalse.accept(parameterizedIterator.hasNext());
    assertUnset.accept(parameterizedIterator);
  }

  @Test
  void testHashCodeAndEquality() {
    final var string1 = String._of("SameContent");
    final var string2 = String._of("SameContent");
    final var string3 = String._of("DifferentContent");
    
    final var iter1 = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string1);
    final var iter2 = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string2);
    final var iter3 = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string3);
    
    // Even with same content, different Iterator instances are not equal
    assertFalse.accept(iter1._eq(iter2));
    assertFalse.accept(iter1._eq(iter3));
    assertFalse.accept(iter2._eq(iter3));
    
    // Hash codes should be different for different Iterator instances
    assertNotEquals(iter1._hashcode(), iter2._hashcode());
    assertNotEquals(iter1._hashcode(), iter3._hashcode());
    assertNotEquals(iter2._hashcode(), iter3._hashcode());
  }
}