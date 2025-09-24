package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BooleanTest extends Common {

  // Use true1 and false1 from Common base class

  @Test
  void testConstruction() {
    final var defaultConstructor = new Boolean();
    assertUnset.accept(defaultConstructor);

    final var asTrue = Boolean._of(true);
    assertSet.accept(asTrue);
    assertTrue(asTrue.state);

    final var asFalse = Boolean._of(false);
    assertSet.accept(asFalse);
    assertFalse(asFalse.state);

    final var alsoTrue = new Boolean(trueBoolean);
    assertEquals(trueBoolean, alsoTrue);

    assertUnset.accept(unsetBoolean._eq(new Boolean()));

    final var viaString = new Boolean(String._of("true"));
    assertEquals(trueBoolean, viaString);

    final var trueByFactory = Boolean._ofTrue();
    assertEquals(trueBoolean, trueByFactory);

    final var falseByFactory = Boolean._ofFalse();
    assertEquals(falseBoolean, falseByFactory);
  }

  /**
   * Test to show the tri-nature of the EK9 Boolean.
   * These methods are not exposed to EK9 developers.
   * But they are used in the IR and Code generation phases.
   */
  @Test
  void testInternalIRAndCodeGenerationMethods() {
    final var unsetBoolean = new Boolean();
    assertFalse(unsetBoolean._set());
    assertFalse(unsetBoolean._true());
    assertFalse(unsetBoolean._false());

    final var trueBoolean = Boolean._of(true);
    assertTrue(trueBoolean._set());
    assertTrue(trueBoolean._true());
    assertFalse(trueBoolean._false());

    final var falseBoolean = Boolean._of(false);
    assertTrue(falseBoolean._set());
    assertFalse(falseBoolean._true());
    assertTrue(falseBoolean._false());


  }

  @Test
  void testByStringConstruction() {

    final var asTrue = Boolean._of("true");
    assertSet.accept(asTrue);
    assertTrue(asTrue.state);

    final var asFalse = Boolean._of("false");
    assertSet.accept(asFalse);
    assertFalse(asFalse.state);

    final var nowUnset = Boolean._of("someOtherValue");
    assertUnset.accept(nowUnset);
  }

  @Test
  void testEquality() {

    assertFalse(unsetBoolean.isSet);

    //Obvious tests
    assertTrue.accept(trueBoolean._eq(trueBoolean));
    assertFalse.accept(trueBoolean._eq(falseBoolean));

    assertTrue.accept(trueBoolean._neq(falseBoolean));
    assertFalse.accept(falseBoolean._eq(trueBoolean));

    assertTrue.accept(falseBoolean._eq(falseBoolean));
    assertFalse.accept(trueBoolean._neq(trueBoolean));

    assertTrue.accept(falseBoolean._neq(trueBoolean));

    //But when testing equality with something unset the result is unset.
    assertUnset.accept(trueBoolean._eq(unsetBoolean));
    assertUnset.accept(trueBoolean._neq(unsetBoolean));

    //Same the other way.
    assertUnset.accept(unsetBoolean._eq(trueBoolean));
    assertUnset.accept(unsetBoolean._neq(trueBoolean));
  }

  @Test
  void testCompare() {
    final var unset = new Boolean();
    //So the value itself is not set
    assertFalse(unset.isSet);

    //The isSet() method returns a Boolean indicating it 'is' set and with a value of false.
    assertTrue(unset._isSet().isSet);
    assertFalse(unset._isSet().state);

    //In EK9 true is considered > false.
    //Hence, false is less than true.
    assertEquals(0L, trueBoolean._fuzzy(trueBoolean).state);
    assertEquals(0L, falseBoolean._fuzzy(falseBoolean).state);
    assertEquals(1L, trueBoolean._fuzzy(falseBoolean).state);
    assertEquals(-1L, falseBoolean._fuzzy(trueBoolean).state);

    //Any comparing unset is itself unset.
    assertUnset.accept(unset._fuzzy(trueBoolean));
    assertUnset.accept(unset._fuzzy(unset));
    assertUnset.accept(trueBoolean._fuzzy(unset));
  }

  @Test
  void testNegation() {
    final var unset1 = new Boolean();
    final var unset2 = new Boolean();

    assertEquals(falseBoolean, trueBoolean._negate());
    assertEquals(trueBoolean, falseBoolean._negate());

    //This shows that a boolean, can be true/false or actually unset.
    assertNotEquals(trueBoolean, unset1._negate());
    assertNotEquals(falseBoolean, unset1._negate());

    //Now, no matter what you do with unset, it will always be unset

    assertUnset.accept(unset2);
    assertUnset.accept( unset2._negate());
  }

  @Test
  void testAsString() {
    final var trueStr = trueBoolean._string();
    assertSet.accept(trueStr);
    final var trueExpected = new Boolean(trueStr);
    assertEquals(trueBoolean, trueExpected);

    final var falseStr = falseBoolean._string();
    assertSet.accept(falseStr);
    final var falseExpected = new Boolean(falseStr);
    assertEquals(falseBoolean, falseExpected);

    assertUnset.accept(new Boolean()._string());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set true value
    final var trueJson = trueBoolean._json();
    assertNotNull(trueJson);
    assertSet.accept(trueJson);
    
    // Test JSON conversion with set false value
    final var falseJson = falseBoolean._json();
    assertSet.accept(falseJson);
    
    // Test JSON conversion with unset value
    assertUnset.accept(new Boolean()._json());
  }

  @Test
  void testHashCode() {
    final var unset = new Boolean();
    assertNotEquals(unset._hashcode(), trueBoolean._hashcode());
    assertEquals(trueBoolean._hashcode(), trueBoolean._hashcode());
    assertEquals(falseBoolean._hashcode(), falseBoolean._hashcode());

    assertNotEquals(trueBoolean._hashcode(), falseBoolean._hashcode());
  }

  @Test
  void testBooleanLogic() {

    final var unset = new Boolean();

    assertEquals(trueBoolean, trueBoolean._and(trueBoolean));
    assertEquals(falseBoolean, trueBoolean._and(falseBoolean));
    assertEquals(falseBoolean, falseBoolean._and(trueBoolean));
    assertUnset.accept(falseBoolean._and(unset));
    assertUnset.accept(unset._and(falseBoolean));

    assertEquals(trueBoolean, trueBoolean._or(trueBoolean));
    assertEquals(trueBoolean, trueBoolean._or(falseBoolean));
    assertEquals(trueBoolean, falseBoolean._or(trueBoolean));
    assertEquals(falseBoolean, falseBoolean._or(falseBoolean));
    assertUnset.accept( falseBoolean._or(unset));
    assertUnset.accept(unset._or(falseBoolean));

    assertEquals(falseBoolean, trueBoolean._xor(trueBoolean));
    assertEquals(falseBoolean, falseBoolean._xor(falseBoolean));
    assertUnset.accept(falseBoolean._xor(unset));
    assertUnset.accept(unset._xor(falseBoolean));

    assertEquals(trueBoolean, trueBoolean._xor(falseBoolean));
    assertEquals(trueBoolean, falseBoolean._xor(trueBoolean));

  }

  @Test
  void testAdditionalAndLogic() {
    final var unset = new Boolean();
    //Same as 'OR'
    assertEquals(trueBoolean, trueBoolean._add(trueBoolean));
    assertEquals(trueBoolean, trueBoolean._add(falseBoolean));
    assertEquals(trueBoolean, falseBoolean._add(trueBoolean));
    assertEquals(falseBoolean, falseBoolean._add(falseBoolean));

    assertUnset.accept(falseBoolean._add(unset));
    assertUnset.accept(unset._add(falseBoolean));

  }

  @Test
  void testAdditionalAndAssignWithUnsetLogic() {
    final var unset = new Boolean();
    var mutatedBoolean = Boolean._of("true");
    assertEquals(trueBoolean, mutatedBoolean);

    mutatedBoolean._addAss(unset);
    assertUnset.accept(mutatedBoolean);

    mutatedBoolean._addAss(trueBoolean);
    assertUnset.accept( mutatedBoolean);

    mutatedBoolean._addAss(falseBoolean);
    assertUnset.accept(mutatedBoolean);

  }

  @Test
  void testAdditionalAndAssignWithTrueLogic() {
    var mutatedBoolean = Boolean._of("true");
    assertEquals(trueBoolean, mutatedBoolean);

    mutatedBoolean._addAss(trueBoolean);
    assertEquals(trueBoolean, mutatedBoolean);

    //Even if we add again it should still be true.
    mutatedBoolean._addAss(trueBoolean);
    assertEquals(trueBoolean, mutatedBoolean);

    //Now even when false is added, it will still be true, once true always true.
    mutatedBoolean._addAss(falseBoolean);
    assertEquals(trueBoolean, mutatedBoolean);
  }

  @Test
  void testAdditionalAndAssignWithFalseLogic() {
    var mutatedBoolean = Boolean._of("false");
    assertEquals(falseBoolean, mutatedBoolean);

    //Now once we add a true it should become mutated to true.
    mutatedBoolean._addAss(trueBoolean);
    assertEquals(trueBoolean, mutatedBoolean);
  }

  @Test
  void testReplaceAndCopyLogic() {

    final var unset = new Boolean();

    var mutatedBoolean = Boolean._of("false");
    assertEquals(falseBoolean, mutatedBoolean);

    mutatedBoolean._replace(trueBoolean);
    assertEquals(trueBoolean, mutatedBoolean);

    mutatedBoolean._replace(falseBoolean);
    assertEquals(falseBoolean, mutatedBoolean);

    mutatedBoolean._replace(unset);
    assertUnset.accept(mutatedBoolean);

    //Now just check that it can take a value after being unset
    mutatedBoolean._replace(trueBoolean);
    assertEquals(trueBoolean, mutatedBoolean);

  }

  @Test
  void testPipeLogic() {

    final var unset = new Boolean();
    var mutatedBoolean = new Boolean();
    assertUnset.accept(mutatedBoolean);

    mutatedBoolean._pipe(unset);
    assertUnset.accept(mutatedBoolean);

    mutatedBoolean._pipe(falseBoolean);
    assertEquals(falseBoolean, mutatedBoolean);

    mutatedBoolean._pipe(trueBoolean);
    assertEquals(trueBoolean, mutatedBoolean);

    //But now it's true it will always be true
    mutatedBoolean._pipe(falseBoolean);
    assertEquals(trueBoolean, mutatedBoolean);

    //Even if we pipe unset back in - that will be ignored for pipes.
    mutatedBoolean._pipe(unset);
    assertEquals(trueBoolean, mutatedBoolean);

  }

  @Test
  void testSimplePipedJSONValue() {
    // Test piping individual JSON boolean values
    final var mutatedBoolean = new Boolean();
    final var jsonFalse = new JSON(falseBoolean);
    final var jsonTrue = new JSON(trueBoolean);
    final var jsonStringTrue = new JSON(String._of("true"));
    final var jsonStringFalse = new JSON(String._of("false"));
    final var jsonStringOther = new JSON(String._of("other")); // Should parse as false

    // Start unset
    assertUnset.accept(mutatedBoolean);

    // Pipe false - should become false
    mutatedBoolean._pipe(jsonFalse);
    assertSet.accept(mutatedBoolean);
    assertEquals(falseBoolean, mutatedBoolean);

    // Pipe true - should become true (once true, always true)
    mutatedBoolean._pipe(jsonTrue);
    assertEquals(trueBoolean, mutatedBoolean);

    // Pipe false again - should remain true  
    mutatedBoolean._pipe(jsonFalse);
    assertEquals(trueBoolean, mutatedBoolean);

    //String are stripped or " and so can be used directly.
    final var stringFalseTest = new Boolean();
    stringFalseTest._pipe(jsonStringFalse);
    assertEquals(falseBoolean, stringFalseTest);

    final var stringTrueTest = new Boolean();
    stringTrueTest._pipe(jsonStringTrue);
    assertEquals(trueBoolean, stringTrueTest);

    // Test non-boolean string (should parse as false)
    final var otherTest = new Boolean();
    otherTest._pipe(jsonStringOther);
    assertEquals(falseBoolean, otherTest);
  }

  @Test
  void testSimplePipedJSONArray() {
    final var mutatedBoolean = new Boolean();
    final var json1Result = new JSON().parse(String._of("[false, true]"));
    final var json2Result = new JSON().parse(String._of("[false, false]"));

    // Check that the JSON text was parsed
    assertSet.accept(json1Result);
    assertSet.accept(json2Result);

    // Pipe array with false and true - should end up true
    mutatedBoolean._pipe(json1Result.ok());
    assertSet.accept(mutatedBoolean);
    assertEquals(trueBoolean, mutatedBoolean); // false OR true = true

    // Create new boolean for second test
    final var anotherBoolean = new Boolean();
    anotherBoolean._pipe(json2Result.ok()); 
    assertSet.accept(anotherBoolean);
    assertEquals(falseBoolean, anotherBoolean); // false OR false = false
  }

  @Test
  void testStructuredPipedJSONObject() {
    final var mutatedBoolean = new Boolean();
    final var jsonStr = """
        {
          "prop1": false,
          "prop2": true
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedBoolean._pipe(jsonResult.ok());

    assertSet.accept(mutatedBoolean);
    assertEquals(trueBoolean, mutatedBoolean); // false OR true = true
  }

  @Test
  void testNestedPipedJSONObject() {
    final var mutatedBoolean = new Boolean();
    final var jsonStr = """
        {
          "prop1": [false, true],
          "prop2": false,
          "prop3": "Just a String",
          "prop4": [{"val1": true, "val2": false}, {"other": false}]
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedBoolean._pipe(jsonResult.ok());

    assertSet.accept(mutatedBoolean);  
    // Should be true because there are multiple true values in the nested structure
    assertEquals(trueBoolean, mutatedBoolean);
  }

}