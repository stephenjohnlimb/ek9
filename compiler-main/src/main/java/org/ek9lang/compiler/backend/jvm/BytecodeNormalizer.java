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
   * Normalize bytecode from .class file bytes with default settings.
   * Includes debug information (LineNumberTable and SourceDebugExtension).
   *
   * @param classBytes Compiled .class file bytes
   * @return Normalized bytecode text suitable for test comparison
   * @throws RuntimeException if normalization fails
   */
  public static String normalize(final byte[] classBytes) {
    return normalize(classBytes, true, true);
  }

  /**
   * Normalize bytecode from .class file bytes with configuration options.
   *
   * @param classBytes Compiled .class file bytes
   * @param includeLineNumberTable Include LineNumberTable debug information
   * @param includeSourceDebugExtension Include SourceDebugExtension (SMAP) information
   * @return Normalized bytecode text suitable for test comparison
   * @throws RuntimeException if normalization fails
   */
  public static String normalize(final byte[] classBytes,
                                  final boolean includeLineNumberTable,
                                  final boolean includeSourceDebugExtension) {
    if (classBytes == null || classBytes.length == 0) {
      throw new IllegalArgumentException("Class bytes cannot be null or empty");
    }

    try {
      // Write bytes to temporary file for javap processing
      final Path tempFile = Files.createTempFile("ek9-bytecode-", ".class");
      try {
        Files.write(tempFile, classBytes);
        final String javapOutput = executeJavap(tempFile);
        return normalizeJavapOutput(javapOutput, includeLineNumberTable, includeSourceDebugExtension);
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
    final ProcessBuilder pb = new ProcessBuilder("javap", "-c", "-p", "-l", "-v", classFile.toString());
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
   *   <li>Remove "Compiled from" header and Classfile metadata</li>
   *   <li>Remove constant pool section</li>
   *   <li>Remove class metadata (version, flags, etc.)</li>
   *   <li>Normalize constant pool references (#7 → #CP)</li>
   *   <li>KEEP constant pool comments for readability (method signatures, strings, etc.)</li>
   *   <li>Remove LocalVariableTable sections</li>
   *   <li>Remove StackMapTable sections</li>
   *   <li>Remove method descriptors and flags</li>
   *   <li>Optionally preserve LineNumberTable sections</li>
   *   <li>Optionally preserve SourceDebugExtension attribute (for SMAP)</li>
   *   <li>Normalize whitespace (collapse multiple blank lines)</li>
   * </ul>
   *
   * @param javapOutput Raw javap output
   * @param includeLineNumberTable Include LineNumberTable debug information
   * @param includeSourceDebugExtension Include SourceDebugExtension (SMAP) information
   * @return Normalized bytecode text
   */
  private static String normalizeJavapOutput(final String javapOutput,
                                              final boolean includeLineNumberTable,
                                              final boolean includeSourceDebugExtension) {
    return javapOutput
        // Remove Classfile header line (path, date, size, SHA-256)
        .replaceAll("Classfile .*\\n", "")
        .replaceAll(" {2}Last modified .*\\n", "")
        .replaceAll(" {2}SHA-256 checksum .*\\n", "")

        // Remove "Compiled from" header
        .replaceAll("Compiled from \".*\"\\n", "")

        // Remove class metadata (minor/major version, flags, this_class, super_class, etc.)
        .replaceAll(" {2}minor version: .*\\n", "")
        .replaceAll(" {2}major version: .*\\n", "")
        .replaceAll(" {2}flags: .*\\n", "")
        .replaceAll(" {2}this_class: .*\\n", "")
        .replaceAll(" {2}super_class: .*\\n", "")
        .replaceAll(" {2}interfaces: .*\\n", "")

        // Remove entire Constant pool section
        .replaceAll("(?s)Constant pool:.*?(?=\\n\\{|\\npublic |\\nprotected |\\nprivate |\\npackage )", "")

        // Remove method/field descriptors and flags
        .replaceAll(" {4}descriptor: .*\\n", "")
        .replaceAll(" {4}flags: .*\\n", "")

        // Remove stack/locals metadata from Code section
        .replaceAll(" {6}stack=\\d+, locals=\\d+, args_size=\\d+\\n", "")

        // Normalize constant pool references: #7 → #CP (but NOT SMAP file IDs like #1:58)
        // SMAP file IDs are always followed by colon, constant pool refs are not
        .replaceAll("#(\\d+)(?!:)", "#CP")

        // Remove LocalVariableTable sections
        .replaceAll("(?s)LocalVariableTable:.*?(?=\\n\\n|\\n +}|\\n})", "")

        // Remove StackMapTable sections
        .replaceAll("(?s)StackMapTable:.*?(?=\\n\\n|\\n +}|\\n})", "")

        // Optionally remove LineNumberTable sections
        .replaceAll(includeLineNumberTable ? "(?!LineNumberTable:)" : "(?s)LineNumberTable:.*?(?=\\n\\n|\\n +}|\\n})", "")

        // Remove SourceFile attribute (appears after closing brace)
        .replaceAll("\\nSourceFile: \".*\"", "")

        // Optionally remove SourceDebugExtension
        .replaceAll(includeSourceDebugExtension ? "(?!SourceDebugExtension:)" : "(?s)SourceDebugExtension:.*?(?=\\n\\n|\\n}|$)", "")

        // Normalize whitespace: multiple blank lines → single blank line
        .replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n")

        // Ensure space between class declaration and opening brace (replace newline with space)
        .replaceAll("(class [\\w.]+)\\n+\\{", "$1 {")

        // Normalize indentation: Code at 4 spaces, LineNumberTable at 6 spaces
        .replaceAll("\\n {6}Code:", "\n    Code:")
        .replaceAll("\\n {4}LineNumberTable:", "\n      LineNumberTable:")

        // Remove extra blank line after field declarations
        .replaceAll("(;)\\n {2}\\n {2}(public|private|protected)", "$1\n\n  $2")

        // Remove blank lines before closing braces
        .replaceAll("\\n\\s+\\n}", "\n}")

        // Trim trailing/leading whitespace
        .trim();
  }
}
