package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JSONOperationsTest extends JSONTestBase {

  @Nested
  class MergeOperationTests {

    @Test
    void testObjectMerge() {
      // Test _merge semantics: only add properties if key is missing

      final var baseObject = createJsonObject();
      baseObject._merge(createNameValuePair("name", "Alice"));
      baseObject._merge(createNameValuePair(30));

      final var newProperties = createJsonObject();
      newProperties._merge(createNameValuePair("name", "Bob")); // Existing key - should be ignored
      newProperties._merge(createNameValuePair("city", "Springfield")); // New key - should be added
      newProperties._merge(createNameValuePair("country", "USA")); // New key - should be added

      // Merge should only add missing keys, ignore existing ones
      baseObject._merge(newProperties);

      // Verify existing property was NOT overwritten
      final var nameAfterMerge = baseObject.get(STR_NAME);
      assertQuoted("Alice", nameAfterMerge); // Should still be Alice, not Bob

      // Verify new properties were added
      final var cityAfterMerge = baseObject.get(String._of("city"));
      assertQuoted("Springfield", cityAfterMerge);

      final var countryAfterMerge = baseObject.get(String._of("country"));
      assertQuoted("USA", countryAfterMerge);

      // Object should now have 4 properties: name, age, city, country
      assertEquals(INT_4, baseObject._len());
    }

    @Test
    void testArrayMerge() {
      // Test array merge semantics
      final var baseArray = createJsonArray();
      addStringElement(baseArray, "first");
      add42Element(baseArray);

      // Create another array to merge
      final var sourceArray = createJsonArray();
      addStringElement(sourceArray, "merged");

      // Merge should add all elements from source array
      baseArray._merge(sourceArray);
      assertEquals(INT_3, baseArray.arrayLength());

      // Verify elements
      assertElementValue(getElement(baseArray, 0), "\"first\"");
      assertElementValue(getElement(baseArray, 1), "42");
      assertElementValue(getElement(baseArray, 2), "\"merged\"");
    }

    @Test
    void testValueMerge() {
      // Test Value + Array merge behavior (should convert to array with value and nested array)
      final var singleValue = jsonFromString("single");
      final var arrayToMerge = createJsonArray();
      addStringElement(arrayToMerge, "from_array");

      singleValue._merge(arrayToMerge);
      assertTrue.accept(singleValue.arrayNature());
      assertEquals(INT_2, singleValue.arrayLength());
      assertElementValue(getElement(singleValue, 0), "\"single\"");
      // Second element is the array itself, so it should be serialized as array
      assertElementValue(getElement(singleValue, 1), "[\"from_array\"]");

      // Test Value + Value merge behavior (should convert to array)
      final var value1 = jsonFromString("first");
      final var value2 = jsonFromString("second");

      value1._merge(value2);
      assertTrue.accept(value1.arrayNature());
      assertEquals(INT_2, value1.arrayLength());
      assertElementValue(getElement(value1, 0), "\"first\"");
      assertElementValue(getElement(value1, 1), "\"second\"");
    }

    @Test
    void testUnsetMerge() {
      // Test _merge on unset JSON (should copy entire structure)
      final var baseObject = createJsonObject();
      addStringProperty(baseObject, "name", "Alice");
      addIntProperty(baseObject, "age", 30);

      final var unsetTarget = new JSON();
      unsetTarget._merge(baseObject);
      assertSet.accept(unsetTarget);
      assertTrue.accept(unsetTarget.objectNature());
      assertEquals(INT_2, unsetTarget._len());

      // Test _merge with empty object (should add nothing)
      final var emptyObject = createJsonObject();
      final var beforeEmptyMerge = baseObject._len();
      baseObject._merge(emptyObject);
      assertEquals(beforeEmptyMerge, baseObject._len()); // Should be unchanged
    }
  }

  @Nested
  class ReplaceOperationTests {

    @Test
    void testObjectReplace() {
      // Test _replace semantics: only update properties if key already exists
      final var baseObject = createJsonObject();
      baseObject._merge(createNameValuePair("name", "Alice"));
      baseObject._merge(createNameValuePair(30));
      baseObject._merge(createNameValuePair("city", "Springfield"));

      final var replaceProperties = createJsonObject();
      replaceProperties._merge(createNameValuePair("name", "Charlie")); // Existing key - should be replaced
      replaceProperties._merge(createNameValuePair(25)); // Existing key - should be replaced
      replaceProperties._merge(createNameValuePair("email", "test@example.com")); // New key - should be ignored

      // Replace should only update existing keys, ignore new ones
      baseObject._replace(replaceProperties);

      // Verify existing properties were updated
      final var nameAfterReplace = baseObject.get(STR_NAME);
      assertQuoted("Charlie", nameAfterReplace); // Should now be Charlie

      final var ageAfterReplace = baseObject.get(String._of("age"));
      assertEquals("25", ageAfterReplace._string().state); // Should now be 25 (number, not quoted)

      // Verify new property was NOT added
      final var emailAfterReplace = baseObject.get(String._of("email"));
      assertUnset.accept(emailAfterReplace); // Should be unset since key didn't exist

      // Object should still have 3 properties (no new ones added)
      assertEquals(INT_3, baseObject._len());
    }

    @Test
    void testArrayReplace() {
      // Test array replace - replaces entire array content
      final var arrayForReplace = createJsonArray();
      addStringElement(arrayForReplace, "original1");
      addStringElement(arrayForReplace, "original2");

      arrayForReplace._replace(jsonFromString("replaced"));
      assertTrue.accept(arrayForReplace.valueNature());
      assertQuoted("replaced", arrayForReplace);
    }

    @Test
    void testUnsetReplace() {
      // Test _replace on unset JSON (should do nothing)
      final var baseObject = createJsonObject();
      addStringProperty(baseObject, "name", "Alice");

      final var unsetTarget2 = new JSON();
      unsetTarget2._replace(baseObject);
      assertUnset.accept(unsetTarget2); // Should remain unset

      // Test _replace with unset JSON arguments (should do nothing)
      final var unsetSource = new JSON();

      // Test object._replace(unset) should do nothing
      final var objectForUnsetReplace = createJsonObject();
      addStringProperty(objectForUnsetReplace, "name", "original");
      addIntProperty(objectForUnsetReplace, "age", 25);
      final var originalLength = objectForUnsetReplace._len();
      final var originalName = objectForUnsetReplace.get(STR_NAME);

      objectForUnsetReplace._replace(unsetSource);
      assertEquals(originalLength, objectForUnsetReplace._len()); // Should be unchanged
      final var nameAfterUnsetReplace = objectForUnsetReplace.get(STR_NAME);
      assertTrue.accept(originalName._eq(nameAfterUnsetReplace)); // Should be unchanged
    }

    @Test
    void testReplaceWithEmpty() {
      // Test _replace with empty object (should change nothing)
      final var baseObject = createJsonObject();
      addStringProperty(baseObject, "name", "Alice");

      final var emptyObject = createJsonObject();
      final var beforeEmptyReplace = baseObject.get(STR_NAME);
      baseObject._replace(emptyObject);
      final var afterEmptyReplace = baseObject.get(STR_NAME);
      assertEquals(beforeEmptyReplace._string().state, afterEmptyReplace._string().state); // Should be unchanged
    }
  }

  @Nested
  class ComparisonOperationTests {

    @Test
    void testEqualityOperators() {
      // Test comprehensive equality operations (_eq, _neq, _cmp, _fuzzy) with various JSON types and Any support

      // Create test values of different types
      final var stringJson1 = jsonFromString("test");
      assertNotNull(stringJson1);
      final var stringJson2 = jsonFromString("test");
      final var stringJson3 = jsonFromString("different");
      final var intJson1 = jsonFromInt(42);
      final var intJson2 = jsonFromInt(42);
      final var intJson3 = jsonFromInt(100);
      final var boolJson1 = jsonFromBoolean(true);
      final var boolJson2 = jsonFromBoolean(true);
      final var boolJson3 = jsonFromBoolean(false);
      final var unsetJson = new JSON();

      // Test _eq operator with same type, same value
      assertTrue.accept(stringJson1._eq(stringJson2));
      assertTrue.accept(intJson1._eq(intJson2));
      assertTrue.accept(boolJson1._eq(boolJson2));

      // Test _eq operator with same type, different value
      assertFalse.accept(stringJson1._eq(stringJson3));
      assertFalse.accept(intJson1._eq(intJson3));
      assertFalse.accept(boolJson1._eq(boolJson3));

      // Test _eq operator with different JSON types (should return false, not unset)
      assertFalse.accept(stringJson1._eq((Any) intJson1)); // Different JSON types return false, not unset
      assertFalse.accept(intJson1._eq((Any) boolJson1));

      // Test _neq operator (opposite of _eq)
      assertFalse.accept(stringJson1._neq(stringJson2));
      assertTrue.accept(stringJson1._neq(stringJson3));

      // Test _eq with unset JSON objects (should return unset)
      assertUnset.accept(unsetJson._eq(stringJson1));
      assertUnset.accept(stringJson1._eq(unsetJson));
      assertUnset.accept(unsetJson._eq(unsetJson));

      // Test _neq with unset JSON objects (should return unset)
      assertUnset.accept(unsetJson._neq(stringJson1));
    }

    @Test
    void testComparisonOperators() {
      // Test _cmp operator for ordering
      final var stringA = jsonFromString("apple");
      final var stringB = jsonFromString("banana");
      final var stringC = jsonFromString("apple");

      // String comparison
      final var cmpResult1 = stringA._cmp(stringB);
      assertSet.accept(cmpResult1);
      assertTrue.accept(cmpResult1._lt(INT_0));  // "apple" < "banana"

      final var cmpResult2 = stringB._cmp(stringA);
      assertSet.accept(cmpResult2);
      assertTrue.accept(cmpResult2._gt(INT_0));  // "banana" > "apple"

      final var cmpResult3 = stringA._cmp(stringC);
      assertSet.accept(cmpResult3);
      assertEquals(INT_0, cmpResult3);  // "apple" == "apple"

      // Test _cmp with unset arguments
      final var unsetJson = new JSON();
      assertUnset.accept(stringA._cmp(unsetJson));
      assertUnset.accept(unsetJson._cmp(stringA));

      // Test _cmp via Any interface
      assertSet.accept(stringA._cmp((Any) jsonFromInt(42)));
    }

    @Test
    void testFuzzyComparison() {
      // Test _fuzzy operator (for JSON, same as regular comparison)
      final var jsonA = jsonFromString("test");
      final var jsonB = jsonFromString("test");
      final var jsonC = jsonFromString("other");

      final var fuzzyResult1 = jsonA._fuzzy(jsonB);
      assertSet.accept(fuzzyResult1);
      assertEquals(INT_0, fuzzyResult1);  // Same values

      final var fuzzyResult2 = jsonA._fuzzy(jsonC);
      assertSet.accept(fuzzyResult2);
      assertFalse.accept(fuzzyResult2._eq(INT_0));  // Different values

      // Test _fuzzy with unset arguments
      final var unsetJson = new JSON();
      assertUnset.accept(jsonA._fuzzy(unsetJson));
    }
  }

  @Nested
  class MutatingOperationTests {

    @Test
    void testPipeOperator() {
      // Test pipe operator (should behave like merge)
      final var baseObject = createJsonObject();
      addStringProperty(baseObject, "name", "Alice");

      final var additionalData = createJsonObject();
      addIntProperty(additionalData, "age", 30);

      baseObject._pipe(additionalData);
      assertEquals(INT_2, baseObject._len());

      final var name = baseObject.get(STR_NAME);
      assertQuoted("Alice", name);

      final var age = baseObject.get(String._of("age"));
      assertEquals("30", age._string().state);
    }

    @Test
    void testCopyOperator() {
      // Test copy operator with valid JSON
      final var sourceJson = jsonFromString("source");
      final var targetJson = jsonFromString("target");

      targetJson._copy(sourceJson);
      assertTrue.accept(targetJson._eq(sourceJson));
      assertQuoted("source", targetJson);

      // Test copy operator with unset parameter
      final var validJson = jsonFromString("valid");
      final var unsetJson = new JSON();

      validJson._copy(unsetJson);
      assertUnset.accept(validJson); // Should become unset when copying unset JSON

      // Test copying arrays
      final var sourceArray = createJsonArray();
      addStringElement(sourceArray, "item1");
      add42Element(sourceArray);

      final var targetArray = jsonFromString("willBeReplaced");
      targetArray._copy(sourceArray);
      assertTrue.accept(targetArray.arrayNature());
      assertEquals(INT_2, targetArray.arrayLength());

      // Test copying objects
      final var sourceObject = createNamedJson("key", jsonFromString("value"));
      final var targetObject = jsonFromInt(123);
      targetObject._copy(sourceObject);
      assertTrue.accept(targetObject.objectNature());
      assertEquals(INT_1, targetObject._len());
    }

    static Stream<JSON> getClearMethodTestCases() {

      return Stream.of(jsonFromString("test"),
          createJsonArray()._add(jsonFromString("item")),
          createNamedJson("key", jsonFromString("value")));
    }

    @ParameterizedTest
    @MethodSource("getClearMethodTestCases")
    void testClearMethodMakesJsonUnset(JSON json) {
      assertNotNull(json);
      assertSet.accept(json); // Verify JSON is initially set

      final var result = json.clear();
      assertUnset.accept(result);
      assertUnset.accept(json); // Original should also be unset
    }
  }

  @Nested
  class AddOperationTests {

    @Test
    void testArrayAddition() {
      // Test adding to arrays
      final var emptyArray = createJsonArray();

      // Test array + value
      final var arrayPlusValue = emptyArray._add(jsonFromString("added"));
      assertSet.accept(arrayPlusValue);
      assertTrue.accept(arrayPlusValue.arrayNature());
      assertEquals(INT_1, arrayPlusValue.arrayLength());

      // Test array + array (combines arrays)
      final var array1 = createJsonArray();
      addStringElement(array1, "item1");
      final var array2 = createJsonArray();
      addStringElement(array2, "item2");

      final var combinedArray = array1._add(array2);
      assertSet.accept(combinedArray);
      assertTrue.accept(combinedArray.arrayNature());
      assertEquals(INT_2, combinedArray.arrayLength());

      // Test value + value (creates array)
      final var value1 = jsonFromString("first");
      final var value2 = jsonFromString("second");
      final var newArray = value1._add(value2);
      assertSet.accept(newArray);
      assertTrue.accept(newArray.arrayNature());
      assertEquals(INT_2, newArray.arrayLength());
    }

    @Test
    void testObjectAddition() {
      // Test adding to objects
      final var person = createJsonObject();
      addStringProperty(person, "name", "John");
      addIntProperty(person, "age", 30);
      addBooleanProperty(person);

      final var address = createJsonObject();
      addStringProperty(address, "street", "123 Main St");
      addStringProperty(address, "city", "Springfield");

      // Test object + object (merges objects)
      final var merged = person._add(address);
      assertSet.accept(merged);
      assertTrue.accept(merged.objectNature());
      assertEquals(INT_5, merged._len()); // 3 from person + 2 from address

      // Verify properties
      assertQuoted("John", merged.get(STR_NAME));
      assertQuoted("123 Main St", merged.get(String._of("street")));

      // Test object + value (creates object with indexed property)
      final var objectPlusValue = person._add(jsonFromString("standalone"));
      assertSet.accept(objectPlusValue);
      assertTrue.accept(objectPlusValue.objectNature());
      assertEquals(INT_4, objectPlusValue._len()); // 3 original + 1 indexed
    }

    @Test
    void testCrossMixAddition() {
      // Test adding different types creates arrays
      final var valueJson = jsonFromString("value");
      final var person = createJsonObject();
      addStringProperty(person, "name", "John");

      // Test value + object conversion to array
      final var valueArray = valueJson._add(person);
      assertSet.accept(valueArray);
      assertTrue.accept(valueArray.arrayNature());
      assertEquals(INT_2, valueArray.arrayLength());

      // Test object + array
      final var testArray = createJsonArray();
      addStringElement(testArray, "arrayItem");

      final var objectPlusArray = person._add(testArray);
      assertSet.accept(objectPlusArray);
      assertTrue.accept(objectPlusArray.objectNature());
    }

    @Test
    void testAddWithUnset() {
      // Test adding with unset arguments
      final var validJson = jsonFromString("valid");
      assertNotNull(validJson);

      final var unsetJson = new JSON();

      final var result = validJson._add(unsetJson);
      assertUnset.accept(result); // Adding unset should return unset

      // Test unset + valid
      final var result2 = unsetJson._add(validJson);
      assertUnset.accept(result2); // Unset + anything should return unset
    }
  }
}