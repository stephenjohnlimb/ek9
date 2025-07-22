package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 DictEntry of (String, String) - Parameterized generic type using delegation pattern.
 * This class provides type-safe key-value operations while delegating to the base DictEntry implementation.
 * K → String, V → String
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("""
    DictEntry of (String, String)""")
public class _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 extends BuiltinType {

  private final DictEntry delegate;

  // Default constructor
  public _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487() {
    this.delegate = new DictEntry();
  }

  // Two-parameter constructor - accepts String key and String value
  public _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(String k, String v) {
    this.delegate = new DictEntry(k, v);
  }

  // Internal constructor for factory methods
  private _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(DictEntry delegate) {
    this.delegate = delegate;
  }

  // Accessor to get the delegate (needed for Dict operations)
  public DictEntry getDelegate() {
    return delegate;
  }

  // Method 1: key() with String return type (K → String)
  public String key() {
    return (String) delegate.key();
  }

  // Method 2: value() with String return type (V → String)
  public String value() {
    return (String) delegate.value();
  }

  // Operator 1: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  // Operator 2: == with same parameterized type
  public Boolean _eq(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 arg) {
    if (arg != null) {
      return delegate._eq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 3: == with Any (polymorphic)
  @Override
  public Boolean _eq(Any arg) {
    if (arg instanceof _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 paramEntry) {
      return delegate._eq(paramEntry.delegate);
    }
    return delegate._eq(arg);
  }

  // Operator 4: <> with same parameterized type
  public Boolean _neq(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 arg) {
    if (arg != null) {
      return delegate._neq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 5: <=> with same parameterized type
  public Integer _cmp(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 arg) {
    if (arg != null) {
      return delegate._cmp(arg.delegate);
    }
    return new Integer();
  }

  // Operator 6: <=> with Any (polymorphic)
  @Override
  public Integer _cmp(Any arg) {
    if (arg instanceof _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 paramEntry) {
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

  // Factory method - empty entry
  public static _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 _of() {
    return new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487();
  }

  // Factory method - from String key and String value
  public static _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 _of(
      String key, String value) {
    return new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(key, value);
  }

  // Factory method - from base DictEntry
  public static _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 _of(DictEntry dictEntry) {
    if (dictEntry != null) {
      return new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487(dictEntry);
    }
    return new _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487();
  }

}