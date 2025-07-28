package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JSONConstructionTest extends JSONTestBase {

  // Test case record for parameterized unset constructor testing
  record UnsetConstructorTestCase(java.lang.String typeName, Supplier<JSON> constructor) {}

  @Nested
  class BasicConstructorTests {

    static Stream<JSON> getQuotedConstructedJson() {
      return Stream.of(jsonFromString("test"),
          jsonFromChar('A'),
          jsonFromDate("2025-07-24"),
          jsonFromDateTime("2025-07-24T10:30:00Z"),
          jsonFromTime("09:00:00"),
          jsonFromDuration("PT1H"),
          jsonFromMillisecond(1000),
          jsonFromMoney("100.50#USD")
      );
    }

    static Stream<JSON> getNotQuotedConstructedJson() {
      return Stream.of(jsonFromInt(123),
          jsonFromBoolean(true),
          jsonFromFloat(45.67)
      );
    }

    @Test
    void testDefaultConstructor() {
      final var defaultConstructor = new JSON();
      assertNotNull(defaultConstructor);
      assertUnset.accept(defaultConstructor);
    }

    @ParameterizedTest
    @MethodSource("getQuotedConstructedJson")
    void testConstructorsExpectQuoted(final JSON json) {
      assertNotNull(json);
      assertQuoted(json._string().state);
    }

    @ParameterizedTest
    @MethodSource("getNotQuotedConstructedJson")
    void testConstructorsExpectNotQuoted(final JSON json) {
      assertNotNull(json);
      assertNotQuoted(json._string().state);

    }
  }

  @Nested
  class FactoryMethodTests {

    @Test
    void testArrayFactory() {
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
    }

    @Test
    void testObjectFactory() {
      // Test new JSON().object() factory method
      final var emptyObject = new JSON().object();
      assertSet.accept(emptyObject);
      assertTrue.accept(emptyObject.objectNature());
      assertFalse.accept(emptyObject.arrayNature());
      assertFalse.accept(emptyObject.valueNature());

      // Test empty object properties
      assertEquals(INT_0, emptyObject._len());
      assertTrue.accept(emptyObject._empty());
    }

    @Test
    void testStringFactory() {
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
    }

    @Test
    void testJavaStringFactory() {
      // Test factory method with Java String
      final var basicValue = JSON._of("42");
      assertSet.accept(basicValue);
      assertTrue.accept(basicValue.valueNature());
      assertEquals("42", basicValue._string().state);

      final var stringValue = JSON._of("\"hello\"");
      assertSet.accept(stringValue);
      assertTrue.accept(stringValue.valueNature());
      assertEquals("\"hello\"", stringValue._string().state);

      final var arrayValue = JSON._of("[1, 2, 3]");
      assertSet.accept(arrayValue);
      assertTrue.accept(arrayValue.arrayNature());
      assertEquals(INT_3, arrayValue.arrayLength());

      final var objectValue = JSON._of("{\"key\": \"value\"}");
      assertSet.accept(objectValue);
      assertTrue.accept(objectValue.objectNature());
      assertEquals(INT_1, objectValue._len());

      // Test malformed JSON returns unset
      final var malformedJson = JSON._of("{malformed");
      assertUnset.accept(malformedJson);

      // Test null string returns unset
      final var nullJson = JSON._of((java.lang.String) null);
      assertUnset.accept(nullJson);
    }
  }

  @Nested
  class UnsetConstructorTests {

    static Stream<UnsetConstructorTestCase> getUnsetConstructorTestCases() {
      return Stream.of(
          new UnsetConstructorTestCase("String", () -> new JSON(new String())),
          new UnsetConstructorTestCase("Integer", () -> new JSON(new Integer())),
          new UnsetConstructorTestCase("Boolean", () -> new JSON(new Boolean())),
          new UnsetConstructorTestCase("Float", () -> new JSON(new Float())),
          new UnsetConstructorTestCase("Character", () -> new JSON(new Character())),
          new UnsetConstructorTestCase("JSON", () -> new JSON(new JSON())),
          new UnsetConstructorTestCase("Date", () -> new JSON(new Date())),
          new UnsetConstructorTestCase("DateTime", () -> new JSON(new DateTime())),
          new UnsetConstructorTestCase("Time", () -> new JSON(new Time())),
          new UnsetConstructorTestCase("Money", () -> new JSON(new Money()))
      );
    }

    @ParameterizedTest
    @MethodSource("getUnsetConstructorTestCases")
    void testUnsetConstructorCreatesUnsetJson(UnsetConstructorTestCase testCase) {
      // Test that constructor with unset argument creates unset JSON object
      final var unsetJson = testCase.constructor().get();
      assertNotNull(unsetJson);
      assertUnset.accept(unsetJson);
    }

    @ParameterizedTest
    @MethodSource("getUnsetConstructorTestCases")
    void testUnsetJsonOperationsReturnUnset(UnsetConstructorTestCase testCase) {
      // Test that all operations on unset JSON objects return unset results
      final var unsetJson = testCase.constructor().get();
      assertNotNull(unsetJson);

      // All should return unset for nature methods
      assertUnset.accept(unsetJson.arrayNature());
      assertUnset.accept(unsetJson.objectNature());
      assertUnset.accept(unsetJson.valueNature());

      // All should return unset for length operations
      assertUnset.accept(unsetJson.arrayLength());
      assertUnset.accept(unsetJson._len());
      assertUnset.accept(unsetJson._empty());
    }

    @Test
    void testJsonNullValueMapping() {
      // Test EK9 tri-state semantics: unset EK9 values should map to JSON null

      // Create unset EK9 value
      final var unsetValue = new String(); // Unset EK9 String
      assertUnset.accept(unsetValue);

      // Create named JSON with unset value - should map unset EK9 value to JSON null
      final var namedJson = new JSON(STR_NAME, new JSON(unsetValue));
      assertSet.accept(namedJson);
      // Should create {"name": null} - unset EK9 value mapped to JSON null
      assertTrue.accept(namedJson.objectNature());
      final var nameProperty = namedJson.get(STR_NAME);
      assertSet.accept(nameProperty);
      assertTrue.accept(nameProperty.valueNature());
      assertEquals("null", nameProperty._string().state);

      // Test with unset name parameter - entire JSON should be unset
      final var unsetName = new String(); // Unset EK9 String
      final var valueJson = jsonFromString("test");
      final var unsetNameJson = new JSON(unsetName, valueJson);
      assertUnset.accept(unsetNameJson); // If name is unset, entire JSON remains unset
    }
  }

  @Nested
  class NamedConstructorTests {

    @Test
    void testObjectCreationAndStructure() {
      // Test named constructor for object creation

      // Create objects with basic EK9 types as property values
      final var stringProperty = createNamedJson("name", jsonFromString("John Doe"));
      assertSet.accept(stringProperty);
      assertTrue.accept(stringProperty.objectNature());
      assertEquals(INT_1, stringProperty._len());
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
    }

    @Test
    void testComplexNestedObjects() {
      // Test building complex object structure by merging properties
      var complexObject = createJsonObject();
      addStringProperty(complexObject, "name", "John Doe");
      addIntProperty(complexObject, "age", 30);
      addBooleanProperty(complexObject);
      addFloatProperty(complexObject, 95.5);
      addCharProperty(complexObject);
      addDateProperty(complexObject, "birthDate", "1990-05-15");

      assertSet.accept(complexObject);
      assertTrue.accept(complexObject.objectNature());
      assertEquals(INT_6, complexObject._len());
      assertFalse.accept(complexObject._empty());

      // Verify all properties are accessible
      assertJohnDoeQuoted(complexObject);
      final var age = getProperty(complexObject, "age");
      assertSet.accept(age);
      assertEquals("30", age._string().state);

      final var active = getProperty(complexObject, "active");
      assertSet.accept(active);
      assertEquals("true", active._string().state);
    }
  }

  @Nested
  class CopyConstructorTests {

    @Test
    void testJSONCopyConstructor() {
      // Test JSON copy constructor with deep copy behavior
      final var originalJson = jsonFromString("original");
      assertSet.accept(originalJson);

      // Create copy using JSON constructor
      final var copiedJson = new JSON(originalJson);
      assertSet.accept(copiedJson);

      // Verify they are equal but separate objects
      assertTrue.accept(originalJson._eq(copiedJson));
      assertNotNull(copiedJson);

      // Test copying arrays
      final var originalArray = createJsonArray();
      addStringElement(originalArray, "item1");
      add42Element(originalArray);

      final var copiedArray = new JSON(originalArray);
      assertSet.accept(copiedArray);
      assertTrue.accept(copiedArray.arrayNature());
      assertEquals(INT_2, copiedArray.arrayLength());
      assertTrue.accept(originalArray._eq(copiedArray));

      // Test copying objects
      final var originalObject = createNamedJson("key", jsonFromString("value"));
      final var copiedObject = new JSON(originalObject);
      assertSet.accept(copiedObject);
      assertTrue.accept(copiedObject.objectNature());
      assertTrue.accept(originalObject._eq(copiedObject));
    }
  }
}