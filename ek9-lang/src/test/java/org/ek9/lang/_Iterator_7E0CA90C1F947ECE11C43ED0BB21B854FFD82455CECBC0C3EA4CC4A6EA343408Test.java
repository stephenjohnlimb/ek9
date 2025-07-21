package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
class _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408Test extends Common {


  @Test
  void testConstruction() {
    final var defaultConstructor = new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408();
    assertUnset.accept(defaultConstructor);
    assertFalse.accept(defaultConstructor.hasNext());

    // Constructor with single element
    final var char1 = Character._of('A');
    final var singleElementIterator =
        new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408(char1);
    assertSet.accept(singleElementIterator);
    assertTrue.accept(singleElementIterator.hasNext());
    assertEquals(char1, singleElementIterator.next());
    assertFalse.accept(singleElementIterator.hasNext());
  }

  @Test
  void testFactoryMethods() {
    final var unset1 = _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of();
    assertNotNull(unset1);
    assertUnset.accept(unset1);

    final var unset2 = _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of(null);
    assertNotNull(unset2);
    assertUnset.accept(unset2);

    final var set1 = _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of(Iterator._of(Character._of("S")));
    assertNotNull(set1);
    assertSet.accept(set1);
    assertTrue.accept(set1.hasNext());
    assertEquals(Character._of('S'), set1.next());

    assertFalse.accept(set1.hasNext());
    assertUnset.accept(set1);

  }

  @Test
  void testIteratorEquality() {

    final var char1 = Character._of('A');
    final var iter1 = new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408(char1);
    final var iter2 = new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408(char1);
    assertFalse.accept(iter1._eq(iter2));
    assertNotEquals(iter1, iter2);
    assertNotEquals(iter1.hashCode(), iter2.hashCode());
  }
}
