package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template EK9 Function type.
 * In this case the EK9 Acceptor generic function type has been parameterised with String.
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
    Acceptor of String""")
public class _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA extends BuiltinType {

  private final Acceptor delegate;

  public _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA() {
    this(new Acceptor());
  }

  //Internal constructor.
  private _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA(Acceptor delegate) {
    this.delegate = delegate;
  }

  /**
   * The primary function call method - accepts String parameter instead of Any.
   * This provides type safety for the parameterized Acceptor of String.
   */
  public void _call(String t) {
    delegate._call(t); // Delegate to base Acceptor, String is compatible with Any
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
    if (arg instanceof _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA asAcceptor) {
      return _eq(asAcceptor);
    }
    return new Boolean();
  }

  public Boolean _eq(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public static _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA _of() {
    return new _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA();
  }

  public static _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA _of(Acceptor acceptor) {
    if (acceptor != null) {
      return new _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA(acceptor);
    }
    return new _Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA();
  }

}