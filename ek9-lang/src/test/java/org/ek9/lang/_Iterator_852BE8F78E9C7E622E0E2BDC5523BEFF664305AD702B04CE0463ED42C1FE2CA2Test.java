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
  void testEqualityAndHashcode() {
    final var string1 = String._of("Test");
    final var iter1 = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string1);
    final var iter2 = new _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2(string1);
    final var emptyIter = _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of();

    // Iterator instances are not equal even with same content
    assertFalse.accept(iter1._eq(iter2));
    assertNotEquals(iter1, iter2);
    assertNotEquals(iter1.hashCode(), iter2.hashCode());


    // Test _hashcode operator
    assertSet.accept(iter1._hashcode());
    assertSet.accept(emptyIter._hashcode());
  }

}