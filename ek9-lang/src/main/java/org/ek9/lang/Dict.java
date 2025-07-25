package org.ek9.lang;

import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Represents the Dict type in EK9.
 * <p>
 * A Dict is a collection of key-value pairs, similar to a Map or Dictionary
 * in other languages. Keys must be unique, and the Dict maintains the
 * association between keys and their corresponding values.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Dict of type (K, V) as open""")
public class Dict extends BuiltinType {

  // Internal Java map for storage
  java.util.Map<Any, Any> state = new java.util.LinkedHashMap<>();

  @Ek9Constructor("Dict() as pure")
  public Dict() {
    set();
  }

  @Ek9Constructor("""
      Dict() as pure
        ->
          k as K
          v as V""")
  public Dict(Any k, Any v) {
    this();
    if (canProcess(k) && canProcess(v)) {
      state.put(k, v);
    }
  }

  @Ek9Method("""
      get() as pure
        -> arg0 as K
        <- rtn as V?""")
  public Any get(Any arg0) {
    if (canProcess(arg0)) {
      final var located = state.get(arg0);
      if (located != null) {
        return located;
      }
      throw new Exception(String._of("arg0 (key) provided is not valid: " + arg0.toString()));
    }
    throw new Exception(String._of("arg0 (key) provided is not valid"));
  }

  @Ek9Method("""
      getOrDefault() as pure
        ->
          arg0 as K
          arg1 as V
        <-
          rtn as V?""")
  public Any getOrDefault(Any arg0, Any arg1) {
    if (canProcess(arg0)) {
      final var value = state.get(arg0);
      return value != null ? value : arg1;
    }
    return arg1;
  }

  @Ek9Method("""
      iterator() as pure
        <- rtn as DictEntry of (K, V)?""")
  public Iterator iterator() {
    final var entries = new java.util.ArrayList<Any>();
    for (final var entry : state.entrySet()) {
      entries.add(DictEntry._of(entry.getKey(), entry.getValue()));
    }
    return Iterator._of(entries);
  }

  @Ek9Method("""
      keys() as pure
        <- rtn as Iterator of K?""")
  public Iterator keys() {
    return Iterator._of(new java.util.ArrayList<>(state.keySet()));
  }

  @Ek9Method("""
      values() as pure
        <- rtn as Iterator of V?""")
  public Iterator values() {
    return Iterator._of(new java.util.ArrayList<>(state.values()));
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof Dict asDict) {
      return _eq(asDict);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Dict of (K, V)
        <- rtn as Boolean?""")
  public Boolean _eq(Dict arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Dict of (K, V)
        <- rtn as Boolean?""")
  public Boolean _neq(Dict arg) {
    if (canProcess(arg)) {
      return Boolean._of(!this.state.equals(arg.state));
    }
    return new Boolean();
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
        -> arg as Dict of (K, V)
        <- rtn as Dict of (K, V)?""")
  public Dict _add(Dict arg) {
    final var rtn = _new();
    rtn.state = new java.util.LinkedHashMap<>(this.state);

    if (canProcess(arg)) {
      rtn.state.putAll(arg.state);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as DictEntry of (K, V)
        <- rtn as Dict of (K, V)?""")
  public Dict _add(DictEntry arg) {
    final var rtn = _new();
    rtn.state = new java.util.LinkedHashMap<>(this.state);

    if (canProcess(arg)) {
      rtn.state.put(arg.key(), arg.value());
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Dict of (K, V)
        <- rtn as Dict of (K, V)?""")
  public Dict _sub(Dict arg) {
    final var rtn = _new();
    rtn.state = new java.util.LinkedHashMap<>(this.state);

    if (canProcess(arg)) {
      for (final var key : arg.state.keySet()) {
        rtn.state.remove(key);
      }
    }

    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(asString());
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (isSet) {
      return Integer._of(Objects.hashCode(state));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (isSet) {
      final var jsonObject = new JSON().object(); // Create JSON object
      for (final var entry : state.entrySet()) {
        final var keyString = entry.getKey()._string();
        final var valueJson = entry.getValue()._json();
        // Use JSON constructor with name-value pair and merge into object
        final var keyValuePair = new JSON(keyString, valueJson);
        jsonObject._merge(keyValuePair);
      }
      return jsonObject;
    }
    return new JSON();
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (isSet) {
      return Boolean._of(state.isEmpty());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    if (isSet) {
      return Integer._of(state.size());
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as K
        <- rtn as Boolean?""")
  public Boolean _contains(Any arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.containsKey(arg));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Dict of (K, V)""")
  public void _merge(Dict arg) {
    if (canProcess(arg)) {
      this.state.putAll(arg.state);
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Dict of (K, V)""")
  public void _replace(Dict arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Dict of (K, V)""")
  public void _copy(Dict arg) {
    if (canProcess(arg)) {
      this.state = new java.util.LinkedHashMap<>(arg.state);
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as DictEntry of (K, V)""")
  public void _pipe(DictEntry arg) {
    if (canProcess(arg)) {
      this.state.put(arg.key(), arg.value());
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as Dict of (K, V)""")
  public void _addAss(Dict arg) {
    if (canProcess(arg)) {
      this.state.putAll(arg.state);
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as DictEntry of (K, V)""")
  public void _addAss(DictEntry arg) {
    if (canProcess(arg)) {
      this.state.put(arg.key(), arg.value());
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as K""")
  public void _subAss(Any arg) {
    if (canProcess(arg)) {
      this.state.remove(arg);
    }
  }

  // Utility methods

  @Override
  protected Dict _new() {
    return new Dict();
  }

  private java.lang.String asString() {
    if (isSet) {
      final var builder = new StringBuilder();
      builder.append("{");
      boolean first = true;
      for (final var entry : state.entrySet()) {
        if (!first) {
          builder.append(", ");
        }
        builder.append(entry.getKey()).append("=").append(entry.getValue());
        first = false;
      }
      builder.append("}");
      return builder.toString();
    }
    return "";
  }

  // Factory methods
  public static Dict _of() {
    return new Dict();
  }

  public static Dict _of(Any key, Any value) {
    return new Dict(key, value);
  }

  public static Dict _of(java.util.Map<Any, Any> map) {
    final var dict = new Dict();
    if (map != null) {
      dict.state.putAll(map);
    }
    return dict;
  }
}