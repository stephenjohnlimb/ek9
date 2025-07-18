package org.ek9.lang;

import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Similar to the Java Optional class.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Optional of type T""")
public class Optional extends BuiltinType {

  private Any state;

  @Ek9Constructor("""
      Optional() as pure""")
  public Optional() {
    unSet();
    //default constructor
  }

  @Ek9Constructor("""
      Optional() as pure
        -> arg0 as T""")
  public Optional(Any arg0) {
    unSet();
    assign(arg0);
  }

  @Ek9Method("""
      asEmpty() as pure
        <- rtn as Optional of T?""")
  public Optional asEmpty() {
    //Note that parameterised version of Optional will return their appropriate type.
    return _new();
  }

  @Ek9Method("""
      get() as pure
        <- rtn as T?""")
  public Any get() {
    if (isSet) {
      return state;
    }
    throw new Exception(String._of("No such element"));
  }

  @Ek9Method("""
      getOrDefault() as pure
        -> arg as T
        <- rtn as T?""")
  public Any getOrDefault(Any arg) {
    if (isSet) {
      return state;
    }
    return arg;
  }

  @Ek9Method("""
      iterator() as pure
        <- rtn as Iterator of T?""")
  public Iterator iterator() {
    if (isSet) {
      return Iterator._of(state);
    }
    return Iterator._of();
  }

  @Ek9Method("""
      whenPresent()
        -> acceptor as Acceptor of T""")
  public void whenPresent(Acceptor acceptor) {
    if (canProcess(acceptor)) {
      acceptor._call(state);
    }
  }

  @Ek9Method("""
      whenPresent() as pure
        -> consumer as Consumer of T""")
  public void whenPresent(Consumer consumer) {
    if (canProcess(consumer)) {
      consumer._call(state);
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    final var rtn = new String();
    if (isSet) {
      return state._string();
    }
    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      return state._hashcode();
    }
    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof Optional asOptional) {
      return _eq(asOptional);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Optional of T
        <- rtn as Boolean?""")
  public Boolean _eq(Optional arg) {
    if (canProcess(arg)) {
      return Boolean._of(Objects.equals(state, arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    return Boolean._of(!isSet);
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as T
        <- rtn as Boolean?""")
  public Boolean _contains(Any arg) {
    if (canProcess(arg)) {
      final var eqResult = this.state._eq(arg);
      if (eqResult.isSet) {
        return Boolean._of(eqResult.state);
      }
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Any""")
  public void _merge(Any arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Any""")
  public void _replace(Any arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Any""")
  public void _copy(Any arg) {
    if (arg != null) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Any""")
  public void _pipe(Any arg) {
    _copy(arg);
  }

  //Start of Utility methods

  void assign(Any arg) {
    if (arg != null) {
      this.state = arg;
      set();
    }
  }

  @Override
  protected Optional _new() {
    return new Optional();
  }

  public static Optional _of() {
    return new Optional();
  }

  public static Optional _of(Any any) {
    return new Optional(any);
  }
}
