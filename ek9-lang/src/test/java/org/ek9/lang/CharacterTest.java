package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CharacterTest extends Common {

  final Boolean true1 = Boolean._of("true");
  final Boolean false1 = Boolean._of("false");

  final Character unset = new Character();
  final Character cA = Character._of('A');
  final Character cB = Character._of('B');
  final Character cC = Character._of('C');
  final Character cLowerA = Character._of('a');
  final Character cSpace = Character._of(' ');
  final Character cZ = Character._of('Z');
  final Character cZero = Character._of('0');
  final Character cOne = Character._of('1');

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
    assertEquals(true1, cA._eq(cAA));

    assertUnset.accept(cA._eq(unset));
    assertUnset.accept(unset._eq(unset));
    assertUnset.accept(unset._eq(cA));

    //Neq
    assertEquals(false1, cA._neq(cAA));
    assertUnset.accept(cA._neq(unset));
    assertUnset.accept(unset._neq(unset));
    assertUnset.accept(unset._neq(cA));

    //Lt
    assertTrue.accept(cA._lt(cB));
    assertFalse.accept(cB._lt(cA));
    assertUnset.accept(unset._lt(cB));
    assertUnset.accept(cA._lt(unset));

    //gt
    assertTrue.accept(cB._gt(cA));
    assertFalse.accept(cA._gt(cB));
    assertUnset.accept(unset._gt(cB));
    assertUnset.accept(cA._gt(unset));

    //Lteq
    assertTrue.accept(cA._lteq(cAA));
    assertTrue.accept(cA._lteq(cB));
    assertFalse.accept(cB._lteq(cA));
    assertUnset.accept(unset._lteq(cA));
    assertUnset.accept(cA._lteq(unset));

    //Gteq
    assertTrue.accept(cA._gteq(cAA));
    assertTrue.accept(cB._gteq(cA));
    assertFalse.accept(cA._gteq(cB));
    assertUnset.accept(unset._gteq(cA));
    assertUnset.accept(cA._gteq(unset));
  }

  @Test
  void testComparison() {
    final var cAA = Character._of('A');

    assertEquals(0, cA._cmp(cAA).state);
    assertTrue(cA._cmp(cB).state < 0);
    assertTrue(cB._cmp(cA).state > 0);
    assertUnset.accept(unset._cmp(cA));
    assertUnset.accept(cA._cmp(unset));
    assertUnset.accept(cA._cmp(new Any(){}));

  }

  @Test
  void testIsSet() {
    assertNotNull(unset);
    assertFalse.accept(unset._isSet());

    final var v1 = Character._of('X');
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());
  }

  @Test
  void testCaseOperations() {
    assertUnset.accept(unset.upperCase());
    assertUnset.accept(unset.lowerCase());

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

    assertUnset.accept(unset._inc());
    assertUnset.accept(unset._dec());
  }

  @Test
  void testAsString() {
    assertUnset.accept(unset._string());

    assertEquals(String._of("A"), cA._string());
    assertEquals(String._of("B"), cB._string());
    assertEquals(String._of(" "), cSpace._string());

    assertEquals("A", cA.toString());
    assertEquals("", unset.toString());
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
    assertUnset.accept(unset._json());
  }

  @Test
  void testHashCode() {
    assertUnset.accept(unset._hashcode());
    assertEquals(cA._hashcode(), cA._hashcode());
    assertNotEquals(cA._hashcode(), cB._hashcode());
    assertNotEquals(cA._hashcode(), cLowerA._hashcode());
  }

  @Test
  void testUtilityOperators() {
    assertUnset.accept(unset._len());
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

    mutatedValue._replace(unset);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unset
    mutatedValue._replace(cA);
    assertEquals(cA, mutatedValue);
  }

  @Test
  void testMergeLogic() {
    var mutatedValue = new Character();
    assertUnset.accept(mutatedValue);

    mutatedValue._merge(unset);
    assertUnset.accept(mutatedValue);

    mutatedValue._merge(cA);
    assertEquals(cA, mutatedValue);

    mutatedValue._merge(cB);
    assertEquals(cB, mutatedValue);

    mutatedValue._merge(unset);
    assertEquals(cB, mutatedValue);
  }

  @Test
  void testPipeLogic() {
    var mutatedValue = new Character();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unset);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(cA);
    assertEquals(cA, mutatedValue);

    mutatedValue._pipe(cB);
    assertEquals(cB, mutatedValue);

    mutatedValue._pipe(unset);
    assertEquals(cB, mutatedValue);
  }

  @Test
  void testPromotion() {
    assertUnset.accept(unset._promote());

    assertEquals(String._of("A"), cA._promote());
    assertEquals(String._of("B"), cB._promote());
    assertEquals(String._of(" "), cSpace._promote());
  }

  @Test
  void testFuzzy() {
    assertUnset.accept(unset._fuzzy(cA));
    assertUnset.accept(cA._fuzzy(unset));
    assertUnset.accept(unset._fuzzy(unset));

    final var cAA = Character._of('A');
    assertEquals(Integer._of(0), cA._fuzzy(cAA));

    assertEquals(Integer._of(0x7fffffff), cA._fuzzy(cB));
    assertEquals(Integer._of(0x7fffffff), cB._fuzzy(cA));

    assertEquals(Integer._of(1), cA._fuzzy(cLowerA));
    assertEquals(Integer._of(1), cLowerA._fuzzy(cA));

    assertEquals(Integer._of(0x7fffffff), cZero._fuzzy(cOne));
    assertEquals(Integer._of(0x7fffffff), cOne._fuzzy(cZero));

  }
}