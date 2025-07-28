package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JSONAccessTest extends JSONTestBase {

  @Nested
  class ArrayAccessTests {

    @Test
    void testArrayFunctionality() {
      // Test array creation and nature detection

      // Test new JSON().array() factory method
      final var emptyArray = new JSON().array();
      assertSet.accept(emptyArray);
      assertTrue.accept(emptyArray.arrayNature());
      assertFalse.accept(emptyArray.objectNature());
      assertFalse.accept(emptyArray.valueNature());

      // Test empty array properties
      assertEquals(INT_0, emptyArray.arrayLength());
      assertEquals(INT_0, emptyArray._len());
      assertTrue.accept(emptyArray._empty());

      // Test array length operations

      // Create populated array using helper methods  
      var populatedArray = createJsonArray();
      addStringElement(populatedArray, "first");
      add42Element(populatedArray);
      addBooleanElement(populatedArray, true);

      assertSet.accept(populatedArray);
      assertTrue.accept(populatedArray.arrayNature());
      assertEquals(INT_3, populatedArray.arrayLength());
      assertEquals(INT_3, populatedArray._len());
      assertFalse.accept(populatedArray._empty());

      // Test array element access
      final var firstElement = getElement(populatedArray, 0);
      assertSet.accept(firstElement);
      assertElementValue(firstElement, "\"first\"");

      final var secondElement = getElement(populatedArray, 1);
      assertSet.accept(secondElement);
      assertElementValue(secondElement, "42");

      final var thirdElement = getElement(populatedArray, 2);
      assertSet.accept(thirdElement);
      assertElementValue(thirdElement, "true");

      // Test out of bounds access
      final var outOfBounds = getElement(populatedArray, 10);
      assertUnset.accept(outOfBounds);
    }

    @Test
    void testArrayIndexing() {
      // Test various array indexing scenarios
      final var testArray = createJsonArray();
      addStringElement(testArray, "zero");
      addStringElement(testArray, "one");
      addStringElement(testArray, "two");

      // Test valid indices
      assertElementValue(getElement(testArray, 0), "\"zero\"");
      assertElementValue(getElement(testArray, 1), "\"one\"");
      assertElementValue(getElement(testArray, 2), "\"two\"");

      // Test invalid indices
      assertUnset.accept(getElement(testArray, -1));
      assertUnset.accept(getElement(testArray, 3));
      assertUnset.accept(getElement(testArray, 100));

      // Test accessing with unset index
      final var unsetIndex = new Integer();
      final var unsetResult = testArray.get(unsetIndex);
      assertUnset.accept(unsetResult);
    }

    @Test
    void testArrayBoundaryAccess() {
      // Test array access at boundaries
      final var singleElementArray = createJsonArray();
      addStringElement(singleElementArray, "only");

      // Single element array
      assertElementValue(getElement(singleElementArray, 0), "\"only\"");
      assertUnset.accept(getElement(singleElementArray, 1));

      // Empty array access
      final var emptyArray = createJsonArray();
      assertUnset.accept(getElement(emptyArray, 0));
    }

    @Test
    void testArrayOperationsOnUnset() {
      // Array operations on unset JSON objects
      final var unsetJson = new JSON();
      assertUnset.accept(unsetJson.arrayNature());
      assertUnset.accept(unsetJson.arrayLength());
      assertUnset.accept(unsetJson.get(INT_0));

      // Test unset JSON treated as empty when used in array context
      final var emptyTestArray = createJsonArray();

      // Empty array should still be a valid array
      assertTrue.accept(emptyTestArray.arrayNature());
      assertTrue.accept(emptyTestArray._empty());
      assertEquals(INT_0, emptyTestArray._len());
    }
  }

  @Nested
  class ObjectAccessTests {

    @Test
    void testPropertyAccess() {
      // Test object property access
      final var person = createJsonObject();
      addStringProperty(person, "name", "John Doe");
      addIntProperty(person, "age", 30);
      addBooleanProperty(person);

      // Test accessing existing properties
      final var name = getProperty(person, "name");
      assertQuoted("John Doe", name);

      final var age = getProperty(person, "age");
      assertSet.accept(age);
      assertEquals("30", age._string().state);

      final var active = getProperty(person, "active");
      assertSet.accept(active);
      assertEquals("true", active._string().state);

      // Test accessing non-existent property
      final var nonExistent = getProperty(person, "nonexistent");
      assertUnset.accept(nonExistent);
    }

    @Test
    void testNestedObjectAccess() {
      // Test accessing nested object properties
      final var address = createJsonObject();
      addStringProperty(address, "street", "123 Main St");
      addStringProperty(address, "city", "Springfield");

      final var person = createJsonObject();
      addStringProperty(person, "name", "John");
      addProperty(person, "address", address);

      // Access nested properties
      final var personAddress = getProperty(person, "address");
      assertSet.accept(personAddress);
      assertTrue.accept(personAddress.objectNature());

      final var street = getProperty(personAddress, "street");
      assertQuoted("123 Main St", street);

      final var city = getProperty(personAddress, "city");
      assertQuoted("Springfield", city);
    }

    @Test
    void testMissingPropertyAccess() {
      // Test various scenarios of missing property access
      final var obj = createJsonObject();
      assertNotNull(obj);
      addStringProperty(obj, "existing", "value");

      // Test accessing missing key
      assertUnset.accept(getProperty(obj, "missing"));

      // Test accessing with unset key
      final var unsetKey = new String();
      final var unsetResult = obj.get(unsetKey);
      assertUnset.accept(unsetResult);

      // Test accessing property on unset JSON
      final var unsetJson = new JSON();
      assertUnset.accept(getProperty(unsetJson, "any"));
    }
  }

  @Nested
  class PathQueryTests {

    @Test
    void testBasicPathQueries() {
      // Test JSONPath functionality
      final var testData = createComplexTestData();
      assertSet.accept(testData);

      // Test basic property access via path
      final var nameResult = testData.read(new Path(String._of(".name")));
      assertSet.accept(nameResult);
      final var nameValue = nameResult.getOrDefault(new JSON());
      assertQuoted("TechCorp", nameValue);

      // Test numeric property access
      final var foundedResult = testData.read(new Path(String._of(".founded")));
      assertSet.accept(foundedResult);
      final var foundedValue = foundedResult.getOrDefault(new JSON());
      assertEquals("2010", foundedValue._string().state);

      // Test boolean property access
      final var activeResult = testData.read(new Path(String._of(".active")));
      assertSet.accept(activeResult);
      final var activeValue = activeResult.getOrDefault(new JSON());
      assertEquals("true", activeValue._string().state);
    }

    @Test
    void testPathConversionBehavior() {
      // Test path conversion and error handling
      final var testData = createComplexTestData();

      // Test that EK9 .name format works
      final var ek9Path = new Path(String._of(".name"));
      final var result = testData.read(ek9Path);
      assertSet.accept(result);
      final var resultJson = result.getOrDefault(new JSON());
      assertSet.accept(resultJson);
      assertQuoted("TechCorp", resultJson);

      // Test with unset Path - should return unset
      final var unsetPath = new Path();
      final var unsetResult = testData.read(unsetPath);
      assertUnset.accept(unsetResult);

      // Test with unset JSON - should return unset  
      final var unsetJson = new JSON();
      final var validPath = new Path(String._of(".name"));
      final var unsetJsonResult = unsetJson.read(validPath);
      assertUnset.accept(unsetJsonResult);
    }

    @Test
    void testArrayPathQueries() {
      // Test JSONPath with array access
      final var testData = createComplexTestData();

      // Test array access via path
      final var tagsResult = testData.read(new Path(String._of(".tags")));
      assertSet.accept(tagsResult);
      final var tags = tagsResult.getOrDefault(new JSON());
      assertTrue.accept(tags.arrayNature());
      assertEquals(INT_3, tags.arrayLength());

      // Test array element access - first element
      final var firstTagResult = testData.read(new Path(String._of(".tags[0]")));
      assertSet.accept(firstTagResult);
      final var firstTag = firstTagResult.getOrDefault(new JSON());
      assertQuoted("technology", firstTag);

      // Test deeply nested array access within object
      final var employeesResult = testData.read(new Path(String._of(".departments[0].employees")));
      assertSet.accept(employeesResult);
      final var employees = employeesResult.getOrDefault(new JSON());
      assertTrue.accept(employees.arrayNature());
      assertEquals(INT_2, employees.arrayLength());

      // Test deeply nested element access
      final var firstEmployeeResult = testData.read(new Path(String._of(".departments[0].employees[0]")));
      assertSet.accept(firstEmployeeResult);
      final var firstEmployee = firstEmployeeResult.getOrDefault(new JSON());
      assertTrue.accept(firstEmployee.objectNature());
    }

    @Test
    void testObjectArrayQueries() {
      // Test complex object and array combinations
      final var testData = createComplexTestData();

      // Test nested object access via path
      final var companyResult = testData.read(new Path(String._of(".company")));
      assertSet.accept(companyResult);
      final var company = companyResult.getOrDefault(new JSON());
      assertTrue.accept(company.objectNature());

      // Test nested object property
      final var headquartersResult = testData.read(new Path(String._of(".company.headquarters")));
      assertSet.accept(headquartersResult);
      final var headquarters = headquartersResult.getOrDefault(new JSON());
      assertQuoted("San Francisco", headquarters);

      // Test deeply nested object access
      final var contactResult = testData.read(new Path(String._of(".departments[0].manager.contact")));
      assertSet.accept(contactResult);
      final var contact = contactResult.getOrDefault(new JSON());
      assertTrue.accept(contact.objectNature());

      // Test deeply nested value access
      final var emailResult = testData.read(new Path(String._of(".departments[0].manager.contact.email")));
      assertSet.accept(emailResult);
      final var email = emailResult.getOrDefault(new JSON());
      assertQuoted("alice@techcorp.com", email);
    }
  }

  @Nested
  class IteratorTests {

    @Test
    void testArrayIterator() {
      // Test iterator functionality for arrays
      final var testArray = createJsonArray();
      addStringElement(testArray, "first");
      add42Element(testArray);
      addBooleanElement(testArray, true);

      final var iterator = testArray.iterator();
      assertSet.accept(iterator);

      // Test iterator has elements
      assertTrue.accept(iterator.hasNext());

      // Test iteration through array elements
      var count = 0;
      while (iterator.hasNext().state) {
        final var element = iterator.next();
        assertSet.accept(element);
        count++;
      }
      assertEquals(3, count);

      // Test iterator is exhausted
      assertFalse.accept(iterator.hasNext());
    }

    @Test
    void testObjectIterator() {
      // Test iterator functionality for objects
      final var testObject = createJsonObject();
      addStringProperty(testObject, "name", "John");
      addIntProperty(testObject, "age", 30);

      final var iterator = testObject.iterator();
      assertSet.accept(iterator);

      // Test iterator has elements
      assertTrue.accept(iterator.hasNext());

      // Test iteration through object properties
      var count = 0;
      while (iterator.hasNext().state) {
        final var property = iterator.next();
        assertSet.accept(property);
        // Each property should be an object with name/value structure
        assertTrue.accept(property.objectNature());
        count++;
      }
      assertEquals(2, count);
    }

    @Test
    void testValueIterator() {
      // Test iterator functionality for values
      final var valueJson = jsonFromString("single");
      assertNotNull(valueJson);
      final var iterator = valueJson.iterator();
      assertSet.accept(iterator);

      // Value iterator should have one element
      assertTrue.accept(iterator.hasNext());
      final var element = iterator.next();
      assertSet.accept(element);
      assertTrue.accept(element._eq(valueJson));

      // Should be exhausted after one element
      assertFalse.accept(iterator.hasNext());
    }

    @Test
    void testComplexStructureIterator() {
      // Test iterator with complex nested structures
      final var complexData = createComplexTestData();
      assertNotNull(complexData);
      final var iterator = complexData.iterator();
      assertSet.accept(iterator);

      // Should be able to iterate through top-level properties
      assertTrue.accept(iterator.hasNext());

      var propertyCount = 0;
      while (iterator.hasNext().state) {
        final var property = iterator.next();
        assertSet.accept(property);
        propertyCount++;
      }

      // Complex data should have multiple top-level properties
      assertTrue.accept(Boolean._of(propertyCount > 0));

      // Test iterator on unset JSON
      final var unsetJson = new JSON();
      final var unsetIterator = unsetJson.iterator();
      assertUnset.accept(unsetIterator);
    }

    @Test
    void testEmptyIterators() {
      // Test iterators on empty structures

      // Test Empty Array Iterator - no elements to iterate over
      final var emptyArray = createJsonArray();
      assertNotNull(emptyArray);
      final var emptyArrayIterator = emptyArray.iterator();
      assertUnset.accept(emptyArrayIterator); // No elements, so iterator is unset

      // Test Empty Object Iterator - no properties to iterate over
      final var emptyObject = createJsonObject();
      final var emptyObjectIterator = emptyObject.iterator();
      assertUnset.accept(emptyObjectIterator); // No properties, so iterator is unset
    }
  }

  @Nested
  class StateQueryTests {

    @Test
    void testLengthOperator() {
      // Test _len operator across different JSON types and states

      // Test _len operator with array nature
      final var emptyArray = createJsonArray();
      assertTrue.accept(emptyArray.arrayNature());
      assertEquals(INT_0, emptyArray._len()); // Empty array length

      final var populatedArray = createJsonArray();
      addStringElement(populatedArray, "item1");
      add42Element(populatedArray);
      addBooleanElement(populatedArray, true);
      assertTrue.accept(populatedArray.arrayNature());
      assertEquals(INT_3, populatedArray._len()); // Populated array length

      // Test _len operator with object nature
      final var emptyObject = createJsonObject();
      assertTrue.accept(emptyObject.objectNature());
      assertEquals(INT_0, emptyObject._len()); // Empty object length

      final var populatedObject = createJsonObject();
      addStringProperty(populatedObject, "key1", "value1");
      addIntProperty(populatedObject, "key2", 123);
      assertTrue.accept(populatedObject.objectNature());
      assertEquals(INT_2, populatedObject._len()); // Populated object length

      // Test _len operator with value nature
      final var stringValue = jsonFromString("test");
      assertTrue.accept(stringValue.valueNature());
      assertEquals(INT_4, stringValue._len()); // String length without quotes (4 for "test")

      final var intValue = jsonFromInt(42);
      assertTrue.accept(intValue.valueNature());
      assertEquals(INT_1, intValue._len()); // Single value length

      final var boolValue = jsonFromBoolean(true);
      assertTrue.accept(boolValue.valueNature());
      assertEquals(INT_1, boolValue._len()); // Single value length

      // Test _len operator with unset JSON (should return unset)
      final var unsetJson = new JSON();
      assertUnset.accept(unsetJson._len());
    }

    @Test
    void testEmptyOperatorComprehensive() {
      // Test _empty operator with array nature
      final var emptyArray = createJsonArray();
      assertNotNull(emptyArray);
      assertTrue.accept(emptyArray.arrayNature());
      assertTrue.accept(emptyArray._empty()); // Empty array should be empty

      final var nonEmptyArray = createJsonArray();
      addStringElement(nonEmptyArray, "item");
      assertTrue.accept(nonEmptyArray.arrayNature());
      assertFalse.accept(nonEmptyArray._empty()); // Non-empty array should not be empty

      // Test _empty operator with object nature
      final var emptyObject = createJsonObject();
      assertTrue.accept(emptyObject.objectNature());
      assertTrue.accept(emptyObject._empty()); // Empty object should be empty

      final var nonEmptyObject = createJsonObject();
      addStringProperty(nonEmptyObject, "key", "value");
      assertTrue.accept(nonEmptyObject.objectNature());
      assertFalse.accept(nonEmptyObject._empty()); // Non-empty object should not be empty

      // Test _empty operator with value nature - textual nodes
      final var emptyString = jsonFromString("");
      assertTrue.accept(emptyString.valueNature());
      assertTrue.accept(emptyString._empty()); // Empty string should be empty

      final var nonEmptyString = jsonFromString("hello");
      assertTrue.accept(nonEmptyString.valueNature());
      assertFalse.accept(nonEmptyString._empty()); // Non-empty string should not be empty

      // Test _empty operator with value nature - numeric nodes (always false)
      final var zeroInt = jsonFromInt(0);
      assertTrue.accept(zeroInt.valueNature());
      assertFalse.accept(zeroInt._empty()); // Zero integer is not empty

      final var positiveInt = jsonFromInt(42);
      assertTrue.accept(positiveInt.valueNature());
      assertFalse.accept(positiveInt._empty()); // Positive integer is not empty

      // Test _empty operator with unset JSON (should return unset)
      final var unsetJson = new JSON();
      assertUnset.accept(unsetJson._empty());
    }

    @Test
    void testNatureDetection() {
      // Test nature detection methods

      // Array nature
      final var array = createJsonArray();
      assertNotNull(array);
      assertTrue.accept(array.arrayNature());
      assertFalse.accept(array.objectNature());
      assertFalse.accept(array.valueNature());

      // Object nature
      final var object = createJsonObject();
      assertFalse.accept(object.arrayNature());
      assertTrue.accept(object.objectNature());
      assertFalse.accept(object.valueNature());

      // Value nature
      final var value = jsonFromString("test");
      assertFalse.accept(value.arrayNature());
      assertFalse.accept(value.objectNature());
      assertTrue.accept(value.valueNature());

      // Unset nature (all should return unset)
      final var unset = new JSON();
      assertUnset.accept(unset.arrayNature());
      assertUnset.accept(unset.objectNature());
      assertUnset.accept(unset.valueNature());
    }

    @Test
    void testContainsOperator() {
      // Test _contains operator functionality

      // Test contains on arrays
      final var testArray = createJsonArray();
      assertNotNull(testArray);
      addStringElement(testArray, "apple");
      addStringElement(testArray, "banana");
      add42Element(testArray);

      assertTrue.accept(testArray._contains(jsonFromString("apple")));
      assertTrue.accept(testArray._contains(jsonFromString("banana")));
      assertTrue.accept(testArray._contains(jsonFromInt(42)));
      assertFalse.accept(testArray._contains(jsonFromString("orange")));

      // Test contains on objects (checks for key existence)
      final var testObject = createJsonObject();
      addStringProperty(testObject, "name", "John");
      addIntProperty(testObject, "age", 30);

      assertTrue.accept(testObject._contains(jsonFromString("name")));
      assertTrue.accept(testObject._contains(jsonFromString("age")));
      assertFalse.accept(testObject._contains(jsonFromString("city")));

      // Test contains with unset JSON
      final var unsetJson = new JSON();
      assertUnset.accept(unsetJson._contains(jsonFromString("anything")));

      // Test contains with unset argument
      final var validJson = jsonFromString("test");
      assertUnset.accept(validJson._contains(new JSON()));
    }
  }
}