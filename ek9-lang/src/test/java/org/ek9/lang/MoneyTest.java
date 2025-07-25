package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Money class.
 * Tests currency-safe operations, precision handling, and Money format parsing.
 */
class MoneyTest extends Common {

  private final Money unset = new Money();
  private final Money tenPounds = Money._of("10.00#GBP");
  private final Money thirtyDollars = Money._of("30.20#USD");
  private final Money fivePounds = Money._of("5.00#GBP");
  private final Money zeroPounds = Money._of("0.00#GBP");
  private final Money negativePounds = Money._of("-25.50#GBP");

  @Test
  void testConstruction() {
    // Default constructor creates unset
    final var defaultConstructor = new Money();
    assertUnset.accept(defaultConstructor);

    // Copy constructor
    final var copyConstructor = new Money(tenPounds);
    assertSet.accept(copyConstructor);

    assertTrue.accept(copyConstructor._eq(tenPounds));
    // String constructor with valid format
    final var stringConstructor = new Money(String._of("99.99#EUR"));
    assertSet.accept(stringConstructor);
    assertEquals("99.99#EUR", stringConstructor._string().state);

    // String constructor with invalid format
    final var invalidStringConstructor = new Money(String._of("invalid"));
    assertUnset.accept(invalidStringConstructor);

    // Null handling
    final var nullConstructor = new Money((Money) null);
    assertUnset.accept(nullConstructor);

    final var nullStringConstructor = new Money((String) null);
    assertUnset.accept(nullStringConstructor);
  }

  @Test
  void testFactoryMethods() {
    // Empty factory
    final var empty = Money._of();
    assertUnset.accept(empty);

    // String factory
    final var fromString = Money._of("45.67#CAD");
    assertSet.accept(fromString);
    assertEquals("45.67#CAD", fromString._string().state);

    // BigDecimal factory
    final var fromBigDecimal = Money._of(BigDecimal.valueOf(123.45), "USD");
    assertSet.accept(fromBigDecimal);
    assertEquals("123.45#USD", fromBigDecimal._string().state);

    // Double factory
    final var fromDouble = Money._of(78.90, "GBP");
    assertSet.accept(fromDouble);
    assertEquals("78.90#GBP", fromDouble._string().state);

    // Invalid currency code
    final var invalidCurrency = Money._of(100.0, "XYZ");
    assertUnset.accept(invalidCurrency);
  }

  @Test
  void testStringParsing() {
    // Valid formats
    final var gbp = Money._of("10.50#GBP");
    assertSet.accept(gbp);
    assertEquals("10.50#GBP", gbp._string().state);

    final var usd = Money._of("30.2#USD");
    assertSet.accept(usd);
    assertEquals("30.20#USD", usd._string().state); // Should pad to 2 decimal places

    final var jpy = Money._of("1000#JPY");
    assertSet.accept(jpy);
    assertEquals("1000#JPY", jpy._string().state); // JPY has 0 decimal places

    // Test rounding to currency decimal places
    final var rounded = Money._of("99.999#USD");
    assertSet.accept(rounded);
    assertEquals("100.00#USD", rounded._string().state); // Should round up

    // Invalid formats
    final var noHash = Money._of("10.50GBP");
    assertUnset.accept(noHash);

    final var noAmount = Money._of("#GBP");
    assertUnset.accept(noAmount);

    final var noCurrency = Money._of("10.50#");
    assertUnset.accept(noCurrency);

    final var invalidCurrency = Money._of("10.50#XYZ");
    assertUnset.accept(invalidCurrency);

    final var wrongCurrencyLength = Money._of("10.50#GBPP");
    assertUnset.accept(wrongCurrencyLength);
  }

