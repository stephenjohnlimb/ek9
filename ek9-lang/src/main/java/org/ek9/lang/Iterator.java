package org.ek9.lang;

import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * While this is abstract in an EK9 sense, it is possible to create the Java version.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Iterator of type T as abstract""")
public class Iterator extends BuiltinType {

  java.util.Iterator<Any> state;

  @Ek9Constructor("""
      Iterator() as pure""")
  public Iterator() {
    //Default constructor
    assign(new java.util.Iterator<>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Any next() {
        throw new Exception(String._of("No such element"));
      }
    });
  }

  @Ek9Constructor("""
      Iterator() as pure
        -> arg0 as T""")
  public Iterator(Any arg0) {
    //Really just for Ek9 type inference.

    assign(new java.util.Iterator<>() {
      private boolean supplied;

      @Override
      public boolean hasNext() {
        return !supplied;
      }

      @Override
      public Any next() {
        if (!supplied) {
          supplied = true;
          return arg0;
        }
        throw new Exception(String._of("No such element"));
      }
    });
  }

  @Ek9Method("""
      hasNext() as pure abstract
        <- rtn as Boolean?""")
  public Boolean hasNext() {
    return Boolean._of(state.hasNext());
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      next() as abstract
        <- rtn as T?""")
  public Any next() {
    //Might result in an EK9 Exception if nothing present.
    try {
      return state.next();
    } catch (java.lang.Exception _) {
      throw new Exception(String._of("No such element"));
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return hasNext();
  }

  //Start of Utility methods

  private void assign(java.util.Iterator<Any> value) {
    this.state = value;
    set();
  }

  @Override
  protected Iterator _new() {
    return new Iterator();
  }

  @Override
  public final boolean equals(final Object o) {
    if (!(o instanceof final Iterator iterator)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    return Objects.equals(state, iterator.state);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Objects.hashCode(state);
    return result;
  }

  public static Iterator _of(java.util.List<Any> value) {
    Iterator rtn = new Iterator();
    if (value != null) {
      rtn.assign(value.iterator());
    }
    return rtn;
  }

  public static Iterator _of(java.util.Iterator<Any> value) {
    Iterator rtn = new Iterator();
    if (value != null) {
      rtn.assign(value);
    }
    return rtn;
  }

  public static Iterator _of(Any value) {
    if (value != null) {
      return new Iterator(value);
    }
    return new Iterator();
  }

  public static Iterator _of() {
    return new Iterator();
  }
}
