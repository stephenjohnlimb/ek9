package org.ek9.lang;

import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Result type for EK9 - represents either a success (OK) value or an error (ERROR) value.
 * Similar to Result types in other languages, providing explicit error handling.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Result of type (O, E)""")
public class Result extends BuiltinType {

  private Any okValue;
  private Any errorValue;

  @Ek9Constructor("""
      Result() as pure""")
  public Result() {
    unSet();
  }

  @Ek9Constructor("""
      Result() as pure
        ->
          ok as O
          error as E""")
  public Result(Any ok, Any error) {
    //Note only set if actually valid, is isSet.
    unSet();
    if (isValid(ok)) {
      okValue = ok;
      set();
    }
    if (isValid(error)) {
      errorValue = error;
      set();
    }
  }

  @Ek9Method("""
      asEmpty() as pure
        <- rtn as Result of (O, E)?""")
  public Result asEmpty() {
    return _new();
  }

  @Ek9Method("""
      asOk() as pure
        -> arg0 as O
        <- rtn as Result of (O, E)?""")
  public Result asOk(Any arg0) {
    Result result = _new();
    if (isValid(arg0)) {
      result.okValue = arg0;
      result.set();
    }
    return result;
  }

  @Ek9Method("""
      asError() as pure
        -> arg0 as E
        <- rtn as Result of (O, E)?""")
  public Result asError(Any arg0) {
    Result result = _new();
    if (isValid(arg0)) {
      result.errorValue = arg0;
      result.set();
    }
    return result;
  }

  @Ek9Method("""
      isOk() as pure
        <- rtn as Boolean?""")
  public Boolean isOk() {
    return Boolean._of(okValue != null);
  }

  @Ek9Method("""
      ok() as pure
        <- rtn as O?""")
  public Any ok() {
    if (okValue != null) {
      return okValue;
    }
    throw new Exception(String._of("No such element"));
  }

  @Ek9Method("""
      okOrDefault() as pure
        -> arg0 as O
        <- rtn as O?""")
  public Any okOrDefault(Any arg0) {
    if (okValue != null) {
      return okValue;
    }
    return arg0;
  }

  @Ek9Method("""
      getOrDefault() as pure
        -> arg0 as O
        <- rtn as O?""")
  public Any getOrDefault(Any arg0) {
    return okOrDefault(arg0);
  }

  @Ek9Method("""
      whenOk()
        -> acceptor as Acceptor of O""")
  public void whenOk(Acceptor acceptor) {
    if (okValue != null && canProcess(acceptor)) {
      acceptor._call(okValue);
    }
  }

  @Ek9Method("""
      whenOk() as pure
        -> consumer as Consumer of O""")
  public void whenOk(Consumer consumer) {
    if (okValue != null && canProcess(consumer)) {
      consumer._call(okValue);
    }
  }

  @Ek9Method("""
      isError() as pure
        <- rtn as Boolean?""")
  public Boolean isError() {
    return Boolean._of(errorValue != null);
  }

  @Ek9Method("""
      error() as pure
        <- rtn as E?""")
  public Any error() {
    if (errorValue != null) {
      return errorValue;
    }
    throw new Exception(String._of("No such element"));
  }

  @Ek9Method("""
      whenError()
        -> acceptor as Acceptor of E""")
  public void whenError(Acceptor acceptor) {
    if (errorValue != null && canProcess(acceptor)) {
      acceptor._call(errorValue);
    }
  }

  @Ek9Method("""
      whenError() as pure
        -> consumer as Consumer of E""")
  public void whenError(Consumer consumer) {
    if (errorValue != null && canProcess(consumer)) {
      consumer._call(errorValue);
    }
  }

  @Ek9Method("""
      iterator() as pure
        <- rtn as Iterator of O?""")
  public Iterator iterator() {
    if (okValue != null) {
      return Iterator._of(okValue);
    }
    return Iterator._of();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Result of (O, E)
        <- rtn as Boolean?""")
  public Boolean _eq(Result arg) {
    final var rtn = new Boolean();

    if (canProcess(arg)) {

      if ((okValue != null && arg.okValue == null)
          || (okValue == null && arg.okValue != null)) {
        return Boolean._of(false);
      }

      if ((errorValue != null && arg.errorValue == null)
          || (errorValue == null && arg.errorValue != null)) {
        return Boolean._of(false);
      }

      if (okValue != null) {
        rtn._pipe(this.okValue._eq(arg.okValue));
      }
      if (errorValue != null) {
        rtn._pipe(this.errorValue._eq(arg.errorValue));
      }

    }
    return rtn;
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Result of (O, E)
        <- rtn as Boolean?""")
  public Boolean _neq(Result arg) {
    Boolean eq = _eq(arg);
    if (eq.isSet) {
      return eq._negate();
    }
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(okValue != null);
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {

    if (isSet) {
      StringBuilder builder = new StringBuilder("{");
      if (okValue != null) {
        builder.append(okValue._string());
      }
      if (errorValue != null) {
        if (builder.length() > 1) {
          builder.append(", ");
        }
        builder.append(errorValue._string());
      }
      builder.append("}");
      return String._of(builder.toString());
    }
    return new String();
  }

  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      rtn.assign(this.hashCode());
    }
    return rtn;
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    return Boolean._of(okValue == null && errorValue == null);
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as O
        <- rtn as Boolean?""")
  public Boolean _contains(Any arg) {
    if (okValue != null && arg != null) {
      return okValue._eq(arg);
    }
    return Boolean._of(false);
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as E
        <- rtn as Boolean?""")
  public Boolean _containsError(Any arg) {
    if (errorValue != null && arg != null) {
      return errorValue._eq(arg);
    }
    return Boolean._of(false);
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Result of (O, E)""")
  public void _merge(Result arg) {
    if (arg != null) {
      if (this.okValue == null) {
        this.okValue = arg.okValue;
      }
      if (this.errorValue == null) {
        this.errorValue = arg.errorValue;
      }
      if (this.okValue != null && this.errorValue != null) {
        set();
      }
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Result of (O, E)""")
  public void _replace(Result arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Result of (O, E)""")
  public void _copy(Result arg) {
    if (arg != null) {
      this.okValue = arg.okValue;
      this.errorValue = arg.errorValue;
      if (arg.isSet) {
        set();
      } else {
        unSet();
      }
    } else {
      unSet();
      this.okValue = null;
      this.errorValue = null;
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as O""")
  public void _pipe(Any arg) {
    if (isValid(arg)) {
      this.okValue = arg;
      set();
    }
  }

  //Start of Utility methods

  @Override
  protected Result _new() {
    return new Result();
  }

  @Override
  public final boolean equals(final Object o) {
    if (!(o instanceof final Result result)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    return Objects.equals(okValue, result.okValue) && Objects.equals(errorValue, result.errorValue);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Objects.hashCode(okValue);
    result = 31 * result + Objects.hashCode(errorValue);
    return result;
  }

  @Override
  public java.lang.String toString() {
    return _string().state;
  }

  // Factory methods
  public static Result _of() {
    return new Result();
  }

  public static Result _of(Any okValue, Any errorValue) {
    return new Result(okValue, errorValue);
  }

  public static Result _ofOk(Any okValue) {
    return new Result(okValue, new Any() {
    });
  }

  public static Result _ofError(Any errorValue) {
    return new Result(new Any() {
    }, errorValue);
  }
}