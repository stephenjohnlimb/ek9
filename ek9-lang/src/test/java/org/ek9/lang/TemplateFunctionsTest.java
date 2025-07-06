package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * From a Java point of view these all look like they are duplicated.
 * But from an EK9 point of view some are 'pure' in nature and others are not.
 * <p>
 *   The 'pure' aspect is totally within the Ek9 compiler checking. Once we get down
 *   to the implementation level it all looks the same. The EK9 compiler does the
 *   checks at compile time. There are no runtime checks.
 * </p>
 * <p>
 *   This means you could declare something as pure and then not honour that!
 * </p>
 */
class TemplateFunctionsTest extends Common {

  @Test
  void testBasicSupplierUse() {
    final var underTest = new Supplier();
    assertNotNull(underTest);
    final var any = underTest._call();
    assertNotNull(any);

    assertFunctionIsSetAndString(underTest);

  }

  @Test
  void testBasicProducerUse() {
    final var underTest = new Producer();
    assertNotNull(underTest);
    final var any = underTest._call();
    assertNotNull(any);

    assertFunctionIsSetAndString(underTest);

  }

  @Test
  void testBasicConsumerUse() {
    final var underTest = new Consumer();
    assertNotNull(underTest);
    final var any = new Any() {};
    underTest._call(any);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicBiConsumerUse() {
    final var underTest = new BiConsumer();
    assertNotNull(underTest);
    final var anyT = new Any() {};
    final var anyU = new Any() {};
    underTest._call(anyT, anyU);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicAcceptorUse() {
    final var underTest = new Acceptor();
    assertNotNull(underTest);
    final var any = new Any() {};
    underTest._call(any);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicBiAcceptorUse() {
    final var underTest = new BiAcceptor();
    assertNotNull(underTest);
    final var anyT = new Any() {};
    final var anyU = new Any() {};
    underTest._call(anyT, anyU);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicUnaryOperatorUse() {
    final var underTest = new UnaryOperator();
    assertNotNull(underTest);
    final var any = new Any() {};
    final var alsoAny = underTest._call(any);
    //Not bothered what the value is, but not null.
    assertNotNull(alsoAny);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicFunctionUse() {
    final var underTest = new Function();
    assertNotNull(underTest);
    final var any = new Any() {};
    final var alsoAny = underTest._call(any);
    //Not bothered what the value is, but not null.
    assertNotNull(alsoAny);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicBiFunctionUse() {
    final var underTest = new BiFunction();
    assertNotNull(underTest);
    final var anyT = new Any() {};
    final var anyU = new Any() {};
    final var alsoAny = underTest._call(anyT, anyU);
    //Not bothered what the value is, but not null.
    assertNotNull(alsoAny);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicRoutineUse() {
    final var underTest = new Routine();
    assertNotNull(underTest);
    final var any = new Any() {};
    final var alsoAny = underTest._call(any);
    //Not bothered what the value is, but not null.
    assertNotNull(alsoAny);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicBiRoutineUse() {
    final var underTest = new BiRoutine();
    assertNotNull(underTest);
    final var anyT = new Any() {};
    final var anyU = new Any() {};
    final var alsoAny = underTest._call(anyT, anyU);
    //Not bothered what the value is, but not null.
    assertNotNull(alsoAny);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicPredicateUse() {
    final var underTest = new Predicate();
    assertNotNull(underTest);
    final var any = new Any() {};
    final Boolean aBoolean = underTest._call(any);
    //Not bothered what the value is, but not null.
    assertNotNull(aBoolean);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicBiPredicateUse() {
    final var underTest = new BiPredicate();
    assertNotNull(underTest);
    final var anyT = new Any() {};
    final var anyU = new Any() {};
    final Boolean aBoolean = underTest._call(anyT, anyU);
    //Not bothered what the value is, but not null.
    assertNotNull(aBoolean);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicAssessorUse() {
    final var underTest = new Assessor();
    assertNotNull(underTest);
    final var any = new Any() {};
    final Boolean aBoolean = underTest._call(any);
    //Not bothered what the value is, but not null.
    assertNotNull(aBoolean);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicBiAssessorUse() {
    final var underTest = new BiAssessor();
    assertNotNull(underTest);
    final var anyT = new Any() {};
    final var anyU = new Any() {};
    final Boolean aBoolean = underTest._call(anyT, anyU);
    //Not bothered what the value is, but not null.
    assertNotNull(aBoolean);

    assertFunctionIsSetAndString(underTest);
  }

  @Test
  void testBasicComparatorUse() {
    final var underTest = new Comparator();
    assertNotNull(underTest);
    final var t1 = new Any() {};
    final var t2 = new Any() {};
    final Integer result = underTest._call(t1, t2);
    //Not bothered what the value is, but not null.
    assertNotNull(result);

    assertFunctionIsSetAndString(underTest);
  }

  private void assertFunctionIsSetAndString(Any any) {
    final var set = any._isSet();
    assertSet.accept(set);

    final var asString = any._string();
    assertUnset.accept(asString);
  }
}