  @Test
  void testStateManagement() {
    // Test _isSet operator
    assertFalse.accept(unset._isSet());
    assertTrue.accept(tenPounds._isSet());

    // Test string conversion
    final var str = tenPounds._string();
    assertSet.accept(str);
    assertEquals("10.00#GBP", str.state);

    // Unset string conversion
    final var unsetStr = unset._string();
    assertUnset.accept(unsetStr);

    // Hash code
    final var hash = tenPounds._hashcode();
    assertSet.accept(hash);

    final var hash2 = tenPounds._hashcode();
    assertTrue.accept(hash._eq(hash2));
  }

  @Test
  void testEquality() {
    // Same currency equality
    final var anotherTenPounds = Money._of("10.00#GBP");
    assertNotNull(anotherTenPounds);
    assertTrue.accept(tenPounds._eq(anotherTenPounds));
    assertFalse.accept(tenPounds._eq(fivePounds));
    assertTrue.accept(fivePounds._neq(tenPounds));

    // Different currency comparison - should be unset
    assertUnset.accept(tenPounds._eq(thirtyDollars));

    // Unset propagation
    assertUnset.accept(unset._eq(tenPounds));
    assertUnset.accept(tenPounds._eq(unset));
    assertUnset.accept(unset._eq(unset));

    assertUnset.accept(unset._neq(tenPounds));
    assertUnset.accept(tenPounds._neq(unset));

    // Polymorphic equality with Any
    assertTrue.accept(tenPounds._eq((Any) anotherTenPounds));
    assertUnset.accept(tenPounds._eq(String._of("not money")));
  }

  @Test
  void testComparison() {
    // Same currency comparisons
    assertTrue.accept(fivePounds._lt(tenPounds));
    assertTrue.accept(fivePounds._lteq(tenPounds));
    assertTrue.accept(tenPounds._gt(fivePounds));
    assertTrue.accept(tenPounds._gteq(fivePounds));

    assertUnset.accept(fivePounds._lt(new Money()));
    assertUnset.accept(fivePounds._lteq(new Money()));
    assertUnset.accept(fivePounds._gt(new Money()));
    assertUnset.accept(fivePounds._gteq(new Money()));

    final var equalAmount = Money._of("10.00#GBP");
    assertNotNull(equalAmount);

    assertTrue.accept(tenPounds._lteq(equalAmount));
    assertTrue.accept(tenPounds._gteq(equalAmount));

    // Different currency comparisons - should be unset
    assertUnset.accept(tenPounds._lt(thirtyDollars));
    assertUnset.accept(tenPounds._gt(thirtyDollars));

    // _cmp operator
    final var cmpResult = tenPounds._cmp(fivePounds);
    assertSet.accept(cmpResult);
    assertTrue.accept(Boolean._of(cmpResult.state > 0));

    assertUnset.accept(tenPounds._cmp(thirtyDollars));

    // Polymorphic comparison
    assertSet.accept(tenPounds._cmp((Any) fivePounds));
    assertUnset.accept(tenPounds._cmp(String._of("not money")));
  }

  @Test
  void testFuzzyComparison() {
    // Same currency fuzzy comparison
    final var fuzzyResult = tenPounds._fuzzy(fivePounds);
    assertNotNull(fuzzyResult);
    assertSet.accept(fuzzyResult);
    assertTrue.accept(Boolean._of(fuzzyResult.state > 0));

    // Different currency - should be unset
    assertUnset.accept(tenPounds._fuzzy(thirtyDollars));
  }

  @Test
  void testArithmeticOperations() {
    // Addition - same currency
    final var sum = tenPounds._add(fivePounds);
    assertSet.accept(sum);
    assertEquals("15.00#GBP", sum._string().state);

    // Addition - different currency should be unset
    assertUnset.accept(tenPounds._add(thirtyDollars));

    // Subtraction - same currency
    final var difference = tenPounds._sub(fivePounds);
    assertSet.accept(difference);
    assertEquals("5.00#GBP", difference._string().state);

    // Subtraction - different currency should be unset
    assertUnset.accept(tenPounds._sub(thirtyDollars));

    // Negation
    final var negated = tenPounds._negate();
    assertSet.accept(negated);
    assertEquals("-10.00#GBP", negated._string().state);

    // Unset operand propagation
    assertUnset.accept(unset._add(tenPounds));
    assertUnset.accept(tenPounds._add(unset));
  }

