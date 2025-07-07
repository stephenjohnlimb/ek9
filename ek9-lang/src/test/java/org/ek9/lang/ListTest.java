package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ListTest extends Common {

  final List unset = new List();
  final List emptyList = new List();
  final String str1 = String._of("first");
  final String str2 = String._of("second");
  final String str3 = String._of("third");
  final Integer int1 = Integer._of(1);

  @Test
  void testConstruction() {
    // Default constructor creates empty but set list
    final var defaultConstructor = new List();
    assertSet.accept(defaultConstructor);
    assertTrue.accept(defaultConstructor._empty());
    assertEquals(0, defaultConstructor._len().state);

    // Constructor with single element
    final var singleElementList = new List(str1);
    assertSet.accept(singleElementList);
    assertFalse.accept(singleElementList._empty());
    assertEquals(1, singleElementList._len().state);
    assertEquals(str1, singleElementList.first());

    // Static factory with Java list
    final var javaList = new ArrayList<Any>();
    javaList.add(str1);
    javaList.add(str2);
    final var factoryList = List._of(javaList);
    assertSet.accept(factoryList);
    assertEquals(2, factoryList._len().state);
    assertEquals(str1, factoryList.first());
    assertEquals(str2, factoryList.last());

    // Static factory with null
    final var nullList = List._of(null);
    assertSet.accept(nullList); // Returns empty but set list

    // Static factory with empty Java list
    final var emptyJavaList = new ArrayList<Any>();
    final var emptyFactoryList = List._of(emptyJavaList);
    assertSet.accept(emptyFactoryList);
    assertTrue.accept(emptyFactoryList._empty());
  }

  @Test
  void testIsSet() {
    assertNotNull(unset);
    assertTrue.accept(unset._isSet());

    assertNotNull(emptyList);
    assertTrue.accept(emptyList._isSet());

    final var populatedList = new List(str1);
    assertNotNull(populatedList);
    assertTrue.accept(populatedList._isSet());
  }

  @Test
  void testGetMethod() {
    final var testList = new List(str1);
    testList._addAss(str2);
    testList._addAss(str3);

    // Valid indices
    assertEquals(str1, testList.get(Integer._of(0)));
    assertEquals(str2, testList.get(Integer._of(1)));
    assertEquals(str3, testList.get(Integer._of(2)));

    // Invalid index - positive index out of bounds throws EK9 Exception
    assertThrows(Exception.class, () -> testList.get(Integer._of(3)));
    // Negative index throws IndexOutOfBoundsException from underlying ArrayList  
    assertThrows(Exception.class, () -> testList.get(Integer._of(-1)));

    // Unset index should throw Exception
    assertThrows(Exception.class, () -> testList.get(new Integer()));
  }

  @Test
  void testFirstAndLast() {
    // Empty list should throw exception
    assertThrows(Exception.class, emptyList::first);
    assertThrows(Exception.class, emptyList::last);

    // Single element list
    final var singleList = new List(str1);
    assertEquals(str1, singleList.first());
    assertEquals(str1, singleList.last());

    // Multiple element list
    final var multiList = new List(str1);
    multiList._addAss(str2);
    multiList._addAss(str3);
    assertEquals(str1, multiList.first());
    assertEquals(str3, multiList.last());
  }

  @Test
  void testReverse() {
    // Empty list reverse
    final var reversedEmpty = emptyList.reverse();
    assertTrue.accept(reversedEmpty._empty());
    assertEquals(emptyList, reversedEmpty); // Both are empty lists

    // Single element reverse
    final var singleList = new List(str1);
    final var reversedSingle = singleList.reverse();
    assertEquals(1, reversedSingle._len().state);
    assertEquals(str1, reversedSingle.first());
    assertEquals(singleList, reversedSingle); // Content is same for single element

    // Multiple element reverse
    final var originalList = new List(str1);
    originalList._addAss(str2);
    originalList._addAss(str3);
    final var reversedList = originalList.reverse();

    assertEquals(str3, reversedList.first());
    assertEquals(str2, reversedList.get(Integer._of(1)));
    assertEquals(str1, reversedList.last());

    // Original list unchanged
    assertEquals(str1, originalList.first());
    assertEquals(str3, originalList.last());
  }

  @Test
  void testIterator() {
    // Empty list iterator
    final var emptyIterator = emptyList.iterator();
    assertNotNull(emptyIterator);
    assertFalse.accept(emptyIterator.hasNext());

    // Single element iterator
    final var singleList = new List(str1);
    final var singleIterator = singleList.iterator();
    assertTrue.accept(singleIterator.hasNext());
    assertEquals(str1, singleIterator.next());
    assertFalse.accept(singleIterator.hasNext());

    // Multiple element iterator
    final var multiList = new List(str1);
    multiList._addAss(str2);
    multiList._addAss(str3);
    final var multiIterator = multiList.iterator();

    assertTrue.accept(multiIterator.hasNext());
    assertEquals(str1, multiIterator.next());
    assertTrue.accept(multiIterator.hasNext());
    assertEquals(str2, multiIterator.next());
    assertTrue.accept(multiIterator.hasNext());
    assertEquals(str3, multiIterator.next());
    assertFalse.accept(multiIterator.hasNext());
  }

  @Test
  void testEquality() {
    final var list1 = new List(str1);
    list1._addAss(str2);
    final var list2 = new List(str1);
    list2._addAss(str2);
    final var list3 = new List(str1);
    list3._addAss(str3);

    // Equality - all lists are set, so comparisons return Boolean values
    assertTrue.accept(list1._eq(list2));
    assertFalse.accept(list1._eq(list3));
    assertFalse.accept(emptyList._eq(list1)); // Both are set, so comparison works
    assertFalse.accept(list1._eq(emptyList));

    // Inequality
    assertFalse.accept(list1._neq(list2));
    assertTrue.accept(list1._neq(list3));
    assertTrue.accept(emptyList._neq(list1)); // Both are set, so comparison works
    assertTrue.accept(list1._neq(emptyList));

    // Java equals
    assertEquals(list1, list2);
    assertNotEquals(list1, list3);
    assertEquals(list1.hashCode(), list2.hashCode());
  }

  @Test
  void testImmutableAddition() {
    final var list1 = new List(str1);
    final var list2 = new List(str2);
    list2._addAss(str3);

    // Add list to list (immutable)
    final var addedList = list1._add(list2);
    assertEquals(3, addedList._len().state);
    assertEquals(str1, addedList.get(Integer._of(0)));
    assertEquals(str2, addedList.get(Integer._of(1)));
    assertEquals(str3, addedList.get(Integer._of(2)));

    // Original lists unchanged
    assertEquals(1, list1._len().state);
    assertEquals(2, list2._len().state);

    // Add element to list (immutable)
    final var addedElement = list1._add(str2);
    assertEquals(2, addedElement._len().state);
    assertEquals(str1, addedElement.first());
    assertEquals(str2, addedElement.last());

    // Original list unchanged
    assertEquals(1, list1._len().state);

    // Add with empty list 
    final var addedEmptyList = list1._add(emptyList);
    assertEquals(1, addedEmptyList._len().state); // Adding empty list doesn't change size


    final var addedNull = list1._add(null);
    assertSet.accept(addedNull);
    assertEquals(list1, addedNull);
  }

  @Test
  void testImmutableSubtraction() {
    final var list1 = new List(str1);
    list1._addAss(str2);
    list1._addAss(str3);
    list1._addAss(str2); // Duplicate

    final var list2 = new List(str2);

    // Subtract list from list (immutable)
    final var subtractedList = list1._sub(list2);
    assertEquals(2, subtractedList._len().state);
    assertEquals(str1, subtractedList.first());
    assertEquals(str3, subtractedList.last());

    // Original lists unchanged
    assertEquals(4, list1._len().state);
    assertEquals(1, list2._len().state);

    // Subtract element from list (immutable)
    final var subtractedElement = list1._sub(str2);
    assertEquals(3, subtractedElement._len().state); // Removes first occurrence
    assertEquals(str1, subtractedElement.first());
    assertEquals(str3, subtractedElement.get(Integer._of(1)));
    assertEquals(str2, subtractedElement.last()); // Second occurrence remains

    // Original list unchanged
    assertEquals(4, list1._len().state);

    // Subtract with empty list  
    final var subtractedEmptyList = list1._sub(emptyList);
    assertEquals(4, subtractedEmptyList._len().state); // Subtracting empty list doesn't change size

    final var subtractedNull = list1._sub(null);
    assertSet.accept(subtractedNull);
    assertEquals(list1, subtractedNull);
  }

  @Test
  void testMutableAddition() {
    final var list1 = new List(str1);
    final var list2 = new List(str2);
    list2._addAss(str3);

    // Add list to list (mutable)
    final var originalSize = list1._len().state;
    list1._addAss(list2);
    assertEquals(originalSize + 2, list1._len().state);
    assertEquals(str1, list1.get(Integer._of(0)));
    assertEquals(str2, list1.get(Integer._of(1)));
    assertEquals(str3, list1.get(Integer._of(2)));

    // Add element to list (mutable)
    list1._addAss(String._of("fourth"));
    assertEquals(4, list1._len().state);
    assertEquals(String._of("fourth"), list1.last());

    // Add with empty list should do nothing
    final var beforeEmptyAdd = list1._len().state;
    list1._addAss(emptyList);
    assertEquals(beforeEmptyAdd, list1._len().state);

    // Add with null element should do nothing
    final var beforeNullAdd = list1._len().state;
    list1._addAss(null);
    assertEquals(beforeNullAdd, list1._len().state);
  }

  @Test
  void testMutableSubtraction() {
    final var list1 = new List(str1);
    list1._addAss(str2);
    list1._addAss(str3);
    list1._addAss(str2); // Duplicate

    final var list2 = new List(str2);

    // Subtract list from list (mutable)
    list1._subAss(list2);
    assertEquals(2, list1._len().state);
    assertEquals(str1, list1.first());
    assertEquals(str3, list1.last());

    // Add back for next test
    list1._addAss(str2);
    list1._addAss(str2);

    // Subtract element from list (mutable) - removes only first occurrence
    list1._subAss(str2);
    assertEquals(3, list1._len().state); // Removes only first occurrence, not all
    assertEquals(str1, list1.first());
    assertEquals(str2, list1.last()); // Second str2 remains

    // Subtract with empty list should do nothing
    final var beforeEmptySub = list1._len().state;
    list1._subAss(emptyList);
    assertEquals(beforeEmptySub, list1._len().state);

    // Subtract with null element should do nothing
    final var beforeNullSub = list1._len().state;
    list1._subAss(null);
    assertEquals(beforeNullSub, list1._len().state);
  }

  @Test
  void testSpecialOperators() {
    final var testList = new List(str1);
    testList._addAss(str2);
    testList._addAss(str3);

    // Negate operator (reverse) - immutable
    final var negatedList = testList._negate();
    assertEquals(str3, negatedList.first());
    assertEquals(str1, negatedList.last());
    // Original unchanged
    assertEquals(str1, testList.first());

    // Prefix operator (first element)
    assertEquals(str1, testList._prefix());

    // Suffix operator (last element)
    assertEquals(str3, testList._suffix());

    // Empty check
    assertFalse.accept(testList._empty());
    assertTrue.accept(emptyList._empty());

    // Length
    assertEquals(3, testList._len().state);
    assertEquals(0, emptyList._len().state);

    // Contains
    assertTrue.accept(testList._contains(str1));
    assertTrue.accept(testList._contains(str2));
    assertTrue.accept(testList._contains(str3));
    assertFalse.accept(testList._contains(String._of("not-found")));
    assertUnset.accept(testList._contains(null));
  }

  @Test
  void testCopyAndAssignmentOperators() {
    final var list1 = new List(str1);
    list1._addAss(str2);
    final var list2 = new List(str3);

    // Copy operation
    list1._copy(list2);
    assertEquals(1, list1._len().state);
    assertEquals(str3, list1.first());

    // Replace operation (same as copy)
    list1._replace(emptyList);
    assertTrue.accept(list1._empty());

    // Merge operation with unset list
    final var list3 = new List();
    list3._merge(new List(str1));
    assertEquals(1, list3._len().state);
    assertEquals(str1, list3.first());

    // Merge operation with set list
    final var list4 = new List(str1);
    list4._merge(new List(str2));
    assertEquals(2, list4._len().state);

    // Merge single element
    final var list5 = new List();
    list5._merge(str1);
    assertEquals(1, list5._len().state);
    assertEquals(str1, list5.first());

    // Pipe operation (same as merge)
    final var list6 = new List();
    list6._pipe(str1);
    assertEquals(1, list6._len().state);
    assertEquals(str1, list6.first());
  }

  @Test
  void testUtilityMethods() {
    final var testList = new List(str1);
    testList._addAss(str2);

    // Hash code
    assertSet.accept(testList._hashcode());
    assertSet.accept(emptyList._hashcode()); // Empty list is still set

    // String conversion
    assertSet.accept(testList._string());
    final var stringRep = testList.toString();
    assertNotNull(stringRep);
    assertTrue.accept(Boolean._of(stringRep.contains(str1.toString())));
    assertTrue.accept(Boolean._of(stringRep.contains(str2.toString())));

    // Empty list string
    final var emptyString = emptyList.toString();
    assertEquals("[]", emptyString);
  }

  @Test
  void testEdgeCases() {
    // Large list operations
    final var largeList = new List();
    for (int i = 0; i < 1000; i++) {
      largeList._addAss(Integer._of(i));
    }
    assertEquals(1000, largeList._len().state);
    assertEquals(Integer._of(0), largeList.first());
    assertEquals(Integer._of(999), largeList.last());

    // Operations on empty list
    assertTrue.accept(emptyList._empty());
    assertEquals(0, emptyList._len().state);
    assertFalse.accept(emptyList._contains(str1));

    // Mixed type list (all inherit from Any)
    final var mixedList = new List(str1);
    mixedList._addAss(int1);
    mixedList._addAss(Boolean._of(true));
    assertEquals(3, mixedList._len().state);

    // String representation with different types
    final var stringListRep = mixedList.toString();
    assertNotNull(stringListRep);
    assertTrue.accept(Boolean._of(stringListRep.contains("\"" + str1 + "\"")));
  }

  @Test
  void testConstraintValidation() {
    // Test that constraint validation is called during assign operations
    final var javaList = new ArrayList<Any>();
    javaList.add(str2);
    javaList.add(str3);

    // This should work fine as basic lists don't have constraints
    final var list2 = List._of(javaList);
    assertSet.accept(list2);
    assertEquals(2, list2._len().state);
  }
}