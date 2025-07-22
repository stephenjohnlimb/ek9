package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 DictEntry of (String, Integer) - Parameterized generic type using delegation pattern.
 * This class provides type-safe key-value operations while delegating to the base DictEntry implementation.
 * K → String, V → Integer
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("DictEntry of (String, Integer)")
public class _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E extends BuiltinType {

  private final DictEntry delegate;

  // Default constructor
  public _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E() {
    this.delegate = new DictEntry();
  }

  // Two-parameter constructor - accepts String key and Integer value
  public _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E(String k, Integer v) {
    this.delegate = new DictEntry(k, v);
  }

  // Internal constructor for factory methods
  private _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E(DictEntry delegate) {
    this.delegate = delegate;
  }

  // Factory method - empty entry
  public static _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E _of() {
    return new _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E();
  }

  // Factory method - from String key and Integer value
  public static _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E _of(
      String key, Integer value) {
    return new _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E(key, value);
  }

  // Factory method - from base DictEntry
  public static _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E _of(DictEntry dictEntry) {
    if (dictEntry != null) {
      return new _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E(dictEntry);
    }
    return new _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E();
  }

  // Accessor to get the delegate (needed for Dict operations)
  public DictEntry getDelegate() {
    return delegate;
  }

  // Method 1: key() with String return type (K → String)
  public String key() {
    return (String) delegate.key();
  }

  // Method 2: value() with Integer return type (V → Integer)
  public Integer value() {
    return (Integer) delegate.value();
  }

  // Operator 1: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  // Operator 2: == with same parameterized type
  public Boolean _eq(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E arg) {
    if (arg != null) {
      return delegate._eq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 3: == with Any (polymorphic)
  @Override
  public Boolean _eq(Any arg) {
    if (arg instanceof _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E paramEntry) {
      return delegate._eq(paramEntry.delegate);
    }
    return delegate._eq(arg);
  }

  // Operator 4: <> with same parameterized type
  public Boolean _neq(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E arg) {
    if (arg != null) {
      return delegate._neq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 5: <=> with same parameterized type
  public Integer _cmp(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E arg) {
    if (arg != null) {
      return delegate._cmp(arg.delegate);
    }
    return new Integer();
  }

  // Operator 6: <=> with Any (polymorphic)
  @Override
  public Integer _cmp(Any arg) {
    if (arg instanceof _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E paramEntry) {
      return delegate._cmp(paramEntry.delegate);
    }
    return delegate._cmp(arg);
  }

  // Operator 7: $ (string representation)
  @Override
  public String _string() {
    return delegate._string();
  }

  // Operator 8: #? (hashcode)
  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }
}