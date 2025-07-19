package org.ek9.lang;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Internal Java thread pool implementation.
 * <p>
 * Uses Java 23 virtual threads for scalable concurrent execution.
 * This class is not exposed to EK9 language - it's an internal implementation utility.
 * </p>
 * <p>
 * While this is called a thread pool, it actually just uses new virtual threads which are unbound.
 * However, there is always a need to 'create' bottlenecks and have control.
 * </p>
 * <p>
 * So for example while it may be possible to have unlimited threads, there will be some resources
 * (such as databases) that do need to have the number of concurrent accesses limited.
 * It is also the case that there will be 'finite' memory and so it is necessary to control and constrain that
 * use by limiting the number of concurrent threads.
 * </p>
 */
public class EK9ThreadPool {

  private final ExecutorService executor;
  private volatile boolean shutdown = false;

  //Keep a track of the active tasks, we must limit the amount to maxConcurrent
  private final Semaphore semaphore;

  /**
   * Creates a thread pool with the specified maximum number of concurrent threads.
   * Uses virtual threads for lightweight, scalable concurrency.
   *
   * @param maxConcurrent Maximum number of concurrent tasks to execute
   */
  public EK9ThreadPool(int maxConcurrent) {
    if (maxConcurrent <= 0) {
      throw Exception._of("maxConcurrent must be positive");
    }
    semaphore = new Semaphore(maxConcurrent);

    // Use virtual thread executor for scalable concurrency
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Submits a callable task for execution.
   * Note that the Optional may not return a Future if the thread pool has been exhausted.
   */
  public <T> Future<T> submit(Callable<T> task) {
    if (shutdown) {
      throw Exception._of("ThreadPool has been shutdown");
    }

    try {

      //This will block until a semaphore is released and hence we honour the max concurrent setting.
      semaphore.acquire();
      return executor.submit(() -> {
        try {
          return task.call();
        } finally {
          semaphore.release();
        }
      });

    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw org.ek9.lang.Exception._of(ex);
    } catch (java.lang.Exception ex) {
      throw org.ek9.lang.Exception._of(ex);
    }
  }

  /**
   * Initiates graceful shutdown of the thread pool.
   * Previously submitted tasks are executed, but no new tasks will be accepted.
   */
  public void shutdown() {
    if (!shutdown) {
      shutdown = true;
      executor.shutdown();
    }
  }

  /**
   * Attempts to stop all actively executing tasks and halts processing of waiting tasks.
   * This method does not wait for actively executing tasks to terminate.
   */
  public void shutdownNow() {
    if (!shutdown) {
      shutdown = true;
      executor.shutdownNow();
    }
  }

  /**
   * Blocks until all tasks have completed execution after a shutdown request,
   * or the timeout occurs, whichever happens first.
   *
   * @param timeout The maximum time to wait
   * @param unit    The time unit of the timeout argument
   * @return true if this executor terminated and false if the timeout elapsed
   * @throws InterruptedException if interrupted while waiting
   */
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }

  /**
   * Returns true if this executor has been shut down.
   *
   * @return true if shutdown
   */
  public boolean isShutdown() {
    return shutdown;
  }

  /**
   * Returns true if all tasks have completed following shut down.
   *
   * @return true if terminated
   */
  public boolean isTerminated() {
    return executor.isTerminated();
  }

  /**
   * Closes the thread pool and releases all resources.
   * This method performs a graceful shutdown with a timeout.
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  public String close() {
    final var anyErrorMessage = new String();
    shutdown();
    try {
      // Wait up to 5 seconds for graceful shutdown
      if (!awaitTermination(5, TimeUnit.SECONDS)) {
        shutdownNow();
        // Wait a bit more for tasks to respond to being cancelled
        if (!awaitTermination(2, TimeUnit.SECONDS)) {
          anyErrorMessage._copy(String._of("Non Graceful ThreadPool shutdown"));
        }
      }
    } catch (InterruptedException _) {
      // Re-cancel if current thread also interrupted
      shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
    return anyErrorMessage;
  }
}