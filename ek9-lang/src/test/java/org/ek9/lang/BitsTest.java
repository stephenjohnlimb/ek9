package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class BitsTest extends Common {

  // Test data from documentation examples
  private final Bits bits010011 = Bits._of("0b010011");  // 19 decimal
  private final Bits bits101010 = Bits._of("0b101010");  // 42 decimal
  private final Bits unsetBits = new Bits();
  private final Bits bitsSingle = Bits._of("0b1");

  @Test
  void testConstruction() {
    // Default constructor creates unset
    final var defaultConstructor = new Bits();
    assertUnset.accept(defaultConstructor);

    // String constructor with 0b prefix
    final var withPrefix = new Bits(String._of("0b010011"));
    assertSet.accept(withPrefix);
    assertEquals("010011", withPrefix.toString());

    // String constructor without prefix
    final var withoutPrefix = new Bits(String._of("010011"));
    assertSet.accept(withoutPrefix);
    assertEquals("010011", withoutPrefix.toString());

    // Copy constructor
    final var copied = new Bits(bits010011);
    assertSet.accept(copied);
    assertEquals(bits010011, copied);

    // Boolean constructor
    final var fromTrue = new Bits(Boolean._of(true));
    assertSet.accept(fromTrue);
    assertEquals("1", fromTrue.toString());

    final var fromFalse = new Bits(Boolean._of(false));
    assertSet.accept(fromFalse);
    assertEquals("0", fromFalse.toString());

    // Null/unset handling
    final var fromNull = new Bits((Bits) null);
    assertUnset.accept(fromNull);

    final var fromUnsetString = new Bits(new String());
    assertUnset.accept(fromUnsetString);

    final var fromUnsetBoolean = new Bits(new Boolean());
    assertUnset.accept(fromUnsetBoolean);

    // Colour constructor
    final var redColour = Colour._of("FF0000");
    final var fromColour = new Bits(redColour);
    assertSet.accept(fromColour);

    // Verify roundtrip conversion: Bits(colour) should equal colour.bits()
    final var colourBits = redColour.bits();
    assertSet.accept(colourBits);
    assertEquals(colourBits, fromColour);
    assertEquals(colourBits.toString(), fromColour.toString());

    // Null/unset Colour handling
    final var fromUnsetColour = new Bits(new Colour());
    assertUnset.accept(fromUnsetColour);

    assertEquals("", Bits._of().toString());
    assertEquals("1", Bits._of(true).toString());
    assertEquals("0", Bits._of(false).toString());

    assertEquals("010011", Bits._of(withoutPrefix).toString());
  }

  @Test
  void testStringParsing() {
    // Valid binary strings
    final var empty = Bits._of("");
    assertSet.accept(empty);
    assertTrue.accept(empty._empty());

    final var single = Bits._of("1");
    assertSet.accept(single);
    assertEquals("1", single.toString());

    final var multi = Bits._of("101010");
    assertSet.accept(multi);
    assertEquals("101010", multi.toString());

    // Invalid strings should create unset Bits
    final var invalid1 = Bits._of("abc");
    assertUnset.accept(invalid1);

    final var invalid2 = Bits._of("0b");
    assertUnset.accept(invalid2);

    final var invalid3 = Bits._of("012");
    assertUnset.accept(invalid3);

    assertUnset.accept(Bits._of((java.lang.String) null));
  }

  @Test
  void testStateManagement() {
    // Test _isSet operator
    assertFalse.accept(unsetBits._isSet());
    assertTrue.accept(bits010011._isSet());

    // Test _empty operator
    assertTrue.accept(Bits._of("")._empty());
    assertFalse.accept(bits010011._empty());
    assertUnset.accept(unsetBits._empty());

    // Test _length operator
    assertEquals(6L, bits010011._len().state);
    assertEquals(6L, bits101010._len().state);
    assertEquals(1L, bitsSingle._len().state);
    assertUnset.accept(unsetBits._len());
  }

  @Test
  void testFirstAndLastBits() {
    // Test first bit (LSB/rightmost)
    assertNotNull(bits010011);
    assertNotNull(bits101010);

    assertTrue.accept(bits010011._prefix()); // 010011 -> rightmost is 1
    assertFalse.accept(bits101010._prefix()); // 101010 -> rightmost is 0

    // Test last bit (MSB/leftmost)  
    assertFalse.accept(bits010011._suffix()); // 010011 -> leftmost is 0
    assertTrue.accept(bits101010._suffix()); // 101010 -> leftmost is 1

    // Unset handling
    assertUnset.accept(unsetBits._prefix());
    assertUnset.accept(unsetBits._suffix());
  }

  @Test
  void testConcatenationNotArithmetic() {
    // Documentation example: 0b010011 + 0b101010 = 0b010011101010
    final var result = bits010011._add(bits101010);
    assertSet.accept(result);
    assertEquals("010011101010", result.toString());

    // Verify NOT arithmetic (19 + 42 != result)
    assertNotEquals("111101", result.toString()); // 61 in binary

    // Test reverse concatenation
    final var reverse = bits101010._add(bits010011);
    assertEquals("101010010011", reverse.toString());
    assertNotEquals(result.toString(), reverse.toString());
  }

  @Test
  void testBooleanConcatenation() {
    // Documentation examples:
    // 0b010011 + true = 0b0100111
    final var withTrue = bits010011._add(Boolean._of(true));
    assertEquals("0100111", withTrue.toString());

    // 0b010011 + false = 0b0100110  
    final var withFalse = bits010011._add(Boolean._of(false));
    assertEquals("0100110", withFalse.toString());

    // Edge case: empty + boolean
    final var emptyBits = Bits._of("");
    final var emptyPlusTrue = emptyBits._add(Boolean._of(true));
    assertEquals("1", emptyPlusTrue.toString());
  }

  @Test
  void testComparisonFromDocs() {
    // Examples from HTML documentation: a=0b010011, b=0b101010
    assertFalse.accept(bits010011._eq(bits101010));     // a == b -> false
    assertTrue.accept(bits010011._neq(bits101010));     // a <> b -> true  
    assertTrue.accept(bits010011._lt(bits101010));      // a < b -> true
    assertTrue.accept(bits010011._lteq(bits101010));    // a <= b -> true
    assertFalse.accept(bits010011._gt(bits101010));     // a > b -> false
    assertFalse.accept(bits010011._gteq(bits101010));   // a >= b -> false

    //Now just check the other way
    assertFalse.accept(bits101010._eq(bits010011));
    assertTrue.accept(bits101010._neq(bits010011));
    assertFalse.accept(bits101010._lt(bits010011));
    assertFalse.accept(bits101010._lteq(bits010011));
    assertTrue.accept(bits101010._gt(bits010011));
    assertTrue.accept(bits101010._gteq(bits010011));

    assertUnset.accept(unsetBits._eq(bits010011));
    assertUnset.accept(unsetBits._neq(bits010011));
    assertUnset.accept(unsetBits._lt(bits010011));
    assertUnset.accept(unsetBits._lteq(bits010011));
    assertUnset.accept(unsetBits._gt(bits010011));
    assertUnset.accept(unsetBits._gteq(bits010011));
    // Test equality
    final var copy = Bits._of("0b010011");
    assertNotNull(copy);
    assertTrue.accept(bits010011._eq(copy));
    assertTrue.accept(copy._eq(bits010011));
  }

  @Test
  void testComparison() {
    // Same length comparison
    assertEquals(-1L, bits010011._cmp(bits101010).state); // 010011 < 101010
    assertEquals(1L, bits101010._cmp(bits010011).state);  // 101010 > 010011
    assertEquals(0L, bits010011._cmp(bits010011).state);  // equal

    // Different length comparison (longer is typically greater)
    final var longer = Bits._of("1010101");
    assertTrue.accept(longer._gt(bits010011));
    assertTrue.accept(bits010011._lt(longer));

    // Fuzzy comparison is same as regular comparison for bits
    assertEquals(bits010011._cmp(bits101010).state, bits010011._fuzzy(bits101010).state);
  }

  @Test
  void testBitwiseFromDocs() {
    // Documentation examples: set6=0b010011, set7=0b101010

    // OR result: 0b111011
    final var ored = bits010011._or(bits101010);
    assertEquals("111011", ored.toString());

    // XOR result: 0b111001  
    final var xored = bits010011._xor(bits101010);
    assertEquals("111001", xored.toString());

    // AND result: 0b000010
    final var anded = bits010011._and(bits101010);
    assertEquals("000010", anded.toString());

    // NOT result: 0b101100
    final var notted = bits010011._negate();
    assertEquals("101100", notted.toString());

    assertUnset.accept(unsetBits._negate());
  }

  @Test
  void testBitwiseOperations() {
    // Test with different lengths
    final var short1 = Bits._of("101");
    final var long1 = Bits._of("010011");

    // AND pads shorter with zeros
    final var andResult = short1._and(long1);
    assertEquals("000001", andResult.toString()); // 101 AND 010011 with zero padding

    // OR combines all bits
    final var orResult = short1._or(long1);
    assertEquals("010111", orResult.toString()); // 101 OR 010011 with zero padding

    // XOR
    final var xorResult = short1._xor(long1);
    assertEquals("010110", xorResult.toString()); // 101 XOR 010011 with zero padding
  }

  @Test
  void testShiftOperations() {
    // Left shift (adds zeros on right)
    final var leftShift = bits010011._shiftLeft(Integer._of(2));
    assertEquals("01001100", leftShift.toString());

    // Right shift (removes bits from right)
    final var rightShift = bits010011._shiftRight(Integer._of(2));
    assertEquals("0100", rightShift.toString());

    // Shift beyond length
    final var shiftBeyond = bits010011._shiftRight(Integer._of(10));
    assertTrue.accept(shiftBeyond._empty());

    // Negative shift should return unset
    final var negativeShift = bits010011._shiftLeft(Integer._of(-1));
    assertUnset.accept(negativeShift);
  }

  @Test
  void testIteratorLSBFirst() {
    // 0b010011 should stream as: [true, true, false, false, true, false]
    final var iter = bits010011.iterator();

    // Verify return type is parameterized Iterator of Boolean
    assertNotNull(iter);
    assertSet.accept(iter);
    assertInstanceOf(_Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65.class, iter);

    final var bits = new ArrayList<Boolean>();

    while (iter.hasNext()._isSet().state && iter.hasNext().state) {
      final var next = iter.next();
      // With parameterized Iterator of Boolean, next() returns Boolean directly
      assertNotNull(next);
      assertInstanceOf(Boolean.class, next);
      bits.add(next);
    }

    // Verify LSB-first order (right to left)
    assertEquals(6, bits.size());
    assertTrue.accept(Boolean._of(bits.get(0).state));   // rightmost bit (LSB) - position 0
    assertTrue.accept(Boolean._of(bits.get(1).state));   // position 1  
    assertFalse.accept(Boolean._of(bits.get(2).state));  // position 2
    assertFalse.accept(Boolean._of(bits.get(3).state));  // position 3
    assertTrue.accept(Boolean._of(bits.get(4).state));   // position 4
    assertFalse.accept(Boolean._of(bits.get(5).state));  // leftmost bit (MSB) - position 5

    // Test iterator on unset Bits
    final var unsetIterator = unsetBits.iterator();
    assertNotNull(unsetIterator);
    assertUnset.accept(unsetIterator);
    assertFalse.accept(unsetIterator.hasNext());
  }

  @Test
  void testAssignmentOperators() {
    // Test += for concatenation (from docs)
    final var mutable = Bits._of("010011");
    mutable._addAss(Boolean._of(true));
    assertEquals("0100111", mutable.toString()); // 010011 + true = 0100111

    // Test copy operator
    final var target1 = new Bits();
    target1._copy(bits010011);
    assertEquals(bits010011, target1);

    final var target2 = new Bits();
    target2._replace(bits010011);
    assertEquals(bits010011, target2);

    // Test merge operator
    final var mergeTarget = new Bits();
    mergeTarget._merge(bits010011);
    assertEquals(bits010011, mergeTarget);

    // Test merge with concatenation
    final var setTarget = Bits._of("111");
    setTarget._merge(bits010011);
    assertEquals("111010011", setTarget.toString()); // Concatenation when both set
  }

  @Test
  void testPipeOperations() {
    final var pipeline = new Bits();

    // Pipe ignores unset and accumulates set values
    pipeline._pipe(unsetBits); // Should ignore unset
    assertUnset.accept(pipeline);

    pipeline._pipe(bits010011); // First value
    assertEquals(bits010011, pipeline);

    pipeline._pipe(bits101010); // Concatenate
    assertEquals("010011101010", pipeline.toString());
  }

  @Test
  void testUnsetPropagation() {
    // Operations with unset operands should return unset
    assertNotNull(unsetBits);
    assertNotNull(bits010011);
    assertUnset.accept(unsetBits._add(bits010011));
    assertUnset.accept(bits010011._add(unsetBits));
    assertUnset.accept(unsetBits._and(bits010011));
    assertUnset.accept(bits010011._or(unsetBits));
    assertUnset.accept(unsetBits._xor(bits010011));
    assertUnset.accept(unsetBits._sub(bits010011));

    // Comparisons with unset return unset
    assertUnset.accept(unsetBits._eq(bits010011));
    assertUnset.accept(bits010011._lt(unsetBits));
    assertUnset.accept(unsetBits._cmp(bits010011));
    assertUnset.accept(bits010011._cmp(new Any() {
    }));
  }

  @Test
  void testString() {
    // String representation
    final var str = bits010011._string();
    assertSet.accept(str);
    assertEquals("010011", str.state);

    // Unset string
    assertUnset.accept(unsetBits._string());

    // JSON operations
    final var bitsJson = bits010011._json();
    assertSet.accept(bitsJson);

    assertUnset.accept(unsetBits._json());

    // Hash code
    final var hash = bits010011._hashcode();
    assertSet.accept(hash);

    // Equal objects have equal hash codes
    final var copy = Bits._of("010011");
    assertEquals(bits010011._hashcode().state, copy._hashcode().state);
  }

  @Test
  void testEdgeCases() {
    // Empty bits
    final var empty = Bits._of("");
    assertSet.accept(empty);
    assertTrue.accept(empty._empty());
    assertEquals(0L, empty._len().state);

    // Single bit
    final var single = Bits._of("1");
    assertEquals(1L, single._len().state);
    assertTrue.accept(single._prefix());
    assertTrue.accept(single._suffix());

    // Very long bit string
    final var longBits = Bits._of("10101010101010101010");
    assertEquals(20L, longBits._len().state);

    // Operations with empty bits
    final var emptyPlus = empty._add(bits010011);
    assertEquals("010011", emptyPlus.toString());

    final var emptyAnd = empty._and(bits010011);
    assertTrue.accept(emptyAnd._empty());
  }

  @Test
  void testAdditionOperatorComprehensive() {
    // Test Bits + Bits concatenation with various combinations
    final var bits1 = Bits._of("1");
    final var bits0 = Bits._of("0");
    final var bitsLong = Bits._of("11110000");

    // Single bit concatenations
    assertEquals("10", bits1._add(bits0).toString());
    assertEquals("01", bits0._add(bits1).toString());
    assertEquals("11", bits1._add(bits1).toString());
    assertEquals("00", bits0._add(bits0).toString());

    // Different length concatenations
    assertEquals("111100001", bitsLong._add(bits1).toString());
    assertEquals("111100000", bitsLong._add(bits0).toString());
    assertEquals("111110000", bits1._add(bitsLong).toString());
    assertEquals("011110000", bits0._add(bitsLong).toString());

    // Multiple concatenations
    final var triple = bits1._add(bits0)._add(bits1);
    assertEquals("101", triple.toString());

    // Empty bits concatenation
    final var empty = Bits._of("");
    assertEquals("1", empty._add(bits1).toString());
    assertEquals("0", empty._add(bits0).toString());
    assertEquals("1", bits1._add(empty).toString());
    assertEquals("0", bits0._add(empty).toString());
    assertEquals("", empty._add(empty).toString());

    // Concatenation maintains bit order
    final var pattern1 = Bits._of("1010");
    final var pattern2 = Bits._of("0101");
    assertEquals("10100101", pattern1._add(pattern2).toString());
    assertEquals("01011010", pattern2._add(pattern1).toString());
  }

  @Test
  void testBooleanAdditionOperatorComprehensive() {
    // Test Bits + Boolean concatenation with various bit patterns
    final var bits1 = Bits._of("1");
    final var bits0 = Bits._of("0");
    final var bitsPattern = Bits._of("1010");
    final var empty = Bits._of("");

    // Single bit + boolean
    assertEquals("11", bits1._add(Boolean._of(true)).toString());
    assertEquals("10", bits1._add(Boolean._of(false)).toString());
    assertEquals("01", bits0._add(Boolean._of(true)).toString());
    assertEquals("00", bits0._add(Boolean._of(false)).toString());

    // Pattern + boolean
    assertEquals("10101", bitsPattern._add(Boolean._of(true)).toString());
    assertEquals("10100", bitsPattern._add(Boolean._of(false)).toString());

    // Empty + boolean
    assertEquals("1", empty._add(Boolean._of(true)).toString());
    assertEquals("0", empty._add(Boolean._of(false)).toString());

    // Chain boolean additions
    final var chained = empty._add(Boolean._of(true))._add(Boolean._of(false))._add(Boolean._of(true));
    assertEquals("101", chained.toString());

    // Boolean addition with unset boolean
    final var unsetBool = new Boolean();
    assertUnset.accept(bits1._add(unsetBool));
    assertUnset.accept(empty._add(unsetBool));
  }

  @Test
  void testAdditionAssignmentOperator() {
    // Test += operator for Bits
    final var mutable1 = Bits._of("101");
    mutable1._addAss(Bits._of("010"));
    assertEquals("101010", mutable1.toString());

    // Test += operator for Boolean
    final var mutable2 = Bits._of("11");
    mutable2._addAss(Boolean._of(false));
    assertEquals("110", mutable2.toString());

    mutable2._addAss(Boolean._of(true));
    assertEquals("1101", mutable2.toString());

    // Test += with empty bits
    final var mutable3 = Bits._of("");
    mutable3._addAss(Bits._of("1"));
    assertEquals("1", mutable3.toString());

    // Test += with unset operands
    final var mutable4 = Bits._of("1");
    mutable4._addAss(new Bits()); // unset
    assertUnset.accept(mutable4);

    final var mutable5 = Bits._of("1");
    mutable5._addAss(new Boolean()); // unset
    assertUnset.accept(mutable5);
  }

  @Test
  void testSubtractionOperatorComprehensive() {
    // Test Bits - Bits (XOR difference operation)
    final var bits1010 = Bits._of("1010");
    final var bits0110 = Bits._of("0110");
    final var bits1100 = Bits._of("1100");

    // XOR operations (subtraction as difference)
    assertEquals("1100", bits1010._sub(bits0110).toString()); // 1010 XOR 0110 = 1100
    assertEquals("1100", bits0110._sub(bits1010).toString()); // XOR is commutative
    assertEquals("0110", bits1010._sub(bits1100).toString()); // 1010 XOR 1100 = 0110

    // Different length XOR with zero-padding
    final var shortBits = Bits._of("10");
    final var longBits = Bits._of("1010");
    assertEquals("1000", shortBits._sub(longBits).toString()); // 10 XOR 1010 (zero-padded) = 1000
    assertEquals("1000", longBits._sub(shortBits).toString()); // Commutative

    // XOR with identical bits results in zeros
    assertEquals("0000", bits1010._sub(bits1010).toString());
    assertEquals("0000", bits0110._sub(bits0110).toString());

    // XOR with all zeros
    final var allZeros = Bits._of("0000");
    assertEquals("1010", bits1010._sub(allZeros).toString());
    assertEquals("1010", allZeros._sub(bits1010).toString());

    // XOR with all ones
    final var allOnes = Bits._of("1111");
    assertEquals("0101", bits1010._sub(allOnes).toString()); // Inverts bits
    assertEquals("0101", allOnes._sub(bits1010).toString());

    // Single bit XOR
    final var bit1 = Bits._of("1");
    final var bit0 = Bits._of("0");
    assertEquals("1", bit1._sub(bit0).toString());
    assertEquals("1", bit0._sub(bit1).toString());
    assertEquals("0", bit1._sub(bit1).toString());
    assertEquals("0", bit0._sub(bit0).toString());
  }

  @Test
  void testSubtractionAssignmentOperator() {
    // Test -= operator (XOR assignment)
    final var mutable1 = Bits._of("1010");
    mutable1._subAss(Bits._of("0110"));
    assertEquals("1100", mutable1.toString());

    // Test -= with same value (should become zeros)
    final var mutable2 = Bits._of("1111");
    mutable2._subAss(Bits._of("1111"));
    assertEquals("0000", mutable2.toString());

    // Test -= with different lengths
    final var mutable3 = Bits._of("11");
    mutable3._subAss(Bits._of("1010"));
    assertEquals("1001", mutable3.toString());

    // Test -= with unset operand
    final var mutable4 = Bits._of("101");
    mutable4._subAss(new Bits()); // unset
    assertUnset.accept(mutable4);
  }

  @Test
  void testAdditionSubtractionEdgeCases() {
    // Test with maximum practical bit lengths
    final var longBits1 = Bits._of("1".repeat(64));
    final var longBits2 = Bits._of("0".repeat(64));

    // Long concatenation
    final var longResult = longBits1._add(longBits2);
    assertEquals(128, longResult._len().state);
    assertEquals("1".repeat(64) + "0".repeat(64), longResult.toString());

    // Long XOR
    final var longXor = longBits1._sub(longBits2);
    assertEquals(64, longXor._len().state);
    assertEquals("1".repeat(64), longXor.toString());

    // Mixed operations
    final var mixed = Bits._of("10")._add(Boolean._of(true))._sub(Bits._of("111"));
    assertEquals("010", mixed.toString()); // 101 XOR 111 = 010

    // Zero-length operations
    final var empty = Bits._of("");
    assertEquals("", empty._sub(empty).toString());
    assertEquals("101", empty._add(Bits._of("101")).toString());
    assertEquals("101", Bits._of("101")._add(empty).toString());
    assertEquals("101", empty._sub(Bits._of("101")).toString()); // Empty XOR anything = anything
  }

  @Test
  void testErrorConditions() {
    // Invalid operations should be handled gracefully
    final var result = bits010011._shiftRight(Integer._of(-5));
    assertNotNull(result);
    assertUnset.accept(result);

    // Null argument handling
    final var nullAdd = bits010011._add((Bits) null);
    assertUnset.accept(nullAdd);

    final var nullSub = bits010011._sub(null);
    assertUnset.accept(nullSub);

    final var nullAddBool = bits010011._add((Boolean) null);
    assertUnset.accept(nullAddBool);
  }

  @Test
  void testEqualsAndHashCode() {
    // Test equals contract
    assertEquals(bits010011, bits010011); // reflexive
    assertEquals(bits010011, Bits._of("010011")); // symmetric
    assertNotEquals(bits010011, bits101010);
    assertNotEquals(null, bits010011);
    assertNotEquals("not a bits", bits010011);

    //No point in comparing to unset bits, they are unset and so that has no meaning.
    assertUnset.accept(new Bits());

    // Hash code consistency
    assertEquals(bits010011.hashCode(), Bits._of("010011").hashCode());
    assertNotEquals(bits010011.hashCode(), bits101010.hashCode());
  }
}