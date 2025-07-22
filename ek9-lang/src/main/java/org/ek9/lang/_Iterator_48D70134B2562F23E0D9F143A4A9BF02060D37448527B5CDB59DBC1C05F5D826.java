package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 Iterator of DictEntry of (String, Integer) - Parameterized generic type using delegation pattern.
 * This class provides type-safe iteration over DictEntry of (String, Integer) objects while delegating
 * to the base Iterator implementation. This demonstrates nested parameterization.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("Iterator of DictEntry of (String, Integer)")
public class _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 extends BuiltinType {

  private final Iterator delegate;

  // Default constructor
  public _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826() {
    this.delegate = new Iterator();
  }

  // Value constructor - accepts DictEntry of (String, Integer) parameter
  public _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(
      _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E arg0) {
    this.delegate = new Iterator(arg0.getDelegate());
  }

  // Internal constructor for factory methods
  private _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(Iterator delegate) {
    this.delegate = delegate;
  }

  // Factory method - empty iterator
  public static _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 _of() {
    return new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826();
  }

  // Factory method - from base Iterator
  public static _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 _of(Iterator iterator) {
    if (iterator != null) {
      return new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826(iterator);
    }
    return new _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826();
  }

  // Method 1: hasNext() - direct delegation
  public Boolean hasNext() {
    return delegate.hasNext();
  }

  // Method 2: next() with DictEntry of (String, Integer) return type
  public _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E next() {
    final var baseEntry = (DictEntry) delegate.next();
    return _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E._of(baseEntry);
  }

  // Operator 1: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate.hasNext();
  }

  // Operator 2: == with same parameterized type
  public Boolean _eq(_Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 arg) {
    if (arg != null) {
      return delegate._eq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 3: #? (hashcode)
  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }
}