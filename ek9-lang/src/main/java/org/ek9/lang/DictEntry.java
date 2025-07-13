package org.ek9.lang;

import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Represents a key-value pair entry for Dict in EK9.
 * <p>
 * DictEntry is a simple container for holding a key-value pair,
 * typically used when iterating over Dict entries or when adding
 * entries to a Dict.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    DictEntry of type (K, V) as open""")
public class DictEntry extends BuiltinType {

  // Key and value for this entry
  private Any keyValue = new Any() {
  };
  private Any entryValue = new Any() {
  };

  @Ek9Constructor("DictEntry() as pure")
  public DictEntry() {
    unSet();
  }

  @Ek9Constructor("""
      DictEntry() as pure
        ->
          k as K
          v as V""")
  public DictEntry(Any k, Any v) {
    unSet();
    if (isValid(k) && isValid(v)) {
      this.keyValue = k;
      this.entryValue = v;
      set();
    }
  }

  @Ek9Method("""
      key() as pure
        <- rtn as K?""")
  public Any key() {
    return keyValue;
  }

  @Ek9Method("""
      value() as pure
        <- rtn as V?""")
  public Any value() {
    return entryValue;
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as DictEntry of (K, V)
        <- rtn as Boolean?""")
  public Boolean _eq(DictEntry arg) {
    if (canProcess(arg)) {
      final var cmpResult = _cmp(arg);
      if (cmpResult.isSet) {
        return Boolean._of(cmpResult.state == 0);
      }
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as DictEntry of (K, V)
        <- rtn as Boolean?""")
  public Boolean _neq(DictEntry arg) {
    if (canProcess(arg)) {
      return _eq(arg)._negate();
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as DictEntry of (K, V)
        <- rtn as Integer?""")
  public Integer _cmp(DictEntry arg) {
    if (canProcess(arg)) {
      final var keyCompare = this.keyValue._cmp(arg.keyValue);
      if (keyCompare.isSet && keyCompare.state == 0) {
        //then now use comparison of the values as the final comparison.
        return this.entryValue._cmp(arg.entryValue);
      }
      return keyCompare;
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof DictEntry asDictEntry) {
      return _cmp(asDictEntry);
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(keyValue + "=" + entryValue);
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (isSet) {
      int result = Objects.hashCode(keyValue);
      result = 31 * result + Objects.hashCode(entryValue);
      return Integer._of(result);
    }
    return new Integer();
  }

  // Utility methods

  @Override
  protected DictEntry _new() {
    return new DictEntry();
  }


  // Factory methods
  public static DictEntry _of() {
    return new DictEntry();
  }

  public static DictEntry _of(Any key, Any value) {
    return new DictEntry(key, value);
  }
}