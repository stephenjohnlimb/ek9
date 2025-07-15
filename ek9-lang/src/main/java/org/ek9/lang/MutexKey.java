package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Used with a MutexLock to gain access to the item inside the MutexLock.
 * It is within the function that you implement that extends this where you can
 * be sure safe access to the item in the MutexLock in the thread safe way.
 * <p>
 * This is a parameterised type, here we use 'Any', the Ek9 compiler will use this
 * as a template and ensure type safety.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    MutexKey() of type T as abstract
      -> value as T""")
public class MutexKey implements Any {

  @Ek9Constructor("MutexKey() as pure")
  public MutexKey() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a MutexKey to implement this.

  public void _call(Any t) {
    //No op in this as it is Ek9 abstract.
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }


}
