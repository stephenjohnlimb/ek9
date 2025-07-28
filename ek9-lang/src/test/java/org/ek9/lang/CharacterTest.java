package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CharacterTest extends Common {

  @Test
  void testConstruction() {
    final var defaultConstructor = new Character();
    assertUnset.accept(defaultConstructor);

    final var unset1 = Character._of("");
    assertUnset.accept(unset1);

    final var unset2 = Character._of((java.lang.String) null);
    assertUnset.accept(unset2);

    final var unset3 = new Character(new String());
    assertUnset.accept(unset3);

    final var checkA1 = new Character(String._of("A"));
    assertEquals('A', checkA1.state);

    final var checkA2 = Character._of('A');
    assertEquals('A', checkA2.state);

    final var checkA3 = Character._of("A");
    assertEquals('A', checkA3.state);

    assertSet.accept(cA);
    assertEquals('A', cA.state);

    assertSet.accept(cB);
    assertEquals('B', cB.state);

    assertSet.accept(cC);
    assertEquals('C', cC.state);

    final var alsoA = Character._of(cA);
    assertSet.accept(alsoA);
    assertEquals('A', alsoA.state);

    final var againA = new Character(cA);
    assertSet.accept(againA);
    assertEquals('A', againA.state);

    final var checkB = new Character(cB);
    assertSet.accept(checkB);
    assertEquals(cB, checkB);
  }

  @Test
  void testEquality() {
    final var cAA = Character._of('A');

    //Eq
    assertEquals(cAA, cA);
    assertEquals(trueBoolean, cA._eq(cAA));

    assertUnset.accept(cA._eq(unsetCharacter));
    assertUnset.accept(unsetCharacter._eq(unsetCharacter));
    assertUnset.accept(unsetCharacter._eq(cA));

    //Neq
    assertEquals(falseBoolean, cA._neq(cAA));
    assertUnset.accept(cA._neq(unsetCharacter));
    assertUnset.accept(unsetCharacter._neq(unsetCharacter));
    assertUnset.accept(unsetCharacter._neq(cA));

    //Lt
    assertTrue.accept(cA._lt(cB));
    assertFalse.accept(cB._lt(cA));
    assertUnset.accept(unsetCharacter._lt(cB));
    assertUnset.accept(cA._lt(unsetCharacter));

    //gt
    assertTrue.accept(cB._gt(cA));
    assertFalse.accept(cA._gt(cB));
    assertUnset.accept(unsetCharacter._gt(cB));
    assertUnset.accept(cA._gt(unsetCharacter));

    //Lteq
    assertTrue.accept(cA._lteq(cAA));
    assertTrue.accept(cA._lteq(cB));
    assertFalse.accept(cB._lteq(cA));
    assertUnset.accept(unsetCharacter._lteq(cA));
    assertUnset.accept(cA._lteq(unsetCharacter));

    //Gteq
    assertTrue.accept(cA._gteq(cAA));
    assertTrue.accept(cB._gteq(cA));
    assertFalse.accept(cA._gteq(cB));
    assertUnset.accept(unsetCharacter._gteq(cA));
    assertUnset.accept(cA._gteq(unsetCharacter));
  }

  @Test
  void testComparison() {
    final var cAA = Character._of('A');

    assertEquals(0, cA._cmp(cAA).state);
    assertTrue(cA._cmp(cB).state < 0);
    assertTrue(cB._cmp(cA).state > 0);
    assertUnset.accept(unsetCharacter._cmp(cA));
    assertUnset.accept(cA._cmp(unsetCharacter));
    assertUnset.accept(cA._cmp(new Any(){}));

  }

  @Test
  void testIsSet() {
    assertNotNull(unsetCharacter);
    assertFalse.accept(unsetCharacter._isSet());

    final var v1 = Character._of('X');
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());
  }

  @Test
  void testCaseOperations() {
    assertUnset.accept(unsetCharacter.upperCase());
    assertUnset.accept(unsetCharacter.lowerCase());

    assertEquals(cA, cA.upperCase());
    assertEquals(cLowerA, cA.lowerCase());

    assertEquals(cA, cLowerA.upperCase());
    assertEquals(cLowerA, cLowerA.lowerCase());

    final var cZUpper = cZ.upperCase();
    assertEquals(cZ, cZUpper);

    final var cZLower = Character._of('z');
    assertEquals(cZLower, cZ.lowerCase());
  }

  @Test
  void testIncrementDecrement() {
    final var mutatedChar = Character._of('A');
    assertEquals(cA, mutatedChar);

    mutatedChar._inc();
    assertEquals(cB, mutatedChar);

    mutatedChar._inc();
    assertEquals(cC, mutatedChar);

    mutatedChar._dec();
    assertEquals(cB, mutatedChar);

    mutatedChar._dec();
    assertEquals(cA, mutatedChar);

    //Just check that when decrementing for the lowest value the result is unset.
    final var underFlow = Character._of(0);
    assertSet.accept(underFlow);
    underFlow._dec();
    assertUnset.accept(underFlow);

    assertUnset.accept(unsetCharacter._inc());
    assertUnset.accept(unsetCharacter._dec());
  }

  @Test
  void testAsString() {
    assertUnset.accept(unsetCharacter._string());

    assertEquals(String._of("A"), cA._string());
    assertEquals(String._of("B"), cB._string());
    assertEquals(String._of(" "), cSpace._string());

    assertEquals("A", cA.toString());
    assertEquals("", unsetCharacter.toString());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var aJson = cA._json();
    assertNotNull(aJson);
    assertSet.accept(aJson);
    
    final var bJson = cB._json();
    assertSet.accept(bJson);
    
    final var spaceJson = cSpace._json();
    assertSet.accept(spaceJson);
    
    // Test JSON conversion with unset value
    assertUnset.accept(unsetCharacter._json());
  }

  @Test
  void testHashCode() {
    assertUnset.accept(unsetCharacter._hashcode());
    assertEquals(cA._hashcode(), cA._hashcode());
    assertNotEquals(cA._hashcode(), cB._hashcode());
    assertNotEquals(cA._hashcode(), cLowerA._hashcode());
  }

  @Test
  void testUtilityOperators() {
    assertUnset.accept(unsetCharacter._len());
    assertEquals(Integer._of(1), cA._len());
    assertEquals(Integer._of(1), cB._len());
    assertEquals(Integer._of(1), cSpace._len());
  }

  @Test
  void testReplaceAndCopyLogic() {
    var mutatedValue = Character._of('A');
    assertEquals(cA, mutatedValue);

    mutatedValue._replace(cB);
    assertEquals(cB, mutatedValue);

    mutatedValue._replace(cC);
    assertEquals(cC, mutatedValue);

    mutatedValue._replace(unsetCharacter);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unset
    mutatedValue._replace(cA);
    assertEquals(cA, mutatedValue);
  }

  @Test
  void testMergeLogic() {
    var mutatedValue = new Character();
    assertUnset.accept(mutatedValue);

    mutatedValue._merge(unsetCharacter);
    assertUnset.accept(mutatedValue);

    mutatedValue._merge(cA);
    assertEquals(cA, mutatedValue);

    mutatedValue._merge(cB);
    assertEquals(cB, mutatedValue);

    mutatedValue._merge(unsetCharacter);
    assertEquals(cB, mutatedValue);
  }

  @Test
  void testPipeLogic() {
    var mutatedValue = new Character();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unsetCharacter);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(cA);
    assertEquals(cA, mutatedValue);

    mutatedValue._pipe(cB);
    assertEquals(cB, mutatedValue);

    mutatedValue._pipe(unsetCharacter);
    assertEquals(cB, mutatedValue);
  }

  @Test
  void testPromotion() {
    assertUnset.accept(unsetCharacter._promote());

    assertEquals(String._of("A"), cA._promote());
    assertEquals(String._of("B"), cB._promote());
    assertEquals(String._of(" "), cSpace._promote());
  }

  @Test
  void testFuzzy() {
    assertUnset.accept(unsetCharacter._fuzzy(cA));
    assertUnset.accept(cA._fuzzy(unsetCharacter));
    assertUnset.accept(unsetCharacter._fuzzy(unsetCharacter));

    final var cAA = Character._of('A');
    assertEquals(Integer._of(0), cA._fuzzy(cAA));

    assertEquals(Integer._of(0x7fffffff), cA._fuzzy(cB));
    assertEquals(Integer._of(0x7fffffff), cB._fuzzy(cA));

    assertEquals(Integer._of(1), cA._fuzzy(cLowerA));
    assertEquals(Integer._of(1), cLowerA._fuzzy(cA));

    assertEquals(Integer._of(0x7fffffff), cZero._fuzzy(cOne));
    assertEquals(Integer._of(0x7fffffff), cOne._fuzzy(cZero));

  }

  @Test
  void testSimplePipedJSONValue() {
    // Test piping individual JSON character values
    final var mutatedCharacter = new Character();
    final var jsonA = new JSON(cA);
    final var jsonB = new JSON(cB);
    final var jsonStringHello = new JSON(String._of("Hello")); // Should take first char 'H'
    final var jsonEmptyString = new JSON(String._of("")); // Should be ignored (unset)

    // Start unset
    assertUnset.accept(mutatedCharacter);

    // Pipe 'A' - should become 'A'
    mutatedCharacter._pipe(jsonA);
    assertSet.accept(mutatedCharacter);
    assertEquals(cA, mutatedCharacter);

    // Pipe 'B' - should replace with 'B' 
    mutatedCharacter._pipe(jsonB);
    assertEquals(cB, mutatedCharacter);

    // Test string parsing - should take first character
    final var stringTest = new Character();
    stringTest._pipe(jsonStringHello);
    assertEquals('H', stringTest.state);

    // Test empty string - should be ignored
    stringTest._pipe(jsonEmptyString);  
    assertEquals('H', stringTest.state); // Should remain 'H'
  }

  @Test
  void testSimplePipedJSONArray() {
    final var mutatedCharacter = new Character();
    final var json1Result = new JSON().parse(String._of("[\"A\", \"Hello\"]"));
    final var json2Result = new JSON().parse(String._of("[\"X\", \"Y\", \"Z\"]"));

    // Check that the JSON text was parsed
    assertSet.accept(json1Result);
    assertSet.accept(json2Result);

    // Pipe array with "A" and "Hello" - should end up as 'H' (last valid)
    mutatedCharacter._pipe(json1Result.ok());
    assertSet.accept(mutatedCharacter);
    assertEquals('H', mutatedCharacter.state); // "Hello" -> 'H' was the last value

    // Create new character for second test - should end up as 'Z' (last value)
    final var anotherCharacter = new Character();
    anotherCharacter._pipe(json2Result.ok());
    assertSet.accept(anotherCharacter);
    assertEquals('Z', anotherCharacter.state);
  }

  @Test
  void testStructuredPipedJSONObject() {
    final var mutatedCharacter = new Character();
    final var jsonStr = """
        {
          "prop1": "A",
          "prop2": "World"
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedCharacter._pipe(jsonResult.ok());

    assertSet.accept(mutatedCharacter);
    // Should be 'W' from "World" (as that would be processed last in traversal)
    assertEquals('W', mutatedCharacter.state);
  }

  @Test
  void testNestedPipedJSONObject() {
    final var mutatedCharacter = new Character();
    final var jsonStr = """
        {
          "prop1": ["A", "B"],
          "prop2": "C",
          "prop3": "Just a String",
          "prop4": [{"val1": "X", "val2": "Y"}, {"other": "Z"}]
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedCharacter._pipe(jsonResult.ok());

    assertSet.accept(mutatedCharacter);
    // Should end up with 'Z' as that's likely the last character processed
    assertEquals('Z', mutatedCharacter.state);
  }

}