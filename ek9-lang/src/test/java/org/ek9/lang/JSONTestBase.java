package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Abstract base class for JSON tests providing shared helper methods and utilities.
 * Extends Common to reuse existing constants and assertions without duplication.
 */
abstract class JSONTestBase extends Common {

  // ===== JSON CREATION HELPERS =====

  static JSON jsonFromString(java.lang.String value) {
    return new JSON(String._of(value));
  }

  static JSON jsonFromInt(int value) {
    return new JSON(Integer._of(value));
  }

  static JSON jsonFromBoolean(boolean value) {
    return new JSON(Boolean._of(value));
  }

  static JSON jsonFromFloat(double value) {
    return new JSON(Float._of(value));
  }

  static JSON jsonFromChar(char value) {
    return new JSON(Character._of(value));
  }

  // EK9 temporal and complex type JSON creation helpers
  static JSON jsonFromDate(java.lang.String dateString) {
    return new JSON(new Date(String._of(dateString)));
  }

  static JSON jsonFromDateTime(java.lang.String dateTimeString) {
    return new JSON(new DateTime(String._of(dateTimeString)));
  }

  static JSON jsonFromTime(java.lang.String timeString) {
    return new JSON(new Time(String._of(timeString)));
  }

  static JSON jsonFromDuration(java.lang.String isoString) {
    return new JSON(Duration._of(isoString));
  }

  static JSON jsonFromMillisecond(long value) {
    return new JSON(Millisecond._of(value));
  }

  static JSON jsonFromMoney(java.lang.String moneyString) {
    return new JSON(new Money(String._of(moneyString)));
  }

  // ===== STRUCTURE CREATION HELPERS =====

  static JSON createJsonArray() {
    return new JSON().array();
  }

  static JSON createJsonObject() {
    return new JSON().object();
  }

  static JSON createNamedJson(java.lang.String key, JSON value) {
    return new JSON(String._of(key), value);
  }

  // ===== OBJECT PROPERTY HELPERS =====

  protected void addProperty(JSON obj, java.lang.String key, JSON value) {
    obj._merge(createNamedJson(key, value));
  }

  protected JSON getProperty(JSON obj, java.lang.String key) {
    return obj.get(String._of(key));
  }

  // Name-value pair creation helpers
  protected JSON createNameValuePair(java.lang.String key, java.lang.String value) {
    return createNamedJson(key, jsonFromString(value));
  }

  protected JSON createNameValuePair(int value) {
    return createNamedJson("age", jsonFromInt(value));
  }

  // Convenience overloads for common value types
  protected void addStringProperty(JSON obj, java.lang.String key, java.lang.String value) {
    addProperty(obj, key, jsonFromString(value));
  }

  protected void addIntProperty(JSON obj, java.lang.String key, int value) {
    addProperty(obj, key, jsonFromInt(value));
  }

  protected void addBooleanProperty(JSON obj) {
    addProperty(obj, "active", jsonFromBoolean(true));
  }

  protected void addFloatProperty(JSON obj, double value) {
    addProperty(obj, "score", jsonFromFloat(value));
  }

  protected void addCharProperty(JSON obj) {
    addProperty(obj, "grade", jsonFromChar('A'));
  }

  // Temporal property helpers
  protected void addDateProperty(JSON obj, java.lang.String key, java.lang.String dateString) {
    addProperty(obj, key, jsonFromDate(dateString));
  }

  // ===== ARRAY ELEMENT HELPERS =====

  protected JSON getElement(JSON array, int index) {
    return array.get(Integer._of(index));
  }

  protected void addElement(JSON array, JSON value) {
    array._merge(value);
  }

  // Convenience overloads for array elements
  protected void addStringElement(JSON array, java.lang.String value) {
    addElement(array, jsonFromString(value));
  }

  protected void add42Element(JSON array) {
    addElement(array, jsonFromInt(42));
  }

  protected void addBooleanElement(JSON array, boolean value) {
    addElement(array, jsonFromBoolean(value));
  }

  // ===== JSON-SPECIFIC ASSERTION HELPERS =====

  // Utility method to check if a string is quoted
  protected void assertQuoted(java.lang.String jsonString) {
    assertEquals("\"", jsonString.substring(0, 1)); // Should start with quote
    assertEquals("\"", jsonString.substring(jsonString.length() - 1)); // Should end with quote
  }

  protected void assertNotQuoted(java.lang.String jsonString) {
    assertFalse(jsonString.contains("\""));
  }

  protected void assertQuoted(java.lang.String expect, JSON json) {
    assertSet.accept(json);
    var provided = json._string();
    assertQuoted(provided.state);
    var trimmed = provided.trim(Character._of('"')).state;
    assertEquals(expect, trimmed);
  }

  // Object property version with expected value
  protected void assertJohnDoeQuoted(JSON obj) {
    final var retrieved = getProperty(obj, "name");
    assertQuoted("John Doe", retrieved);
  }

  // Element assertion helpers for arrays
  protected void assertElementValue(JSON element, java.lang.String expectedValue) {
    assertSet.accept(element);
    assertEquals(expectedValue, element._string().state);
  }

  // ===== COMPLEX TEST DATA CREATION =====

  protected JSON createComplexTestData() {
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
                  "phone": "+1-555-0202"
                }
              },
              "employees": [
                {
                  "name": "Eve Brown",
                  "role": "Marketing Manager",
                  "skills": ["Brand Management", "Digital Marketing"],
                  "salary": 72000
                }
              ]
            }
          ]
        }
        """;

    return JSON._of(jsonString);
  }
}