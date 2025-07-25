package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 Dict of (String, Integer) - Parameterized generic type using delegation pattern.
 * This class provides type-safe dictionary operations while delegating to the base Dict implementation.
 * K → String, V → Integer
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("Dict of (String, Integer)")
public class _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 extends BuiltinType {

  private final Dict delegate;

  // Default constructor
  public _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6() {
    this.delegate = new Dict();
  }

  // Two-parameter constructor - accepts String key and Integer value
  public _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(String k, Integer v) {
    this.delegate = new Dict(k, v);
  }

  // Internal constructor for factory methods
  private _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(Dict delegate) {
    this.delegate = delegate;
  }

  // Method 1: get() with Integer return type (V → Integer)
  public Integer get(String arg0) {
    return (Integer) delegate.get(arg0);
  }

  // Method 2: getOrDefault() with String, Integer parameters and Integer return type
  public Integer getOrDefault(String arg0, Integer arg1) {
    return (Integer) delegate.getOrDefault(arg0, arg1);
  }

  // Method 3: iterator() - returns Iterator of DictEntry of (String, Integer)
  public _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826 iterator() {
    final var baseIterator = delegate.iterator();
    return _Iterator_48D70134B2562F23E0D9F143A4A9BF02060D37448527B5CDB59DBC1C05F5D826._of(baseIterator);
  }

  // Method 4: keys() - returns Iterator of String  
  public _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 keys() {
    final var baseIterator = delegate.keys();
    return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(baseIterator);
  }

  // Method 5: values() - returns Iterator of Integer
  public _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 values() {
    final var baseIterator = delegate.values();
    return _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4._of(baseIterator);
  }

  // Operator 1: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  // Operator 2: == with same parameterized type
  public Boolean _eq(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      return delegate._eq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 3: == with Any (polymorphic)
  @Override
  public Boolean _eq(Any arg) {
    if (arg instanceof _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 paramDict) {
      return delegate._eq(paramDict.delegate);
    }
    return delegate._eq(arg);
  }

  // Operator 4: <> with same parameterized type
  public Boolean _neq(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      return delegate._neq(arg.delegate);
    }
    return new Boolean();
  }

  @Override
  public JSON _json() {
    return delegate._json();
  }

  // Operator 5: $ (string representation)
  @Override
  public String _string() {
    return delegate._string();
  }

  // Operator 6: #? (hashcode)
  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }

  // Operator 7: empty
  public Boolean _empty() {
    return delegate._empty();
  }

  // Operator 8: length
  public Integer _len() {
    return delegate._len();
  }

  // Operator 9: contains with String parameter (K → String)
  public Boolean _contains(String arg) {
    return delegate._contains(arg);
  }

  // Operator 10: + with same parameterized type
  public _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 _add(
      _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(delegate._add(arg.delegate));
    }
    return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(delegate._add((Dict) null));
  }

  // Operator 11: + with DictEntry of (String, Integer)
  public _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 _add(
      _DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E arg) {
    return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(delegate._add(arg.getDelegate()));
  }

  // Operator 12: - with same parameterized type
  public _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 _sub(
      _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(delegate._sub(arg.delegate));
    }
    return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(delegate._sub(null));
  }

  // Operator 13: :~: (merge) with same parameterized type
  public void _merge(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      delegate._merge(arg.delegate);
    }
  }

  // Operator 14: :^: (replace) with same parameterized type
  public void _replace(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      delegate._replace(arg.delegate);
    }
  }

  // Operator 15: :=: (copy) with same parameterized type
  public void _copy(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      delegate._copy(arg.delegate);
    }
  }

  // Operator 16: | (pipe) with DictEntry of (String, Integer)
  public void _pipe(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E arg) {
    delegate._pipe(arg.getDelegate());
  }

  // Operator 17: += with same parameterized type
  public void _addAss(_Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 arg) {
    if (arg != null) {
      delegate._addAss(arg.delegate);
    }
  }

  // Operator 18: += with DictEntry of (String, Integer)
  public void _addAss(_DictEntry_87A55D447A2FC20E1611D0A0F5F49C2A4B57F40CD33E7FB15E43352011BFDD4E arg) {
    delegate._addAss(arg.getDelegate());
  }

  // Operator 19: -= with String parameter (K → String)
  public void _subAss(String arg) {
    delegate._subAss(arg);
  }

  // Factory method - empty dict
  public static _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 _of() {
    return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6();
  }

  // Factory method - from String key and Integer value
  public static _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 _of(String key, Integer value) {
    return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(key, value);
  }

  // Factory method - from base Dict
  public static _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6 _of(Dict dict) {
    if (dict != null) {
      return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6(dict);
    }
    return new _Dict_7E7710D38A91EC202D64601DF4D9FB5B4AF2026CC3EF59394F5CF6B738812BB6();
  }
}