  @Test
  void testMultiplicationAndDivision() {
    // Multiplication by Integer
    final var doubledInt = tenPounds._mul(Integer._of(2));
    assertSet.accept(doubledInt);
    assertEquals("20.00#GBP", doubledInt._string().state);

    // Multiplication by Float with rounding
    final var multipliedFloat = tenPounds._mul(Float._of(1.5));
    assertSet.accept(multipliedFloat);
    assertEquals("15.00#GBP", multipliedFloat._string().state);

    // Division by Integer
    final var halvedInt = tenPounds._div(Integer._of(2));
    assertSet.accept(halvedInt);
    assertEquals("5.00#GBP", halvedInt._string().state);

    // Division by Float
    final var dividedFloat = tenPounds._div(Float._of(4.0));
    assertSet.accept(dividedFloat);
    assertEquals("2.50#GBP", dividedFloat._string().state);

    // Division by zero should return unset
    assertUnset.accept(tenPounds._div(Integer._of(0)));
    assertUnset.accept(tenPounds._div(Float._of(0.0)));

    // Money division returns Float ratio
    final var ratio = tenPounds._div(fivePounds);
    assertSet.accept(ratio);
    assertTrue.accept(Boolean._of(Math.abs(ratio.state - 2.0) < 0.001));

    // Division by different currency should be unset
    assertUnset.accept(tenPounds._div(thirtyDollars));

    // Unset operand propagation
    assertUnset.accept(unset._mul(Integer._of(2)));
    assertUnset.accept(tenPounds._mul(new Integer()));
  }

  @Test
  void testAdvancedMathOperations() {
    // Absolute value
    final var abs = negativePounds._abs();
    assertSet.accept(abs);
    assertEquals("25.50#GBP", abs._string().state);

    // Square root
    final var twentyFivePounds = Money._of("25.00#GBP");
    final var sqrt = twentyFivePounds._sqrt();
    assertSet.accept(sqrt);
    assertEquals("5.00#GBP", sqrt._string().state);

    // Square root of negative should be unset
    assertUnset.accept(negativePounds._sqrt());

    // Power
    final var squared = fivePounds._pow(Integer._of(2));
    assertSet.accept(squared);
    assertEquals("25.00#GBP", squared._string().state);

  }

  @Test
  void testAssignmentOperators() {
    // Addition assignment
    final var mutable1 = new Money(tenPounds);
    mutable1._addAss(fivePounds);
    assertEquals("15.00#GBP", mutable1._string().state);

    // Addition assignment with different currency should unset
    final var mutable2 = new Money(tenPounds);
    mutable2._addAss(thirtyDollars);
    assertUnset.accept(mutable2);

    // Subtraction assignment
    final var mutable3 = new Money(tenPounds);
    mutable3._subAss(fivePounds);
    assertEquals("5.00#GBP", mutable3._string().state);

    // Multiplication assignment
    final var mutable4 = new Money(tenPounds);
    mutable4._mulAss(Integer._of(3));
    assertEquals("30.00#GBP", mutable4._string().state);

    final var mutable5 = new Money(tenPounds);
    mutable5._mulAss(Float._of(1.5));
    assertEquals("15.00#GBP", mutable5._string().state);

    // Division assignment
    final var mutable6 = new Money(tenPounds);
    mutable6._divAss(Integer._of(2));
    assertEquals("5.00#GBP", mutable6._string().state);

    final var mutable7 = new Money(tenPounds);
    mutable7._divAss(Float._of(4.0));
    assertEquals("2.50#GBP", mutable7._string().state);

    // Division by zero should unset
    final var mutable8 = new Money(tenPounds);
    mutable8._divAss(Integer._of(0));
    assertUnset.accept(mutable8);
  }

