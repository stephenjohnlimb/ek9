package org.ek9lang.integration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes EK9 wrapper binary or compiler JAR and captures results.
 * Supports both wrapper execution and direct JAR execution for comparison testing.
 */
final class Ek9ProcessExecutor {

  private final File wrapperBinary;
  private final File compilerJar;
  private final File workingDirectory;
  private final int timeoutSeconds;

  /**
   * Create executor with default 30-second timeout.
   */
  public Ek9ProcessExecutor(File wrapperBinary, File compilerJar, File workingDirectory) {
    this(wrapperBinary, compilerJar, workingDirectory, 30);
  }

  /**
   * Create executor with custom timeout.
   */
  public Ek9ProcessExecutor(File wrapperBinary, File compilerJar, File workingDirectory,
                            int timeoutSeconds) {
    this.wrapperBinary = wrapperBinary;
    this.compilerJar = compilerJar;
    this.workingDirectory = workingDirectory;
    this.timeoutSeconds = timeoutSeconds;

    if (!wrapperBinary.exists() || !wrapperBinary.canExecute()) {
      throw new IllegalStateException(
          "EK9 wrapper not found or not executable: " + wrapperBinary.getAbsolutePath());
    }

    if (!compilerJar.exists() || !compilerJar.canRead()) {
      throw new IllegalStateException(
          "EK9 compiler JAR not found: " + compilerJar.getAbsolutePath());
    }

    if (!workingDirectory.exists() || !workingDirectory.isDirectory()) {
      throw new IllegalStateException(
          "Working directory not found: " + workingDirectory.getAbsolutePath());
    }
  }

  /**
   * Execute EK9 wrapper with arguments.
   * Example: executeWrapper("-c", "HelloWorld.ek9")
   */
  public ProcessResult executeWrapper(final String... args) {
    final var command = new ArrayList<String>();
    command.add(wrapperBinary.getAbsolutePath());
    command.addAll(Arrays.asList(args));

    return executeProcess(command, null);
  }

  /**
   * Execute EK9 wrapper with stdin input and arguments.
   * Example: executeWrapperWithStdin("test input", "program.ek9", "-r", "ProgramName")
   */
  public ProcessResult executeWrapperWithStdin(final String stdinInput, final String... args) {
    final var command = new ArrayList<String>();
    command.add(wrapperBinary.getAbsolutePath());
    command.addAll(Arrays.asList(args));

    return executeProcess(command, stdinInput);
  }

  /**
   * Execute compiler JAR directly (bypasses wrapper).
   * Example: executeJar("-c", "HelloWorld.ek9")
   */
  public ProcessResult executeJar(final String... args) {
    final var command = new ArrayList<String>();
    command.add("java");
    command.add("-jar");
    command.add(compilerJar.getAbsolutePath());
    command.addAll(Arrays.asList(args));

    return executeProcess(command, null);
  }

  /**
   * Execute process and capture output.
   */
  private ProcessResult executeProcess(final List<String> command, final String stdinInput) {
    try {
      final var pb = new ProcessBuilder(command);
      pb.directory(workingDirectory);

      // Set EK9_HOME to compiler JAR directory
      pb.environment().put("EK9_HOME", compilerJar.getParentFile().getAbsolutePath());

      // Set compiler memory to 512MB for integration tests (avoids memory pressure with 8 parallel tests)
      pb.environment().put("EK9_COMPILER_MEMORY", "-Xmx512m");

      final var process = pb.start();

      // Write stdin if provided
      if (stdinInput != null && !stdinInput.isEmpty()) {
        try (final var outputStream = process.getOutputStream()) {
          outputStream.write(stdinInput.getBytes(StandardCharsets.UTF_8));
          outputStream.flush();
        }
      }

      // Capture stdout
      final var stdout = readStream(process.getInputStream());

      // Capture stderr
      final var stderr = readStream(process.getErrorStream());

      // Wait for completion with timeout
      final var completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

      if (!completed) {
        process.destroyForcibly();
        throw new RuntimeException(
            "Process timed out after " + timeoutSeconds + " seconds: " + command);
      }

      final var exitCode = process.exitValue();

      return new ProcessResult(exitCode, stdout, stderr);

    } catch (final IOException | InterruptedException e) {
      throw new RuntimeException("Failed to execute process: " + command, e);
    }
  }

  /**
   * Read stream completely into string.
   */
  private String readStream(final java.io.InputStream stream) throws IOException {
    final var result = new StringBuilder();
    try (final var reader =
             new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line).append("\n");
      }
    }
    return result.toString();
  }
}
