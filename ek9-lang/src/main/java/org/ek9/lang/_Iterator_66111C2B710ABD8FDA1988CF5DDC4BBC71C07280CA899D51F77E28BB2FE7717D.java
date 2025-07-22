package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 Iterator of DictEntry of (String, String) - Parameterized generic type using delegation pattern.
 * This class provides type-safe iteration over DictEntry of (String, String) objects while delegating
 * to the base Iterator implementation. This demonstrates nested parameterization.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("Iterator of DictEntry of (String, String)")
public class _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D extends BuiltinType {

  private final Iterator delegate;

  // Default constructor
  public _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D() {
    this.delegate = new Iterator();
  }

  // Value constructor - accepts DictEntry of (String, String) parameter
  public _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(
      _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 arg0) {
    this.delegate = new Iterator(arg0.getDelegate());
  }

  // Internal constructor for factory methods
  private _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(Iterator delegate) {
    this.delegate = delegate;
  }

  // Factory method - empty iterator
  public static _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D _of() {
    return new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D();
  }

  // Factory method - from base Iterator
  public static _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D _of(Iterator iterator) {
    if (iterator != null) {
      return new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D(iterator);
    }
    return new _Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D();
  }

  // Method 1: hasNext() - direct delegation
  public Boolean hasNext() {
    return delegate.hasNext();
  }

  // Method 2: next() with DictEntry of (String, String) return type
  public _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487 next() {
    final var baseEntry = (DictEntry) delegate.next();
    return _DictEntry_8179CD93B60C7CEF656D347EFAA68A41FCC0DFA832E9FC8E5DE6D645B02AC487._of(baseEntry);
  }

  // Operator 1: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  // Operator 2: == with same parameterized type
  public Boolean _eq(_Iterator_66111C2B710ABD8FDA1988CF5DDC4BBC71C07280CA899D51F77E28BB2FE7717D arg) {
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