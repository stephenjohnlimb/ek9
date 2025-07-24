package org.ek9.lang;

import org.ek9tooling.Ek9ParameterisedType;

/**
 * Definition of the parameterization of a generic/template EK9 Result type.
 * In this case the EK9 Result generic type has been parameterised with JSON as OK type and String as ERROR type.
 * <p>
 * The solution in EK9 is to create an entirely new type and then just use the EK9 Generic Result type
 * (which is actually just a normal type in Java where the 'O' and 'E' are just 'Any').
 * When parameterising with types the O (Any) is replaced with JSON and E (Any) is replaced with String
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
    Result of (JSON, String)""")
public class _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 extends BuiltinType {

  private final Result delegate;

  public _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456() {
    this(new Result());
  }

  //Internal constructor.
  private _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456(Result delegate) {
    this.delegate = delegate;
  }

  public _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 asEmpty() {
    return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456(delegate.asEmpty());
  }

  public _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 asOk(JSON arg0) {
    return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456(delegate.asOk(arg0));
  }

  public _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 asError(String arg0) {
    return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456(delegate.asError(arg0));
  }

  public Boolean isOk() {
    return delegate.isOk();
  }

  public JSON ok() {
    return (JSON) delegate.ok(); // Safe cast due to parameterization
  }

  public JSON okOrDefault(JSON arg0) {
    return (JSON) delegate.okOrDefault(arg0);
  }

  public JSON getOrDefault(JSON arg0) {
    return okOrDefault(arg0);
  }

  public void whenOk(_Acceptor_C11B98C5CC2836C6E2F7989D1C00504F1BCA1AC727BCB48944C8C88EA9306868 acceptor) {
    if (delegate.isOk().state && acceptor != null) {
      acceptor._call(ok());
    }
  }

  public void whenOk(_Consumer_376C0B739AC00DD9FA60B1DE33853E8AA9F76AF08832303EB1027CCB027B8588 consumer) {
    if (delegate.isOk().state && consumer != null) {
      consumer._call(ok());
    }
  }

  public Boolean isError() {
    return delegate.isError();
  }

  public String error() {
    return (String) delegate.error(); // Safe cast due to parameterization
  }

  public String errorOrDefault(String arg0) {
    return (String) delegate.errorOrDefault(arg0);
  }

  public void whenError(_Acceptor_49176569D07D81D30581FB294F0767BF3C9A372BB2B21E1876D8263E8C7070AA acceptor) {
    if (delegate.isError().state && acceptor != null) {
      acceptor._call(error());
    }
  }

  public void whenError(_Consumer_1E1E02606C5968CCC6142A837F3CE0F81F440D54B5B564E9138AF830959EB9E0 consumer) {
    if (delegate.isError().state && consumer != null) {
      consumer._call(error());
    }
  }

  public _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterator() {
    // Delegate to base Iterator and wrap with parameterized type
    final var baseIterator = delegate.iterator();
    return _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C._of(baseIterator);
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
    if (arg instanceof _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 asResult) {
      return _eq(asResult);
    }
    return new Boolean();
  }

  public Boolean _eq(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 arg) {
    if (arg == null) {
      return new Boolean();
    }
    return delegate._eq(arg.delegate);
  }

  public Boolean _neq(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 arg) {
    return _eq(arg)._negate();
  }

  public Boolean _empty() {
    return delegate._empty();
  }

  public Boolean _contains(JSON arg) {
    return delegate._contains(arg);
  }

  public Boolean _containsError(String arg) {
    return delegate._containsError(arg);
  }

  public void _merge(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 arg) {
    if (arg != null) {
      delegate._merge(arg.delegate);
    }
  }

  public void _replace(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 arg) {
    if (arg != null) {
      delegate._replace(arg.delegate);
    }
  }

  public void _copy(_Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 arg) {
    if (arg != null) {
      delegate._copy(arg.delegate);
    } else {
      delegate._copy(null);
    }
  }

  public void _pipe(JSON arg) {
    delegate._pipe(arg);
  }

  // Factory methods
  public static _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 _of() {
    return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456();
  }

  public static _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 _of(
      JSON okValue, String errorValue) {
    return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456(
        Result._of(okValue, errorValue));
  }

  public static _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456 _of(
      Result result) {
    if (result != null) {
      return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456(result);
    }
    return new _Result_F734611776882C04A5CCDA69711ED473DD228A77B04C00E81CD193016B024456();
  }

}