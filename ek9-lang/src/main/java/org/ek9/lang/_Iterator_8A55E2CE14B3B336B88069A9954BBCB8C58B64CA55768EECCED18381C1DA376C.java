package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template type.
 * In this case the EK9 Iterator generic type has been parameterised with JSON.
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
    Iterator of JSON""")
public class _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C extends BuiltinType {

  private final Iterator delegate;

  public _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C() {
    this(new Iterator());
  }

  public _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C(JSON arg0) {
    this(new Iterator(arg0));
  }

  //Internal constructor.
  private _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C(Iterator delegate) {
    this.delegate = delegate;
  }

  public Boolean hasNext() {
    return delegate.hasNext();
  }

  public JSON next() {
    return (JSON) delegate.next();
  }

  @Override
  public Boolean _isSet() {
    return delegate.hasNext();
  }

  public Boolean _eq(_Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C arg) {
    return delegate._eq(arg.delegate);
  }

  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }

  public static _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C _of() {
    return new _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C();
  }

  public static _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C _of(Iterator iterator) {
    if (iterator != null) {
      return new _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C(iterator);
    }
    return new _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C();
  }

}