  @Test
  void testCopyMergeReplace() {
    // Copy operation
    final var target = new Money();
    target._copy(tenPounds);
    assertSet.accept(target);
    assertTrue.accept(target._eq(tenPounds));

    // Copy null should unset
    target._copy(null);
    assertUnset.accept(target);

    // Merge operation - unset target
    final var mergeTarget1 = new Money();
    mergeTarget1._merge(fivePounds);
    assertSet.accept(mergeTarget1);
    assertTrue.accept(mergeTarget1._eq(fivePounds));

    // Merge operation - same currency addition
    final var mergeTarget2 = new Money(fivePounds);
    mergeTarget2._merge(tenPounds);
    assertEquals("15.00#GBP", mergeTarget2._string().state);

    // Merge with different currency - you cannot do this it is meaningless.
    //If you want to replace then us replace/copy.
    final var mergeTarget3 = new Money(tenPounds);
    mergeTarget3._merge(thirtyDollars);
    assertUnset.accept(mergeTarget3);

    // Replace operation
    final var replaceTarget = new Money(tenPounds);
    replaceTarget._replace(fivePounds);
    assertTrue.accept(replaceTarget._eq(fivePounds));

    // Pipe operation (delegates to merge)
    final var pipeTarget = new Money();
    pipeTarget._pipe(tenPounds);
    assertTrue.accept(pipeTarget._eq(tenPounds));
  }

  @Test
  void testConversion() {
    // Convert to same currency (1:1 rate)
    final var converted1 = tenPounds.convert(fivePounds);
    assertSet.accept(converted1);
    assertEquals("10.00#GBP", converted1._string().state);

    final var unSetConversion = tenPounds.convert(new Money());
    assertUnset.accept(unSetConversion);

    // Convert with exchange rate
    final var convertedUSD = tenPounds.convert(Float._of(1.25), String._of("USD"));
    assertSet.accept(convertedUSD);
    assertEquals("12.50#USD", convertedUSD._string().state);

    // Convert with invalid currency code
    assertUnset.accept(tenPounds.convert(Float._of(1.0), String._of("XYZ")));

    // Convert unset money
    assertUnset.accept(unset.convert(Float._of(1.0), String._of("USD")));

    // Convert with unset parameters
    assertUnset.accept(tenPounds.convert(new Float(), String._of("USD")));
    assertUnset.accept(tenPounds.convert(Float._of(1.0), new String()));
  }

  @Test
  void testCurrencyValidation() {
    // Valid currency codes
    assertSet.accept(Money._of("100.00#USD"));
    assertSet.accept(Money._of("100.00#EUR"));
    assertSet.accept(Money._of("100.00#GBP"));
    assertSet.accept(Money._of("100.00#JPY"));
    assertSet.accept(Money._of("100.00#CAD"));

    // Invalid currency codes  
    assertUnset.accept(Money._of("100.00#ZZZ"));
    assertUnset.accept(Money._of("100.00#ABC"));
    assertUnset.accept(Money._of("100.00#123"));
  }

  @Test
  void testCurrencyDecimalPlaces() {
    // USD and GBP have 2 decimal places
    final var usd = Money._of("10.123#USD");
    assertSet.accept(usd);
    assertEquals("10.12#USD", usd._string().state); // Should round to 2 places

    // JPY has 0 decimal places
    final var jpy = Money._of("1000.5#JPY");
    assertSet.accept(jpy);
    assertEquals("1001#JPY", jpy._string().state); // Should round to 0 places

    // Test with specific currencies that have different decimal places
    // Note: Some currencies like CLF have 4 decimal places
    try {
      final var clf = Money._of("45.99999#CLF");
      if (clf._isSet().state) {
        assertEquals("46.0000#CLF", clf._string().state);
      }
    } catch (Exception _) {
      // CLF might not be available in all Java installations
    }
  }

