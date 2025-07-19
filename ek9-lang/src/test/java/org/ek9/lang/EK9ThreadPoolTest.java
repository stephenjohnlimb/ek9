package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Test the internal EK9ThreadPool class.
 * This tests the Java implementation that supports EK9 TCP functionality.
 */
class EK9ThreadPoolTest {

  @Test
  void testConstruction() {
    final var threadPool = new EK9ThreadPool(5);
    assertNotNull(threadPool);
    assertFalse(threadPool.isShutdown());
    assertFalse(threadPool.isTerminated());
    threadPool.close();
  }

  @Test
  void testConstructionWithInvalidMaxConcurrent() {
    assertThrows(Exception.class, () -> new EK9ThreadPool(0));
    assertThrows(Exception.class, () -> new EK9ThreadPool(-1));
  }

  @Test
  void testShutdown() throws java.lang.Exception {
    final var threadPool = new EK9ThreadPool(2);

    // Submit a task
    Future<String> future = threadPool.submit(() -> String._of("test"));
    assertEquals(String._of("test"), future.get());

    // Shutdown
    threadPool.shutdown();
    assertTrue(threadPool.isShutdown());

    // Should not accept new tasks
    assertThrows(Exception.class, () ->
        threadPool.submit(() -> "should fail"));

    // Wait for termination
    assertTrue(threadPool.awaitTermination(1, TimeUnit.SECONDS));
    assertTrue(threadPool.isTerminated());
  }

  @Test
  void testShutdownNow() {
    final var threadPool = new EK9ThreadPool(2);

    threadPool.shutdownNow();
    assertTrue(threadPool.isShutdown());

    // Should not accept new tasks
    assertThrows(Exception.class, () ->
        threadPool.submit(() -> "should fail"));
  }

  @Test
  void testClose() throws java.lang.Exception {
    final var threadPool = new EK9ThreadPool(2);
    AtomicInteger completedTasks = new AtomicInteger(0);

    // Submit some quick tasks
    for (int i = 0; i < 5; i++) {
      threadPool.submit(() -> {
        completedTasks.incrementAndGet();
        return null;
      });
    }

    // Close should shutdown gracefully
    String closeResult = threadPool.close();
    assertNotNull(closeResult);
    assertTrue(threadPool.isShutdown());

    // Give tasks time to complete
    Thread.sleep(100);
    assertEquals(5, completedTasks.get());
  }

  @Test
  void testCloseWithLongRunningTasks() throws java.lang.Exception {
    final var threadPool = new EK9ThreadPool(2);
    CountDownLatch taskStarted = new CountDownLatch(1);

    // Submit a long-running task
    threadPool.submit(() -> {
      try {
        taskStarted.countDown();
        Thread.sleep(2000); // Long running task
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
      return null;
    });

    // Wait for task to start
    assertTrue(taskStarted.await(1, TimeUnit.SECONDS));

    // Close should complete within reasonable time even with long-running task
    long startTime = System.currentTimeMillis();
    String closeResult = threadPool.close();
    long duration = System.currentTimeMillis() - startTime;
    assertNotNull(closeResult);

    assertTrue(threadPool.isShutdown());
    // Should complete within timeout period (5 + 2 = 7 seconds max)
    assertTrue(duration < 8000, "Close took too long: " + duration + "ms");
  }

  @Test
  void testMultipleClose() {
    final var threadPool = new EK9ThreadPool(1);

    // Multiple close calls should be safe
    String result1 = threadPool.close();
    String result2 = threadPool.close();
    String result3 = threadPool.close();
    assertNotNull(result1);
    assertNotNull(result2);
    assertNotNull(result3);

    assertTrue(threadPool.isShutdown());
  }

  @Test
  void testAwaitTermination() throws java.lang.Exception {
    final var threadPool = new EK9ThreadPool(1);

    // Submit a short task
    threadPool.submit(() -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
      return null;
    });

    threadPool.shutdown();

    // Should terminate within timeout
    assertTrue(threadPool.awaitTermination(1, TimeUnit.SECONDS));
    assertTrue(threadPool.isTerminated());
  }

  @Test
  void testAwaitTerminationTimeout() throws java.lang.Exception {
    final var threadPool = new EK9ThreadPool(1);

    // Submit a long-running task
    threadPool.submit(() -> {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
      return null;
    });

    threadPool.shutdown();

    // Should timeout
    assertFalse(threadPool.awaitTermination(100, TimeUnit.MILLISECONDS));
    assertFalse(threadPool.isTerminated());

    // Cleanup
    threadPool.shutdownNow();
  }

  @Test
  void testTaskException() throws Exception {
    final var threadPool = new EK9ThreadPool(1);

    // Task that throws exception
    Future<String> future = threadPool.submit(() -> {
      throw new RuntimeException("Test exception");
    });

    // Exception should be wrapped in ExecutionException
    assertThrows(ExecutionException.class, future::get);

    threadPool.close();
  }

  @Test
  void testMaxConcurrentLimit() throws InterruptedException {
    // Create pool limited to 2 concurrent tasks
    final var threadPool = new EK9ThreadPool(2);

    AtomicInteger tasksRunning = new AtomicInteger(0);
    AtomicInteger maxConcurrent = new AtomicInteger(0);
    CountDownLatch allTasksComplete = new CountDownLatch(4);

    // Submit 4 quick tasks
    for (int i = 0; i < 4; i++) {
      threadPool.submit(() -> {
        // Track concurrent execution
        int current = tasksRunning.incrementAndGet();
        maxConcurrent.updateAndGet(max -> Math.max(max, current));

        try {
          // Very short work simulation
          Thread.sleep(100);
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
        } finally {
          tasksRunning.decrementAndGet();
          allTasksComplete.countDown();
        }

        return null;
      });
    }

    // Wait for all tasks to complete
    assertTrue(allTasksComplete.await(5, TimeUnit.SECONDS), "All tasks should complete");

    // Verify concurrency was limited
    assertTrue(maxConcurrent.get() <= 2,
        "Max concurrent should not exceed 2, but was: " + maxConcurrent.get());
    assertEquals(0, tasksRunning.get(), "All tasks should be done");

    threadPool.close();
  }

  @Test
  void testSemaphoreWithTaskExceptions() throws InterruptedException {
    // Test that semaphore permits are properly released even when tasks throw exceptions
    final var threadPool = new EK9ThreadPool(1);

    AtomicInteger taskCount = new AtomicInteger(0);

    // Submit a task that throws an exception
    Future<?> failingTask = threadPool.submit(() -> {
      taskCount.incrementAndGet();
      throw new RuntimeException("Test exception");
    });

    // Wait for the task to complete (and fail)
    try {
      failingTask.get(1, TimeUnit.SECONDS);
    } catch (ExecutionException | TimeoutException _) {
      // Expected - task threw exception
    }

    // Submit another task - should be able to run (semaphore permit was released)
    CountDownLatch secondTaskStarted = new CountDownLatch(1);
    threadPool.submit(() -> {
      taskCount.incrementAndGet();
      secondTaskStarted.countDown();
      return null;
    });

    // Second task should be able to start (proving semaphore permit was released)
    assertTrue(secondTaskStarted.await(1, TimeUnit.SECONDS),
        "Second task should start, proving semaphore permit was released after exception");
    assertEquals(2, taskCount.get(), "Both tasks should have run");

    threadPool.close();
  }
}