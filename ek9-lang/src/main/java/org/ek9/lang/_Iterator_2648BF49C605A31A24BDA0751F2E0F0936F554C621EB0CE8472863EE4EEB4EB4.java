package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * EK9 Iterator of Integer - Parameterized generic type using delegation pattern.
 * This class provides type-safe Integer iteration while delegating to the base Iterator implementation.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("Iterator of Integer")
public class _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 extends BuiltinType {

  private final Iterator delegate;

  // Default constructor
  public _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4() {
    this.delegate = new Iterator();
  }

  // Value constructor - accepts Integer parameter
  public _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4(Integer arg0) {
    this.delegate = new Iterator(arg0);
  }

  // Internal constructor for factory methods
  private _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4(Iterator delegate) {
    this.delegate = delegate;
  }

  // Factory method - empty iterator
  public static _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 _of() {
    return new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4();
  }

  // Factory method - from base Iterator
  public static _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 _of(Iterator iterator) {
    if (iterator != null) {
      return new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4(iterator);
    }
    return new _Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4();
  }

  // Method 1: hasNext() - direct delegation
  public Boolean hasNext() {
    return delegate.hasNext();
  }

  // Method 2: next() with Integer return type
  public Integer next() {
    return (Integer) delegate.next();
  }

  // Operator 1: ? (isSet)
  @Override
  public Boolean _isSet() {
    return delegate.hasNext();
  }

  // Operator 2: == with same parameterized type
  public Boolean _eq(_Iterator_2648BF49C605A31A24BDA0751F2E0F0936F554C621EB0CE8472863EE4EEB4EB4 arg) {
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