package org.ek9lang.core.threads;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.ek9lang.core.exception.AssertValue;

/**
 * Concept here is to be able to protect raw java objects from multiple threaded access.
 * These could be real full platform threads or the new virtualThreads from Java 19 onwards.
 * We need a range of objects structures that are not synchronized (so access is quick)
 * so we want a thread to effectively own the whole tree of data when it accesses it.
 * This has been modeled on the 'Consumer' so hand your data over and then call 'accept' on this
 * object with your own consumer, and you'll get data back (don't hold references to it).
 * Keep it protected within this context.
 */
public class SharedThreadContext<T> implements Consumer<Consumer<T>> {
  private final ReentrantLock lock = new ReentrantLock();
  private final T protectedData;

  /**
   * Wraps the object being protected - hides it, so it can only be accessed via your consumer.
   */
  public SharedThreadContext(T toBeProtected) {
    AssertValue.checkNotNull("Data to be protected cannot be null", toBeProtected);
    this.protectedData = toBeProtected;
  }

  /**
   * Take ownership of the reentrant lock - wait if another thread has it.
   * Once ownership is taken then your consumer 'accept'
   * method will be called with your protected data as the parameter.
   */
  @Override
  public void accept(Consumer<T> consumer) {
    try {
      lock.lock();
      consumer.accept(protectedData);
    } finally {
      lock.unlock();

    }
  }
}
