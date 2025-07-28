package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class
JSONRepresentationTest extends JSONTestBase {

  @Nested
  class StringRepresentationTests {

    record StringConversionTestCase(JSON json, java.lang.String expectedString, boolean isQuoted) {}

    static Stream<StringConversionTestCase> getStringConversionTestCases() {
      return Stream.of(
          new StringConversionTestCase(jsonFromString("hello"), "\"hello\"", true),
          new StringConversionTestCase(jsonFromInt(42), "42", false),
          new StringConversionTestCase(jsonFromBoolean(true), "true", false),
          new StringConversionTestCase(jsonFromFloat(3.14), "3.14", false)
      );
    }

    @ParameterizedTest
    @MethodSource("getStringConversionTestCases")
    void testBasicStringConversion(StringConversionTestCase testCase) {
      final var result = testCase.json()._string();
      assertSet.accept(result);
      
      if (testCase.isQuoted()) {
        assertQuoted(result.state);
      } else {
        assertEquals(testCase.expectedString(), result.state);
      }
    }

    @Test
    void testUnsetStringConversion() {
      final var unsetJson = new JSON();
      assertNotNull(unsetJson);
      final var unsetResult = unsetJson._string();
      assertUnset.accept(unsetResult);
    }

    @Test
    void testObjectStringRepresentation() {
      // Test comprehensive string representation for objects, arrays, and values

      // Test simple object string representation
      final var simpleObject = createNamedJson("name", jsonFromString("John Doe"));
      assertSet.accept(simpleObject);
      final var simpleStr = simpleObject._string();
      assertSet.accept(simpleStr);
      assertEquals("{\"name\":\"John Doe\"}", simpleStr.state);

      // Test object with multiple properties
      final var person = createJsonObject();
      addStringProperty(person, "name", "Alice");
      addIntProperty(person, "age", 30);
      addBooleanProperty(person);

      final var personStr = person._string();
      assertSet.accept(personStr);
      // Should contain all properties (order may vary in JSON objects)
      final var personStrValue = personStr.state;
      assertTrue.accept(Boolean._of(personStrValue.contains("\"name\":\"Alice\"")));
      assertTrue.accept(Boolean._of(personStrValue.contains("\"age\":30")));
      assertTrue.accept(Boolean._of(personStrValue.contains("\"active\":true")));

      // Test simple array string representation
      final var simpleArray = createJsonArray();
      addStringElement(simpleArray, "first");
      add42Element(simpleArray);
      addBooleanElement(simpleArray, true);

      final var arrayStr = simpleArray._string();
      assertSet.accept(arrayStr);
      assertEquals("[\"first\",42,true]", arrayStr.state);

      // Test nested structure string representation
      final var nestedObject = createJsonObject();
      addStringProperty(nestedObject, "name", "Company");
      addProperty(nestedObject, "employees", simpleArray);

      final var nestedStr = nestedObject._string();
      assertSet.accept(nestedStr);
      // Should contain nested array
      assertTrue.accept(Boolean._of(nestedStr.state.contains("[\"first\",42,true]")));

      // Test empty structures
      assertEquals("{}", createJsonObject()._string().state);
      assertEquals("[]", createJsonArray()._string().state);
    }

    @Test
    void testPrettyPrintFormatting() {
      // Test prettyPrint() method for formatted JSON output
      final var complexObject = createJsonObject();
      addStringProperty(complexObject, "name", "John");
      addIntProperty(complexObject, "age", 30);

      final var nestedArray = createJsonArray();
      addStringElement(nestedArray, "item1");
      addStringElement(nestedArray, "item2");
      addProperty(complexObject, "items", nestedArray);

      final var prettyResult = complexObject.prettyPrint();
      assertSet.accept(prettyResult);

      // Pretty printed JSON should contain newlines and indentation
      final var prettyStr = prettyResult.state;
      assertTrue.accept(Boolean._of(prettyStr.contains("\n")));
      assertTrue.accept(Boolean._of(prettyStr.contains("  ")));

      // Should still be valid JSON content
      assertTrue.accept(Boolean._of(prettyStr.contains("\"name\" : \"John\"")));
      assertTrue.accept(Boolean._of(prettyStr.contains("\"age\" : 30")));

      // Test prettyPrint on unset JSON
      final var unsetJson = new JSON();
      final var unsetPretty = unsetJson.prettyPrint();
      assertUnset.accept(unsetPretty);

      // Test prettyPrint on simple values
      final var simpleValue = jsonFromString("simple");
      final var simplePretty = simpleValue.prettyPrint();
      assertSet.accept(simplePretty);
      assertQuoted(simplePretty.state);
    }

    @Test
    void testComplexObjectRepresentation() {
      // Test string representation of complex nested structures
      final var complexData = createComplexTestData();
      final var complexStr = complexData._string();
      assertSet.accept(complexStr);

      // Should be valid JSON string representation
      final var parsedBack = JSON._of(complexStr.state);
      assertSet.accept(parsedBack);
      assertTrue.accept(parsedBack.objectNature());

      // Test that complex structure maintains its properties
      final var name = parsedBack.get(String._of("name"));
      assertQuoted("TechCorp", name);

      final var founded = parsedBack.get(String._of("founded"));
      assertEquals("2010", founded._string().state);
    }

    @Test
    void testToStringMethod() {
      // Test toString() method (should delegate to _string())
      final var jsonValue = jsonFromString("test");
      final var toStringResult = jsonValue.toString();
      final var stringResult = jsonValue._string().state;
      assertEquals(stringResult, toStringResult);

      // Test toString on complex object
      final var complexObject = createNamedJson("key", jsonFromString("value"));
      final var complexToString = complexObject.toString();
      assertEquals("{\"key\":\"value\"}", complexToString);
    }
  }

  @Nested
  class HashingTests {

    @Test
    void testArrayHashing() {
      // Test _hashcode operator with array nature
      final var emptyArray = createJsonArray();
      assertTrue.accept(emptyArray.arrayNature());
      final var emptyArrayHash = emptyArray._hashcode();
      assertSet.accept(emptyArrayHash);

      final var nonEmptyArray = createJsonArray();
      addStringElement(nonEmptyArray, "item");
      add42Element(nonEmptyArray);
      assertTrue.accept(nonEmptyArray.arrayNature());
      final var nonEmptyArrayHash = nonEmptyArray._hashcode();
      assertSet.accept(nonEmptyArrayHash);

      // Different arrays should have different hash codes
      assertNotEquals(emptyArrayHash, nonEmptyArrayHash);
    }

    @Test
    void testObjectHashing() {
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
      assertNotEquals(emptyObjectHash, nonEmptyObjectHash);
    }

    record ValueHashTestCase(JSON sameValue1, JSON sameValue2, JSON differentValue) {
    }

    static Stream<ValueHashTestCase> getValueHashingTestCases() {
      return Stream.of(
          new ValueHashTestCase(jsonFromString("hello"), jsonFromString("hello"), jsonFromString("world")),
          new ValueHashTestCase(jsonFromInt(42), jsonFromInt(42), jsonFromInt(100)),
          new ValueHashTestCase(jsonFromBoolean(true), jsonFromBoolean(true), jsonFromBoolean(false)),
          new ValueHashTestCase(jsonFromFloat(3.14), jsonFromFloat(3.14), jsonFromFloat(2.71))
      );
    }

    @ParameterizedTest
    @MethodSource("getValueHashingTestCases")
    void testValueHashingBehavior(ValueHashTestCase testCase) {
      // Test that all values are value nature
      assertTrue.accept(testCase.sameValue1().valueNature());
      assertTrue.accept(testCase.sameValue2().valueNature());
      assertTrue.accept(testCase.differentValue().valueNature());

      // Test hashcodes are set
      final var hash1 = testCase.sameValue1()._hashcode();
      final var hash2 = testCase.sameValue2()._hashcode();
      final var hash3 = testCase.differentValue()._hashcode();
      assertSet.accept(hash1);
      assertSet.accept(hash2);
      assertSet.accept(hash3);

      // Same values should have same hash
      assertEquals(hash1, hash2);
      // Different values should have different hash
      assertNotEquals(hash1, hash3);
    }

    @Test
    void testSpecialHashingCases() {
      // Test empty string
      final var emptyString = jsonFromString("");
      final var nonEmptyString = jsonFromString("hello");
      assertTrue.accept(emptyString.valueNature());
      assertSet.accept(emptyString._hashcode());
      assertNotEquals(emptyString._hashcode(), nonEmptyString._hashcode());

      // Integer and string with same content should have different hashes
      final var intFortyTwo = jsonFromInt(42);
      final var stringFortyTwo = jsonFromString("42");
      assertNotEquals(intFortyTwo._hashcode(), stringFortyTwo._hashcode());
    }

    @Test
    void testUnsetJsonHashing() {
      // Test _hashcode operator with unset JSON (should return unset)
      final var unsetJson = new JSON();
      assertNotNull(unsetJson);
      final var unsetHash = unsetJson._hashcode();
      assertUnset.accept(unsetHash);
    }

    @Test
    void testComplexStructureHashing() {
      // Test hashing of complex structures
      final var complexData1 = createComplexTestData();
      final var hash1 = complexData1._hashcode();
      assertSet.accept(hash1);

      final var complexData2 = createComplexTestData();
      final var hash2 = complexData2._hashcode();
      assertSet.accept(hash2);

      // Same complex structures should have same hash
      assertEquals(hash1, hash2);

      // Modified structure should have different hash
      final var modifiedData = createComplexTestData();
      addStringProperty(modifiedData, "newProperty", "newValue");
      final var modifiedHash = modifiedData._hashcode();
      assertSet.accept(modifiedHash);
      assertNotEquals(hash1, modifiedHash);
    }
  }

  @Nested
  class PromotionTests {

    static Stream<JSON> getPromotionTestCases() {
      return Stream.of(
          jsonFromString("test"),
          jsonFromInt(42),
          jsonFromBoolean(true),
          createJsonArray()._add(jsonFromString("item")),
          createNamedJson("key", jsonFromString("value"))
      );
    }

    @ParameterizedTest
    @MethodSource("getPromotionTestCases")
    void testPromoteOperator(JSON json) {
      final var promoted = json._promote();
      final var asString = json._string();
      
      assertSet.accept(promoted);
      assertSet.accept(asString);
      assertEquals(asString.state, promoted.state);
    }

    @Test
    void testStringPromotionConsistency() {
      // Test that _promote and _string return identical results
      final var testValues = new JSON[] {
          jsonFromString("hello"),
          jsonFromInt(123),
          jsonFromBoolean(false),
          jsonFromFloat(2.5),
          createJsonArray(),
          createJsonObject()
      };

      for (final var json : testValues) {
        final var promoted = json._promote();
        final var asString = json._string();

        if (promoted._isSet().state && asString._isSet().state) {
          assertEquals(asString.state, promoted.state);
        } else {
          // Both should be unset or both should be set
          assertEquals(promoted._isSet().state, asString._isSet().state);
        }
      }

      // Test with unset JSON
      final var unsetJson = new JSON();
      final var unsetPromoted = unsetJson._promote();
      final var unsetString = unsetJson._string();

      assertUnset.accept(unsetPromoted);
      assertUnset.accept(unsetString);
    }
  }

  @Nested
  class ParsingTests {


    @Test
    void testStringToJSONParsing() {
      // Test JSON._of(String) parsing functionality

      // Test parsing simple values
      final var stringValue = JSON._of("\"hello\"");
      assertSet.accept(stringValue);
      assertTrue.accept(stringValue.valueNature());
      assertQuoted("hello", stringValue);

      final var numberValue = JSON._of("42");
      assertSet.accept(numberValue);
      assertTrue.accept(numberValue.valueNature());
      assertEquals("42", numberValue._string().state);

      final var boolValue = JSON._of("true");
      assertSet.accept(boolValue);
      assertTrue.accept(boolValue.valueNature());
      assertEquals("true", boolValue._string().state);

      final var nullValue = JSON._of("null");
      assertSet.accept(nullValue);
      assertTrue.accept(nullValue.valueNature());
      assertEquals("null", nullValue._string().state);

      // Test parsing arrays
      final var arrayValue = JSON._of("[1, 2, 3]");
      assertSet.accept(arrayValue);
      assertTrue.accept(arrayValue.arrayNature());
      assertEquals(INT_3, arrayValue.arrayLength());

      // Test parsing objects
      final var objectValue = JSON._of("{\"name\": \"John\", \"age\": 30}");
      assertSet.accept(objectValue);
      assertTrue.accept(objectValue.objectNature());
      assertEquals(INT_2, objectValue._len());

      final var name = objectValue.get(String._of("name"));
      assertQuoted("John", name);

      final var age = objectValue.get(String._of("age"));
      assertEquals("30", age._string().state);
    }

    static Stream<java.lang.String> getMalformedJsonStrings() {
      return Stream.of("{malformed", "[1, 2,", "not json at all", null);
    }

    @ParameterizedTest
    @MethodSource("getMalformedJsonStrings")
    void testMalformedJsonReturnsUnset(java.lang.String malformedJson) {
      final var result = JSON._of(malformedJson);
      assertNotNull(result);
      assertUnset.accept(result);
    }

    static Stream<java.lang.String> getValidEdgeCaseJsonStrings() {
      return Stream.of("", "   ");
    }

    @ParameterizedTest
    @MethodSource("getValidEdgeCaseJsonStrings")
    void testEdgeCaseJsonReturnsSet(java.lang.String edgeCaseJson) {
      final var result = JSON._of(edgeCaseJson);
      assertNotNull(result);
      assertSet.accept(result);
    }

    @Test
    void testRoundTripParsing() {
      // Test that parsing and stringifying are inverse operations

      // Test simple values
      final var originalString = "\"test value\"";
      final var parsed = JSON._of(originalString);
      assertSet.accept(parsed);
      final var backToString = parsed._string();
      assertSet.accept(backToString);
      assertEquals(originalString, backToString.state);

      // Test complex object
      final var complexObject = createJsonObject();
      addStringProperty(complexObject, "name", "Alice");
      addIntProperty(complexObject, "age", 25);

      final var objectString = complexObject._string();
      assertSet.accept(objectString);

      final var reparsed = JSON._of(objectString.state);
      assertSet.accept(reparsed);
      assertTrue.accept(reparsed.objectNature());

      // Should be equivalent (though property order might differ)
      final var reparsedName = reparsed.get(String._of("name"));
      assertQuoted("Alice", reparsedName);

      final var reparsedAge = reparsed.get(String._of("age"));
      assertEquals("25", reparsedAge._string().state);
    }

    static Stream<JSON> getSelfReferenceObjects() {
      return Stream.of(jsonFromString("test"), createJsonArray(), createJsonObject());
    }

    @ParameterizedTest
    @MethodSource("getSelfReferenceObjects")
    void testJSONOperatorSelfReference(final JSON json) {
      assertNotNull(json);
      final var jsonOperatorResult = json._json();

      assertSet.accept(jsonOperatorResult);
      assertTrue.accept(json._eq(jsonOperatorResult));

    }
  }
}