  @Test
  void testRoundingBehavior() {
    // Test rounding with multiplication
    final var baseAmount = Money._of("49.75#GBP");
    final var doubled = baseAmount._mul(Float._of(2.0));
    assertSet.accept(doubled);
    assertEquals("99.50#GBP", doubled._string().state);

    // Test rounding with division (example from documentation: 49.755 becomes 49.76)
    final var amount = Money._of("99.51#GBP");
    final var halved = amount._div(Float._of(2.0));
    assertSet.accept(halved);
    assertEquals("49.76#GBP", halved._string().state); // Should round up

    // Test complex calculation from documentation
    final var fortyEightSeventySix = Money._of("49.76#GBP");
    final var result = fortyEightSeventySix._mul(Float._of(-8.754));
    assertSet.accept(result);
    assertEquals("-435.60#GBP", result._string().state);
  }

  @Test
  void testEdgeCases() {
    // Zero amounts
    assertSet.accept(zeroPounds);
    assertEquals("0.00#GBP", zeroPounds._string().state);

    // Very large amounts
    final var large = Money._of("999999999.99#USD");
    assertSet.accept(large);

    // Very small amounts
    final var small = Money._of("0.01#USD");
    assertSet.accept(small);
    assertEquals("0.01#USD", small._string().state);

    // Negative amounts
    assertSet.accept(negativePounds);
    assertEquals("-25.50#GBP", negativePounds._string().state);

    // Operations with zero
    final var addZero = tenPounds._add(zeroPounds);
    assertTrue.accept(addZero._eq(tenPounds));

    final var subZero = tenPounds._sub(zeroPounds);
    assertTrue.accept(subZero._eq(tenPounds));

    final var mulZero = tenPounds._mul(Integer._of(0));
    assertTrue.accept(mulZero._eq(zeroPounds));
  }

  @Test
  void testCrossCurrencyOperationFailures() {
    // All cross-currency operations should return unset
    assertUnset.accept(tenPounds._add(thirtyDollars));
    assertUnset.accept(tenPounds._sub(thirtyDollars));
    assertUnset.accept(tenPounds._eq(thirtyDollars));
    assertUnset.accept(tenPounds._lt(thirtyDollars));
    assertUnset.accept(tenPounds._gt(thirtyDollars));
    assertUnset.accept(tenPounds._cmp(thirtyDollars));
    assertUnset.accept(tenPounds._fuzzy(thirtyDollars));
    assertUnset.accept(tenPounds._div(thirtyDollars));

    // Assignment operations with different currencies should unset
    final var mutable = new Money(tenPounds);
    assertNotNull(mutable);
    mutable._addAss(thirtyDollars);
    assertUnset.accept(mutable);
  }

  @Test
  void testUnsetPropagation() {
    // All operations with unset should return unset
    assertUnset.accept(unset._add(tenPounds));
    assertUnset.accept(tenPounds._add(unset));
    assertUnset.accept(unset._sub(tenPounds));
    assertUnset.accept(unset._mul(Integer._of(2)));
    assertUnset.accept(unset._div(Integer._of(2)));
    assertUnset.accept(unset._eq(tenPounds));
    assertUnset.accept(unset._lt(tenPounds));
    assertUnset.accept(unset._cmp(tenPounds));
    assertUnset.accept(unset._sqrt());
    assertUnset.accept(unset._abs());
    assertUnset.accept(unset._negate());
  }

