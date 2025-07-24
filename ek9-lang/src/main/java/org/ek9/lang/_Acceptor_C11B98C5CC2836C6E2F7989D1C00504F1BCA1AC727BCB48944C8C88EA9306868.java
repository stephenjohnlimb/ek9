package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template EK9 Function type.
 * In this case the EK9 Acceptor generic function type has been parameterised with JSON.
 * <p>
 * The solution in EK9 is to create an entirely new type and then just use the EK9 Generic function type
 * (which is actually just a normal type in Java where the 'T' is just 'Any').
 * When parameterising with a type the T (Any) is just replaced with the parameterising type in the
 * appropriate places. All the calls are just applied to the 'delegate'.
 * </p>
 * <p>
 * But note it is necessary to 'cast' in specific situations.
 * </p>
 * <p>
 * This whole approach depends upon ensuring only objects of the parameterised type (or subtypes) are
 * allowed into the implementation, thereby ensuring the returning casts can be made without error.
 * </p>
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("""
    Acceptor of JSON""")
public class _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 extends BuiltinType {

  private final Acceptor delegate;

  public _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868() {
    this(new Acceptor());
  }

  //Internal constructor.
  private _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868(Acceptor delegate) {
    this.delegate = delegate;
  }

  /**
   * The primary function call method - accepts JSON parameter instead of Any.
   * This provides type safety for the parameterized Acceptor of JSON.
   */
  public void _call(JSON t) {
    delegate._call(t); // Delegate to base Acceptor, JSON is compatible with Any
  }

  @Override
  public Boolean _isSet() {
    return delegate._isSet();
  }

  @Override
  public String _string() {
    return delegate._string();
  }

  @Override
  public Integer _hashcode() {
    return delegate._hashcode();
  }

  @Override
  public Boolean _eq(Any arg) {
    if (arg instanceof _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 asAcceptor) {
      return _eq(asAcceptor);
    }
    return new Boolean();
  }

  public Boolean _eq(_Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public static _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 _of() {
    return new _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868();
  }

  public static _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 _of(Acceptor acceptor) {
    if (acceptor != null) {
      return new _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868(acceptor);
    }
    return new _Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868();
  }

}