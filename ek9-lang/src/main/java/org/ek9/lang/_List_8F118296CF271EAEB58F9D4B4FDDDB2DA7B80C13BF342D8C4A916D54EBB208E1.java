package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 List of String - Parameterized generic type using delegation pattern.
 * This class provides type-safe String operations while delegating to the base List implementation.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("""
    List of String""")
public class _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 extends BuiltinType {

  private final List delegate;

  // Default constructor
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1() {
    this.delegate = new List();
  }

  // Value constructor - accepts String parameter
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(String arg0) {
    this.delegate = new List(arg0);
  }

  // Internal constructor for factory methods
  private _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(List delegate) {
    this.delegate = delegate;
  }

  // Method 1: get() with String return type
  public String get(Integer index) {
    return (String) delegate.get(index);
  }

  // Method 2: first() with String return type
  public String first() {
    return (String) delegate.first();
  }

  // Method 3: last() with String return type
  public String last() {
    return (String) delegate.last();
  }

  // Method 4: reverse() with List of String return type
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 reverse() {
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate.reverse());
  }

  // Method 5: iterator() with Iterator of String return type
  public _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterator() {
    return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(delegate.iterator());
  }

  // Operator 1: ~ (negate/reverse) with List of String return type
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _negate() {
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate._negate());
  }

  // Operator 2: == with Any parameter (polymorphic)
  @Override
  public Boolean _eq(Any arg) {
    if (arg instanceof _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 paramList) {
      return delegate._eq(paramList.delegate);
    }
    return new Boolean();
  }

  // Operator 3: == with List of String parameter
  public Boolean _eq(_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      return delegate._eq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 4: <> with List of String parameter
  public Boolean _neq(_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      return delegate._neq(arg.delegate);
    }
    return new Boolean();
  }

  // Operator 5: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  // Operator 6: + with List of String parameter
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _add(
      _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate._add(arg.delegate));
    }
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate._add(null));
  }

  // Operator 7: + with String parameter
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _add(String arg) {
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate._add(arg));
  }

  // Operator 8: - with List of String parameter
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _sub(
      _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate._sub(arg.delegate));
    }
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate._sub(null));
  }

  // Operator 9: - with String parameter
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _sub(String arg) {
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(delegate._sub(arg));
  }

  // Operator 10: $ (string representation)
  @Override
  public String _string() {
    return delegate._string();
  }

  // Operator 11: #? (hashcode)
  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }

  // Operator 12: #< (prefix/first) with String return type
  public String _prefix() {
    return (String) delegate._prefix();
  }

  // Operator 13: #> (suffix/last) with String return type
  public String _suffix() {
    return (String) delegate._suffix();
  }

  // Operator 14: empty
  public Boolean _empty() {
    return delegate._empty();
  }

  // Operator 15: length
  public Integer _len() {
    return delegate._len();
  }

  // Operator 16: contains with String parameter
  public Boolean _contains(String arg) {
    return delegate._contains(arg);
  }

  // Operator 17: :~: (merge) with List of String parameter
  public void _merge(_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      delegate._merge(arg.delegate);
    }
  }

  // Operator 18: :~: (merge) with String parameter
  public void _merge(String arg) {
    delegate._merge(arg);
  }

  // Operator 19: :^: (replace) with List of String parameter
  public void _replace(_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      delegate._replace(arg.delegate);
    }
  }

  // Operator 20: :=: (copy) with List of String parameter
  public void _copy(_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      delegate._copy(arg.delegate);
    }
  }

  // Operator 21: | (pipe) with String parameter
  public void _pipe(String arg) {
    delegate._pipe(arg);
  }

  // Operator 22: += (add assignment) with List of String parameter
  public void _addAss(_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      delegate._addAss(arg.delegate);
    }
  }

  // Operator 23: += (add assignment) with String parameter
  public void _addAss(String arg) {
    delegate._addAss(arg);
  }

  // Operator 24: -= (subtract assignment) with List of String parameter
  public void _subAss(_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 arg) {
    if (arg != null) {
      delegate._subAss(arg.delegate);
    }
  }

  // Operator 25: -= (subtract assignment) with String parameter
  public void _subAss(String arg) {
    delegate._subAss(arg);
  }

  // Factory method - empty list
  public static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _of() {
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
  }

  // Factory method - from base List
  public static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _of(List list) {
    if (list != null) {
      return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(list);
    }
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
  }

  // Factory method - from Java List with String filtering
  public static _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 _of(java.util.List<Any> list) {
    if (list != null) {
      final var baseList = List._of(list);
      return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1(baseList);
    }
    return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
  }

}