  @Test
  void testAmountCurrencyExtraction() {
    // Test amount extraction (#<)
    final var amount = tenPounds._amount();
    assertSet.accept(amount);
    assertTrue.accept(Boolean._of(Math.abs(amount.state - 10.0) < 0.001));

    // Test currency extraction (#>)
    final var currency = tenPounds._currency();
    assertSet.accept(currency);
    assertEquals("GBP", currency.state);

    // Test with different currency and decimal places
    final var usd = Money._of("25.50#USD");
    final var usdAmount = usd._amount();
    final var usdCurrency = usd._currency();
    assertSet.accept(usdAmount);
    assertSet.accept(usdCurrency);
    assertTrue.accept(Boolean._of(Math.abs(usdAmount.state - 25.5) < 0.001));
    assertEquals("USD", usdCurrency.state);

    // Test with JPY (0 decimal places)
    final var jpy = Money._of("1000#JPY");
    final var jpyAmount = jpy._amount();
    final var jpyCurrency = jpy._currency();
    assertSet.accept(jpyAmount);
    assertSet.accept(jpyCurrency);
    assertTrue.accept(Boolean._of(Math.abs(jpyAmount.state - 1000.0) < 0.001));
    assertEquals("JPY", jpyCurrency.state);

    // Test with unset Money
    assertUnset.accept(unset._amount());
    assertUnset.accept(unset._currency());

    // Test with zero amount
    final var zeroAmount = zeroPounds._amount();
    final var zeroCurrency = zeroPounds._currency();
    assertSet.accept(zeroAmount);
    assertSet.accept(zeroCurrency);
    assertTrue.accept(Boolean._of(Math.abs(zeroAmount.state - 0.0) < 0.001));
    assertEquals("GBP", zeroCurrency.state);
  }

  @Test
  void testIncrementDecrement() {
    // Test increment with GBP (2 decimal places)
    final var incremented = tenPounds._inc();
    assertSet.accept(incremented);
    assertEquals("10.01#GBP", incremented._string().state);

    // Test decrement with GBP
    final var decremented = tenPounds._dec();
    assertSet.accept(decremented);
    assertEquals("10.00#GBP", decremented._string().state);

    // Test increment with JPY (0 decimal places)
    final var jpy = Money._of("1000#JPY");
    final var jpyIncremented = jpy._inc();
    assertSet.accept(jpyIncremented);
    assertEquals("1001#JPY", jpyIncremented._string().state);

    final var jpyDecremented = jpy._dec();
    assertSet.accept(jpyDecremented);
    assertEquals("1000#JPY", jpyDecremented._string().state);

    // Test multiple increments
    final var twice = tenPounds._inc()._inc();
    assertSet.accept(twice);
    assertEquals("10.02#GBP", twice._string().state);

    // Test multiple decrements
    final var twiceDown = tenPounds._dec()._dec();
    assertSet.accept(twiceDown);
    assertEquals("10.00#GBP", twiceDown._string().state);

    // Test with zero amount
    final var zeroIncremented = zeroPounds._inc();
    assertSet.accept(zeroIncremented);
    assertEquals("0.01#GBP", zeroIncremented._string().state);

    final var zeroDecremented = zeroPounds._dec();
    assertSet.accept(zeroDecremented);
    assertEquals("0.00#GBP", zeroDecremented._string().state);

    // Test with unset Money
    assertUnset.accept(unset._inc());
    assertUnset.accept(unset._dec());

    // Test with negative amounts
    final var negativeIncremented = negativePounds._inc();
    assertSet.accept(negativeIncremented);
    assertEquals("-25.49#GBP", negativeIncremented._string().state);

    final var negativeDecremented = negativePounds._dec();
    assertSet.accept(negativeDecremented);
    assertEquals("-25.50#GBP", negativeDecremented._string().state);
  }

