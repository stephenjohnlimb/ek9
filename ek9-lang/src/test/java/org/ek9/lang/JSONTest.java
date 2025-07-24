package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JSONTest extends Common {

  // Common assertion constants
  private static final Integer ZERO = Integer._of(0);
  private static final Integer ONE = Integer._of(1);
  private static final Integer TWO = Integer._of(2);
  private static final Integer THREE = Integer._of(3);
  private static final Integer FOUR = Integer._of(4);
  private static final Integer FIVE = Integer._of(5);
  private static final Integer SIX = Integer._of(6);
  private static final Integer ELEVEN = Integer._of(11);

  // Utility method to check if a string is quoted
  private void assertQuoted(java.lang.String jsonString) {
    assertEquals("\"", jsonString.substring(0, 1)); // Should start with quote
    assertEquals("\"", jsonString.substring(jsonString.length() - 1)); // Should end with quote
  }

  private void assertQuoted(java.lang.String expect, JSON json) {
    assertSet.accept(json);
    var provided = json._string();
    assertQuoted(provided.state);
    var trimmed = provided.trim(Character._of('"')).state;
    assertEquals(expect, trimmed);
  }

  // Object property version of assertQuoted
  private void assertQuoted(JSON obj, java.lang.String key) {
    final var retrieved = getProperty(obj, key);
    assertSet.accept(retrieved);
    assertQuoted(retrieved._string().state);
  }

  // Object property version with expected value
  private void assertJohnDoeQuoted(JSON obj) {
    final var retrieved = getProperty(obj, "name");
    assertQuoted("John Doe", retrieved);
  }
  // ===== HELPER UTILITY METHODS =====

  // Basic JSON creation helpers
  private JSON jsonFromString(java.lang.String value) {
    return new JSON(String._of(value));
  }

  private JSON jsonFromInt(int value) {
    return new JSON(Integer._of(value));
  }

  private JSON jsonFromBoolean(boolean value) {
    return new JSON(Boolean._of(value));
  }

  private JSON jsonFromFloat(double value) {
    return new JSON(Float._of(value));
  }

  private JSON jsonFromChar(char value) {
    return new JSON(Character._of(value));
  }

  // EK9 temporal and complex type JSON creation helpers (with fixed values)
  private JSON jsonFromDate(java.lang.String dateString) {
    return new JSON(new Date(String._of(dateString)));
  }

  private JSON jsonFromDateTime(java.lang.String dateTimeString) {
    return new JSON(new DateTime(String._of(dateTimeString)));
  }

  private JSON jsonFromTime(java.lang.String timeString) {
    return new JSON(new Time(String._of(timeString)));
  }

  private JSON jsonFromDuration(java.lang.String isoString) {
    return new JSON(Duration._of(isoString));
  }

  private JSON jsonFromMillisecond(long value) {
    return new JSON(Millisecond._of(value));
  }

  private JSON jsonFromMoney(java.lang.String moneyString) {
    return new JSON(new Money(String._of(moneyString)));
  }

  // Structure creation helpers
  private JSON createJsonArray() {
    return new JSON().array();
  }

  private JSON createJsonObject() {
    return new JSON().object();
  }

  // Object property helpers
  private void addProperty(JSON obj, java.lang.String key, JSON value) {
    obj._merge(createNamedJson(key, value));
  }

  private JSON getProperty(JSON obj, java.lang.String key) {
    return obj.get(String._of(key));
  }

  // Name-value pair creation helper
  private JSON createNameValuePair(java.lang.String key, java.lang.String value) {
    return createNamedJson(key, jsonFromString(value));
  }

  private JSON createNameValuePair(int value) {
    return createNamedJson("age", jsonFromInt(value));
  }

  // Convenience overloads for common value types
  private void addStringProperty(JSON obj, java.lang.String key, java.lang.String value) {
    addProperty(obj, key, jsonFromString(value));
  }

  private void addIntProperty(JSON obj, java.lang.String key, int value) {
    addProperty(obj, key, jsonFromInt(value));
  }

  private void addBooleanProperty(JSON obj) {
    addProperty(obj, "active", jsonFromBoolean(true));
  }

  private void addFloatProperty(JSON obj, double value) {
    addProperty(obj, "score", jsonFromFloat(value));
  }

  private void addCharProperty(JSON obj) {
    addProperty(obj, "grade", jsonFromChar('A'));
  }

  // Temporal property helpers
  private void addDateProperty(JSON obj, java.lang.String key, java.lang.String dateString) {
    addProperty(obj, key, jsonFromDate(dateString));
  }


  private void addMoneyProperty(JSON obj, java.lang.String moneyString) {
    addProperty(obj, "salary", jsonFromMoney(moneyString));
  }

  // Array element helpers
  private JSON getElement(JSON array, int index) {
    return array.get(Integer._of(index));
  }

  private void addElement(JSON array, JSON value) {
    array._merge(value);
  }

  // Convenience overloads for array elements
  private void addStringElement(JSON array, java.lang.String value) {
    addElement(array, jsonFromString(value));
  }

  private void addIntElement(JSON array, int value) {
    addElement(array, jsonFromInt(value));
  }

  private void addBooleanElement(JSON array, boolean value) {
    addElement(array, jsonFromBoolean(value));
  }

  // Named JSON creation helpers
  private JSON createNamedJson(java.lang.String key, JSON value) {
    return new JSON(String._of(key), value);
  }

  // Element assertion helpers for arrays
  private void assertElementValue(JSON element, java.lang.String expectedValue) {
    assertSet.accept(element);
    assertEquals(expectedValue, element._string().state);
  }

  // Contains assertion helpers
  private void assertContainsKey(JSON obj, java.lang.String key) {
    assertTrue.accept(obj._contains(jsonFromString(key)));
  }

  private void assertNotContainsKey(JSON obj) {
    assertFalse.accept(obj._contains(jsonFromString("nonexistent")));
  }

  @Test
  void testBasicConstruction() {
    // Default constructor creates unset
    final var defaultJson = new JSON();
    assertUnset.accept(defaultJson);

    // String constructor
    final var stringJson = jsonFromString("test");
    assertQuoted(stringJson._string().state);
    assertQuoted("test", stringJson);

    // Integer constructor
    final var intJson = jsonFromInt(123);
    assertSet.accept(intJson);
    assertEquals("123", intJson.toString());

    // Float constructor
    final var floatJson = jsonFromFloat(45.67);
    assertSet.accept(floatJson);
    assertEquals("45.67", floatJson.toString());

    // Boolean constructor
    final var booleanJson = jsonFromBoolean(true);
    assertSet.accept(booleanJson);
    assertEquals("true", booleanJson.toString());

    // Character constructor
    final var charJson = jsonFromChar('A');
    assertQuoted("A", charJson);

    // Date constructor (using fixed date for predictable testing)
    final var dateJson = jsonFromDate("2025-07-24");
    assertSet.accept(dateJson);
    // Date should be serialized as quoted string
    final var dateStr = dateJson._string().state;
    assertQuoted(dateStr);

    // DateTime constructor (using fixed datetime for predictable testing)
    final var dateTimeJson = jsonFromDateTime("2025-07-24T10:30:00Z");
    assertSet.accept(dateTimeJson);
    // DateTime should be serialized as quoted string
    final var dateTimeStr = dateTimeJson._string().state;
    assertQuoted(dateTimeStr);

    // Time constructor (using fixed time for predictable testing)
    final var timeJson = jsonFromTime("09:00:00");
    assertSet.accept(timeJson);
    // Time should be serialized as quoted string
    final var timeStr = timeJson._string().state;
    assertQuoted(timeStr);

    // Millisecond constructor
    final var millisecondJson = jsonFromMillisecond(1000);
    assertSet.accept(millisecondJson);
    // Millisecond should be serialized as quoted string
    final var millisecondStr = millisecondJson._string().state;
    assertQuoted(millisecondStr);

    // Duration constructor  
    final var durationJson = jsonFromDuration("PT1H");
    assertSet.accept(durationJson);
    // Duration should be serialized as quoted string
    final var durationStr = durationJson._string().state;
    assertQuoted(durationStr);

    // Money constructor
    final var moneyJson = jsonFromMoney("100.50#USD");
    assertSet.accept(moneyJson);
    // Money should be serialized as quoted string
    final var moneyStr = moneyJson._string().state;
    assertQuoted(moneyStr);
  }

  @Test
  void testJSONConstructors() {
    // Test JSON copy constructor with deep copy behavior
    final var originalJson = jsonFromString("original");
    assertSet.accept(originalJson);

    // Create copy using JSON constructor
    final var copyJson = new JSON(originalJson);
    assertSet.accept(copyJson);

    // Verify they have equal content
    assertTrue.accept(copyJson._eq(originalJson));
    assertEquals(originalJson._string().state, copyJson._string().state);

    // Test deep copy: modify original and verify copy is unchanged
    originalJson._copy(jsonFromString("modified"));
    assertQuoted("modified", originalJson);
    assertQuoted("original", copyJson); // Should still be original

    // Test with complex JSON object
    final var complexOriginal = createJsonObject();
    addStringProperty(complexOriginal, "name", "test");
    addIntProperty(complexOriginal, "value", 42);

    final var complexCopy = new JSON(complexOriginal);
    assertSet.accept(complexCopy);
    assertTrue.accept(complexCopy._eq(complexOriginal));

    // Modify original object and verify copy is independent
    addStringProperty(complexOriginal, "newField", "newValue");
    assertFalse.accept(complexCopy._eq(complexOriginal)); // Should now be different

    // Test copy constructor with unset JSON
    final var unsetJson = new JSON();
    final var copyOfUnset = new JSON(unsetJson);
    assertUnset.accept(copyOfUnset);

    // Test String name, JSON value constructor with deep copy
    final var valueJson = jsonFromString("testValue");
    final var namedJson = createNamedJson("key", valueJson);
    assertSet.accept(namedJson);
    assertTrue.accept(namedJson.objectNature());

    // Verify the named JSON contains the key
    final var retrievedValue = getProperty(namedJson, "key");
    assertSet.accept(retrievedValue);
    assertTrue.accept(retrievedValue._eq(valueJson));

    // Test deep copy: modify original value and verify named JSON is unchanged
    valueJson._copy(jsonFromString("changedValue"));
    final var retrievedAfterChange = getProperty(namedJson, "key");
    assertQuoted("testValue", retrievedAfterChange); // Should still be original

    // Test name/value constructor with unset parameters
    final var unsetName = new JSON(new String(), jsonFromString("value"));
    assertUnset.accept(unsetName);

    final var unsetValue = createNamedJson("name", new JSON());
    assertUnset.accept(unsetValue);

    final var bothUnset = new JSON(new String(), new JSON());
    assertUnset.accept(bothUnset);
  }

  @Test
  void testConstructorsWithUnsetArguments() {
    // Test basic type constructors with unset arguments
    // Verify all unset JSON objects behave consistently
    final var allUnsetJsons = new JSON[] {
        new JSON(new Boolean()), new JSON(new String()), new JSON(new Integer()), new JSON(new Float()),
        new JSON(new Character()),
        new JSON(new Date()), new JSON(new DateTime()), new JSON(new Time()), new JSON(new Millisecond()),
        new JSON(new Duration()),
        new JSON(new Money()), new JSON(new JSON())
    };
    assertNotNull(allUnsetJsons);

    for (JSON unsetJson : allUnsetJsons) {
      assertUnset.accept(unsetJson);
      // All should be unset
      assertFalse.accept(unsetJson._isSet());

      // All should have unset string representation
      assertUnset.accept(unsetJson._string());

      // All should have unset hash code
      assertUnset.accept(unsetJson._hashcode());

      // All should return unset for nature methods
      assertUnset.accept(unsetJson.arrayNature());
      assertUnset.accept(unsetJson.objectNature());
      assertUnset.accept(unsetJson.valueNature());

      // All should return unset for length operations
      assertUnset.accept(unsetJson._len());
      assertUnset.accept(unsetJson._empty());
    }
  }

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
    assertEquals(ZERO, emptyArray.arrayLength());
    assertEquals(ZERO, emptyArray._len());
    assertTrue.accept(emptyArray._empty());

    // Test array length operations

    // Create populated array using helper methods  
    var populatedArray = createJsonArray();
    addStringElement(populatedArray, "first");
    addIntElement(populatedArray, 42);
    addBooleanElement(populatedArray, true);

    assertSet.accept(populatedArray);
    assertTrue.accept(populatedArray.arrayNature());
    assertEquals(THREE, populatedArray.arrayLength());
    assertEquals(THREE, populatedArray._len());
    assertFalse.accept(populatedArray._empty());

    // Test arrayLength() on non-arrays (should return unset)
    final var stringJson = jsonFromString("not an array");
    assertUnset.accept(stringJson.arrayLength());
    final var objectJson = createJsonObject();
    assertUnset.accept(objectJson.arrayLength());

    // Test array element access

    // Test valid indices using helper methods
    assertElementValue(getElement(populatedArray, 0), "\"first\"");
    assertElementValue(getElement(populatedArray, 1), "42");
    assertElementValue(getElement(populatedArray, 2), "true");

    // Test out-of-bounds indices (negative tests)
    final var outOfBounds = populatedArray.get(FIVE);
    assertUnset.accept(outOfBounds);

    final var negativeIndex = populatedArray.get(Integer._of(-1));
    assertUnset.accept(negativeIndex);

    // Test get with unset index parameter
    final var unsetIndex = populatedArray.get(new Integer());
    assertUnset.accept(unsetIndex);

    // Test get on non-array JSON objects
    final var getFromString = stringJson.get(ZERO);
    assertUnset.accept(getFromString);

    final var getFromObject = objectJson.get(ZERO);
    assertUnset.accept(getFromObject);

    // Test array addition operations

    // Test adding values to arrays using _add (creates new array)
    final var arrayPlusValue = emptyArray._add(jsonFromString("added"));
    assertSet.accept(arrayPlusValue);
    assertTrue.accept(arrayPlusValue.arrayNature());
    assertEquals(ONE, arrayPlusValue.arrayLength());

    // Test adding arrays to arrays
    final var array1 = createJsonArray();
    array1._merge(jsonFromString("a"));
    final var array2 = createJsonArray();
    array2._merge(jsonFromString("b"));

    final var combinedArray = array1._add(array2);
    assertSet.accept(combinedArray);
    assertTrue.accept(combinedArray.arrayNature());
    assertEquals(TWO, combinedArray.arrayLength());

    // Test creating arrays from two values
    final var value1 = jsonFromString("first");
    final var value2 = jsonFromString("second");
    final var newArray = value1._add(value2);
    assertSet.accept(newArray);
    assertTrue.accept(newArray.arrayNature());
    assertEquals(TWO, newArray.arrayLength());

    // Test _add with unset JSON arguments
    final var unsetJson = new JSON();

    // Test array + unset should return unset
    final var arrayPlusUnset = array1._add(unsetJson);
    assertUnset.accept(arrayPlusUnset);

    // Test unset + array should return unset
    final var unsetPlusArray = unsetJson._add(array1);
    assertUnset.accept(unsetPlusArray);

    // Test value + unset should return unset
    final var valuePlusUnset = value1._add(unsetJson);
    assertUnset.accept(valuePlusUnset);

    // Test unset + value should return unset
    final var unsetPlusValue = unsetJson._add(value1);
    assertUnset.accept(unsetPlusValue);

    // Test unset + unset should return unset
    final var unsetPlusUnset = unsetJson._add(new JSON());
    assertUnset.accept(unsetPlusUnset);

    // Test array merging operations

    // Test merging arrays with arrays (should append)
    final var mergeTarget = createJsonArray();
    mergeTarget._merge(jsonFromString("existing"));

    final var mergeSource = createJsonArray();
    mergeSource._merge(jsonFromString("new1"));
    mergeSource._merge(jsonFromString("new2"));

    mergeTarget._merge(mergeSource);
    assertEquals(THREE, mergeTarget.arrayLength());

    // Test merging values into arrays (should add to array)
    final var arrayForMerge = createJsonArray();
    arrayForMerge._merge(jsonFromString("base"));
    arrayForMerge._merge(jsonFromInt(999));
    assertEquals(TWO, arrayForMerge.arrayLength());

    // Test merging arrays into unset JSON
    final var unsetTarget = new JSON();
    unsetTarget._merge(populatedArray);
    assertSet.accept(unsetTarget);
    assertTrue.accept(unsetTarget.arrayNature());
    assertEquals(THREE, unsetTarget.arrayLength());

    // Test contains operation

    // Test contains with elements present in array
    assertTrue.accept(populatedArray._contains(jsonFromString("first")));
    assertTrue.accept(populatedArray._contains(jsonFromInt(42)));
    assertTrue.accept(populatedArray._contains(jsonFromBoolean(true)));

    // Test contains with elements not in array
    assertFalse.accept(populatedArray._contains(jsonFromString("missing")));
    assertFalse.accept(populatedArray._contains(jsonFromInt(999)));

    // Test contains with unset arguments
    assertUnset.accept(populatedArray._contains(new JSON()));

    // Test contains on non-array JSON objects
    assertTrue.accept(stringJson._contains(stringJson)); // Value equals itself
    assertFalse.accept(stringJson._contains(jsonFromString("different")));

    // Test array equality and comparison

    // Test equality with identical arrays
    final var identicalArray = createJsonArray();
    identicalArray._merge(jsonFromString("first"));
    identicalArray._merge(jsonFromInt(42));
    identicalArray._merge(jsonFromBoolean(true));

    assertTrue.accept(populatedArray._eq(identicalArray));
    assertFalse.accept(populatedArray._neq(identicalArray));

    // Test equality with different arrays
    final var differentArray = createJsonArray();
    differentArray._merge(jsonFromString("different"));

    assertFalse.accept(populatedArray._eq(differentArray));
    assertTrue.accept(populatedArray._neq(differentArray));

    // Test comparison (should work via string representation)
    final var cmpResult = populatedArray._cmp(identicalArray);
    assertSet.accept(cmpResult);
    assertEquals(ZERO, cmpResult); // Should be equal

    // Test negative cases - operations on unset JSON

    // Array operations on unset JSON objects
    assertUnset.accept(unsetJson.arrayNature());
    assertUnset.accept(unsetJson.arrayLength());
    assertUnset.accept(unsetJson.get(ZERO));
    assertUnset.accept(unsetJson._contains(jsonFromString("test")));

    // Test empty array edge cases

    final var emptyTestArray = new JSON().array();

    // Empty array should still be a valid array
    assertTrue.accept(emptyTestArray.arrayNature());
    assertTrue.accept(emptyTestArray._empty());
    assertEquals(ZERO, emptyTestArray._len());

    // Contains on empty array should return false
    assertFalse.accept(emptyTestArray._contains(jsonFromString("anything")));

    // Get on empty array should return unset
    assertUnset.accept(emptyTestArray.get(ZERO));
  }

  @Test
  void testObjectCreationAndStructure() {
    // Test object creation and nature detection

    // Test new JSON().object() factory method
    final var emptyObject = new JSON().object();
    assertSet.accept(emptyObject);
    assertTrue.accept(emptyObject.objectNature());
    assertFalse.accept(emptyObject.arrayNature());
    assertFalse.accept(emptyObject.valueNature());

    // Test empty object properties
    assertEquals(ZERO, emptyObject._len());
    assertTrue.accept(emptyObject._empty());

    // Test named constructor for object creation

    // Create objects with basic EK9 types as property values
    final var stringProperty = createNamedJson("name", jsonFromString("John Doe"));
    assertSet.accept(stringProperty);
    assertTrue.accept(stringProperty.objectNature());
    assertEquals(ONE, stringProperty._len());
    assertFalse.accept(stringProperty._empty());

    final var integerProperty = createNamedJson("age", jsonFromInt(30));
    assertSet.accept(integerProperty);
    assertTrue.accept(integerProperty.objectNature());

    final var booleanProperty = createNamedJson("active", jsonFromBoolean(true));
    assertSet.accept(booleanProperty);
    assertTrue.accept(booleanProperty.objectNature());

    final var floatProperty = createNamedJson("score", jsonFromFloat(95.5));
    assertSet.accept(floatProperty);
    assertTrue.accept(floatProperty.objectNature());

    final var characterProperty = createNamedJson("grade", jsonFromChar('A'));
    assertSet.accept(characterProperty);
    assertTrue.accept(characterProperty.objectNature());

    // Test objects with EK9 temporal types (using fixed dates for predictable testing)
    final var dateProperty = createNamedJson("birthDate", jsonFromDate("1990-05-15"));
    assertSet.accept(dateProperty);
    assertTrue.accept(dateProperty.objectNature());

    final var dateTimeProperty = createNamedJson("lastLogin", jsonFromDateTime("2025-07-24T10:30:00Z"));
    assertSet.accept(dateTimeProperty);
    assertTrue.accept(dateTimeProperty.objectNature());

    final var timeProperty = createNamedJson("startTime", jsonFromTime("09:00:00"));
    assertSet.accept(timeProperty);
    assertTrue.accept(timeProperty.objectNature());

    final var durationProperty = createNamedJson("timeout", jsonFromDuration("PT30M"));
    assertSet.accept(durationProperty);
    assertTrue.accept(durationProperty.objectNature());

    final var millisecondProperty = createNamedJson("delay", jsonFromMillisecond(5000));
    assertSet.accept(millisecondProperty);
    assertTrue.accept(millisecondProperty.objectNature());

    final var moneyProperty = createNamedJson("salary", jsonFromMoney("75000.00#USD"));
    assertSet.accept(moneyProperty);
    assertTrue.accept(moneyProperty.objectNature());

    // Test complex nested objects
    final var nestedObject = createNamedJson("address", stringProperty);
    assertSet.accept(nestedObject);
    assertTrue.accept(nestedObject.objectNature());

    // Test building complex object structure by merging properties
    final var complexObject = createJsonObject();
    addStringProperty(complexObject, "name", "John Doe");
    addIntProperty(complexObject, "age", 30);
    addBooleanProperty(complexObject);
    addFloatProperty(complexObject, 95.5);
    addDateProperty(complexObject, "birthDate", "1990-05-15");
    addMoneyProperty(complexObject, "75000.00#USD");

    assertSet.accept(complexObject);
    assertTrue.accept(complexObject.objectNature());
    assertEquals(SIX, complexObject._len());
    assertFalse.accept(complexObject._empty());

    // Verify all properties are accessible using helper method
    assertJohnDoeQuoted(complexObject);
    assertEquals("30", getProperty(complexObject, "age")._string().state);
    assertEquals("true", getProperty(complexObject, "active")._string().state);
    assertEquals("95.5", getProperty(complexObject, "score")._string().state);

    // Date and Money values should be quoted strings in JSON
    assertQuoted(complexObject, "birthDate");
    assertQuoted(complexObject, "salary");

    // Test property access on unset JSON
    final var unsetJson = new JSON();
    assertUnset.accept(unsetJson.get(String._of("anyProperty")));

    // Test property access on non-object JSON
    final var valueJson = jsonFromString("not an object");
    assertUnset.accept(valueJson.get(String._of("property")));

    final var arrayJson = new JSON().array();
    assertUnset.accept(arrayJson.get(String._of("property")));

    // Test property access with unset property name
    assertUnset.accept(complexObject.get(new String()));

    // Test property access with non-existing property
    assertUnset.accept(complexObject.get(String._of("nonExistentProperty")));
  }

  @Test
  void testObjectOperationsAndAccess() {
    // Create test objects for various operations using helpers
    final var person = createJsonObject();
    addStringProperty(person, "name", "Alice");
    addIntProperty(person, "age", 25);
    addBooleanProperty(person);

    final var address = createJsonObject();
    addStringProperty(address, "street", "123 Main St");
    addStringProperty(address, "city", "Springfield");

    // Test object merging operations

    // Test object + object merging (properties should merge)
    final var merged = person._add(address);
    assertSet.accept(merged);
    assertTrue.accept(merged.objectNature());
    assertEquals(FIVE, merged._len()); // 3 from person + 2 from address

    // Verify merged properties are accessible
    assertSet.accept(merged.get(String._of("name")));
    assertSet.accept(merged.get(String._of("street")));
    assertSet.accept(merged.get(String._of("city")));

    // Test merging objects with _merge (should modify original)
    final var target = new JSON().object();
    target._merge(createNamedJson("id", jsonFromInt(1)));
    target._merge(person);

    assertEquals(FOUR, target._len()); // 1 + 3 from person
    assertSet.accept(target.get(String._of("id")));
    assertSet.accept(target.get(String._of("name")));

    // Test property replacement (only replaces existing properties)
    final var override = new JSON().object();
    override._merge(createNamedJson("name", jsonFromString("Bob")));
    target._replace(override);

    final var replacedName = target.get(String._of("name"));
    assertQuoted("Bob", replacedName);

    // Test object + value operations (creates indexed property)
    final var objectPlusValue = person._add(jsonFromString("standalone"));
    assertSet.accept(objectPlusValue);
    assertTrue.accept(objectPlusValue.objectNature());
    assertEquals(FOUR, objectPlusValue._len()); // 3 original + 1 indexed

    // Test contains operation on objects

    // Test contains with property names (should check keys)
    assertContainsKey(person, "name");
    assertContainsKey(person, "age");
    assertNotContainsKey(person);

    // Test contains with unset arguments
    assertUnset.accept(person._contains(new JSON()));

    // Test object equality and comparison

    // Create identical object using helpers
    final var identicalPerson = createJsonObject();
    addStringProperty(identicalPerson, "name", "Alice");
    addIntProperty(identicalPerson, "age", 25);
    addBooleanProperty(identicalPerson);

    assertTrue.accept(person._eq(identicalPerson));
    assertFalse.accept(person._neq(identicalPerson));

    // Test with different objects
    final var differentPerson = createJsonObject();
    addStringProperty(differentPerson, "name", "Charlie");

    assertFalse.accept(person._eq(differentPerson));
    assertTrue.accept(person._neq(differentPerson));

    // Test comparison operations
    final var cmpResult = person._cmp(identicalPerson);
    assertSet.accept(cmpResult);
    assertEquals(ZERO, cmpResult); // Should be equal

    // Test object length and empty operations

    // Test length consistency
    assertEquals(THREE, person._len());
    assertFalse.accept(person._empty());

    final var emptyObj = new JSON().object();
    assertEquals(ZERO, emptyObj._len());
    assertTrue.accept(emptyObj._empty());

    // Test merging into unset JSON
    final var unsetTarget = new JSON();
    unsetTarget._merge(person);
    assertSet.accept(unsetTarget);
    assertTrue.accept(unsetTarget.objectNature());
    assertEquals(THREE, unsetTarget._len());

    // Test object operations on unset JSON
    final var unsetJson = new JSON();
    assertUnset.accept(unsetJson.objectNature());
    assertUnset.accept(unsetJson._len());
    assertUnset.accept(unsetJson._empty());
    assertUnset.accept(unsetJson.get(String._of("property")));
    assertUnset.accept(unsetJson._contains(jsonFromString("key")));

    // Test mixed type operations
    final var testArray = new JSON().array();
    testArray._merge(jsonFromString("item1"));

    final var objectPlusArray = person._add(testArray);
    assertSet.accept(objectPlusArray);
    assertTrue.accept(objectPlusArray.objectNature());

    // Test value + object conversion to array
    final var valueJson = jsonFromString("standalone");
    final var valueArray = valueJson._add(person);
    assertSet.accept(valueArray);
    assertTrue.accept(valueArray.arrayNature());
    assertEquals(TWO, valueArray.arrayLength());
  }

  @Test
  void testObjectStringRepresentation() {
    // Create comprehensive test object with all EK9 types using helpers
    final var comprehensiveObject = createJsonObject();

    // Add various property types with predictable values
    addStringProperty(comprehensiveObject, "name", "Test User");
    addIntProperty(comprehensiveObject, "age", 42);
    addFloatProperty(comprehensiveObject, 98.7);
    addBooleanProperty(comprehensiveObject);
    addCharProperty(comprehensiveObject);
    addMoneyProperty(comprehensiveObject, "85000.00#USD");
    addDateProperty(comprehensiveObject, "startDate", "2025-01-15");

    // Test _string() produces valid JSON structure
    final var jsonString = comprehensiveObject._string();
    assertSet.accept(jsonString);

    // Verify JSON string format
    final var jsonStr = jsonString.state;

    assertEquals("{", jsonStr.substring(0, 1));
    assertEquals("}", jsonStr.substring(jsonStr.length() - 1));

    // Should contain all expected properties as quoted strings
    assertTrue.accept(Boolean._of(jsonStr.contains("\"name\"")));
    assertTrue.accept(Boolean._of(jsonStr.contains("\"Test User\"")));
    assertTrue.accept(Boolean._of(jsonStr.contains("\"age\"")));
    assertTrue.accept(Boolean._of(jsonStr.contains("42")));
    assertTrue.accept(Boolean._of(jsonStr.contains("\"score\"")));
    assertTrue.accept(Boolean._of(jsonStr.contains("98.7")));
    assertTrue.accept(Boolean._of(jsonStr.contains("\"active\"")));
    assertTrue.accept(Boolean._of(jsonStr.contains("true")));
    assertTrue.accept(Boolean._of(jsonStr.contains("\"grade\"")));
    assertTrue.accept(Boolean._of(jsonStr.contains("\"A\"")));

    // Test prettyPrint() produces formatted JSON
    final var prettyJson = comprehensiveObject.prettyPrint();
    assertSet.accept(prettyJson);

    final var expect = """
        {
          "name" : "Test User",
          "age" : 42,
          "score" : 98.7,
          "active" : true,
          "grade" : "A",
          "salary" : "85000.00#USD",
          "startDate" : "2025-01-15"
        }""";
    final var prettyStr = prettyJson.state;
    assertEquals(expect, prettyStr);

    // Pretty printed should also start/end with braces
    assertEquals("{", prettyStr.substring(0, 1));
    assertEquals("}", prettyStr.substring(prettyStr.length() - 1));

    // Pretty printed should contain newlines and indentation
    assertTrue.accept(Boolean._of(prettyStr.contains("\n")));
    assertTrue.accept(Boolean._of(prettyStr.contains("  "))); // Indentation

    // Both should contain the same data
    assertTrue.accept(Boolean._of(prettyStr.contains("\"name\"")));
    assertTrue.accept(Boolean._of(prettyStr.contains("\"Test User\"")));
    assertTrue.accept(Boolean._of(prettyStr.contains("42")));
    assertTrue.accept(Boolean._of(prettyStr.contains("98.7")));

    // Test that compact and pretty print contain same information
    assertTrue.accept(Boolean._of(jsonStr.contains("\"name\"") && prettyStr.contains("\"name\"")));

    // Test parsing the generated JSON back
    final var parsedJson = JSON._of(jsonStr);
    assertSet.accept(parsedJson);
    assertTrue.accept(parsedJson.objectNature());

    // Verify parsed object has same properties
    final var parsedName = parsedJson.get(String._of("name"));
    assertQuoted("Test User", parsedName);

    final var parsedAge = parsedJson.get(String._of("age"));
    assertSet.accept(parsedAge);
    assertEquals("42", parsedAge._string().state);

    // Test nested object string representation
    final var nestedObject = new JSON().object();
    nestedObject._merge(createNamedJson("person", comprehensiveObject));
    nestedObject._merge(createNamedJson("metadata",
        createNamedJson("version", jsonFromString("1.0"))));

    final var nestedJsonString = nestedObject._string();
    assertSet.accept(nestedJsonString);

    final var nestedStr = nestedJsonString.state;

    // Should be properly nested JSON
    assertEquals("{", nestedStr.substring(0, 1));
    assertEquals("}", nestedStr.substring(nestedStr.length() - 1));
    assertTrue.accept(Boolean._of(nestedStr.contains("\"person\"")));
    assertTrue.accept(Boolean._of(nestedStr.contains("\"metadata\"")));

    // Should contain nested structure
    assertTrue.accept(Boolean._of(nestedStr.contains("\"name\""))); // From nested person
    assertTrue.accept(Boolean._of(nestedStr.contains("\"version\""))); // From metadata

    // Test pretty print of nested structure
    final var nestedPretty = nestedObject.prettyPrint();
    assertSet.accept(nestedPretty);

    final var nestedPrettyStr = nestedPretty.state;
    assertTrue.accept(Boolean._of(nestedPrettyStr.contains("\n")));
    assertTrue.accept(Boolean._of(nestedPrettyStr.contains("  ")));

    // Test string representation of empty object
    final var emptyObject = new JSON().object();
    final var emptyJsonString = emptyObject._string();
    assertSet.accept(emptyJsonString);
    assertEquals("{}", emptyJsonString.state);

    final var emptyPretty = emptyObject.prettyPrint();
    assertSet.accept(emptyPretty);
    assertEquals("{ }", emptyPretty.state);

    // Test mixed array and object structure
    final var mixedObject = new JSON().object();
    final var embeddedArray = new JSON().array();
    embeddedArray._merge(jsonFromString("item1"));
    embeddedArray._merge(jsonFromString("item2"));
    embeddedArray._merge(jsonFromInt(123));

    mixedObject._merge(createNamedJson("items", embeddedArray));
    mixedObject._merge(createNamedJson("count", jsonFromInt(3)));

    final var mixedJsonString = mixedObject._string();
    assertSet.accept(mixedJsonString);

    final var mixedStr = mixedJsonString.state;
    assertEquals("{", mixedStr.substring(0, 1));
    assertEquals("}", mixedStr.substring(mixedStr.length() - 1));

    // Should contain array notation
    assertTrue.accept(Boolean._of(mixedStr.contains("\"items\"")));
    assertTrue.accept(Boolean._of(mixedStr.contains("[")));
    assertTrue.accept(Boolean._of(mixedStr.contains("]")));
    assertTrue.accept(Boolean._of(mixedStr.contains("\"item1\"")));
    assertTrue.accept(Boolean._of(mixedStr.contains("\"item2\"")));
    assertTrue.accept(Boolean._of(mixedStr.contains("123")));

    // Test parsing mixed structure back
    final var parsedMixed = JSON._of(mixedStr);
    assertSet.accept(parsedMixed);
    assertTrue.accept(parsedMixed.objectNature());

    final var parsedItems = parsedMixed.get(String._of("items"));
    assertSet.accept(parsedItems);
    assertTrue.accept(parsedItems.arrayNature());
    assertEquals(THREE, parsedItems.arrayLength());

    // Test string representation with unset JSON
    final var unsetJson = new JSON();
    assertUnset.accept(unsetJson._string());
    assertUnset.accept(unsetJson.prettyPrint());

    // Test comprehensive object with all EK9 temporal types
    final var temporalObject = new JSON().object();
    temporalObject._merge(createNamedJson("currentDate", jsonFromDate("2025-07-24")));
    temporalObject._merge(createNamedJson("timestamp", jsonFromDateTime("2025-07-24T12:30:00Z")));
    temporalObject._merge(createNamedJson("startTime", jsonFromTime("12:30:00")));
    temporalObject._merge(createNamedJson("duration", jsonFromDuration("PT2H30M")));
    temporalObject._merge(createNamedJson("delay", jsonFromMillisecond(1500)));

    final var temporalJsonString = temporalObject._string();
    assertSet.accept(temporalJsonString);

    // All temporal values should be quoted strings
    final var expectTemporal = """
        {"currentDate":"2025-07-24","timestamp":"2025-07-24T12:30:00Z","startTime":"12:30:00","duration":"PT2H30M","delay":"1500ms"}""";
    final var temporalStr = temporalJsonString.state;
    assertEquals(expectTemporal, temporalStr);

    // Verify the temporal object contains all expected properties
    assertTrue.accept(Boolean._of(temporalStr.contains("\"currentDate\"")));
    assertTrue.accept(Boolean._of(temporalStr.contains("\"timestamp\"")));
    assertTrue.accept(Boolean._of(temporalStr.contains("\"startTime\"")));
    assertTrue.accept(Boolean._of(temporalStr.contains("\"duration\"")));
    assertTrue.accept(Boolean._of(temporalStr.contains("\"delay\"")));
  }

  @Test
  void testMergeAndReplaceSemantics() {
    // Test new _merge semantics: only add properties if key is missing

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
    final var nameAfterMerge = baseObject.get(String._of("name"));
    assertQuoted("Alice", nameAfterMerge); // Should still be Alice, not Bob

    // Verify new properties were added
    final var cityAfterMerge = baseObject.get(String._of("city"));
    assertQuoted("Springfield", cityAfterMerge);

    final var countryAfterMerge = baseObject.get(String._of("country"));
    assertQuoted("USA", countryAfterMerge);

    // Object should now have 4 properties: name, age, city, country
    assertEquals(FOUR, baseObject._len());

    // Test new _replace semantics: only update properties if key already exists

    final var replaceProperties = createJsonObject();
    replaceProperties._merge(createNameValuePair("name", "Charlie")); // Existing key - should be replaced
    replaceProperties._merge(createNameValuePair(25)); // Existing key - should be replaced
    replaceProperties._merge(createNameValuePair("email", "test@example.com")); // New key - should be ignored

    // Replace should only update existing keys, ignore new ones
    baseObject._replace(replaceProperties);

    // Verify existing properties were updated
    final var nameAfterReplace = baseObject.get(String._of("name"));
    assertQuoted("Charlie", nameAfterReplace); // Should now be Charlie

    final var ageAfterReplace = baseObject.get(String._of("age"));
    assertEquals("25", ageAfterReplace._string().state); // Should now be 25 (number, not quoted)

    // Verify new property was NOT added
    final var emailAfterReplace = baseObject.get(String._of("email"));
    assertUnset.accept(emailAfterReplace); // Should be unset since key didn't exist

    // Object should still have 4 properties (no new ones added)
    assertEquals(FOUR, baseObject._len());

    // Test _merge with empty object (should add nothing)
    final var emptyObject = createJsonObject();
    final var beforeEmptyMerge = baseObject._len();
    baseObject._merge(emptyObject);
    assertEquals(beforeEmptyMerge, baseObject._len()); // Should be unchanged

    // Test _replace with empty object (should change nothing)
    final var beforeEmptyReplace = baseObject.get(String._of("name"));
    baseObject._replace(emptyObject);
    final var afterEmptyReplace = baseObject.get(String._of("name"));
    assertEquals(beforeEmptyReplace._string().state, afterEmptyReplace._string().state); // Should be unchanged

    // Test _merge on unset JSON (should copy entire structure)
    final var unsetTarget = new JSON();
    unsetTarget._merge(baseObject);
    assertSet.accept(unsetTarget);
    assertTrue.accept(unsetTarget.objectNature());
    assertEquals(FOUR, unsetTarget._len());

    // Test _replace on unset JSON (should do nothing)
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
    final var originalName = objectForUnsetReplace.get(String._of("name"));

    objectForUnsetReplace._replace(unsetSource);
    assertEquals(originalLength, objectForUnsetReplace._len()); // Should be unchanged
    final var nameAfterUnsetReplace = objectForUnsetReplace.get(String._of("name"));
    assertTrue.accept(originalName._eq(nameAfterUnsetReplace)); // Should be unchanged

    // Test array._replace(unset) should do nothing
    final var arrayForUnsetReplace = createJsonArray();
    addStringElement(arrayForUnsetReplace, "item1");
    addStringElement(arrayForUnsetReplace, "item2");
    final var originalArrayLength = arrayForUnsetReplace._len();
    final var originalFirstElement = arrayForUnsetReplace.get(ZERO);

    arrayForUnsetReplace._replace(unsetSource);
    assertEquals(originalArrayLength, arrayForUnsetReplace._len()); // Should be unchanged
    final var firstElementAfterUnsetReplace = arrayForUnsetReplace.get(ZERO);
    assertTrue.accept(originalFirstElement._eq(firstElementAfterUnsetReplace)); // Should be unchanged

    // Test value._replace(unset) should do nothing
    final var valueForUnsetReplace = jsonFromString("original value");
    final var originalValueString = valueForUnsetReplace._string();

    valueForUnsetReplace._replace(unsetSource);
    final var valueAfterUnsetReplace = valueForUnsetReplace._string();
    assertTrue.accept(originalValueString._eq(valueAfterUnsetReplace)); // Should be unchanged

    // Test with non-object types (should maintain existing behavior for _replace)
    final var valueJson = jsonFromString("original");
    valueJson._replace(jsonFromString("replaced"));
    assertQuoted("replaced", valueJson); // Non-object should still do full replacement
  }

  @Test
  void testArrayMergeAndReplaceSemantics() {
    // Test comprehensive array merge and replace behavior

    // Test Array + Array merge behavior (should append elements)
    final var baseArray = createJsonArray();
    addStringElement(baseArray, "first");
    addIntElement(baseArray, 42);

    final var mergeArray = createJsonArray();
    addStringElement(mergeArray, "second");
    addBooleanElement(mergeArray, true);

    // Merge should append all elements from source array
    baseArray._merge(mergeArray);
    assertEquals(FOUR, baseArray.arrayLength());

    // Verify all elements are present in order
    assertElementValue(getElement(baseArray, 0), "\"first\"");
    assertElementValue(getElement(baseArray, 1), "42");
    assertElementValue(getElement(baseArray, 2), "\"second\"");
    assertElementValue(getElement(baseArray, 3), "true");

    // Test Array + Value merge behavior (should add single value to array)
    final var valueArray = createJsonArray();
    addStringElement(valueArray, "existing");

    valueArray._merge(jsonFromString("new"));
    assertEquals(TWO, valueArray.arrayLength());
    assertElementValue(getElement(valueArray, 0), "\"existing\"");
    assertElementValue(getElement(valueArray, 1), "\"new\"");

    // Test Value + Array merge behavior (should convert to array with value and nested array)
    final var singleValue = jsonFromString("single");
    final var arrayToMerge = createJsonArray();
    addStringElement(arrayToMerge, "from_array");

    singleValue._merge(arrayToMerge);
    assertTrue.accept(singleValue.arrayNature());
    assertEquals(TWO, singleValue.arrayLength());
    assertElementValue(getElement(singleValue, 0), "\"single\"");
    // Second element is the array itself, so it should be serialized as array
    assertElementValue(getElement(singleValue, 1), "[\"from_array\"]");

    // Test Value + Value merge behavior (should convert to array)
    final var value1 = jsonFromString("first");
    final var value2 = jsonFromString("second");

    value1._merge(value2);
    assertTrue.accept(value1.arrayNature());
    assertEquals(TWO, value1.arrayLength());
    assertElementValue(getElement(value1, 0), "\"first\"");
    assertElementValue(getElement(value1, 1), "\"second\"");

    // Test Array replace behavior (should do full replacement for non-objects)
    final var targetArray = createJsonArray();
    addStringElement(targetArray, "original1");
    addStringElement(targetArray, "original2");
    assertEquals(TWO, targetArray.arrayLength());

    final var replacementArray = createJsonArray();
    addStringElement(replacementArray, "replacement");

    targetArray._replace(replacementArray);
    assertEquals(ONE, targetArray.arrayLength());
    assertElementValue(getElement(targetArray, 0), "\"replacement\"");

    // Test Array replace with Value (should do full replacement)
    final var arrayForReplace = createJsonArray();
    addIntElement(arrayForReplace, 1);
    addIntElement(arrayForReplace, 2);
    addIntElement(arrayForReplace, 3);

    arrayForReplace._replace(jsonFromString("replaced"));
    assertTrue.accept(arrayForReplace.valueNature());
    assertQuoted("replaced", arrayForReplace);

    // Test empty array merge and replace edge cases
    final var emptyArray1 = createJsonArray();
    final var emptyArray2 = createJsonArray();

    emptyArray1._merge(emptyArray2);
    assertEquals(ZERO, emptyArray1.arrayLength()); // Should remain empty

    emptyArray1._replace(emptyArray2);
    assertEquals(ZERO, emptyArray1.arrayLength()); // Should remain empty

    // Test unset array handling
    final var unsetArray = new JSON();
    final var sourceArray = createJsonArray();
    addStringElement(sourceArray, "test");

    // Merge into unset should copy the source array
    unsetArray._merge(sourceArray);
    assertSet.accept(unsetArray);
    assertTrue.accept(unsetArray.arrayNature());
    assertEquals(ONE, unsetArray.arrayLength());

    // Replace on unset should do nothing
    final var unsetArray2 = new JSON();
    unsetArray2._replace(sourceArray);
    assertUnset.accept(unsetArray2); // Should remain unset

    // Test merge with unset source (should do nothing)
    final var targetForUnset = createJsonArray();
    addStringElement(targetForUnset, "keep");
    final var originalLength = targetForUnset.arrayLength();

    targetForUnset._merge(new JSON());
    assertEquals(originalLength, targetForUnset.arrayLength()); // Should be unchanged

    // Test array merge with mixed types
    final var mixedArray = createJsonArray();
    addStringElement(mixedArray, "text");
    addIntElement(mixedArray, 100);
    addBooleanElement(mixedArray, false);

    final var additionalMixed = createJsonArray();
    addFloatElement(mixedArray);

    mixedArray._merge(additionalMixed);
    assertEquals(FOUR, mixedArray.arrayLength());

    // Verify mixed type elements
    assertElementValue(getElement(mixedArray, 0), "\"text\"");
    assertElementValue(getElement(mixedArray, 1), "100");
    assertElementValue(getElement(mixedArray, 2), "false");
    assertElementValue(getElement(mixedArray, 3), "3.14");
  }

  // Helper method for adding float elements (was missing)
  private void addFloatElement(JSON array) {
    addElement(array, jsonFromFloat(3.14));
  }

  @Test
  void testEqualityOperators() {
    // Test comprehensive equality operations (_eq, _neq, _cmp, _fuzzy) with various JSON types and Any support

    // Create test values of different types
    final var stringJson1 = jsonFromString("test");
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

    // Test _eq operator with different types (returns false, not unset)
    assertFalse.accept(stringJson1._eq(intJson1));
    assertFalse.accept(intJson1._eq(boolJson1));
    assertFalse.accept(stringJson1._eq(boolJson1));

    // Test _eq operator with unset values
    assertUnset.accept(stringJson1._eq(unsetJson));
    assertUnset.accept(unsetJson._eq(stringJson1));
    assertUnset.accept(unsetJson._eq(unsetJson));

    // Test _eq operator with Any (polymorphic)
    assertTrue.accept(stringJson1._eq((Any) stringJson2));
    assertFalse.accept(stringJson1._eq((Any) stringJson3));
    assertFalse.accept(stringJson1._eq((Any) intJson1)); // Different JSON types return false, not unset
    assertUnset.accept(stringJson1._eq(String._of("not json"))); // Non-JSON Any

    // Test _neq operator (inverse of _eq)
    assertFalse.accept(stringJson1._neq(stringJson2));
    assertTrue.accept(stringJson1._neq(stringJson3));
    assertTrue.accept(stringJson1._neq(intJson1)); // Different types -> neq is true
    assertUnset.accept(stringJson1._neq(unsetJson)); // Unset propagation

    // Test _cmp operator with same type
    assertEquals(ZERO, stringJson1._cmp(stringJson2));
    assertEquals(ZERO, intJson1._cmp(intJson2));

    // Test _cmp operator ordering (based on string representation comparison)
    final var cmpResult1 = stringJson1._cmp(stringJson3);
    assertSet.accept(cmpResult1);
    // "different" vs "test" - should be non-zero
    assertTrue.accept(Boolean._of(cmpResult1.state != 0));

    final var cmpResult2 = intJson1._cmp(intJson3);
    assertSet.accept(cmpResult2);
    // 42 vs 100 as strings - should be non-zero
    assertTrue.accept(Boolean._of(cmpResult2.state != 0));

    // Test _cmp operator with different types - returns comparison result, not unset
    assertSet.accept(stringJson1._cmp(intJson1));
    assertSet.accept(intJson1._cmp(boolJson1));

    // Test _cmp operator with unset values
    assertUnset.accept(stringJson1._cmp(unsetJson));
    assertUnset.accept(unsetJson._cmp(stringJson1));

    // Test _cmp operator with Any (polymorphic)
    assertEquals(ZERO, stringJson1._cmp((Any) stringJson2));
    assertSet.accept(stringJson1._cmp((Any) intJson1)); // Different JSON types still return comparison result
    assertUnset.accept(stringJson1._cmp(String._of("not json"))); // Non-JSON Any

    // Test _fuzzy operator (should behave same as _cmp for JSON)
    assertEquals(ZERO, stringJson1._fuzzy(stringJson2));
    final var fuzzyResult = stringJson1._fuzzy(stringJson3);
    assertSet.accept(fuzzyResult);
    assertTrue.accept(Boolean._of(fuzzyResult.state != 0));
    assertUnset.accept(stringJson1._fuzzy(unsetJson));
    assertSet.accept(stringJson1._fuzzy(intJson1)); // Different types still return comparison result

    // Test complex JSON equality (arrays and objects)
    final var array1 = createJsonArray();
    addStringElement(array1, "item1");
    addIntElement(array1, 10);

    final var array2 = createJsonArray();
    addStringElement(array2, "item1");
    addIntElement(array2, 10);

    final var array3 = createJsonArray();
    addStringElement(array3, "item1");
    addIntElement(array3, 20); // Different content

    assertTrue.accept(array1._eq(array2));
    assertFalse.accept(array1._neq(array2));
    assertFalse.accept(array1._eq(array3));
    assertTrue.accept(array1._neq(array3));
    assertEquals(ZERO, array1._cmp(array2));

    final var object1 = createJsonObject();
    addStringProperty(object1, "name", "John");
    addIntProperty(object1, "age", 30);

    final var object2 = createJsonObject();
    addStringProperty(object2, "name", "John");
    addIntProperty(object2, "age", 30);

    final var object3 = createJsonObject();
    addStringProperty(object3, "name", "Jane");
    addIntProperty(object3, "age", 30);

    assertTrue.accept(object1._eq(object2));
    assertFalse.accept(object1._eq(object3));
    assertEquals(ZERO, object1._cmp(object2));

    // Test cross-nature equality (should be false/unset)
    assertFalse.accept(array1._eq(object1));
    assertFalse.accept(object1._eq(stringJson1));
    assertFalse.accept(stringJson1._eq(array1));
  }

  @Test
  void testLengthOperator() {
    // Test _len operator with various JSON natures

    // Test with unset JSON
    final var unsetJson = new JSON();
    assertUnset.accept(unsetJson._len());

    // Test with value nature (single values)
    final var stringJson = jsonFromString("hello world");
    assertEquals(ELEVEN, stringJson._len()); // String length without quotes

    final var intJson = jsonFromInt(12345);
    assertEquals(ONE, intJson._len()); // Single value returns 1

    final var boolJson = jsonFromBoolean(true);
    assertEquals(ONE, boolJson._len()); // Single value returns 1

    final var floatJson = jsonFromFloat(3.14);
    assertEquals(ONE, floatJson._len()); // Single value returns 1

    final var charJson = jsonFromChar('X');
    assertEquals(ONE, charJson._len()); // Character text length (1 char, no quotes)

    // Test with temporal values (should be string length)
    final var dateJson = jsonFromDate("2025-01-15");
    assertSet.accept(dateJson._len());
    assertTrue.accept(Boolean._of(dateJson._len().state >= 10)); // Date string should be at least 10 chars (2025-01-15)

    final var dateTimeJson = jsonFromDateTime("2025-01-15T10:30:00Z");
    assertSet.accept(dateTimeJson._len());
    assertTrue.accept(Boolean._of(dateTimeJson._len().state > 15)); // DateTime string should be longer

    final var timeJson = jsonFromTime("14:30:00");
    assertSet.accept(timeJson._len());
    assertTrue.accept(Boolean._of(timeJson._len().state >= 8)); // Time string should be at least 8 chars (14:30:00)

    final var durationJson = jsonFromDuration("PT2H30M");
    assertSet.accept(durationJson._len());
    assertTrue.accept(Boolean._of(durationJson._len().state > 6)); // Duration string length

    final var moneyJson = jsonFromMoney("100.50#USD");
    assertSet.accept(moneyJson._len());
    assertTrue.accept(
        Boolean._of(moneyJson._len().state >= 10)); // Money string should be at least 10 chars (100.50#USD)

    // Test with array nature (element count)
    final var emptyArray = createJsonArray();
    assertEquals(ZERO, emptyArray._len());

    final var singleItemArray = createJsonArray();
    addStringElement(singleItemArray, "item");
    assertEquals(ONE, singleItemArray._len());

    final var multiItemArray = createJsonArray();
    addStringElement(multiItemArray, "first");
    addIntElement(multiItemArray, 42);
    addBooleanElement(multiItemArray, true);
    addFloatElement(multiItemArray);
    assertEquals(FOUR, multiItemArray._len());

    final var nestedArray = createJsonArray();
    addElement(nestedArray, multiItemArray); // Merge adds all 4 elements
    addStringElement(nestedArray, "outer");
    assertEquals(FIVE, nestedArray._len()); // 4 merged elements + 1 string = 5 total

    // Test with object nature (property count)
    final var emptyObject = createJsonObject();
    assertEquals(ZERO, emptyObject._len());

    final var singlePropertyObject = createJsonObject();
    addStringProperty(singlePropertyObject, "name", "John");
    assertEquals(ONE, singlePropertyObject._len());

    final var multiPropertyObject = createJsonObject();
    addStringProperty(multiPropertyObject, "name", "John");
    addIntProperty(multiPropertyObject, "age", 30);
    addBooleanProperty(multiPropertyObject);
    addFloatProperty(multiPropertyObject, 95.5);
    addDateProperty(multiPropertyObject, "birthdate", "1990-01-01");
    assertEquals(FIVE, multiPropertyObject._len());

    final var nestedObject = createJsonObject();
    addStringProperty(nestedObject, "type", "person");
    addProperty(nestedObject, "details", multiPropertyObject); // Add object as property
    addProperty(nestedObject, "scores", multiItemArray); // Add array as property
    assertEquals(THREE, nestedObject._len()); // 3 properties: type + details + scores

    // Test mixed nested structures
    final var complexNested = createJsonObject();
    addProperty(complexNested, "users", multiItemArray);
    addProperty(complexNested, "metadata", nestedObject);
    addStringProperty(complexNested, "version", "1.0");
    assertEquals(THREE, complexNested._len()); // Top-level property count

    // Verify _len is consistent with _empty for collections
    assertTrue.accept(emptyArray._empty());
    assertEquals(ZERO, emptyArray._len());

    assertTrue.accept(emptyObject._empty());
    assertEquals(ZERO, emptyObject._len());

    assertFalse.accept(multiItemArray._empty());
    assertTrue.accept(Boolean._of(multiItemArray._len().state > 0));

    assertFalse.accept(multiPropertyObject._empty());
    assertTrue.accept(Boolean._of(multiPropertyObject._len().state > 0));

    // Test edge cases
    final var arrayWithEmptyString = createJsonArray();
    addStringElement(arrayWithEmptyString, ""); // Empty string element
    assertEquals(ONE, arrayWithEmptyString._len()); // Still 1 element

    final var objectWithEmptyValue = createJsonObject();
    addStringProperty(objectWithEmptyValue, "empty", "");
    assertEquals(ONE, objectWithEmptyValue._len()); // Still 1 property
  }

  @Test
  void testEmptyOperatorComprehensive() {
    // Test _empty operator with unset JSON
    final var unsetJson = new JSON();
    assertUnset.accept(unsetJson._empty());

    // Test _empty operator with array nature
    final var emptyArray = createJsonArray();
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

    final var negativeInt = jsonFromInt(-5);
    assertTrue.accept(negativeInt.valueNature());
    assertFalse.accept(negativeInt._empty()); // Negative integer is not empty

    final var zeroFloat = jsonFromFloat(0.0);
    assertTrue.accept(zeroFloat.valueNature());
    assertFalse.accept(zeroFloat._empty()); // Zero float is not empty

    final var positiveFloat = jsonFromFloat(3.14);
    assertTrue.accept(positiveFloat.valueNature());
    assertFalse.accept(positiveFloat._empty()); // Positive float is not empty

    // Test _empty operator with value nature - boolean nodes (always false)
    final var trueBoolean = jsonFromBoolean(true);
    assertTrue.accept(trueBoolean.valueNature());
    assertFalse.accept(trueBoolean._empty()); // True boolean is not empty

    final var falseBoolean = jsonFromBoolean(false);
    assertTrue.accept(falseBoolean.valueNature());
    assertFalse.accept(falseBoolean._empty()); // False boolean is not empty

    // Test _empty operator with value nature - character nodes (textual)
    final var emptyChar = jsonFromChar('\0'); // Null character
    assertTrue.accept(emptyChar.valueNature());
    // Note: Character creates text node with string representation, so '\0' becomes "\\u0000"
    assertFalse.accept(emptyChar._empty()); // Null character string representation is not empty

    final var regularChar = jsonFromChar('A');
    assertTrue.accept(regularChar.valueNature());
    assertFalse.accept(regularChar._empty()); // Regular character is not empty

    // Test _empty operator with temporal value nodes (all textual, non-empty)
    final var dateJson = jsonFromDate("2025-01-15");
    assertTrue.accept(dateJson.valueNature());
    assertFalse.accept(dateJson._empty()); // Date string is not empty

    final var timeJson = jsonFromTime("14:30:00");
    assertTrue.accept(timeJson.valueNature());
    assertFalse.accept(timeJson._empty()); // Time string is not empty

    final var dateTimeJson = jsonFromDateTime("2025-01-15T10:30:00Z");
    assertTrue.accept(dateTimeJson.valueNature());
    assertFalse.accept(dateTimeJson._empty()); // DateTime string is not empty

    final var durationJson = jsonFromDuration("PT2H30M");
    assertTrue.accept(durationJson.valueNature());
    assertFalse.accept(durationJson._empty()); // Duration string is not empty

    final var millisecondJson = jsonFromMillisecond(1500);
    assertTrue.accept(millisecondJson.valueNature());
    assertFalse.accept(millisecondJson._empty()); // Millisecond string is not empty

    final var moneyJson = jsonFromMoney("100.50#USD");
    assertTrue.accept(moneyJson.valueNature());
    assertFalse.accept(moneyJson._empty()); // Money string is not empty

    // Test _empty operator consistency with _len operator
    // For arrays and objects: empty if length is 0
    assertEquals(emptyArray._empty().state, emptyArray._len()._eq(ZERO).state);
    assertEquals(nonEmptyArray._empty().state, nonEmptyArray._len()._eq(ZERO).state);
    assertEquals(emptyObject._empty().state, emptyObject._len()._eq(ZERO).state);
    assertEquals(nonEmptyObject._empty().state, nonEmptyObject._len()._eq(ZERO).state);

    // For strings: empty if length is 0
    assertEquals(emptyString._empty().state, emptyString._len()._eq(ZERO).state);
    assertEquals(nonEmptyString._empty().state, nonEmptyString._len()._eq(ZERO).state);

    // Test edge cases with nested structures
    final var arrayWithEmptyArray = createJsonArray();
    final var nestedEmptyArray = createJsonArray();
    addElement(arrayWithEmptyArray, nestedEmptyArray); // Merge adds all elements (0 from empty array)
    assertTrue.accept(arrayWithEmptyArray.arrayNature());
    assertTrue.accept(arrayWithEmptyArray._empty()); // Empty array merged into empty array = still empty
    assertEquals(ZERO, arrayWithEmptyArray._len()); // Length is 0 (no elements merged)

    final var objectWithEmptyObject = createJsonObject();
    final var nestedEmptyObject = createJsonObject();
    addProperty(objectWithEmptyObject, "nested", nestedEmptyObject);
    assertTrue.accept(objectWithEmptyObject.objectNature());
    assertFalse.accept(objectWithEmptyObject._empty()); // Has 1 property (even though value is empty)
    assertEquals(ONE, objectWithEmptyObject._len()); // Length is 1

    final var objectWithEmptyString = createJsonObject();
    addStringProperty(objectWithEmptyString, "emptyValue", "");
    assertTrue.accept(objectWithEmptyString.objectNature());
    assertFalse.accept(objectWithEmptyString._empty()); // Has 1 property (even though value is empty string)
    assertEquals(ONE, objectWithEmptyString._len()); // Length is 1
  }

  @Test
  void testHashcodeOperatorComprehensive() {
    // Test _hashcode operator with unset JSON
    final var unsetJson = new JSON();
    assertNotNull(unsetJson);
    assertUnset.accept(unsetJson._hashcode());

    // Test _hashcode operator with array nature
    final var emptyArray = createJsonArray();
    assertTrue.accept(emptyArray.arrayNature());
    final var emptyArrayHash = emptyArray._hashcode();
    assertSet.accept(emptyArrayHash);

    final var nonEmptyArray = createJsonArray();
    addStringElement(nonEmptyArray, "item");
    addIntElement(nonEmptyArray, 42);
    assertTrue.accept(nonEmptyArray.arrayNature());
    final var nonEmptyArrayHash = nonEmptyArray._hashcode();
    assertSet.accept(nonEmptyArrayHash);

    // Different arrays should have different hash codes
    assertFalse.accept(emptyArrayHash._eq(nonEmptyArrayHash));

    // Test _hashcode operator with object nature
    final var emptyObject = createJsonObject();
    assertTrue.accept(emptyObject.objectNature());
    final var emptyObjectHash = emptyObject._hashcode();
    assertSet.accept(emptyObjectHash);

    final var nonEmptyObject = createJsonObject();
    addStringProperty(nonEmptyObject, "key", "value");
    addIntProperty(nonEmptyObject, "number", 123);
    assertTrue.accept(nonEmptyObject.objectNature());
    final var nonEmptyObjectHash = nonEmptyObject._hashcode();
    assertSet.accept(nonEmptyObjectHash);

    // Different objects should have different hash codes
    assertFalse.accept(emptyObjectHash._eq(nonEmptyObjectHash));

    // Test _hashcode operator with value nature - strings
    final var stringJson1 = jsonFromString("hello");
    assertTrue.accept(stringJson1.valueNature());
    final var stringHash1 = stringJson1._hashcode();
    assertSet.accept(stringHash1);

    final var stringJson2 = jsonFromString("hello"); // Same content
    assertTrue.accept(stringJson2.valueNature());
    final var stringHash2 = stringJson2._hashcode();
    assertSet.accept(stringHash2);

    final var stringJson3 = jsonFromString("world"); // Different content
    assertTrue.accept(stringJson3.valueNature());
    final var stringHash3 = stringJson3._hashcode();
    assertSet.accept(stringHash3);

    // Same strings should have same hash codes
    assertTrue.accept(stringHash1._eq(stringHash2));
    // Different strings should have different hash codes
    assertFalse.accept(stringHash1._eq(stringHash3));

    // Test empty string
    final var emptyString = jsonFromString("");
    assertTrue.accept(emptyString.valueNature());
    final var emptyStringHash = emptyString._hashcode();
    assertSet.accept(emptyStringHash);
    assertFalse.accept(emptyStringHash._eq(stringHash1));

    // Test _hashcode operator with value nature - integers
    final var intJson1 = jsonFromInt(42);
    assertTrue.accept(intJson1.valueNature());
    final var intHash1 = intJson1._hashcode();
    assertSet.accept(intHash1);

    final var intJson2 = jsonFromInt(42); // Same value
    assertTrue.accept(intJson2.valueNature());
    final var intHash2 = intJson2._hashcode();
    assertSet.accept(intHash2);

    final var intJson3 = jsonFromInt(100); // Different value
    assertTrue.accept(intJson3.valueNature());
    final var intHash3 = intJson3._hashcode();
    assertSet.accept(intHash3);

    // Same integers should have same hash codes
    assertTrue.accept(intHash1._eq(intHash2));
    // Different integers should have different hash codes
    assertFalse.accept(intHash1._eq(intHash3));

    // Test special integer values
    final var zeroInt = jsonFromInt(0);
    final var zeroIntHash = zeroInt._hashcode();
    assertSet.accept(zeroIntHash);

    final var negativeInt = jsonFromInt(-42);
    final var negativeIntHash = negativeInt._hashcode();
    assertSet.accept(negativeIntHash);
    assertFalse.accept(intHash1._eq(negativeIntHash));

    // Test _hashcode operator with value nature - floats
    final var floatJson1 = jsonFromFloat(3.14);
    assertTrue.accept(floatJson1.valueNature());
    final var floatHash1 = floatJson1._hashcode();
    assertSet.accept(floatHash1);

    final var floatJson2 = jsonFromFloat(3.14); // Same value
    assertTrue.accept(floatJson2.valueNature());
    final var floatHash2 = floatJson2._hashcode();
    assertSet.accept(floatHash2);

    final var floatJson3 = jsonFromFloat(2.71); // Different value
    assertTrue.accept(floatJson3.valueNature());
    final var floatHash3 = floatJson3._hashcode();
    assertSet.accept(floatHash3);

    // Same floats should have same hash codes
    assertTrue.accept(floatHash1._eq(floatHash2));
    // Different floats should have different hash codes
    assertFalse.accept(floatHash1._eq(floatHash3));

    // Test zero float
    final var zeroFloat = jsonFromFloat(0.0);
    final var zeroFloatHash = zeroFloat._hashcode();
    assertSet.accept(zeroFloatHash);

    // Test _hashcode operator with value nature - booleans
    final var trueJson1 = jsonFromBoolean(true);
    assertTrue.accept(trueJson1.valueNature());
    final var trueHash1 = trueJson1._hashcode();
    assertSet.accept(trueHash1);

    final var trueJson2 = jsonFromBoolean(true); // Same value
    assertTrue.accept(trueJson2.valueNature());
    final var trueHash2 = trueJson2._hashcode();
    assertSet.accept(trueHash2);

    final var falseJson = jsonFromBoolean(false);
    assertTrue.accept(falseJson.valueNature());
    final var falseHash = falseJson._hashcode();
    assertSet.accept(falseHash);

    // Same booleans should have same hash codes
    assertTrue.accept(trueHash1._eq(trueHash2));
    // Different booleans should have different hash codes
    assertFalse.accept(trueHash1._eq(falseHash));

    // Test _hashcode operator with value nature - characters
    final var charJson1 = jsonFromChar('A');
    assertTrue.accept(charJson1.valueNature());
    final var charHash1 = charJson1._hashcode();
    assertSet.accept(charHash1);

    final var charJson2 = jsonFromChar('A'); // Same character
    assertTrue.accept(charJson2.valueNature());
    final var charHash2 = charJson2._hashcode();
    assertSet.accept(charHash2);

    final var charJson3 = jsonFromChar('B'); // Different character
    assertTrue.accept(charJson3.valueNature());
    final var charHash3 = charJson3._hashcode();
    assertSet.accept(charHash3);

    // Same characters should have same hash codes
    assertTrue.accept(charHash1._eq(charHash2));
    // Different characters should have different hash codes
    assertFalse.accept(charHash1._eq(charHash3));

    // Test _hashcode operator with temporal value nodes
    final var dateJson1 = jsonFromDate("2025-01-15");
    assertTrue.accept(dateJson1.valueNature());
    final var dateHash1 = dateJson1._hashcode();
    assertSet.accept(dateHash1);

    final var dateJson2 = jsonFromDate("2025-01-15"); // Same date
    assertTrue.accept(dateJson2.valueNature());
    final var dateHash2 = dateJson2._hashcode();
    assertSet.accept(dateHash2);

    final var dateJson3 = jsonFromDate("2025-01-16"); // Different date
    assertTrue.accept(dateJson3.valueNature());
    final var dateHash3 = dateJson3._hashcode();
    assertSet.accept(dateHash3);

    // Same dates should have same hash codes
    assertTrue.accept(dateHash1._eq(dateHash2));
    // Different dates should have different hash codes
    assertFalse.accept(dateHash1._eq(dateHash3));

    // Test other temporal types
    final var timeJson = jsonFromTime("14:30:00");
    final var timeHash = timeJson._hashcode();
    assertSet.accept(timeHash);

    final var dateTimeJson = jsonFromDateTime("2025-01-15T10:30:00Z");
    final var dateTimeHash = dateTimeJson._hashcode();
    assertSet.accept(dateTimeHash);

    final var durationJson = jsonFromDuration("PT2H30M");
    final var durationHash = durationJson._hashcode();
    assertSet.accept(durationHash);

    final var millisecondJson = jsonFromMillisecond(1500);
    final var millisecondHash = millisecondJson._hashcode();
    assertSet.accept(millisecondHash);

    final var moneyJson = jsonFromMoney("100.50#USD");
    final var moneyHash = moneyJson._hashcode();
    assertSet.accept(moneyHash);

    // All temporal hashes should be different (different content)
    assertFalse.accept(dateHash1._eq(timeHash));
    assertFalse.accept(timeHash._eq(dateTimeHash));
    assertFalse.accept(dateTimeHash._eq(durationHash));
    assertFalse.accept(durationHash._eq(millisecondHash));
    assertFalse.accept(millisecondHash._eq(moneyHash));

    // Test hash code consistency across different JSON natures
    // Different natures with same conceptual content should have different hashes
    assertFalse.accept(stringHash1._eq(intHash1)); // "hello" vs 42
    assertFalse.accept(intHash1._eq(floatHash1)); // 42 vs 3.14
    assertFalse.accept(emptyArrayHash._eq(emptyObjectHash));

    // Test complex nested structures
    final var complexArray = createJsonArray();
    addStringElement(complexArray, "nested");
    addElement(complexArray, nonEmptyObject); // Add object to array
    final var complexArrayHash = complexArray._hashcode();
    assertSet.accept(complexArrayHash);

    final var complexObject = createJsonObject();
    addStringProperty(complexObject, "name", "test");
    addProperty(complexObject, "items", nonEmptyArray); // Add array to object
    final var complexObjectHash = complexObject._hashcode();
    assertSet.accept(complexObjectHash);

    // Complex structures should have different hashes
    assertFalse.accept(complexArrayHash._eq(complexObjectHash));
    assertFalse.accept(complexArrayHash._eq(nonEmptyArrayHash)); // Different content
    assertFalse.accept(complexObjectHash._eq(nonEmptyObjectHash)); // Different content

    // Test hash code stability (same content should always produce same hash)
    final var duplicateString = jsonFromString("hello");
    assertTrue.accept(duplicateString._hashcode()._eq(stringHash1));

    final var duplicateArray = createJsonArray();
    addStringElement(duplicateArray, "item");
    addIntElement(duplicateArray, 42);
    assertTrue.accept(duplicateArray._hashcode()._eq(nonEmptyArrayHash));
  }

  @Test
  void testPipeOperator() {
    // Test pipe with array nature (delegates to _merge)
    var arrayTarget = createJsonArray();
    addStringElement(arrayTarget, "Hello");
    final var arrayToAdd = createJsonArray();
    addStringElement(arrayToAdd, "World");

    arrayTarget._pipe(arrayToAdd);
    assertEquals(TWO, arrayTarget._len());

    // Test pipe with object nature (delegates to _merge)
    var objTarget = createJsonObject();
    addStringProperty(objTarget, "name", "John");
    final var objToAdd = createJsonObject();
    addIntProperty(objToAdd, "age", 30);

    objTarget._pipe(objToAdd);
    assertEquals(TWO, objTarget._len());
    assertQuoted("John", objTarget.get(String._of("name")));

    // Test pipe with value nature (delegates to _merge)
    var valueTarget = jsonFromString("Hello");
    valueTarget._pipe(jsonFromString("World"));
    assertEquals(TWO, valueTarget._len()); // Becomes array with 2 elements

    // Test pipe with unset target (delegates to _merge)
    var unsetTarget = new JSON();
    unsetTarget._pipe(jsonFromString("Test"));
    assertQuoted("Test", unsetTarget);
  }

  @Test
  void testCopyWithUnsetParameter() {
    // Test _copy with unset JSON parameter should make target unset
    var setJson = jsonFromString("original");
    assertSet.accept(setJson);
    assertQuoted("original", setJson);

    // Copy unset JSON should make target unset
    final var unsetSource = new JSON();
    setJson._copy(unsetSource);
    assertUnset.accept(setJson);

    // Test _copy with null parameter should make target unset
    var anotherSetJson = jsonFromInt(42);
    assertSet.accept(anotherSetJson);
    assertEquals("42", anotherSetJson._string().state);

    anotherSetJson._copy(null);
    assertUnset.accept(anotherSetJson);

    // Test _copy from unset to unset should remain unset
    var unsetTarget = new JSON();
    unsetTarget._copy(unsetSource);
    assertUnset.accept(unsetTarget);
  }

  @Test
  void testClearMethod() {
    // Test clear() with array nature
    final var array = createJsonArray();
    addStringElement(array, "item1");
    addIntElement(array, 42);
    assertSet.accept(array);
    assertTrue.accept(array.arrayNature());
    assertEquals(TWO, array._len());

    final var clearedArray = array.clear();
    assertUnset.accept(clearedArray);
    assertUnset.accept(array); // clear() modifies the original

    // Test clear() with object nature
    final var obj = createJsonObject();
    addStringProperty(obj, "name", "John");
    addIntProperty(obj, "age", 30);
    assertSet.accept(obj);
    assertTrue.accept(obj.objectNature());
    assertEquals(TWO, obj._len());

    final var clearedObj = obj.clear();
    assertUnset.accept(clearedObj);
    assertUnset.accept(obj);

    // Test clear() with value nature
    final var value = jsonFromString("test");
    assertSet.accept(value);
    assertTrue.accept(value.valueNature());
    assertQuoted("test", value);

    final var clearedValue = value.clear();
    assertUnset.accept(clearedValue);
    assertUnset.accept(value);

    // Test clear() with already unset JSON
    final var unsetJson = new JSON();
    assertUnset.accept(unsetJson);

    final var clearedUnset = unsetJson.clear();
    assertUnset.accept(clearedUnset);
    assertUnset.accept(unsetJson);
  }

  @Test
  void testPromoteOperator() {
    // Test _promote with array nature (delegates to _string)
    final var array = createJsonArray();
    assertNotNull(array);
    addStringElement(array, "item");
    final var arrayPromoted = array._promote();
    final var arrayString = array._string();
    assertTrue.accept(arrayPromoted._eq(arrayString));

    // Test _promote with object nature (delegates to _string)
    final var obj = createJsonObject();
    addStringProperty(obj, "key", "value");
    final var objPromoted = obj._promote();
    final var objString = obj._string();
    assertTrue.accept(objPromoted._eq(objString));

    // Test _promote with value nature (delegates to _string)
    final var value = jsonFromString("test");
    final var valuePromoted = value._promote();
    final var valueString = value._string();
    assertTrue.accept(valuePromoted._eq(valueString));

    // Test _promote with unset JSON (delegates to _string)
    final var unsetJson = new JSON();
    final var unsetPromoted = unsetJson._promote();
    final var unsetString = unsetJson._string();
    assertUnset.accept(unsetPromoted);
    assertUnset.accept(unsetString);
  }

  @Test
  void testFactoryMethodOfString() {
    // Test _of(String) factory method with valid EK9 String
    final var validString = String._of("hello world");
    final var jsonFromFactory = JSON._of(validString);
    assertSet.accept(jsonFromFactory);
    assertTrue.accept(jsonFromFactory.valueNature());
    assertQuoted("hello world", jsonFromFactory);

    // Test _of(String) factory method with empty EK9 String
    final var emptyString = String._of("");
    final var jsonFromEmptyString = JSON._of(emptyString);
    assertSet.accept(jsonFromEmptyString);
    assertTrue.accept(jsonFromEmptyString.valueNature());
    assertQuoted("", jsonFromEmptyString);

    // Test _of(String) factory method with unset EK9 String
    final var unsetString = new String();
    final var jsonFromUnsetString = JSON._of(unsetString);
    assertUnset.accept(jsonFromUnsetString);

    // Test _of(String) factory method with null EK9 String
    final var jsonFromNullString = JSON._of((String) null);
    assertUnset.accept(jsonFromNullString);

    // Test _of(String) with special characters
    final var specialString = String._of("Special: ñáéíóú & 日本語");
    final var jsonFromSpecial = JSON._of(specialString);
    assertSet.accept(jsonFromSpecial);
    assertTrue.accept(jsonFromSpecial.valueNature());
    assertQuoted("Special: ñáéíóú & 日本語", jsonFromSpecial);

    // Test _of(String) with numeric string
    final var numericString = String._of("12345");
    final var jsonFromNumeric = JSON._of(numericString);
    assertSet.accept(jsonFromNumeric);
    assertTrue.accept(jsonFromNumeric.valueNature());
    assertQuoted("12345", jsonFromNumeric);

    // Verify _of(String) is equivalent to constructor
    final var directConstructor = new JSON(validString);
    final var factoryResult = JSON._of(validString);
    assertTrue.accept(directConstructor._eq(factoryResult));
  }

  @Test
  void testFactoryMethodOfJavaString() {
    // Test _of(java.lang.String) factory method with valid JSON object
    final var validObjectJson = "{\"name\":\"John\",\"age\":30,\"active\":true}";
    final var jsonFromValidObject = JSON._of(validObjectJson);
    assertSet.accept(jsonFromValidObject);
    assertTrue.accept(jsonFromValidObject.objectNature());
    assertEquals(THREE, jsonFromValidObject._len());
    assertQuoted("John", jsonFromValidObject.get(String._of("name")));

    // Test _of(java.lang.String) factory method with valid JSON array
    final var validArrayJson = "[1,2,3,\"test\",true]";
    final var jsonFromValidArray = JSON._of(validArrayJson);
    assertSet.accept(jsonFromValidArray);
    assertTrue.accept(jsonFromValidArray.arrayNature());
    assertEquals(FIVE, jsonFromValidArray._len());
    assertEquals("1", jsonFromValidArray.get(ZERO)._string().state);
    assertQuoted("test", jsonFromValidArray.get(THREE));

    // Test _of(java.lang.String) factory method with simple string value
    final var simpleString = "\"hello world\"";
    final var jsonFromSimpleString = JSON._of(simpleString);
    assertSet.accept(jsonFromSimpleString);
    assertTrue.accept(jsonFromSimpleString.valueNature());
    assertQuoted("hello world", jsonFromSimpleString);

    // Test _of(java.lang.String) factory method with number
    final var numberJson = "42";
    final var jsonFromNumber = JSON._of(numberJson);
    assertSet.accept(jsonFromNumber);
    assertTrue.accept(jsonFromNumber.valueNature());
    assertEquals("42", jsonFromNumber._string().state);

    // Test _of(java.lang.String) factory method with boolean
    final var booleanJson = "true";
    final var jsonFromBoolean = JSON._of(booleanJson);
    assertSet.accept(jsonFromBoolean);
    assertTrue.accept(jsonFromBoolean.valueNature());
    assertEquals("true", jsonFromBoolean._string().state);

    // Test _of(java.lang.String) factory method with null
    final var nullJson = "null";
    final var jsonFromNull = JSON._of(nullJson);
    assertSet.accept(jsonFromNull);
    assertTrue.accept(jsonFromNull.valueNature());
    assertEquals("null", jsonFromNull._string().state);

    // Test _of(java.lang.String) factory method with invalid JSON
    final var invalidJson = "{invalid json}";
    final var jsonFromInvalid = JSON._of(invalidJson);
    assertUnset.accept(jsonFromInvalid);

    // Test _of(java.lang.String) factory method with malformed object
    final var malformedObject = "{\"key\":}";
    final var jsonFromMalformed = JSON._of(malformedObject);
    assertUnset.accept(jsonFromMalformed);

    // Test _of(java.lang.String) factory method with empty string
    final var emptyString = "";
    final var jsonFromEmpty = JSON._of(emptyString);
    assertSet.accept(jsonFromEmpty);

    // Test _of(java.lang.String) factory method with null parameter
    final var jsonFromNullParam = JSON._of((java.lang.String) null);
    assertUnset.accept(jsonFromNullParam);

    // Test _of(java.lang.String) with nested structure
    final var nestedJson = "{\"user\":{\"name\":\"Alice\",\"scores\":[95,87,92]},\"active\":true}";
    final var jsonFromNested = JSON._of(nestedJson);
    assertSet.accept(jsonFromNested);
    assertTrue.accept(jsonFromNested.objectNature());
    final var userObject = jsonFromNested.get(String._of("user"));
    assertTrue.accept(userObject.objectNature());
    assertQuoted("Alice", userObject.get(String._of("name")));
  }

  // ===== PATH TESTING METHODS =====

  // Enhanced test data structure for comprehensive Path testing
  private JSON createComplexTestData() {
    // Create the comprehensive test JSON structure
    final var jsonString = """
        {
          "name": "TechCorp",
          "founded": 2010,
          "active": true,
          "tags": ["technology", "innovation", "global"],
          "company": {
            "name": "TechCorp Industries",
            "headquarters": "San Francisco",
            "revenue": 50000000.75
          },
          "departments": [
            {
              "id": 1,
              "name": "Engineering",
              "budget": 2500000,
              "manager": {
                "name": "Alice Johnson",
                "contact": {
                  "email": "alice@techcorp.com",
                  "phone": "+1-555-0101"
                }
              },
              "employees": [
                {
                  "name": "Bob Smith",
                  "role": "Senior Developer",
                  "skills": ["Java", "Python", "JavaScript"],
                  "salary": 95000
                },
                {
                  "name": "Carol Davis",
                  "role": "DevOps Engineer",
                  "skills": ["Docker", "Kubernetes", "AWS"],
                  "salary": 88000
                }
              ]
            },
            {
              "id": 2,
              "name": "Marketing",
              "budget": 800000,
              "manager": {
                "name": "David Wilson",
                "contact": {
                  "email": "david@techcorp.com",
                  "phone": "+1-555-0102"
                }
              },
              "employees": [
                {
                  "name": "Eva Brown",
                  "role": "Marketing Manager", 
                  "skills": ["SEO", "Content Marketing", "Analytics"],
                  "salary": 72000
                }
              ]
            }
          ]
        }""";

    return JSON._of(jsonString);
  }

  @Test
  void testBasicPathQueries() {
    final var testData = createComplexTestData();
    assertSet.accept(testData);

    // Test simple property access
    final var simplePath = new Path(String._of(".name"));
    final var simpleResult = testData.read(simplePath);
    assertSet.accept(simpleResult);
    final var actualResult = simpleResult.getOrDefault(new JSON());
    assertSet.accept(actualResult);
    assertTrue.accept(actualResult.valueNature());
    assertQuoted("TechCorp", actualResult);

    // Test nested property access  
    final var companyNamePath = new Path(String._of(".company.name"));
    final var companyNameResult = testData.read(companyNamePath);
    assertSet.accept(companyNameResult);
    final var companyNameJson = companyNameResult.getOrDefault(new JSON());
    assertTrue.accept(companyNameJson.valueNature());
    assertQuoted("TechCorp Industries", companyNameJson);

    // Test numeric property
    final var foundedPath = new Path(String._of(".founded"));
    final var foundedResult = testData.read(foundedPath);
    assertSet.accept(foundedResult);
    final var foundedJson = foundedResult.getOrDefault(new JSON());
    assertTrue.accept(foundedJson.valueNature());
    assertEquals("2010", foundedJson._string().state);

    // Test boolean property
    final var activePath = new Path(String._of(".active"));
    final var activeResult = testData.read(activePath);
    assertSet.accept(activeResult);
    final var activeJson = activeResult.getOrDefault(new JSON());
    assertTrue.accept(activeJson.valueNature());
    assertEquals("true", activeJson._string().state);
  }

  @Test
  void testPathConversionBehavior() {
    final var testData = createComplexTestData();

    // Test that EK9 $?.name format works (should be converted to $.name internally)
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
    final var testData = createComplexTestData();

    // Test array element access - first element
    final var firstTagPath = new Path(String._of(".tags[0]"));
    final var firstTagResult = testData.read(firstTagPath);
    assertSet.accept(firstTagResult);
    final var firstTagJson = firstTagResult.getOrDefault(new JSON());
    assertTrue.accept(firstTagJson.valueNature());
    assertQuoted("technology", firstTagJson);

    // Test array element access - middle element
    final var secondTagPath = new Path(String._of(".tags[1]"));
    final var secondTagResult = testData.read(secondTagPath);
    assertSet.accept(secondTagResult);
    final var secondTagJson = secondTagResult.getOrDefault(new JSON());
    assertTrue.accept(secondTagJson.valueNature());
    assertQuoted("innovation", secondTagJson);

    // Test array element access - last element
    final var lastTagPath = new Path(String._of(".tags[2]"));
    final var lastTagResult = testData.read(lastTagPath);
    assertSet.accept(lastTagResult);
    final var lastTagJson = lastTagResult.getOrDefault(new JSON());
    assertTrue.accept(lastTagJson.valueNature());
    assertQuoted("global", lastTagJson);

    // Test out of bounds array access - should return unset Result with error
    final var outOfBoundsPath = new Path(String._of(".tags[999]"));
    final var outOfBoundsResult = testData.read(outOfBoundsPath);
    assertUnset.accept(outOfBoundsResult);
    final var outOfBoundsError = outOfBoundsResult.errorOrDefault(new String());
    assertSet.accept(outOfBoundsError);

    // Test array access on non-array property - should return unset Result with error
    final var nonArrayPath = new Path(String._of(".name[0]"));
    final var nonArrayResult = testData.read(nonArrayPath);
    assertUnset.accept(nonArrayResult);
    final var nonArrayError = nonArrayResult.errorOrDefault(new String());
    assertSet.accept(nonArrayError);

    // Test negative array indexing (if supported by JSONPath)

    final var negativeIndexPath = new Path(String._of(".tags[-1]"));
    final var negativeIndexResult = testData.read(negativeIndexPath);
    // Should either return the last element or unset (depending on JSONPath implementation)
    if (negativeIndexResult._isSet().state) {
      final var negativeIndexJson = negativeIndexResult.getOrDefault(new JSON());
      assertTrue.accept(negativeIndexJson.valueNature());
    }


    // Test entire array access using wildcard
    final var allTagsPath = new Path(String._of(".tags[*]"));
    final var allTagsResult = testData.read(allTagsPath);
    assertSet.accept(allTagsResult);
    final var allTagsJson = allTagsResult.getOrDefault(new JSON());
    assertSet.accept(allTagsJson);
    assertTrue.accept(allTagsJson.arrayNature());
    assertEquals(THREE, allTagsJson._len());
  }

  @Test
  void testObjectArrayQueries() {
    final var testData = createComplexTestData();

    // Test accessing object property from first array element
    final var firstDeptNamePath = new Path(String._of(".departments[0].name"));
    final var firstDeptNameResult = testData.read(firstDeptNamePath);
    assertSet.accept(firstDeptNameResult);
    final var firstDeptNameJson = firstDeptNameResult.getOrDefault(new JSON());
    assertSet.accept(firstDeptNameJson);
    assertTrue.accept(firstDeptNameJson.valueNature());
    assertQuoted("Engineering", firstDeptNameJson);

    // Test accessing object property from second array element
    final var secondDeptNamePath = new Path(String._of(".departments[1].name"));
    final var secondDeptNameResult = testData.read(secondDeptNamePath);
    assertSet.accept(secondDeptNameResult);
    final var secondDeptNameJson = secondDeptNameResult.getOrDefault(new JSON());
    assertSet.accept(secondDeptNameJson);
    assertTrue.accept(secondDeptNameJson.valueNature());
    assertQuoted("Marketing", secondDeptNameJson);

    // Test deep nested object array access - employee name
    final var firstEmployeePath = new Path(String._of(".departments[0].employees[0].name"));
    final var firstEmployeeResult = testData.read(firstEmployeePath);
    assertSet.accept(firstEmployeeResult);
    final var firstEmployeeJson = firstEmployeeResult.getOrDefault(new JSON());
    assertSet.accept(firstEmployeeJson);
    assertTrue.accept(firstEmployeeJson.valueNature());
    assertQuoted("Bob Smith", firstEmployeeJson);

    // Test accessing skill from employee skills array
    final var firstSkillPath = new Path(String._of(".departments[0].employees[0].skills[0]"));
    final var firstSkillResult = testData.read(firstSkillPath);
    assertSet.accept(firstSkillResult);
    final var firstSkillJson = firstSkillResult.getOrDefault(new JSON());
    assertSet.accept(firstSkillJson);
    assertTrue.accept(firstSkillJson.valueNature());
    assertQuoted("Java", firstSkillJson);

    // Test accessing second employee in first department
    final var secondEmployeePath = new Path(String._of(".departments[0].employees[1].name"));
    final var secondEmployeeResult = testData.read(secondEmployeePath);
    assertSet.accept(secondEmployeeResult);
    final var secondEmployeeJson = secondEmployeeResult.getOrDefault(new JSON());
    assertSet.accept(secondEmployeeJson);
    assertTrue.accept(secondEmployeeJson.valueNature());
    assertQuoted("Carol Davis", secondEmployeeJson);

    // Test out of bounds on object array - should return unset Result with error
    final var outOfBoundsDeptPath = new Path(String._of(".departments[999].name"));
    final var outOfBoundsDeptResult = testData.read(outOfBoundsDeptPath);
    assertUnset.accept(outOfBoundsDeptResult);
    final var outOfBoundsDeptError = outOfBoundsDeptResult.errorOrDefault(new String());
    assertSet.accept(outOfBoundsDeptError);

    // Test out of bounds on nested employee array - should return unset Result with error
    final var outOfBoundsEmpPath = new Path(String._of(".departments[0].employees[999].name"));
    final var outOfBoundsEmpResult = testData.read(outOfBoundsEmpPath);
    assertUnset.accept(outOfBoundsEmpResult);
    final var outOfBoundsEmpError = outOfBoundsEmpResult.errorOrDefault(new String());
    assertSet.accept(outOfBoundsEmpError);

    // Test accessing non-existent property in object array
    final var nonExistentPropPath = new Path(String._of(".departments[0].nonexistent"));
    final var nonExistentPropResult = testData.read(nonExistentPropPath);
    assertUnset.accept(nonExistentPropResult);
    final var nonExistentPropError = nonExistentPropResult.errorOrDefault(new String());
    assertSet.accept(nonExistentPropError);

    // Test deep manager contact access
    final var managerEmailPath = new Path(String._of(".departments[1].manager.contact.email"));
    final var managerEmailResult = testData.read(managerEmailPath);
    assertSet.accept(managerEmailResult);
    final var managerEmailJson = managerEmailResult.getOrDefault(new JSON());
    assertSet.accept(managerEmailJson);
    assertTrue.accept(managerEmailJson.valueNature());
    assertQuoted("david@techcorp.com", managerEmailJson);
  }

  @Test
  void testJSONIterator() {
    // Test Array Nature Iterator
    final var arrayJson = createJsonArray();
    addStringElement(arrayJson, "first");
    addIntElement(arrayJson, 42);
    addBooleanElement(arrayJson, true);

    final var arrayIterator = arrayJson.iterator();
    assertSet.accept(arrayIterator);

    // First element
    assertTrue.accept(arrayIterator.hasNext());
    final var firstElement = arrayIterator.next();
    assertSet.accept(firstElement);
    assertTrue.accept(firstElement.valueNature());
    assertQuoted("first", firstElement);

    // Second element
    assertTrue.accept(arrayIterator.hasNext());
    final var secondElement = arrayIterator.next();
    assertSet.accept(secondElement);
    assertTrue.accept(secondElement.valueNature());
    assertEquals("42", secondElement.toString());

    // Third element  
    assertTrue.accept(arrayIterator.hasNext());
    final var thirdElement = arrayIterator.next();
    assertSet.accept(thirdElement);
    assertTrue.accept(thirdElement.valueNature());
    assertEquals("true", thirdElement.toString());

    // No more elements
    assertFalse.accept(arrayIterator.hasNext());

    // Test Object Nature Iterator - Key-Value Pairs
    final var objectJson = createJsonObject();
    addStringProperty(objectJson, "name", "John");
    addIntProperty(objectJson, "age", 30);
    addBooleanProperty(objectJson);

    final var objectIterator = objectJson.iterator();
    assertSet.accept(objectIterator);

    // Collect all field name-value pairs
    final var fieldNames = new java.util.HashSet<java.lang.String>();

    while (objectIterator.hasNext().state) {
      final var namedField = objectIterator.next();
      assertSet.accept(namedField);
      assertTrue.accept(namedField.objectNature()); // Named JSON objects are object nature

      // Extract field name and value from named JSON object
      // For named JSON like JSON("name", JSON("John")), we need to access the structure
      final var fieldString = namedField._string().state;

      // The named JSON should contain the key-value structure
      // We'll verify this by checking the JSON contains expected patterns
      if (fieldString.contains("name")) {
        fieldNames.add("name");
      } else if (fieldString.contains("age")) {
        fieldNames.add("age");
      } else if (fieldString.contains("active")) {
        fieldNames.add("active");
      }
    }

    // Verify all expected fields were found
    assertEquals(THREE, Integer._of(fieldNames.size()));
    Assertions.assertTrue(fieldNames.contains("name"));
    Assertions.assertTrue(fieldNames.contains("age"));
    Assertions.assertTrue(fieldNames.contains("active"));

    // Test Value Nature Iterator
    final var valueJson = jsonFromString("singleValue");
    final var valueIterator = valueJson.iterator();
    assertSet.accept(valueIterator);

    assertTrue.accept(valueIterator.hasNext());
    final var singleValue = valueIterator.next();
    assertSet.accept(singleValue);
    assertTrue.accept(singleValue.valueNature());
    assertQuoted("singleValue", singleValue);

    // Only one element for value nature
    assertFalse.accept(valueIterator.hasNext());

    // Test Unset JSON Iterator
    final var unsetJson = new JSON();
    final var unsetIterator = unsetJson.iterator();
    assertUnset.accept(unsetIterator); // Iterator is unset when there's nothing to iterate over

    // Test Empty Array Iterator - no elements to iterate over
    final var emptyArray = createJsonArray();
    final var emptyArrayIterator = emptyArray.iterator();
    assertUnset.accept(emptyArrayIterator); // No elements, so iterator is unset

    // Test Empty Object Iterator - no properties to iterate over
    final var emptyObject = createJsonObject();
    final var emptyObjectIterator = emptyObject.iterator();
    assertUnset.accept(emptyObjectIterator); // No properties, so iterator is unset
  }

  @Test
  void testJSONIteratorComplexStructures() {
    // Test iterator with nested structures
    final var complexJson = createJsonObject();

    // Add nested array
    final var nestedArray = createJsonArray();
    addStringElement(nestedArray, "nested1");
    addStringElement(nestedArray, "nested2");
    addProperty(complexJson, "items", nestedArray);

    // Add nested object
    final var nestedObject = createJsonObject();
    addStringProperty(nestedObject, "innerKey", "innerValue");
    addProperty(complexJson, "metadata", nestedObject);

    final var iterator = complexJson.iterator();
    assertSet.accept(iterator);

    var fieldsFound = 0;
    while (iterator.hasNext().state) {
      final var namedField = iterator.next();
      assertSet.accept(namedField);
      assertTrue.accept(namedField.objectNature()); // Named JSON objects
      fieldsFound++;
    }

    assertEquals(TWO, Integer._of(fieldsFound)); // Should find 2 fields: items and metadata

    // Test that nested structures maintain their nature when accessed through iteration
    final var itemsProperty = getProperty(complexJson, "items");
    assertTrue.accept(itemsProperty.arrayNature());

    final var itemsIterator = itemsProperty.iterator();
    var itemCount = 0;
    while (itemsIterator.hasNext().state) {
      final var item = itemsIterator.next();
      assertSet.accept(item);
      assertTrue.accept(item.valueNature());
      itemCount++;
    }
    assertEquals(TWO, Integer._of(itemCount));
  }
}