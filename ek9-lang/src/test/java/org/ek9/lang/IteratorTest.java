package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class IteratorTest extends Common {

  final String str1 = String._of("first");
  final String str2 = String._of("second");
  final String str3 = String._of("third");
  final Integer int1 = Integer._of(1);

  @Test
  void testConstruction() {
    // Default constructor creates empty iterator
    final var defaultConstructor = new Iterator();
    assertUnset.accept(defaultConstructor);
    assertFalse.accept(defaultConstructor.hasNext());

    // Constructor with single element
    final var singleElementIterator = new Iterator(str1);
    assertSet.accept(singleElementIterator);
    assertTrue.accept(singleElementIterator.hasNext());
    assertEquals(str1, singleElementIterator.next());
    assertFalse.accept(singleElementIterator.hasNext());

    // Attempting to get next after exhaustion should throw
    assertThrows(Exception.class, singleElementIterator::next);
  }

  @Test
  void testStaticFactoryMethods() {
    // Factory from Java List
    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    javaList.add(str2);
    javaList.add(str3);

    final var iteratorFromList = Iterator._of(javaList);
    assertSet.accept(iteratorFromList);
    assertTrue.accept(iteratorFromList.hasNext());
    assertEquals(str1, iteratorFromList.next());
    assertTrue.accept(iteratorFromList.hasNext());
    assertEquals(str2, iteratorFromList.next());
    assertTrue.accept(iteratorFromList.hasNext());
    assertEquals(str3, iteratorFromList.next());
    assertFalse.accept(iteratorFromList.hasNext());

    // Factory from null Java List
    final var nullListIterator = Iterator._of((java.util.List<Any>) null);
    assertUnset.accept(nullListIterator);

    // Factory from empty Java List
    final var emptyJavaList = new ArrayList<Any>();
    final var emptyIterator = Iterator._of(emptyJavaList);
    assertUnset.accept(emptyIterator);
    assertFalse.accept(emptyIterator.hasNext());

    // Factory from Java Iterator
    final var javaIterator = javaList.iterator();
    final var iteratorFromIterator = Iterator._of(javaIterator);
    assertSet.accept(iteratorFromIterator);
    assertTrue.accept(iteratorFromIterator.hasNext());

    // Factory from null Java Iterator
    final var nullIteratorIterator = Iterator._of((java.util.Iterator<Any>) null);
    assertUnset.accept(nullIteratorIterator);
  }

  @Test
  void testIsSet() {
    final var defaultIterator = new Iterator();
    assertNotNull(defaultIterator);
    assertFalse.accept(defaultIterator._isSet());

    final var singleIterator = new Iterator(str1);
    assertNotNull(singleIterator);
    assertTrue.accept(singleIterator._isSet());

    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    final var factoryIterator = Iterator._of(javaList);
    assertNotNull(factoryIterator);
    assertTrue.accept(factoryIterator._isSet());
  }

  @Test
  void testHasNextBehavior() {
    // Empty iterator
    final var emptyIterator = new Iterator();
    assertFalse.accept(emptyIterator.hasNext());
    // Multiple calls to hasNext should be safe
    assertFalse.accept(emptyIterator.hasNext());
    assertFalse.accept(emptyIterator.hasNext());

    // Single element iterator
    final var singleIterator = new Iterator(str1);
    assertTrue.accept(singleIterator.hasNext());
    assertTrue.accept(singleIterator.hasNext()); // Multiple calls safe
    
    // Consume the element
    assertEquals(str1, singleIterator.next());
    assertFalse.accept(singleIterator.hasNext());
    assertFalse.accept(singleIterator.hasNext()); // Multiple calls safe

    // Multi-element iterator
    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    javaList.add(str2);
    final var multiIterator = Iterator._of(javaList);
    
    assertTrue.accept(multiIterator.hasNext());
    multiIterator.next();
    assertTrue.accept(multiIterator.hasNext());
    multiIterator.next();
    assertFalse.accept(multiIterator.hasNext());
  }

  @Test
  void testNextBehavior() {
    // Single element iterator
    final var singleIterator = new Iterator(str1);
    assertEquals(str1, singleIterator.next());
    
    // Next on exhausted iterator should throw Exception
    assertThrows(Exception.class, singleIterator::next);

    // Multi-element iterator
    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    javaList.add(str2);
    javaList.add(str3);
    final var multiIterator = Iterator._of(javaList);
    
    assertEquals(str1, multiIterator.next());
    assertEquals(str2, multiIterator.next());
    assertEquals(str3, multiIterator.next());
    
    // Should throw after exhaustion
    assertThrows(Exception.class, multiIterator::next);

    // Empty iterator should throw immediately
    final var emptyIterator = new Iterator();
    assertThrows(Exception.class, emptyIterator::next);
  }

  @Test
  void testIterationPattern() {
    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    javaList.add(str2);
    javaList.add(str3);
    final var iterator = Iterator._of(javaList);

    final var collected = new ArrayList<Any>();
    while (iterator.hasNext().state) { // Access the boolean state
      collected.add(iterator.next());
    }

    assertEquals(3, collected.size());
    assertEquals(str1, collected.get(0));
    assertEquals(str2, collected.get(1));
    assertEquals(str3, collected.get(2));
  }

  @Test
  void testListIntegration() {
    // Create List and get its iterator
    final var list = new List(str1);
    list._addAss(str2);
    list._addAss(str3);

    final var iterator = list.iterator();
    assertNotNull(iterator);
    assertSet.accept(iterator);

    // Iterate through list elements
    assertTrue.accept(iterator.hasNext());
    assertEquals(str1, iterator.next());
    assertTrue.accept(iterator.hasNext());
    assertEquals(str2, iterator.next());
    assertTrue.accept(iterator.hasNext());
    assertEquals(str3, iterator.next());
    assertFalse.accept(iterator.hasNext());

    // Empty list iterator
    final var emptyList = new List();
    final var emptyIterator = emptyList.iterator();
    assertNotNull(emptyIterator);
    assertUnset.accept(emptyIterator);
    assertFalse.accept(emptyIterator.hasNext());
  }

  @Test
  void testIteratorStateBehavior() {
    // Test that iterator state is properly managed
    final var singleIterator = new Iterator(str1);
    
    // Before consumption
    assertTrue.accept(singleIterator.hasNext());
    
    // Consume element
    final var element = singleIterator.next();
    assertEquals(str1, element);
    
    // After consumption
    assertFalse.accept(singleIterator.hasNext());
    
    // State should remain consistent
    assertFalse.accept(singleIterator.hasNext());
    assertFalse.accept(singleIterator.hasNext());
  }

  @Test
  void testIteratorEquality() {
    // Iterator equality is based on the underlying Java iterator
    final var javaList1 = new ArrayList<Any>();
    javaList1.add(str1);
    final var iterator1 = Iterator._of(javaList1);
    
    final var javaList2 = new ArrayList<Any>();
    javaList2.add(str1);
    final var iterator2 = Iterator._of(javaList2);
    
    // Different iterators should not be equal even with same content
    assertNotEquals(iterator1, iterator2);
    assertNotEquals(iterator1.hashCode(), iterator2.hashCode());
    
    // Same iterator should be equal to itself
    assertEquals(iterator1, iterator1);
    assertEquals(iterator1.hashCode(), iterator1.hashCode());
  }

  @Test
  void testIteratorWithDifferentTypes() {
    // Test iterator with mixed types (all extend Any)
    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    javaList.add(int1);
    javaList.add(Boolean._of(true));
    
    final var iterator = Iterator._of(javaList);
    
    assertTrue.accept(iterator.hasNext());
    assertEquals(str1, iterator.next());
    assertTrue.accept(iterator.hasNext());
    assertEquals(int1, iterator.next());
    assertTrue.accept(iterator.hasNext());
    assertEquals(Boolean._of(true), iterator.next());
    assertFalse.accept(iterator.hasNext());
  }

  @Test
  void testIteratorExceptionMessages() {
    // Test that exceptions have proper messages
    final var singleIterator = new Iterator(str1);
    singleIterator.next(); // Consume the element
    
    final var exception = assertThrows(Exception.class, singleIterator::next);
    final var exceptionMessage = exception._string();
    assertSet.accept(exceptionMessage);
    assertTrue.accept(exceptionMessage._contains(String._of("No such element")));
    
    // Empty iterator exception
    final var emptyIterator = new Iterator();
    final var emptyException = assertThrows(Exception.class, emptyIterator::next);
    assertNotNull(emptyException.toString());
  }

  @Test
  void testIteratorMemoryEfficiency() {
    // Test that iterator doesn't hold onto entire collection
    final var largeList = new ArrayList<Any>();
    for (int i = 0; i < 10000; i++) {
      largeList.add(Integer._of(i));
    }
    
    final var iterator = Iterator._of(largeList);
    assertNotNull(iterator);
    assertTrue.accept(iterator.hasNext());
    
    // Consume a few elements
    assertEquals(Integer._of(0), iterator.next());
    assertEquals(Integer._of(1), iterator.next());
    assertTrue.accept(iterator.hasNext());
  }

  @Test
  void testIteratorEdgeCases() {
    // Test iterator with null elements (where allowed)
    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    javaList.add(null);
    javaList.add(str2);
    
    final var iterator = Iterator._of(javaList);
    
    assertEquals(str1, iterator.next());
    assertNull(iterator.next()); // Should handle null elements
    assertEquals(str2, iterator.next());
    assertFalse.accept(iterator.hasNext());
    
    // Test iterator created from iterator that's partially consumed
    final var originalJavaList = new ArrayList<Any>();
    originalJavaList.add(str1);
    originalJavaList.add(str2);
    originalJavaList.add(str3);
    
    final var javaIterator = originalJavaList.iterator();
    javaIterator.next(); // Consume first element
    
    final var partialIterator = Iterator._of(javaIterator);
    assertTrue.accept(partialIterator.hasNext());
    assertEquals(str2, partialIterator.next()); // Should start from second element
    assertEquals(str3, partialIterator.next());
    assertFalse.accept(partialIterator.hasNext());
  }
}