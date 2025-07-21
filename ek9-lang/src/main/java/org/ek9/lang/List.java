package org.ek9.lang;

import java.util.Collections;
import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * The Ek9 form of a List.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    List of type T as open""")
public class List extends BuiltinType {

  //This is the actual implementation within the JVM
  //Just wrap this, quite thinly, but give Ek9 semantics.
  java.util.List<Any> state = new java.util.ArrayList<>();

  @Ek9Constructor("""
      List() as pure""")
  public List() {
    set();
    //Default constructor
  }

  @Ek9Constructor("""
      List() as pure
        -> arg0 as T""")
  public List(Any arg0) {
    set();
    state.add(arg0);
  }

  @Ek9Method("""
      get() as pure
        -> index as Integer
        <- rtn as T?""")
  public Any get(Integer index) {
    if (canProcess(index) && index.state >= 0) {
      final var location = (int) index.state;
      return get(location);
    }
    throw new Exception(String._of("Index provided is not valid: " + index.toString()));
  }

  @Ek9Method("""
      first() as pure
        <- rtn as T?""")
  public Any first() {
    if (state.isEmpty()) {
      throw new Exception(String._of("List is empty"));
    }
    return state.getFirst();
  }

  @Ek9Method("""
      last() as pure
        <- rtn as T?""")
  public Any last() {
    if (state.isEmpty()) {
      throw new Exception(String._of("List is empty"));
    }
    return state.getLast();
  }

  @Ek9Method("""
      reverse() as pure
        <- rtn as List of T?""")
  public List reverse() {

    //Do a copy to make a new list
    //only then reverse that new list, leave the original as-is.
    final var rtn = new List();
    rtn._copy(this);
    Collections.reverse(rtn.state);

    return rtn;
  }

  @Ek9Method("""
      iterator() as pure
        <- rtn as Iterator of T?""")
  public Iterator iterator() {
    return Iterator._of(state);
  }

  @Ek9Operator("""
      operator ~ as pure
        <- rtn as List of T?""")
  public List _negate() {
    return reverse();
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof List asList) {
      return _eq(asList);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as List of T
        <- rtn as Boolean?""")
  public Boolean _eq(List arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as List of T
        <- rtn as Boolean?""")
  public Boolean _neq(List arg) {
    return _eq(arg)._negate();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as List of T
        <- rtn as List of T?""")
  public List _add(List arg) {
    List rtn = _new();
    rtn.state = new java.util.ArrayList<>(state.size());
    rtn.state.addAll(state);

    if (canProcess(arg)) {
      rtn.state.addAll(arg.state);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as T
        <- rtn as List of T?""")
  public List _add(Any arg) {
    List rtn = _new();
    //Need to keep the semantics the same as +=
    rtn.state = new java.util.ArrayList<>(state.size() + 1);
    rtn.state.addAll(state);

    if (arg != null) {
      rtn.state.add(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as List of T
        <- rtn as List of T?""")
  public List _sub(List arg) {
    List rtn = _new();

    //First make a copy
    rtn.state = new java.util.ArrayList<>(state.size());
    rtn.state.addAll(state);

    if (canProcess(arg)) {
      //Make a hashset so looks can be quicker for large lists. Then remove.
      java.util.Set<Any> removeSet = new java.util.HashSet<>(arg.state);
      rtn.state.removeAll(removeSet);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as T
        <- rtn as List of T?""")
  public List _sub(Any arg) {
    List rtn = _new();
    rtn.state = new java.util.ArrayList<>(state.size());
    rtn.state.addAll(state);

    if (arg != null) {
      rtn.state.remove(arg);
    }
    return rtn;
  }


  //TODO JSON methods

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    return String._of(asString());
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      rtn.assign(Objects.hashCode(state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator #< as pure
        <- rtn as T?""")
  public Any _prefix() {
    return first();
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as T?""")
  public Any _suffix() {
    return last();
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    Boolean rtn = new Boolean();
    if (isSet) {
      rtn.assign(state.isEmpty());
    }
    return rtn;
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    Integer rtn = new Integer();
    if (isSet) {
      rtn.assign(state.size());
    }
    return rtn;
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as T
        <- rtn as Boolean?""")
  public Boolean _contains(Any arg) {
    if (arg != null) {
      return Boolean._of(this.state.contains(arg));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as List of T""")
  public void _merge(List arg) {
    if (isValid(arg)) {
      _addAss(arg);
    }
  }

  @Ek9Operator("""
      operator :~:
        -> arg as T""")
  public void _merge(Any arg) {
    if (isValid(arg)) {
      _addAss(arg);
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as List of T""")
  public void _replace(List arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as List of T""")
  public void _copy(List arg) {
    this.state = new java.util.ArrayList<>(arg.state.size());
    this.state.addAll(arg.state);
  }

  @Ek9Operator("""
      operator |
        -> arg as T""")
  public void _pipe(Any arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator +=
        -> arg as List of T""")
  public void _addAss(List arg) {
    if (canProcess(arg)) {
      this.state.addAll(arg.state);
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as T""")
  public void _addAss(Any arg) {
    if (canProcess(arg)) {
      this.state.add(arg);
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as List of T""")
  public void _subAss(List arg) {
    if (canProcess(arg)) {
      this.state.removeAll(arg.state);
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as T""")
  public void _subAss(Any arg) {
    if (canProcess(arg)) {
      this.state.remove(arg);
    }
  }

  //Start of Utility methods.

  private void assign(java.util.List<Any> value) {
    java.util.List<Any> before = state;
    boolean beforeIsValid = isSet;
    //Make a new internal list to use.
    this.state = new java.util.ArrayList<>(value.size());
    this.state.addAll(value);
    set();
    if (!validateConstraints().state) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + value);
    }
  }

  @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
  private Any get(int index) {

    final var numElements = state.size();
    if (numElements > index) {
      return state.get(index);
    }
    throw new Exception(String._of("Index out of bounds: " + numElements + "<=" + index));
  }


  private java.lang.String asString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    int size = this.state.size();
    for (int i = 0; i < size; i++) {
      if (i != 0) {
        buffer.append(", ");
      }
      Any obj = state.get(i);
      switch (obj) {
        case String string -> buffer.append("\"").append(string).append("\"");
        case Character character -> buffer.append("'").append(character).append("'");
        default -> buffer.append(obj);
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  @Override
  protected List _new() {
    return new List();
  }

  public static List _of(java.util.List<Any> value) {
    List rtn = new List();
    if (value != null) {
      rtn.assign(value);
    }
    return rtn;
  }
}
