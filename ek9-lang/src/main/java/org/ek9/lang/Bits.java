package org.ek9.lang;

import java.util.BitSet;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Represents the Bits type in EK9.
 * <p>
 * Bits are variable-length bit sequences that support concatenation, bitwise operations,
 * and streaming. Unlike numbers, addition means concatenation (not arithmetic).
 * Bits are ordered right-to-left (LSB rightmost, MSB leftmost).
 * </p>
 * <p>
 * See <a href="https://www.ek9.io/builtInTypes.html#bits">EK9 Bits</a>.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Bits as open""")
public class Bits extends BuiltinType {

  BitSet state;
  int length; // Track actual bit count since BitSet doesn't preserve trailing zeros

  @Ek9Constructor("""
      Bits() as pure""")
  public Bits() {
    super.unSet();
  }

  @Ek9Constructor("""
      Bits() as pure
        -> arg0 as Bits""")
  public Bits(Bits arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0);
    }
  }

  @Ek9Constructor("""
      Bits() as pure
        -> arg0 as String""")
  public Bits(String arg0) {
    unSet();
    if (isValid(arg0)) {
      final var parsed = parseBitString(arg0.state);
      if (parsed != null) {
        assign(parsed.state, parsed.length);
      }
    }
  }

  @Ek9Constructor("""
      Bits() as pure
        -> arg0 as Boolean""")
  public Bits(Boolean arg0) {
    unSet();
    if (isValid(arg0)) {
      final var bitSet = new BitSet(1);
      if (arg0.state) {
        bitSet.set(0);
      }
      assign(bitSet, 1);
    }
  }

  @Ek9Constructor("""
      Bits() as pure
        -> arg0 as Colour""")
  public Bits(Colour arg0) {
    unSet();
    if (isValid(arg0)) {
      final var colourBits = arg0.bits();
      if (isValid(colourBits)) {
        assign(colourBits);
      }
    }
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (isSet) {
      return new JSON(this);
    }
    return new JSON();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(toBitString());
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      rtn.assign(java.util.Objects.hash(state, length));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (isSet) {
      return Boolean._of(length == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    if (isSet) {
      return Integer._of(length);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator #< as pure
        <- rtn as Boolean?""")
  public Boolean _prefix() {
    // First bit (rightmost/LSB)
    if (isSet && length > 0) {
      return Boolean._of(state.get(0));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as Boolean?""")
  public Boolean _suffix() {
    // Last bit (leftmost/MSB)
    if (isSet && length > 0) {
      return Boolean._of(state.get(length - 1));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Bits
        <- rtn as Bits?""")
  public Bits _add(Bits arg) {
    // Concatenation: this + arg (NOT arithmetic addition)
    // Example: 0b010011 + 0b101010 = 0b010011101010
    if (canProcess(arg)) {
      final var result = new BitSet(this.length + arg.length);

      // Copy these bits (left operand) to the left side of result
      for (int i = 0; i < this.length; i++) {
        if (this.state.get(i)) {
          result.set(i + arg.length);
        }
      }

      // Copy arg bits (right operand) to the right side of result
      for (int i = 0; i < arg.length; i++) {
        if (arg.state.get(i)) {
          result.set(i);
        }
      }

      final var rtn = _new();
      rtn.assign(result, this.length + arg.length);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Boolean
        <- rtn as Bits?""")
  public Bits _add(Boolean arg) {
    // Concatenate single boolean as bit
    // Example: 0b010011 + true = 0b0100111
    if (canProcess(arg)) {
      final var result = new BitSet(this.length + 1);

      // Copy existing bits shifted left by 1
      for (int i = 0; i < this.length; i++) {
        if (this.state.get(i)) {
          result.set(i + 1);
        }
      }

      // Add new bit at position 0 (rightmost)
      if (arg.state) {
        result.set(0);
      }

      final var rtn = _new();
      rtn.assign(result, this.length + 1);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Bits
        <- rtn as Bits?""")
  public Bits _sub(Bits arg) {
    // Difference operation (likely XOR based on bitwise context)
    return _xor(arg);
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Bits
        <- rtn as Boolean?""")
  public Boolean _lt(Bits arg) {
    if (canProcess(arg)) {
      return Boolean._of(compareNumeric(arg) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Bits
        <- rtn as Boolean?""")
  public Boolean _lteq(Bits arg) {
    if (canProcess(arg)) {
      return Boolean._of(compareNumeric(arg) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Bits
        <- rtn as Boolean?""")
  public Boolean _gt(Bits arg) {
    if (canProcess(arg)) {
      return Boolean._of(compareNumeric(arg) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Bits
        <- rtn as Boolean?""")
  public Boolean _gteq(Bits arg) {
    if (canProcess(arg)) {
      return Boolean._of(compareNumeric(arg) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Bits
        <- rtn as Boolean?""")
  public Boolean _eq(Bits arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.length == arg.length && this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Bits
        <- rtn as Boolean?""")
  public Boolean _neq(Bits arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.length != arg.length || !this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Bits
        <- rtn as Integer?""")
  public Integer _cmp(Bits arg) {
    if (canProcess(arg)) {
      return Integer._of(compareNumeric(arg));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Bits asBits) {
      return _cmp(asBits);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as Bits
        <- rtn as Integer?""")
  public Integer _fuzzy(Bits arg) {
    // For bits, fuzzy is the same as numeric comparison
    return _cmp(arg);
  }

  @Ek9Operator("""
      operator ~ as pure
        <- rtn as Bits?""")
  public Bits _negate() {
    // Bitwise NOT - complement all bits
    if (isSet) {
      final var result = new BitSet(this.length);
      for (int i = 0; i < this.length; i++) {
        if (!this.state.get(i)) {
          result.set(i);
        }
      }
      final var rtn = _new();
      rtn.assign(result, this.length);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Operator("""
      operator and as pure
        -> arg as Bits
        <- rtn as Bits?""")
  public Bits _and(Bits arg) {
    // Bitwise AND - from docs: 0b010011 and 0b101010 = 0b000010
    if (canProcess(arg)) {
      // If either operand is empty, result is empty
      if (this.length == 0 || arg.length == 0) {
        final var rtn = _new();
        rtn.assign(new BitSet(), 0);
        return rtn;
      }

      final var maxLen = Math.max(this.length, arg.length);
      final var result = new BitSet(maxLen);

      for (int i = 0; i < maxLen; i++) {
        final var thisBit = i < this.length && this.state.get(i);
        final var argBit = i < arg.length && arg.state.get(i);
        if (thisBit && argBit) {
          result.set(i);
        }
      }

      final var rtn = _new();
      rtn.assign(result, maxLen);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Operator("""
      operator or as pure
        -> arg as Bits
        <- rtn as Bits?""")
  public Bits _or(Bits arg) {
    // Bitwise OR - from docs: 0b010011 or 0b101010 = 0b111011
    if (canProcess(arg)) {
      final var maxLen = Math.max(this.length, arg.length);
      final var result = new BitSet(maxLen);

      for (int i = 0; i < maxLen; i++) {
        final var thisBit = i < this.length && this.state.get(i);
        final var argBit = i < arg.length && arg.state.get(i);
        if (thisBit || argBit) {
          result.set(i);
        }
      }

      final var rtn = _new();
      rtn.assign(result, maxLen);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Operator("""
      operator xor as pure
        -> arg as Bits
        <- rtn as Bits?""")
  public Bits _xor(Bits arg) {
    // Bitwise XOR - from docs: 0b010011 xor 0b101010 = 0b111001
    if (canProcess(arg)) {
      final var maxLen = Math.max(this.length, arg.length);
      final var result = new BitSet(maxLen);

      for (int i = 0; i < maxLen; i++) {
        final var thisBit = i < this.length && this.state.get(i);
        final var argBit = i < arg.length && arg.state.get(i);
        if (thisBit ^ argBit) {
          result.set(i);
        }
      }

      final var rtn = _new();
      rtn.assign(result, maxLen);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Operator("""
      operator >> as pure
        -> arg as Integer
        <- rtn as Bits?""")
  public Bits _shiftRight(Integer arg) {
    // Right shift bits
    if (canProcess(arg) && arg.state >= 0) {
      if (arg.state >= this.length) {
        // Shift beyond length results in empty bits
        final var rtn = _new();
        rtn.assign(new BitSet(), 0);
        return rtn;
      }

      final var newLength = this.length - (int) arg.state;
      final var result = new BitSet(newLength);

      for (int i = 0; i < newLength; i++) {
        if (this.state.get(i + (int) arg.state)) {
          result.set(i);
        }
      }

      final var rtn = _new();
      rtn.assign(result, newLength);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Operator("""
      operator << as pure
        -> arg as Integer
        <- rtn as Bits?""")
  public Bits _shiftLeft(Integer arg) {
    // Left shift bits
    if (canProcess(arg) && arg.state >= 0) {
      final var newLength = this.length + (int) arg.state;
      final var result = new BitSet(newLength);

      // Copy existing bits shifted left
      for (int i = 0; i < this.length; i++) {
        if (this.state.get(i)) {
          result.set(i + (int) arg.state);
        }
      }

      final var rtn = _new();
      rtn.assign(result, newLength);
      return rtn;
    }
    return new Bits();
  }

  @Ek9Method("""
      iterator() as pure
        <- rtn as Iterator of Boolean?""")
  public _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65 iterator() {
    if (isSet) {
      // Create iterator that streams bits from LSB to MSB (right to left)
      // Example: 0b010011 streams as [false, true, false, false, true, false]
      final var booleanList = new java.util.ArrayList<Any>();
      for (int i = 0; i < this.length; i++) {
        booleanList.add(Boolean._of(this.state.get(i)));
      }
      return _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65._of(Iterator._of(booleanList));
    }
    return _Iterator_6E53B4C9D56633C9606BBF50DB03B8B3D0E6FDD8FC70D7C04705495D86D9FD65._of();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Bits""")
  public void _merge(Bits arg) {
    if (isValid(arg)) {
      if (isSet) {
        // If both are set, concatenate
        final var merged = this._add(arg);
        if (merged.isSet) {
          assign(merged.state, merged.length);
        }
      } else {
        // If this is unset, copy arg
        assign(arg);
      }
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Bits""")
  public void _replace(Bits arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Bits""")
  public void _copy(Bits arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Bits""")
  public void _pipe(Bits arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator |
        -> arg as JSON""")
  public void _pipe(JSON arg) {

    jsonTraversal.accept(arg, str -> _pipe(new Bits(str)));
  }

  @Ek9Operator("""
      operator +=
        -> arg as Bits""")
  public void _addAss(Bits arg) {
    if (canProcess(arg)) {
      final var result = this._add(arg);
      if (result.isSet) {
        assign(result.state, result.length);
      }
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as Boolean""")
  public void _addAss(Boolean arg) {
    if (canProcess(arg)) {
      final var result = this._add(arg);
      if (result.isSet) {
        assign(result.state, result.length);
      }
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as Bits""")
  public void _subAss(Bits arg) {
    if (canProcess(arg)) {
      final var result = this._sub(arg);
      if (result.isSet) {
        assign(result.state, result.length);
      }
    } else {
      unSet();
    }
  }


  // Start of utility methods

  @Override
  protected Bits _new() {
    return new Bits();
  }

  private void assign(Bits arg) {
    if (arg.isSet) {
      assign((BitSet) arg.state.clone(), arg.length);
    }
  }

  private void assign(BitSet bitSet, int bitLength) {
    this.state = (BitSet) bitSet.clone();
    this.length = bitLength;
    set();
    validateConstraints();
  }

  /**
   * Parse a bit string in formats like "010011" or "0b010011".
   * Returns null if invalid.
   */
  private static ParsedBits parseBitString(java.lang.String str) {
    if (str == null) {
      return null;
    }

    // Handle empty string as valid empty bits
    if (str.isEmpty()) {
      return new ParsedBits(new BitSet(), 0);
    }

    java.lang.String bitStr = str;
    if (str.startsWith("0b") || str.startsWith("0B")) {
      bitStr = str.substring(2);
    }

    // Handle empty string after removing prefix - this is invalid
    if (bitStr.isEmpty()) {
      return null;
    }

    // Validate all characters are 0 or 1
    for (char c : bitStr.toCharArray()) {
      if (c != '0' && c != '1') {
        return null;
      }
    }

    // Parse right-to-left (LSB first)
    final var bitSet = new BitSet(bitStr.length());
    for (int i = 0; i < bitStr.length(); i++) {
      if (bitStr.charAt(bitStr.length() - 1 - i) == '1') {
        bitSet.set(i);
      }
    }

    return new ParsedBits(bitSet, bitStr.length());
  }

  /**
   * Convert to bit string representation (without 0b prefix).
   */
  private java.lang.String toBitString() {
    if (length == 0) {
      return "";
    }

    final var sb = new StringBuilder(length);
    for (int i = length - 1; i >= 0; i--) {
      sb.append(state.get(i) ? '1' : '0');
    }
    return sb.toString();
  }

  /**
   * Compare bits numerically (treating as binary numbers).
   * Returns negative if this < arg, 0 if equal, positive if this > arg.
   */
  private int compareNumeric(Bits arg) {
    // First compare by length (longer bit sequences are typically larger)
    if (this.length != arg.length) {
      return java.lang.Integer.compare(this.length, arg.length);
    }

    // Same length, compare bit by bit from MSB to LSB
    for (int i = this.length - 1; i >= 0; i--) {
      final var thisBit = this.state.get(i);
      final var argBit = arg.state.get(i);
      if (thisBit != argBit) {
        return thisBit ? 1 : -1; // true > false
      }
    }

    return 0; // Equal
  }

  // Factory methods
  public static Bits _of() {
    return new Bits();
  }

  public static Bits _of(java.lang.String bitString) {
    final var bits = new Bits();
    if (bitString != null) {
      final var parsed = parseBitString(bitString);
      if (parsed != null) {
        bits.assign(parsed.state, parsed.length);
      }
    }
    return bits;
  }

  public static Bits _of(boolean bit) {
    return new Bits(Boolean._of(bit));
  }

  public static Bits _of(Bits other) {
    return new Bits(other);
  }

  /**
   * Helper class for parsed bit string results.
   */
  private record ParsedBits(BitSet state, int length) {
  }
}