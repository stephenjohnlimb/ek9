package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BooleanTest extends Common {

  final Boolean true1 = Boolean._of("true");
  final Boolean true2 = Boolean._of("true");

  final Boolean false1 = Boolean._of("false");
  final Boolean false2 = Boolean._of("false");

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

    final var alsoTrue = new Boolean(true1);
    assertEquals(true1, alsoTrue);

    final var unset = new Boolean(new Boolean());
    assertUnset.accept(unset._eq(new Boolean()));

    final var viaString = new Boolean(String._of("true"));
    assertEquals(true1, viaString);
  }

  @Test
  void testByStringConstruction() {

    final var asTrue = Boolean._of("true");
    assertSet.accept(asTrue);
    assertTrue(asTrue.state);

    final var asFalse = Boolean._of("false");
    assertSet.accept(asFalse);
    assertFalse(asFalse.state);

    final var alsoAsFalse = Boolean._of("someOtherValue");
    assertSet.accept(alsoAsFalse);
    assertFalse(alsoAsFalse.state);
  }

  @Test
  void testEquality() {

    final var unset = new Boolean();
    assertFalse(unset.isSet);

    //Obvious tests
    assertTrue.accept(true1._eq(true2));
    assertFalse.accept(true1._eq(false1));

    assertTrue.accept(true1._neq(false2));
    assertFalse.accept(false1._eq(true1));

    assertTrue.accept(false1._eq(false2));
    assertFalse.accept(true1._neq(true2));

    assertTrue.accept(false1._neq(true1));

    //But when testing equality with something unset the result is unset.
    assertUnset.accept(true1._eq(unset));
    assertUnset.accept(true1._neq(unset));

    //Same the other way.
    assertUnset.accept(unset._eq(true1));
    assertUnset.accept(unset._neq(true1));
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
    assertEquals(0L, true1._fuzzy(true2).state);
    assertEquals(0L, false1._fuzzy(false2).state);
    assertEquals(1L, true1._fuzzy(false1).state);
    assertEquals(-1L, false1._fuzzy(true1).state);

    //Any comparing unset is itself unset.
    assertUnset.accept(unset._fuzzy(true1));
    assertUnset.accept(unset._fuzzy(unset));
    assertUnset.accept(true1._fuzzy(unset));
  }

  @Test
  void testNegation() {
    final var unset1 = new Boolean();
    final var unset2 = new Boolean();

    assertEquals(false1, true1._negate());
    assertEquals(true1, false1._negate());

    //This shows that a boolean, can be true/false or actually unset.
    assertNotEquals(true1, unset1._negate());
    assertNotEquals(false1, unset1._negate());

    //Now, no matter what you do with unset, it will always be unset

    assertUnset.accept(unset2);
    assertUnset.accept( unset2._negate());
  }

  @Test
  void testAsString() {
    final var trueStr = true1._string();
    assertSet.accept(trueStr);
    final var trueExpected = new Boolean(trueStr);
    assertEquals(true1, trueExpected);

    final var falseStr = false1._string();
    assertSet.accept(falseStr);
    final var falseExpected = new Boolean(falseStr);
    assertEquals(false1, falseExpected);

    assertUnset.accept(new Boolean()._string());
  }

  @Test
  void testHashCode() {
    final var unset = new Boolean();
    assertNotEquals(unset._hashcode(), true1._hashcode());
    assertEquals(true1._hashcode(), true2._hashcode());
    assertEquals(false1._hashcode(), false2._hashcode());

    assertNotEquals(true1._hashcode(), false1._hashcode());
  }

  @Test
  void testBooleanLogic() {

    final var unset = new Boolean();

    assertEquals(Boolean._of("true"), true1._and(true2));
    assertEquals(Boolean._of("false"), true1._and(false1));
    assertEquals(Boolean._of("false"), false1._and(true1));
    assertUnset.accept(false1._and(unset));
    assertUnset.accept(unset._and(false1));

    assertEquals(Boolean._of("true"), true1._or(true2));
    assertEquals(Boolean._of("true"), true1._or(false1));
    assertEquals(Boolean._of("true"), false1._or(true1));
    assertEquals(Boolean._of("false"), false1._or(false2));
    assertUnset.accept( false1._or(unset));
    assertUnset.accept(unset._or(false1));

    assertEquals(Boolean._of("false"), true1._xor(true2));
    assertEquals(Boolean._of("false"), false1._xor(false2));
    assertUnset.accept(false1._xor(unset));
    assertUnset.accept(unset._xor(false1));

    assertEquals(Boolean._of("true"), true1._xor(false2));
    assertEquals(Boolean._of("true"), false1._xor(true2));

  }

  @Test
  void testAdditionalAndLogic() {
    final var unset = new Boolean();
    //Same as 'OR'
    assertEquals(Boolean._of("true"), true1._add(true2));
    assertEquals(Boolean._of("true"), true1._add(false1));
    assertEquals(Boolean._of("true"), false1._add(true1));
    assertEquals(Boolean._of("false"), false1._add(false2));

    assertUnset.accept(false1._add(unset));
    assertUnset.accept(unset._add(false1));

  }

  @Test
  void testAdditionalAndAssignWithUnsetLogic() {
    final var unset = new Boolean();
    var mutatedBoolean = Boolean._of("true");
    assertEquals(true1, mutatedBoolean);

    mutatedBoolean._addAss(unset);
    assertUnset.accept(mutatedBoolean);

    mutatedBoolean._addAss(true1);
    assertUnset.accept( mutatedBoolean);

    mutatedBoolean._addAss(false1);
    assertUnset.accept(mutatedBoolean);

  }

  @Test
  void testAdditionalAndAssignWithTrueLogic() {
    var mutatedBoolean = Boolean._of("true");
    assertEquals(true1, mutatedBoolean);

    mutatedBoolean._addAss(true2);
    assertEquals(true1, mutatedBoolean);

    //Even if we add again it should still be true.
    mutatedBoolean._addAss(true1);
    assertEquals(true1, mutatedBoolean);

    //Now even when false is added, it will still be true, once true always true.
    mutatedBoolean._addAss(false1);
    assertEquals(true1, mutatedBoolean);
  }

  @Test
  void testAdditionalAndAssignWithFalseLogic() {
    var mutatedBoolean = Boolean._of("false");
    assertEquals(false1, mutatedBoolean);

    //Now once we add a true it should become mutated to true.
    mutatedBoolean._addAss(true2);
    assertEquals(true1, mutatedBoolean);
  }

  @Test
  void testReplaceAndCopyLogic() {

    final var unset = new Boolean();

    var mutatedBoolean = Boolean._of("false");
    assertEquals(false1, mutatedBoolean);

    mutatedBoolean._replace(true1);
    assertEquals(true1, mutatedBoolean);

    mutatedBoolean._replace(false1);
    assertEquals(false1, mutatedBoolean);

    mutatedBoolean._replace(unset);
    assertUnset.accept(mutatedBoolean);

    //Now just check that it can take a value after being unset
    mutatedBoolean._replace(true1);
    assertEquals(true1, mutatedBoolean);

  }

  @Test
  void testPipeLogic() {

    final var unset = new Boolean();
    var mutatedBoolean = new Boolean();
    assertUnset.accept(mutatedBoolean);

    mutatedBoolean._pipe(unset);
    assertUnset.accept(mutatedBoolean);

    mutatedBoolean._pipe(false1);
    assertEquals(false1, mutatedBoolean);

    mutatedBoolean._pipe(true1);
    assertEquals(true1, mutatedBoolean);

    //But now it's true it will always be true
    mutatedBoolean._pipe(false1);
    assertEquals(true1, mutatedBoolean);

    //Even if we pipe unset back in - that will be ignored for pipes.
    mutatedBoolean._pipe(unset);
    assertEquals(true1, mutatedBoolean);

  }

}