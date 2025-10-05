package org.ek9lang.compiler.backend.jvm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Normalizes JVM bytecode output from javap for stable test comparisons.
 * <p>
 * Normalization removes fragile elements like constant pool indices,
 * line number tables, and debug information while preserving essential
 * instruction sequences and method signatures.
 * </p>
 * <p>
 * This utility is specifically designed for testing bytecode generation
 * by enabling reliable comparison of expected vs actual bytecode output.
 * </p>
 */
public final class BytecodeNormalizer {

  private BytecodeNormalizer() {
    // Utility class - prevent instantiation
  }

  /**
   * Normalize bytecode from .class file bytes.
   *
   * @param classBytes Compiled .class file bytes
   * @return Normalized bytecode text suitable for test comparison
   * @throws RuntimeException if normalization fails
   */
  public static String normalize(final byte[] classBytes) {
    if (classBytes == null || classBytes.length == 0) {
      throw new IllegalArgumentException("Class bytes cannot be null or empty");
    }

    try {
      // Write bytes to temporary file for javap processing
      final Path tempFile = Files.createTempFile("ek9-bytecode-", ".class");
      try {
        Files.write(tempFile, classBytes);
        final String javapOutput = executeJavap(tempFile);
        return normalizeJavapOutput(javapOutput);
      } finally {
        Files.deleteIfExists(tempFile);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to normalize bytecode: " + e.getMessage(), e);
    }
  }

  /**
   * Execute javap on class file and capture output.
   *
   * @param classFile Path to .class file
   * @return Raw javap output
   * @throws Exception if javap execution fails
   */
  private static String executeJavap(final Path classFile) throws Exception {
    final ProcessBuilder pb = new ProcessBuilder("javap", "-c", "-p", classFile.toString());
    final Process process = pb.start();

    // Capture stdout
    final String output;
    try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      output = reader.lines().collect(Collectors.joining("\n"));
    }

    // Capture stderr for error reporting
    final String error;
    try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
      error = reader.lines().collect(Collectors.joining("\n"));
    }

    final int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
          "javap failed with exit code " + exitCode + "\nError: " + error);
    }

    return output;
  }

  /**
   * Normalize javap output by removing fragile elements.
   * <p>
   * Normalization rules:
   * </p>
   * <ul>
   *   <li>Remove "Compiled from" header</li>
   *   <li>Normalize constant pool references (#7 → #CP)</li>
   *   <li>Remove line number tables</li>
   *   <li>Remove LocalVariableTable sections</li>
   *   <li>Remove StackMapTable sections</li>
   *   <li>Remove LineNumberTable sections</li>
   *   <li>Normalize whitespace (collapse multiple blank lines)</li>
   * </ul>
   *
   * @param javapOutput Raw javap output
   * @return Normalized bytecode text
   */
  private static String normalizeJavapOutput(final String javapOutput) {
    return javapOutput
        // Remove "Compiled from" header
        .replaceAll("Compiled from \".*\"\\n", "")

        // Normalize constant pool references: #7 → #CP
        .replaceAll("#\\d+", "#CP")

        // Remove line number tables (format: "line 42: 0")
        .replaceAll("\\s+line \\d+:\\s*\\d+\\n", "")

        // Remove LocalVariableTable sections
        // Pattern: "LocalVariableTable:" followed by content until double newline or closing brace
        .replaceAll("(?s)LocalVariableTable:.*?(?=\\n\\n|\\n +}|\\n})", "")

        // Remove StackMapTable sections
        .replaceAll("(?s)StackMapTable:.*?(?=\\n\\n|\\n +}|\\n})", "")

        // Remove LineNumberTable sections
        .replaceAll("(?s)LineNumberTable:.*?(?=\\n\\n|\\n +}|\\n})", "")

        // Normalize whitespace: multiple blank lines → single blank line
        .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n")

        // Trim trailing/leading whitespace
        .trim();
  }
}
