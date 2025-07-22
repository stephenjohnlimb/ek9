package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 Dict of (String, String) - Parameterized generic type using delegation pattern.
 * This class provides type-safe dictionary operations while delegating to the base Dict implementation.
 * K → String, V → String
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("""
    Dict of (String, String)""")
public class _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F extends BuiltinType {

  private final Dict delegate;

  // Default constructor
  public _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F() {
    this.delegate = new Dict();
  }

  // Two-parameter constructor - accepts String key and String value
  public _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(String k, String v) {
    this.delegate = new Dict(k, v);
  }

  // Internal constructor for factory methods
  private _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(Dict delegate) {
    this.delegate = delegate;
  }

  // Method 1: get() with String return type (V → String)
  public String get(String arg0) {
    return (String) delegate.get(arg0);
  }

  // Method 2: getOrDefault() with String, String parameters and String return type
  public String getOrDefault(String arg0, String arg1) {
    return (String) delegate.getOrDefault(arg0, arg1);
  }

  // Method 3: iterator() - returns Iterator of DictEntry of (String, String)
  public _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D iterator() {
    final var baseIterator = delegate.iterator();
    return _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D._of(baseIterator);
  }

  // Method 4: keys() - returns Iterator of String  
  public _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 keys() {
    final var baseIterator = delegate.keys();
    return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(baseIterator);
  }

  // Method 5: values() - returns Iterator of String (both K and V are String)
  public _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 values() {
    final var baseIterator = delegate.values();
    return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(baseIterator);
  }

  // Operator 1: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  // Operator 2: == with same parameterized type
  public Boolean _eq(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      return delegate._eq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 3: == with Any (polymorphic)
  @Override
  public Boolean _eq(Any arg) {
    if (arg instanceof _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F paramDict) {
      return delegate._eq(paramDict.delegate);
    }
    return delegate._eq(arg);
  }

  // Operator 4: <> with same parameterized type
  public Boolean _neq(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      return delegate._neq(arg.delegate);
    }
    return new Boolean();
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
  public _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F _add(
      _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(delegate._add(arg.delegate));
    }
    return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(delegate._add((Dict) null));
  }

  // Operator 11: + with DictEntry of (String, String)
  public _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F _add(
      _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 arg) {
    return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(delegate._add(arg.getDelegate()));
  }

  // Operator 12: - with same parameterized type
  public _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F _sub(
      _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(delegate._sub(arg.delegate));
    }
    return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(delegate._sub(null));
  }

  // Operator 13: :~: (merge) with same parameterized type
  public void _merge(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      delegate._merge(arg.delegate);
    }
  }

  // Operator 14: :^: (replace) with same parameterized type
  public void _replace(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      delegate._replace(arg.delegate);
    }
  }

  // Operator 15: :=: (copy) with same parameterized type
  public void _copy(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      delegate._copy(arg.delegate);
    }
  }

  // Operator 16: | (pipe) with DictEntry of (String, String)
  public void _pipe(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 arg) {
    delegate._pipe(arg.getDelegate());
  }

  // Operator 17: += with same parameterized type
  public void _addAss(_Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F arg) {
    if (arg != null) {
      delegate._addAss(arg.delegate);
    }
  }

  // Operator 18: += with DictEntry of (String, String)
  public void _addAss(_DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 arg) {
    delegate._addAss(arg.getDelegate());
  }

  // Operator 19: -= with String parameter (K → String)
  public void _subAss(String arg) {
    delegate._subAss(arg);
  }

  // Factory method - empty dict
  public static _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F _of() {
    return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F();
  }

  // Factory method - from String key and String value
  public static _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F _of(String key, String value) {
    return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(key, value);
  }

  // Factory method - from base Dict
  public static _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F _of(Dict dict) {
    if (dict != null) {
      return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F(dict);
    }
    return new _Dict_E9A1EFF0D62E8EB35F7B0572E7F2C5492D6C980FE8B69376B38612DE6EBEC25F();
  }
}