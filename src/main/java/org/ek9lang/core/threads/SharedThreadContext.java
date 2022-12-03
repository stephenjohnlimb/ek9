package org.ek9lang.core.threads;

import java.util.concurrent.locks.ReentrantLock;
import org.ek9lang.core.exception.CompilerException;

/**
 * Concept here is to be able to protect raw java objects from multiple threaded access.
 * These could be real full platform threads or the new virtualThreads from Java 19 onwards.
 * We need a range of objects structures that are not synchronized (so access is quick)
 * so we want a thread to effectively own the whole tree of data when it accesses it.
 */
public class SharedThreadContext<T> {
  private final ReentrantLock lock = new ReentrantLock();

  private final ProtectedData<T> protectedData;

  public SharedThreadContext(T toBeProtected) {
    this.protectedData = new ProtectedData<>(toBeProtected);
  }

  public interface ISharedThreadRunnable<T> {
    void run(SharedThreadContext<T> stc);
  }

  public void assertHeldByThread() {
    if (!lock.isHeldByCurrentThread()) {
      String msg = "An attempt was made to call a method in shared context without owning the required lock.";
      throw new CompilerException(msg);
    }
  }

  public void own(ISharedThreadRunnable<T> r) {
    try {
      lock.lock();
      r.run(this);
    } finally {
      lock.unlock();
    }
  }

  public T get() {
    assertHeldByThread();
    return protectedData.data();
  }

  /**
   * Used to wrap the actual data we want to protect.
   */
  private record ProtectedData<S>(S data) {
  }
}
