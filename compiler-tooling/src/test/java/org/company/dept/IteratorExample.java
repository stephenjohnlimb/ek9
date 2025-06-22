package org.company.dept;

import java.util.Iterator;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Demonstration of how an EK9 generic type can be expressed as a Java class.
 * Note, just really uses objects, the Parameterised type does the casting to
 * the correct type.
 * You can implement this in any way you see fit. Here I've just used a mix
 * of Iterator from List or a really simple implementation.
 */
@Ek9Class("IteratorExample of type T as abstract")
public class IteratorExample {

  private final Iterator<Object> implementation;

  /**
   * Just to be used internally.
   *
   * @param arg0 The list to iterate over.
   */
  public IteratorExample(final java.util.List<Object> arg0) {
    this.implementation = arg0.iterator();
  }

  @Ek9Constructor("""
      IteratorExample() as pure""")
  public IteratorExample() {
    //Just a default constructor.
    this.implementation = new Iterator<>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Object next() {
        return null;
      }
    };
  }

  @Ek9Constructor("""
      IteratorExample() as pure
        -> arg0 as T""")
  public IteratorExample(final Object arg0) {

    this.implementation = new Iterator<>() {
      private boolean supplied;

      @Override
      public boolean hasNext() {
        return !supplied;
      }

      @Override
      public Object next() {
        if (supplied) {
          return null;
        }
        supplied = true;
        return arg0;
      }
    };
  }

  @Ek9Method("""
      hasNext() as pure abstract
        <- rtn as Boolean?""")
  public Boolean hasNext() {
    return implementation.hasNext();
  }


  @Ek9Method("""
      next() as abstract
        <- rtn as T?""")
  public Object next() {
    return implementation.next();
  }

  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return hasNext();
  }

  public static IteratorExample _of(final java.util.List<Object> value) {
    return new IteratorExample(value);
  }

}
