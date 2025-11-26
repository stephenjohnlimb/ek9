package org.ek9lang.compiler.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A PrintStream that delegates to thread-local outputs.
 * This enables parallel test execution where each thread captures
 * its own output without interference from other threads.
 *
 * <p>When a thread has registered an output stream (via {@link #setThreadOutput}),
 * all writes from that thread go to its specific stream. Otherwise, writes go
 * to the fallback stream (typically the original System.out).</p>
 *
 * <p>This class is thread-safe. Each thread's output is completely isolated.</p>
 */
final class ThreadLocalPrintStream extends PrintStream {

  private static final ThreadLocal<ByteArrayOutputStream> THREAD_OUTPUT = new ThreadLocal<>();

  private final PrintStream fallback;

  /**
   * Create a thread-local delegating PrintStream.
   *
   * @param fallback The stream to use when no thread-local output is registered
   */
  ThreadLocalPrintStream(final PrintStream fallback) {
    super(new DelegatingOutputStream(fallback), true);
    this.fallback = fallback;
  }

  /**
   * Set the output capture for the current thread.
   * All subsequent writes from this thread will go to this stream.
   *
   * @param output The stream to capture output to
   */
  static void setThreadOutput(final ByteArrayOutputStream output) {
    THREAD_OUTPUT.set(output);
  }

  /**
   * Clear the thread-local output and return the captured content.
   *
   * @return The captured output, or empty string if nothing was captured
   */
  static String clearAndGet() {
    final ByteArrayOutputStream output = THREAD_OUTPUT.get();
    THREAD_OUTPUT.remove();
    return output != null ? output.toString() : "";
  }

  /**
   * Get the fallback stream (original System.out).
   */
  PrintStream getFallback() {
    return fallback;
  }

  /**
   * OutputStream that delegates to thread-local or fallback.
   */
  private static class DelegatingOutputStream extends OutputStream {
    private final PrintStream fallback;

    DelegatingOutputStream(final PrintStream fallback) {
      this.fallback = fallback;
    }

    @Override
    public void write(final int b) {
      final ByteArrayOutputStream threadOutput = THREAD_OUTPUT.get();
      if (threadOutput != null) {
        threadOutput.write(b);
      } else {
        fallback.write(b);
      }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
      final ByteArrayOutputStream threadOutput = THREAD_OUTPUT.get();
      if (threadOutput != null) {
        threadOutput.write(b, off, len);
      } else {
        fallback.write(b, off, len);
      }
    }

    @Override
    public void flush() {
      final ByteArrayOutputStream threadOutput = THREAD_OUTPUT.get();
      if (threadOutput == null) {
        fallback.flush();
      }
      // ByteArrayOutputStream doesn't need flushing
    }
  }
}
