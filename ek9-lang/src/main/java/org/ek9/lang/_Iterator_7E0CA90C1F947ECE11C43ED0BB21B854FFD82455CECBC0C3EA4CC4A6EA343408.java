package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template type.
 * In this case the EK9 Iterator generic type has been parameterised with Character.
 * <p>
 * The solution in EK9 is to create an entirely new type and then just use the EK9 Generic type
 * (which is actually just a normal type in Java where the 'T' is just 'Any').
 * When parameterising with a type the T (Any) is just replaced with the parameterising type in the
 * appropriate places. The all the calls are just applied to the 'delegate.
 * </p>
 * <p>
 * But note it is necessary to 'cast' in specific situations.
 * </p>
 * <p>
 * This whole approach depends upon ensuring on the object of the parameterised type (or subtypes) are
 * allowed into the implementation, thereby ensuring the the returning casts can be made without error.
 * </p>
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("""
    Iterator of Character""")
public class _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 extends BuiltinType {

  private final Iterator delegate;

  public _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408() {
    this(new Iterator());
  }

  public _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408(Character arg0) {
    this(new Iterator(arg0));
  }

  //Internal constructor.
  private _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408(Iterator delegate) {
    this.delegate = delegate;
  }

  public Boolean hasNext() {
    return delegate.hasNext();
  }

  public Character next() {
    return (Character) delegate.next();
  }

  @Override
  public Boolean _isSet() {
    return delegate.hasNext();
  }

  public Boolean _eq(_Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 arg) {
    return delegate._eq(arg.delegate);
  }

  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }

  public static _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 _of() {
    return new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408();
  }

  public static _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 _of(Iterator iterator) {
    if (iterator != null) {
      return new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408(iterator);
    }
    return new _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408();
  }

}
