package org.ek9.lang;

import java.util.concurrent.locks.ReentrantLock;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Threadsafe Mutex Lock.
 * Basically provide a value to 'give' to the Lock.
 * Then only access that lock by using a MutexKey.
 * <p>
 * Keep no other references to the data held in the lock anywhere.
 * Always do a 'replace' (:^:) or copy (:=:) or a merge (:^:) on the data item
 * when accessing via the MutexKey function you implement.
 * </p>
 * <p>
 * That way you are always just modifying the data item held in the MutexLock, when it is safe.
 * Be very careful with this class, you must make sure that the item held in the lock <b>and</b>
 * all the data it references is hold accessible via the MutexKey.
 * </p>
 * <p>
 * If you don't do this, you will get weird changes or race conditions as multiple threads access parts
 * of the locked item (which pretty much makes the MutexLock pointless).
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    MutexLock of type T""")
public class MutexLock extends BuiltinType {

  //This is the underlying locking mechanism.
  private final ReentrantLock lock = new ReentrantLock();

  //This is the actual item we are locking on.
  private Any state;

  @Ek9Constructor("""
      MutexLock() as pure""")
  public MutexLock() {
    unSet();
    //default constructor
  }

  @Ek9Constructor("""
      MutexLock() as pure
        -> arg0 as T""")
  public MutexLock(Any arg0) {
    unSet();
    assign(arg0);
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      enter() as pure
        -> withKey as MutexKey of T
        <- rtn as Boolean?""")
  public Boolean enter(MutexKey withKey) {
    if (canProcess(withKey)) {
      lock.lock();
      try {
        withKey._call(state);
      } catch (Exception _) {
        return Boolean._of(false);
      } finally {
        lock.unlock();
      }
      return Boolean._of(true);
    }
    return new Boolean();
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      tryEnter() as pure
        -> withKey as MutexKey of T
        <- rtn as Boolean?""")
  public Boolean tryEnter(MutexKey withKey) {
    if (canProcess(withKey)) {
      if (lock.tryLock()) {
        try {
          withKey._call(state);
          return Boolean._of(true);
        } catch (Exception _) {
          //Ignore this error.
          return Boolean._of(false);
        } finally {
          lock.unlock();
        }
      }
      return Boolean._of(false);
    }
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
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
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    final var checkEquals = _eq(arg);
    if (checkEquals.isSet && checkEquals.state) {
      return Integer._of(0);
    }
    //No concept of less than or greater than.
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof MutexLock asMutexLock) {
      return _eq(asMutexLock);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as MutexLock of T
        <- rtn as Boolean?""")
  public Boolean _eq(MutexLock arg) {
    if (canProcess(arg)) {
      //Only if actually the same object are they equal.
      return Boolean._of(this == arg);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    return Boolean._of(!isSet);
  }

  //Start of Utility methods

  void assign(Any arg) {
    //Can store unset objects in the MutexLock
    //They can then be mutated if needs be, to become set.
    if (arg != null) {
      this.state = arg;
      set();
    }
  }

  public static MutexLock _of() {
    return new MutexLock();
  }

  public static MutexLock _of(Any any) {
    return new MutexLock(any);
  }
}
