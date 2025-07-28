package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class PriorityQueueTest extends Common {

  // Test data
  private final String bill = String._of("Bill");
  private final String ted = String._of("Ted");
  private final String excellent = String._of("Excellent");
  private final String adventure = String._of("Adventure");
  private final String outrageous = String._of("Outrageous");
  private final String bogus = String._of("Bogus");
  private final String and = String._of("And");
  private final String missy = String._of("Missy");

  private final Integer int1 = Integer._of(1);
  private final Integer int2 = Integer._of(2);
  private final Integer int3 = Integer._of(3);
  private final Integer int5 = Integer._of(5);
  private final Integer int10 = Integer._of(10);

  private final Comparator stringComparator = new Comparator() {
    @Override
    public Integer _call(Any o1, Any o2) {
      return o1._cmp(o2);
    }
  };

  private final Comparator reverseStringComparator = new Comparator() {
    @Override
    public Integer _call(Any o1, Any o2) {
      return o1._cmp(o2)._negate();
    }
  };

  @Test
  void testConstruction() {
    // Default constructor creates empty but set queue
    final var defaultQueue = new PriorityQueue();
    assertSet.accept(defaultQueue);
    assertTrue.accept(defaultQueue._empty());

    // Constructor with single element
    final var singleElementQueue = new PriorityQueue(bill);
    assertSet.accept(singleElementQueue);
    assertFalse.accept(singleElementQueue._empty());
    assertEquals(1L, singleElementQueue._len().state);

    // Constructor with unset elements, useful for type inference.
    //But it will be empty as comparing unset elements is pointless.
    final var unsetElementQueue = new PriorityQueue(new String());
    assertSet.accept(unsetElementQueue);
    assertTrue.accept(unsetElementQueue._empty());

    // Factory methods
    final var factoryEmpty = PriorityQueue._of();
    assertSet.accept(factoryEmpty);
    assertTrue.accept(factoryEmpty._empty());

    final var factoryWithItem = PriorityQueue._of(ted);
    assertSet.accept(factoryWithItem);
    assertEquals(1L, factoryWithItem._len().state);

    // Factory with Java collection
    final var javaList = new ArrayList<Any>();
    javaList.add(bill);
    javaList.add(ted);
    final var factoryFromCollection = PriorityQueue._of(javaList);
    assertSet.accept(factoryFromCollection);
    assertEquals(2L, factoryFromCollection._len().state);
  }

  @Test
  void testFluentAPIWithComparator() {
    // Create queue with comparator
    final var queue = new PriorityQueue(bill).withComparator(stringComparator);
    assertSet.accept(queue);

    // Add more elements
    queue._addAss(ted);
    queue._addAss(excellent);
    queue._addAss(adventure);

    // Check ordering - should be reverse alphabetical
    final var list = queue.list();
    assertEquals(4L, list._len().state);

    // With comparator: Ted > Excellent > Bill > Adventure
    assertEquals(ted, list.get(Integer._of(0)));
    assertEquals(excellent, list.get(Integer._of(1)));
    assertEquals(bill, list.get(Integer._of(2)));
    assertEquals(adventure, list.get(Integer._of(3)));
  }

  @Test
  void testFluentAPIWithReverseComparator() {
    // Create queue with comparator
    final var queue = new PriorityQueue(bill).withComparator(reverseStringComparator);
    assertSet.accept(queue);

    // Add more elements
    queue._addAss(ted);
    queue._addAss(excellent);
    queue._addAss(adventure);

    // Check ordering - should be alphabetical now because the comparator is reverse ordering.
    final var list = queue.list();
    assertEquals(4L, list._len().state);

    // With comparator: Ted < Excellent < Bill < Adventure
    assertEquals(adventure, list.get(Integer._of(0)));
    assertEquals(bill, list.get(Integer._of(1)));
    assertEquals(excellent, list.get(Integer._of(2)));
    assertEquals(ted, list.get(Integer._of(3)));
  }

  @Test
  void testFluentAPIWithSize() {
    // Create queue with size limit
    final var queue = new PriorityQueue(bill).withSize(Integer._of(3));
    assertSet.accept(queue);

    // Add elements up to limit
    queue._addAss(ted);
    queue._addAss(excellent);
    assertEquals(3L, queue._len().state);

    // Add one more - should still be limited to 3
    queue._addAss(adventure);
    assertEquals(3L, queue._len().state);
  }

  @Test
  void testFluentAPIWithBothComparatorAndSize() {
    // Create queue with both comparator and size limit
    final var queue = new PriorityQueue(bill)
        .withComparator(stringComparator)
        .withSize(Integer._of(3));

    // Add more elements than the size limit
    queue._addAss(ted);
    queue._addAss(excellent);
    queue._addAss(adventure);
    queue._addAss(outrageous);
    queue._addAss(bogus);

    // Should maintain only 3 highest priority elements, using the reverse string comparator
    assertEquals(3L, queue._len().state);

    final var list = queue.list();
    // Highest priority elements in reverse alphabetical order: Ted, Outrageous, Excellent
    assertEquals(ted, list.get(Integer._of(0)));
    assertEquals(outrageous, list.get(Integer._of(1)));
    assertEquals(excellent, list.get(Integer._of(2)));
  }

  @Test
  void testFluentAPIReversedWithBothComparatorAndSize() {
    // Create queue with both comparator and size limit, but use reverse comparator.
    final var queue = new PriorityQueue(bill)
        .withComparator(reverseStringComparator)
        .withSize(Integer._of(3));

    // Add more elements than the size limit
    queue._addAss(ted);
    queue._addAss(excellent);
    queue._addAss(adventure);
    queue._addAss(outrageous);
    queue._addAss(bogus);

    // Should maintain only 3 highest priority elements, using the reverse string comparator
    assertEquals(3L, queue._len().state);

    final var list = queue.list();
    // Highest priority elements in alphabetical order: Adventure, Bill, Bogus
    //This is because a reverse comparator was used.
    assertEquals(adventure, list.get(Integer._of(0)));
    assertEquals(bill, list.get(Integer._of(1)));
    assertEquals(bogus, list.get(Integer._of(2)));
  }

  @Test
  void testDocumentationExample() {
    // Reproduce the exact example from documentation
    final var qOne = new PriorityQueue(bill).withComparator(stringComparator);

    qOne._addAss(and);
    qOne._addAss(ted);
    qOne._addAss(excellent);
    qOne._addAss(adventure);
    qOne._addAss(outrageous);
    qOne._addAss(bogus);

    final var allEntries = qOne.list();

    // Check they are in reverse order by String comparison not order added
    // Expected: [Ted, Outrageous, Excellent, Bogus, Bill, And, Adventure]
    assertEquals(7L, allEntries._len().state);
    assertEquals(ted, allEntries.get(Integer._of(0)));
    assertEquals(outrageous, allEntries.get(Integer._of(1)));
    assertEquals(excellent, allEntries.get(Integer._of(2)));
    assertEquals(bogus, allEntries.get(Integer._of(3)));
    assertEquals(bill, allEntries.get(Integer._of(4)));
    assertEquals(and, allEntries.get(Integer._of(5)));
    assertEquals(adventure, allEntries.get(Integer._of(6)));

    // Now make the list finite with just 3 entries
    final var qOneWithSize = qOne.withSize(Integer._of(3));

    // Should contain top 3 elements: Bill, And, Adventure
    final var limitedEntries = qOneWithSize.list();
    assertEquals(3L, limitedEntries._len().state);
    assertEquals(ted, limitedEntries.get(Integer._of(0)));
    assertEquals(outrageous, limitedEntries.get(Integer._of(1)));
    assertEquals(excellent, limitedEntries.get(Integer._of(2)));

    //Missy will make the cut!
    qOneWithSize._addAss(missy);

    // Should still be 3 elements, and Missy will make it by replacing 'excellent'.
    final var afterMissy = qOneWithSize.list();
    assertEquals(3L, afterMissy._len().state);
    assertTrue.accept(qOneWithSize._contains(missy));
  }

  @Test
  void testAdditionOperators() {
    // Test + operator with single element
    final var queue1 = new PriorityQueue(bill);
    final var queue2 = queue1._add(ted);
    assertSet.accept(queue2);
    assertEquals(2L, queue2._len().state);
    assertTrue.accept(queue2._contains(bill));
    assertTrue.accept(queue2._contains(ted));

    // Original queue unchanged
    assertEquals(1L, queue1._len().state);

    // Test + operator with List
    final var list = new List();
    list._addAss(excellent);
    list._addAss(adventure);

    final var queue3 = queue2._add(list);
    assertEquals(4L, queue3._len().state);
    assertTrue.accept(queue3._contains(excellent));
    assertTrue.accept(queue3._contains(adventure));

    // Test + operator with PriorityQueue
    final var queue5 = new PriorityQueue(outrageous);
    queue5._addAss(bogus);
    
    final var queue6 = queue2._add(queue5);
    assertEquals(4L, queue6._len().state);
    assertTrue.accept(queue6._contains(bill));
    assertTrue.accept(queue6._contains(ted));
    assertTrue.accept(queue6._contains(outrageous));
    assertTrue.accept(queue6._contains(bogus));
    
    // Original queues unchanged
    assertEquals(2L, queue2._len().state);
    assertEquals(2L, queue5._len().state);

    // Test += operator
    final var queue4 = new PriorityQueue(bogus);
    queue4._addAss(outrageous);
    assertEquals(2L, queue4._len().state);

    // Test += with List
    queue4._addAss(list);
    assertEquals(4L, queue4._len().state);

    // Test += with PriorityQueue
    final var queue7 = new PriorityQueue(and);
    final var queue8 = new PriorityQueue(missy);
    queue8._addAss(excellent);
    
    queue7._addAss(queue8);
    assertEquals(3L, queue7._len().state);
    assertTrue.accept(queue7._contains(and));
    assertTrue.accept(queue7._contains(missy));
    assertTrue.accept(queue7._contains(excellent));
  }

  @Test
  void testSubtractionOperators() {
    // Set up base queue with multiple elements
    final var baseQueue = new PriorityQueue(bill);
    baseQueue._addAss(ted);
    baseQueue._addAss(excellent);
    baseQueue._addAss(adventure);
    assertEquals(4L, baseQueue._len().state);

    // Test - operator with single element
    final var queue1 = baseQueue._sub(ted);
    assertSet.accept(queue1);
    assertEquals(3L, queue1._len().state);
    assertFalse.accept(queue1._contains(ted));
    assertTrue.accept(queue1._contains(bill));
    assertTrue.accept(queue1._contains(excellent));
    assertTrue.accept(queue1._contains(adventure));

    // Original queue unchanged
    assertEquals(4L, baseQueue._len().state);
    assertTrue.accept(baseQueue._contains(ted));

    // Test - operator with element not in queue (should be no-op)
    final var queue2 = baseQueue._sub(bogus);
    assertEquals(4L, queue2._len().state);
    assertTrue.accept(queue2._contains(bill));
    assertTrue.accept(queue2._contains(ted));
    assertTrue.accept(queue2._contains(excellent));
    assertTrue.accept(queue2._contains(adventure));

    // Test - operator with List
    final var list = new List();
    list._addAss(bill);
    list._addAss(excellent);

    final var queue3 = baseQueue._sub(list);
    assertEquals(2L, queue3._len().state);
    assertFalse.accept(queue3._contains(bill));
    assertFalse.accept(queue3._contains(excellent));
    assertTrue.accept(queue3._contains(ted));
    assertTrue.accept(queue3._contains(adventure));

    // Test - operator with PriorityQueue
    final var toRemove = new PriorityQueue(ted);
    toRemove._addAss(adventure);

    final var queue4 = baseQueue._sub(toRemove);
    assertEquals(2L, queue4._len().state);
    assertFalse.accept(queue4._contains(ted));
    assertFalse.accept(queue4._contains(adventure));
    assertTrue.accept(queue4._contains(bill));
    assertTrue.accept(queue4._contains(excellent));

    // Original queues unchanged
    assertEquals(4L, baseQueue._len().state);
    assertEquals(2L, toRemove._len().state);

    // Test with unset arguments
    final var unsetQueue = new PriorityQueue(new String());
    final var queue5 = baseQueue._sub(unsetQueue);
    assertEquals(4L, queue5._len().state); // Should be unchanged

    // Test empty queue subtraction
    final var emptyQueue = new PriorityQueue();
    final var queue6 = emptyQueue._sub(bill);
    assertSet.accept(queue6);
    assertTrue.accept(queue6._empty());
  }

  @Test
  void testSubtractionAssignmentOperators() {
    // Test -= operator with single element
    final var queue1 = new PriorityQueue(bill);
    queue1._addAss(ted);
    queue1._addAss(excellent);
    queue1._addAss(adventure);
    assertEquals(4L, queue1._len().state);

    queue1._subAss(ted);
    assertEquals(3L, queue1._len().state);
    assertFalse.accept(queue1._contains(ted));
    assertTrue.accept(queue1._contains(bill));
    assertTrue.accept(queue1._contains(excellent));
    assertTrue.accept(queue1._contains(adventure));
    assertEquals(3L, queue1._len().state);

    // Test -= operator with element not in queue (should be no-op)
    queue1._subAss(bogus);
    assertEquals(3L, queue1._len().state); // No change

    // Test -= operator with List
    final var queue2 = new PriorityQueue(bill);
    queue2._addAss(ted);
    queue2._addAss(excellent);
    queue2._addAss(adventure);
    queue2._addAss(outrageous);

    final var listToRemove = new List();
    listToRemove._addAss(bill);
    listToRemove._addAss(excellent);
    listToRemove._addAss(bogus); // Not in queue

    queue2._subAss(listToRemove);
    assertEquals(3L, queue2._len().state);
    assertFalse.accept(queue2._contains(bill));
    assertFalse.accept(queue2._contains(excellent));
    assertTrue.accept(queue2._contains(ted));
    assertTrue.accept(queue2._contains(adventure));
    assertTrue.accept(queue2._contains(outrageous));

    // Test -= operator with PriorityQueue
    final var queue3 = new PriorityQueue(bill);
    queue3._addAss(ted);
    queue3._addAss(excellent);
    queue3._addAss(adventure);
    queue3._addAss(outrageous);

    final var toRemoveQueue = new PriorityQueue(ted);
    toRemoveQueue._addAss(adventure);
    toRemoveQueue._addAss(missy); // Not in queue3

    queue3._subAss(toRemoveQueue);
    assertEquals(3L, queue3._len().state);
    assertFalse.accept(queue3._contains(ted));
    assertFalse.accept(queue3._contains(adventure));
    assertTrue.accept(queue3._contains(bill));
    assertTrue.accept(queue3._contains(excellent));
    assertTrue.accept(queue3._contains(outrageous));

    // toRemoveQueue should be unchanged
    assertEquals(3L, toRemoveQueue._len().state);
    assertTrue.accept(toRemoveQueue._contains(ted));
    assertTrue.accept(toRemoveQueue._contains(adventure));
    assertTrue.accept(toRemoveQueue._contains(missy));

    // Test with unset arguments
    final var queue4 = new PriorityQueue(bill);
    queue4._addAss(ted);
    final var originalSize = queue4._len().state;

    final var unsetQueue = new PriorityQueue(new String());
    queue4._subAss(unsetQueue);
    assertEquals(originalSize, queue4._len().state); // Should be unchanged

    // Test empty queue subtraction assignment
    final var emptyQueue = new PriorityQueue();
    emptyQueue._subAss(bill);
    assertTrue.accept(emptyQueue._empty()); // Should remain empty

    // Test self-subtraction (edge case)
    final var queue5 = new PriorityQueue(bill);
    queue5._addAss(ted);
    queue5._subAss(queue5); // Remove all elements from itself
    assertTrue.accept(queue5._empty());
  }

  @Test
  void testSubtractionWithComparatorAndSizeLimit() {
    // Test subtraction operations preserve comparator and size settings
    final var baseQueue = new PriorityQueue(bill)
        .withComparator(stringComparator)
        .withSize(Integer._of(3));
    
    baseQueue._addAss(ted);
    baseQueue._addAss(excellent);
    baseQueue._addAss(adventure);
    baseQueue._addAss(outrageous); // Will be limited to 3 elements

    // Test pure subtraction preserves settings
    final var subtractedQueue = baseQueue._sub(ted);
    assertEquals(2L, subtractedQueue._len().state);
    assertFalse.accept(subtractedQueue._contains(ted));
    
    // Add element to verify comparator is preserved
    subtractedQueue._addAss(and);
    final var resultList = subtractedQueue.list();
    // Should still be ordered by comparator (reverse alphabetical)
    assertTrue.accept(resultList.get(Integer._of(0))._cmp(resultList.get(Integer._of(1)))._gt(Integer._of(0)));

    // Test subtraction with size limit interactions
    final var sizedQueue = new PriorityQueue()
        .withComparator(stringComparator)
        .withSize(Integer._of(2));
    
    sizedQueue._addAss(bill);
    sizedQueue._addAss(ted);
    sizedQueue._addAss(excellent); // Will evict lowest priority
    assertEquals(2L, sizedQueue._len().state);

    // Remove element, then add back - should respect size limit
    final var afterSubtraction = sizedQueue._sub(ted);
    assertEquals(1L, afterSubtraction._len().state);
    
    afterSubtraction._addAss(adventure);
    afterSubtraction._addAss(outrageous);
    assertEquals(2L, afterSubtraction._len().state); // Size limit respected

    // Test null/unset edge cases for subtraction
    final var queue = new PriorityQueue(bill);
    queue._addAss(ted);
    
    // Subtraction with null should not crash - cast to specific type to avoid ambiguity
    final var afterNullSub = queue._sub((Any) null);
    assertEquals(2L, afterNullSub._len().state); // Should be unchanged
    
    queue._subAss((Any) null); // Should not crash
    assertEquals(2L, queue._len().state); // Should be unchanged
  }

  @Test
  void testComparisonOperators() {
    // Test equality
    final var queue1 = new PriorityQueue(bill);
    assertNotNull(queue1);
    queue1._addAss(ted);

    final var queue2 = new PriorityQueue(bill);
    queue2._addAss(ted);

    assertUnset.accept(queue1._eq(null));
    assertTrue.accept(queue1._eq(queue2));

    assertUnset.accept(queue1._neq(null));
    assertFalse.accept(queue1._neq(queue2));

    // Test inequality
    final var queue3 = new PriorityQueue(excellent);
    assertFalse.accept(queue1._eq(queue3));
    assertTrue.accept(queue1._neq(queue3));

    // Test with unset,
    final var unsetQueue = new PriorityQueue(new String());
    assertSet.accept(unsetQueue);
    assertTrue.accept(queue1._neq(unsetQueue));

    // Test _isSet
    assertTrue.accept(queue1._isSet());
    assertTrue.accept(unsetQueue._isSet());
  }

  @Test
  void testCollectionOperators() {
    final var queue = new PriorityQueue(bill);
    queue._addAss(ted);
    queue._addAss(excellent);

    // Test empty
    assertFalse.accept(queue._empty());
    final var emptyQueue = new PriorityQueue();
    assertTrue.accept(emptyQueue._empty());

    // Test length
    assertEquals(3L, queue._len().state);
    assertEquals(0L, emptyQueue._len().state);

    // Test contains
    assertTrue.accept(queue._contains(bill));
    assertTrue.accept(queue._contains(ted));
    assertFalse.accept(queue._contains(adventure));

  }

  @Test
  void testAssignmentOperators() {
    // Test copy (:=:)
    final var original = new PriorityQueue(bill).withComparator(stringComparator);
    original._addAss(ted);
    original._addAss(excellent);

    final var copy = new PriorityQueue();
    copy._copy(original);

    assertTrue.accept(original._eq(copy));
    assertEquals(original._len().state, copy._len().state);

    // Test replace (:^:)
    final var replace = new PriorityQueue(adventure);
    replace._replace(original);
    assertTrue.accept(original._eq(replace));

    // Test merge (:~:)
    final var queue1 = new PriorityQueue(bill);
    final var queue2 = new PriorityQueue(ted);
    queue2._addAss(excellent);

    queue1._merge(queue2);
    assertEquals(3L, queue1._len().state);
    assertTrue.accept(queue1._contains(bill));
    assertTrue.accept(queue1._contains(ted));
    assertTrue.accept(queue1._contains(excellent));
  }

  @Test
  void testConversionOperators() {
    final var queue = new PriorityQueue(bill);
    queue._addAss(ted);
    queue._addAss(excellent);

    // Test string conversion
    final var str = queue._string();
    assertSet.accept(str);
    assertNotNull(str.toString());

    // Test hash code
    final var hash = queue._hashcode();
    assertSet.accept(hash);

    // Equal queues should have equal hash codes
    final var queue2 = new PriorityQueue(bill);
    queue2._addAss(ted);
    queue2._addAss(excellent);

    // Hash codes should be consistent for same object
    assertEquals(queue._hashcode().state, queue._hashcode().state);
    assertEquals(queue2._hashcode().state, queue2._hashcode().state);
  }

  @Test
  void testPipelineOperator() {
    final var queue = new PriorityQueue();

    // Pipe elements into queue
    queue._pipe(bill);
    queue._pipe(ted);
    queue._pipe(excellent);

    assertEquals(3L, queue._len().state);
    assertTrue.accept(queue._contains(bill));
    assertTrue.accept(queue._contains(ted));
    assertTrue.accept(queue._contains(excellent));
  }

  @Test
  void testIterator() {
    final var queue = new PriorityQueue(bill).withComparator(stringComparator);
    queue._addAss(ted);
    queue._addAss(excellent);
    queue._addAss(adventure);

    final var iter = queue.iterator();
    assertSet.accept(iter);

    final var items = new ArrayList<Any>();
    while (iter.hasNext()._isSet().state && iter.hasNext().state) {
      items.add(iter.next());
    }

    // Should iterate in priority order
    assertEquals(4, items.size());
    assertEquals(ted, items.get(0));
    assertEquals(excellent, items.get(1));
    assertEquals(bill, items.get(2));
    assertEquals(adventure, items.get(3));
  }

  @Test
  void testWithIntegers() {
    // Test with integers using natural ordering (no comparator)
    final var queue = new PriorityQueue(int5);
    queue._addAss(int1);
    queue._addAss(int10);
    queue._addAss(int3);
    queue._addAss(int2);

    //Without a comparator, the order is arbitrary and not guaranteed.
    //But there should be 5!
    final var list = queue.list();
    assertEquals(5L, list._len().state);
  }

  @Test
  void testSizeLimitWithPriorityOrdering() {
    // Test that size limit respects priority ordering
    final var queue = new PriorityQueue()
        .withComparator(stringComparator)
        .withSize(Integer._of(3));

    // Add elements one by one and verify size limit behavior
    queue._addAss(bill);
    assertEquals(1L, queue._len().state);

    queue._addAss(adventure);
    assertEquals(2L, queue._len().state);

    queue._addAss(ted);
    assertEquals(3L, queue._len().state);

    queue._addAss(excellent); // [Ted, Excellent, Bill] (Adventure evicted)
    assertEquals(3L, queue._len().state);

    assertFalse.accept(queue._contains(adventure));
    assertTrue.accept(queue._contains(ted));
    assertTrue.accept(queue._contains(excellent));
    assertTrue.accept(queue._contains(bill));

    // Add lower priority item - should not be added
    queue._addAss(and);       // Should not change queue
    assertEquals(3L, queue._len().state);
    assertFalse.accept(queue._contains(and));
  }

  @Test
  void testEdgeCases() {
    // Empty queue operations
    final var empty = new PriorityQueue();
    assertNotNull(empty);
    assertNotNull(empty.toString());
    assertUnset.accept(empty._contains(null));
    assertUnset.accept(empty._contains(new Any(){}));

    assertSet.accept(empty);
    assertTrue.accept(empty._empty());
    assertEquals(0L, empty._len().state);
    assertFalse.accept(empty._contains(bill));

    // Zero size limit, ignored and defaults to unlimited.
    final var zeroSize = new PriorityQueue().withSize(Integer._of(0));
    zeroSize._addAss(bill);
    assertEquals(1L, zeroSize._len().state);

    // Very large queue
    final var large = new PriorityQueue();
    for (int i = 0; i < 100; i++) {
      large._addAss(Integer._of(i));
    }
    assertEquals(100L, large._len().state);
  }

  @Test
  void testErrorConditions() {
    // Null handling in factory methods
    final var fromNull = PriorityQueue._of((java.util.Collection<Any>) null);
    assertNotNull(fromNull);
    assertSet.accept(fromNull);
    assertTrue.accept(fromNull._empty());

    // Invalid size - will just be ignored.
    final var queue = new PriorityQueue(bill);
    final var withNegativeSize = queue.withSize(Integer._of(-1));
    assertSet.accept(withNegativeSize);

    // Unset comparator - new Comparator() is actually set, so this should be set too
    final var withUnsetComparator = queue.withComparator(new Comparator());
    assertSet.accept(withUnsetComparator);
  }

  @Test
  void testDuplicateValueHandling() {
    // Test adding duplicate values
    final var alsoBill = String._of("Bill");
    final var queue = new PriorityQueue(bill);
    queue._addAss(bill);  // Add "Bill" again
    queue._addAss(alsoBill);  // Add "Bill" third time but as a different object
    queue._addAss(ted);   // Add "Ted"
    
    assertEquals(4L, queue._len().state);
    assertTrue.accept(queue._contains(bill));
    assertTrue.accept(queue._contains(ted));
    
    // Test that list() contains all duplicates
    final var list = queue.list();
    assertEquals(4L, list._len().state);
    
    // Count occurrences of "Bill" in the list
    int billCount = 0;
    int tedCount = 0;
    for (int i = 0; i < list._len().state; i++) {
      final var item = list.get(Integer._of(i));
      if (bill._eq(item).state) {
        billCount++;
      } else if (ted._eq(item).state) {
        tedCount++;
      }
    }
    assertEquals(3, billCount);
    assertEquals(1, tedCount);

    // Test removal of duplicates - should only remove one instance
    queue._subAss(bill);
    assertEquals(3L, queue._len().state);
    assertTrue.accept(queue._contains(bill)); // Still contains "Bill"
    
    // Verify two instances of "Bill" remain
    final var listAfterRemoval = queue.list();
    int remainingBillCount = 0;
    for (int i = 0; i < listAfterRemoval._len().state; i++) {
      final var item = listAfterRemoval.get(Integer._of(i));
      if (bill._eq(item).state) {
        remainingBillCount++;
      }
    }
    assertEquals(2, remainingBillCount);

    // Test duplicates with size limit
    final var limitedQueue = new PriorityQueue()
        .withComparator(stringComparator)
        .withSize(Integer._of(3));
    
    limitedQueue._addAss(bill);
    limitedQueue._addAss(bill);
    limitedQueue._addAss(bill);
    assertEquals(3L, limitedQueue._len().state);
    
    // Add higher priority item - should evict lowest priority duplicate
    limitedQueue._addAss(ted);
    assertEquals(3L, limitedQueue._len().state);
    assertTrue.accept(limitedQueue._contains(ted));
    assertTrue.accept(limitedQueue._contains(bill));
    
    // Verify only 2 instances of "Bill" remain after eviction
    final var limitedList = limitedQueue.list();
    int limitedBillCount = 0;
    for (int i = 0; i < limitedList._len().state; i++) {
      final var item = limitedList.get(Integer._of(i));
      if (bill._eq(item).state) {
        limitedBillCount++;
      }
    }
    assertEquals(2, limitedBillCount);

    // Test duplicate removal with List operation
    final var removalList = new List();
    removalList._addAss(bill);
    removalList._addAss(bill); // Two instances to remove
    
    final var queueForListRemoval = new PriorityQueue();
    queueForListRemoval._addAss(bill);
    queueForListRemoval._addAss(bill);
    queueForListRemoval._addAss(bill);
    queueForListRemoval._addAss(ted);
    assertEquals(4L, queueForListRemoval._len().state);
    
    queueForListRemoval._subAss(removalList);
    assertEquals(2L, queueForListRemoval._len().state); // Should remove 2 instances of "Bill"
    assertTrue.accept(queueForListRemoval._contains(bill)); // One "Bill" should remain
    assertTrue.accept(queueForListRemoval._contains(ted));

    // Test equality with duplicates - queues with same duplicates should be equal
    final var queue1 = new PriorityQueue(excellent);
    queue1._addAss(excellent);
    queue1._addAss(adventure);
    
    final var queue2 = new PriorityQueue(excellent);
    queue2._addAss(excellent);
    queue2._addAss(adventure);
    
    assertTrue.accept(queue1._eq(queue2));
    
    // Different number of duplicates should not be equal
    queue2._addAss(excellent); // One more duplicate
    assertFalse.accept(queue1._eq(queue2));
  }

  @Test
  void testJsonOperator() {
    // Test empty PriorityQueue - should produce empty JSON array
    final var emptyQueue = new PriorityQueue();
    final var emptyJson = emptyQueue._json();
    assertSet.accept(emptyJson);
    assertTrue.accept(emptyJson.arrayNature());
    assertTrue.accept(emptyJson._empty());
    assertEquals(Integer._of(0), emptyJson._len());

    // Test empty queue JSON pretty print format
    final var emptyJsonString = emptyQueue._json().prettyPrint();
    assertSet.accept(emptyJsonString);
    
    final var expectedEmptyJson = """
        [ ]"""; // Empty JSON array with pretty print formatting
    
    assertEquals(expectedEmptyJson.trim(), emptyJsonString.state.trim());

    // Test PriorityQueue with single element
    final var singleQueue = new PriorityQueue(bill);
    final var singleJson = singleQueue._json();
    assertSet.accept(singleJson);
    assertTrue.accept(singleJson.arrayNature());
    assertEquals(Integer._of(1), singleJson._len());
    
    // Verify the first element matches the string's JSON representation
    final var firstElement = singleJson.get(Integer._of(0));
    assertSet.accept(firstElement);
    final var expectedBillJson = bill._json();
    assertTrue.accept(firstElement._eq(expectedBillJson));

    // Test single element JSON pretty print format
    final var singleJsonString = singleQueue._json().prettyPrint();
    assertSet.accept(singleJsonString);
    
    final var expectedSingleJson = """
        [ "Bill" ]"""; // Array with one String element (pretty printed)
    
    assertEquals(expectedSingleJson.trim(), singleJsonString.state.trim());

    // Test PriorityQueue with multiple elements and comparator (shows priority ordering)
    final var priorityQueue = new PriorityQueue(bill).withComparator(stringComparator);
    priorityQueue._addAss(ted);
    priorityQueue._addAss(excellent);
    priorityQueue._addAss(adventure);
    
    final var priorityJson = priorityQueue._json();
    assertSet.accept(priorityJson);
    assertTrue.accept(priorityJson.arrayNature());
    assertEquals(Integer._of(4), priorityJson._len());
    
    // Verify priority order is preserved in JSON (Ted > Excellent > Bill > Adventure)
    final var elem0 = priorityJson.get(Integer._of(0));
    final var elem1 = priorityJson.get(Integer._of(1));
    final var elem2 = priorityJson.get(Integer._of(2));
    final var elem3 = priorityJson.get(Integer._of(3));
    
    assertTrue.accept(elem0._eq(ted._json()));
    assertTrue.accept(elem1._eq(excellent._json()));
    assertTrue.accept(elem2._eq(bill._json()));
    assertTrue.accept(elem3._eq(adventure._json()));

    // Test multiple elements JSON pretty print format showing priority order
    final var priorityJsonString = priorityQueue._json().prettyPrint();
    assertSet.accept(priorityJsonString);
    
    final var expectedPriorityJson = """
        [ "Ted", "Excellent", "Bill", "Adventure" ]"""; // Array showing priority order (pretty printed)
    
    assertEquals(expectedPriorityJson.trim(), priorityJsonString.state.trim());

    // Test size-limited PriorityQueue JSON
    final var limitedQueue = new PriorityQueue()
        .withComparator(stringComparator)
        .withSize(Integer._of(2));
    limitedQueue._addAss(bill);
    limitedQueue._addAss(ted);
    limitedQueue._addAss(excellent);
    limitedQueue._addAss(adventure); // Will be evicted
    
    final var limitedJson = limitedQueue._json();
    assertSet.accept(limitedJson);
    assertEquals(Integer._of(2), limitedJson._len());
    
    // Should contain top 2 elements: Ted and Excellent
    final var topElem0 = limitedJson.get(Integer._of(0));
    final var topElem1 = limitedJson.get(Integer._of(1));
    assertTrue.accept(topElem0._eq(ted._json()));
    assertTrue.accept(topElem1._eq(excellent._json()));

    // Test size-limited queue JSON pretty print format
    final var limitedJsonString = limitedQueue._json().prettyPrint();
    assertSet.accept(limitedJsonString);
    
    final var expectedLimitedJson = """
        [ "Ted", "Excellent" ]"""; // Array with top 2 priority elements only (pretty printed)
    
    assertEquals(expectedLimitedJson.trim(), limitedJsonString.state.trim());

    // Test PriorityQueue with different string values showing clear priority structure
    final var demonstrationQueue = new PriorityQueue()
        .withComparator(stringComparator)
        .withSize(Integer._of(3));
    demonstrationQueue._addAss(String._of("Alpha"));
    demonstrationQueue._addAss(String._of("Zulu"));
    demonstrationQueue._addAss(String._of("Beta"));
    demonstrationQueue._addAss(String._of("Charlie"));
    demonstrationQueue._addAss(String._of("Delta"));

    final var demonstrationJsonString = demonstrationQueue._json().prettyPrint();
    assertSet.accept(demonstrationJsonString);
    
    final var expectedDemonstrationJson = """
        [ "Zulu", "Delta", "Charlie" ]"""; // Top 3 in reverse alphabetical order (pretty printed)
    
    assertEquals(expectedDemonstrationJson.trim(), demonstrationJsonString.state.trim());
  }

  @Test
  void testEqualsAndHashCode() {
    // Test equals contract
    final var queue1 = new PriorityQueue(bill);
    queue1._addAss(ted);

    final var queue2 = new PriorityQueue(bill);
    queue2._addAss(ted);

    assertEquals(queue1, queue1); // reflexive
    assertEquals(queue1, queue2); // symmetric
    assertEquals(queue2, queue1); // symmetric
    assertNotEquals(null, queue1);
    assertNotEquals("not a queue", queue1);

    // Unset queues are equal
    assertEquals(new PriorityQueue(), new PriorityQueue());

    // Hash code consistency - equal objects should have equal hash codes
    if (queue1.equals(queue2)) {
      assertEquals(queue1.hashCode(), queue2.hashCode());
    }

    // Different queues have different hash codes
    final var queue3 = new PriorityQueue(excellent);
    assertNotEquals(queue1.hashCode(), queue3.hashCode());
  }
}