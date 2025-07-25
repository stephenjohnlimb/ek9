package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template EK9 Result type.
 * In this case the EK9 Result generic type has been parameterised with String as OK type and Boolean as ERROR type.
 * <p>
 * The solution in EK9 is to create an entirely new type and then just use the EK9 Generic Result type
 * (which is actually just a normal type in Java where the 'O' and 'E' are just 'Any').
 * When parameterising with types the O (Any) is replaced with String and E (Any) is replaced with Boolean
 * in the appropriate places. All the calls are just applied to the 'delegate'.
 * </p>
 * <p>
 * But note it is necessary to 'cast' in specific situations.
 * </p>
 * <p>
 * This whole approach depends upon ensuring only objects of the parameterised types (or subtypes) are
 * allowed into the implementation, thereby ensuring the returning casts can be made without error.
 * </p>
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:AbbreviationAsWordInName", "checkstyle:TypeName"})
@Ek9ParameterisedType("""
    Result of (String, Boolean)""")
public class _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 extends BuiltinType {

  private final Result delegate;

  public _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65() {
    this(new Result());
  }

  //Internal constructor.
  private _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65(Result delegate) {
    this.delegate = delegate;
  }

  public _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 asEmpty() {
    return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65(delegate.asEmpty());
  }

  public _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 asOk(String arg0) {
    return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65(delegate.asOk(arg0));
  }

  public _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 asError(Boolean arg0) {
    return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65(delegate.asError(arg0));
  }

  public Boolean isOk() {
    return delegate.isOk();
  }

  public String ok() {
    return (String) delegate.ok(); // Safe cast due to parameterization
  }

  public String okOrDefault(String arg0) {
    return (String) delegate.okOrDefault(arg0);
  }

  public String getOrDefault(String arg0) {
    return okOrDefault(arg0);
  }

  public void whenOk(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptor) {
    if (delegate.isOk().state && acceptor != null) {
      acceptor._call(ok());
    }
  }

  public void whenOk(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 consumer) {
    if (delegate.isOk().state && consumer != null) {
      consumer._call(ok());
    }
  }

  public Boolean isError() {
    return delegate.isError();
  }

  public Boolean error() {
    return (Boolean) delegate.error(); // Safe cast due to parameterization
  }

  public Boolean errorOrDefault(Boolean arg0) {
    return (Boolean) delegate.errorOrDefault(arg0);
  }

  public void whenError(_Acceptor_2C592A33B214C92F1431194345F7C7534F5C064A0710DC28946BDF0EC13E6817 acceptor) {
    if (delegate.isError().state && acceptor != null) {
      acceptor._call(error());
    }
  }

  public void whenError(_Consumer_6CA5F5837A13FA80F86ADE72D1698B4441C50614A56BEA015D595402027DE326 consumer) {
    if (delegate.isError().state && consumer != null) {
      consumer._call(error());
    }
  }

  public _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2 iterator() {
    // Delegate to base Iterator and wrap with parameterized type (Iterator of String for OK values)
    final var baseIterator = delegate.iterator();
    return _Iterator_852BE8F78E9C7E622E0E2BDC5523BEFF664305AD702B04CE0463ED42C1FE2CA2._of(baseIterator);
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
    if (arg instanceof _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 asResult) {
      return _eq(asResult);
    }
    return new Boolean();
  }

  public Boolean _eq(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public Boolean _neq(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 arg) {
    return _eq(arg)._negate();
  }

  public Boolean _empty() {
    return delegate._empty();
  }

  public Boolean _contains(String arg) {
    return delegate._contains(arg);
  }

  public Boolean _containsError(Boolean arg) {
    return delegate._containsError(arg);
  }

  public void _merge(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 arg) {
    if (arg != null) {
      delegate._merge(arg.delegate);
    }
  }

  public void _replace(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 arg) {
    if (arg != null) {
      delegate._replace(arg.delegate);
    }
  }

  public void _copy(_Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 arg) {
    if (arg != null) {
      delegate._copy(arg.delegate);
    } else {
      delegate._copy(null);
    }
  }

  public void _pipe(String arg) {
    delegate._pipe(arg);
  }

  @Override
  public JSON _json() {
    return delegate._json();
  }

  // Factory methods
  public static _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 _of() {
    return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65();
  }

  public static _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 _of(
      String okValue, Boolean errorValue) {
    return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65(
        Result._of(okValue, errorValue));
  }

  public static _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65 _of(
      Result result) {
    if (result != null) {
      return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65(result);
    }
    return new _Result_8B848533C5497B92D394BCE7DBD6BA96340DC392994BAC3C8A54E22C80ABAC65();
  }

}