  @Test
  void testEmptyOperator() {
    // Test with zero amounts - should be empty
    assertTrue.accept(zeroPounds._empty());

    // Test with non-zero amounts - should not be empty
    assertFalse.accept(tenPounds._empty());
    assertFalse.accept(fivePounds._empty());
    assertFalse.accept(negativePounds._empty());

    // Test with different currencies
    final var zeroUSD = Money._of("0.00#USD");
    assertTrue.accept(zeroUSD._empty());

    final var zeroJPY = Money._of("0#JPY");
    assertTrue.accept(zeroJPY._empty());

    final var nonZeroUSD = Money._of("1.00#USD");
    assertFalse.accept(nonZeroUSD._empty());

    // Test with unset Money - should return unset Boolean
    assertUnset.accept(unset._empty());

    // Compare with _isSet behavior
    assertTrue.accept(zeroPounds._isSet()); // Zero is set
    assertTrue.accept(zeroPounds._empty()); // Zero is empty

    assertFalse.accept(unset._isSet()); // Unset is not set
    assertUnset.accept(unset._empty()); // Unset empty is unset

    // Test edge case: very small amount that rounds to zero
    final var verySmall = Money._of("0.000001#USD");
    // Should be rounded to 0.00 for USD (2 decimal places)
    assertTrue.accept(verySmall._empty());
    assertEquals("0.00#USD", verySmall._string().state);
  }

  @Test
  void testCurrencySpecificIncrements() {
    // Test different currency decimal place behaviors
    //Remember ++ and -- mutate the value - they are not pure.

    // USD/EUR/GBP: 2 decimal places, increment by 0.01
    final var usd = Money._of("5.99#USD");
    final var usdInc = usd._inc();
    assertEquals("6.00#USD", usdInc._string().state);

    // JPY: 0 decimal places, increment by 1
    final var jpy = Money._of("999#JPY");
    final var jpyInc = jpy._inc();
    assertEquals("1000#JPY", jpyInc._string().state);

    // Test currencies with different decimal places if available
    try {
      // Test CLF (Chilean Unidad de Fomento) - has 4 decimal places
      final var clf = Money._of("99.9999#CLF");
      if (clf._isSet().state) {
        final var clfInc = clf._inc();
        assertEquals("100.0000#CLF", clfInc._string().state);
      }
    } catch (Exception _) {
      // CLF might not be available in all Java installations - that's ok
    }

    // Test increment around currency boundaries
    final var almostDollar = Money._of("0.99#USD");
    final var dollar = almostDollar._inc();
    assertEquals("1.00#USD", dollar._string().state);

    // Test decrement around currency boundaries
    final var oneDollar = Money._of("1.00#USD");
    final var cents = oneDollar._dec();
    assertEquals("0.99#USD", cents._string().state);

    // Test with negative amounts crossing zero
    final var negativeCent = Money._of("-0.01#USD");
    final var zero = negativeCent._inc();
    assertEquals("0.00#USD", zero._string().state);
    assertTrue.accept(zero._empty());

    final var backToNegative = zero._dec();
    assertEquals("-0.01#USD", backToNegative._string().state);

    // Test large increments don't affect precision
    final var large = Money._of("999999.98#USD");
    final var largeInc = large._inc();
    assertEquals("999999.99#USD", largeInc._string().state);

    final var largeInc2 = largeInc._inc();
    assertEquals("1000000.00#USD", largeInc2._string().state);

    // Test precision is maintained with JPY (integer amounts)
    final var jpyZero = Money._of("0#JPY");
    final var jpyOne = jpyZero._inc();
    assertEquals("1#JPY", jpyOne._string().state);

    final var jpyMinusOne = jpyZero._dec();
    assertEquals("0#JPY", jpyMinusOne._string().state);

    // Verify increment/decrement maintains currency
    assertTrue.accept(usdInc._currency()._eq(String._of("USD")));
    assertTrue.accept(jpyInc._currency()._eq(String._of("JPY")));
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var tenPoundsJson = tenPounds._json();
    assertNotNull(tenPoundsJson);
    assertSet.accept(tenPoundsJson);

    final var thirtyDollarsJson = thirtyDollars._json();
    assertSet.accept(thirtyDollarsJson);

    final var zeroPoundsJson = zeroPounds._json();
    assertSet.accept(zeroPoundsJson);

    // Test JSON conversion with unset value
    assertUnset.accept(unset._json());
  }
}