package org.ek9lang.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

/**
 * Extracts EK9 runtime classes (org.ek9.lang and ek9 packages) from the running JVM
 * and packages them into a versioned JAR file with SHA-256 checksum.
 * <p>
 * This enables per-project, versioned runtime caching:
 * - First build: Extract ~125-130 classes from compiler JVM → create JAR + checksum
 * - Subsequent builds: Reuse cached JAR if checksum valid
 * - Multiple compiler versions: Each gets its own versioned JAR
 * </p>
 * <p>
 * Pattern follows Packager and FileHandling in core module.
 * Uses Reflections library (same as ClassLister in java-introspection).
 * </p>
 */
public final class RuntimeClassExtractor {

  /**
   * Packages to extract from the running JVM.
   * These contain all EK9 built-in types and runtime support classes.
   */
  private static final List<String> RUNTIME_PACKAGES = List.of(
      "org.ek9.lang",  // Core EK9 built-in types (~120 classes)
      "ek9"            // Runtime support (ProgramLauncher, etc.)
  );

  /**
   * Extract EK9 runtime classes into a versioned JAR file.
   * Creates JAR at: {@code <projectDir>/.ek9/runtime/ek9-runtime-<version>.jar}
   * Also creates corresponding .sha256 checksum file.
   *
   * @param fileHandling FileHandling instance for JAR creation and checksums
   * @param projectDir   Project directory (source file directory)
   * @param version      Compiler version (e.g., "0.0.1-0")
   * @return Optional containing the runtime JAR file if successful
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  public Optional<File> extractRuntimeJar(final FileHandling fileHandling,
                                          final String projectDir,
                                          final String version) {

    AssertValue.checkNotNull("FileHandling cannot be null", fileHandling);
    AssertValue.checkNotEmpty("Project directory cannot be empty", projectDir);
    AssertValue.checkNotEmpty("Version cannot be empty", version);

    // Use FileHandling to get proper runtime JAR path
    final var jarFile = fileHandling.getRuntimeJarFile(projectDir, version);
    final var jarPath = jarFile.getAbsolutePath();
    final var checksumFile = new File(jarPath + ".sha256");

    // Check if cached JAR exists and is valid
    if (jarFile.exists() && checksumFile.exists() && Digest.check(jarFile, checksumFile)) {
      return Optional.of(jarFile);
    }

    // Extract runtime classes from both packages
    try {
      final var allClasses = new ArrayList<Class<?>>();
      for (final var packageName : RUNTIME_PACKAGES) {
        allClasses.addAll(findAllClassesUsingClassLoader(packageName).values());
      }

      // Convert classes to ZipBinaryContent entries
      final var entries = new ArrayList<ZipBinaryContent>();
      for (final var cls : allClasses) {
        final var resourceName = cls.getName().replace('.', '/') + ".class";
        try (final var is = cls.getClassLoader().getResourceAsStream(resourceName)) {
          if (is != null) {
            final var bytes = is.readAllBytes();
            entries.add(new ZipBinaryContent(resourceName, bytes));
          }
        }
      }

      // Ensure runtime directory exists (parent of JAR file)
      fileHandling.makeDirectoryIfNotExists(jarFile.getParentFile());

      // Create JAR from extracted class bytes
      final var success = fileHandling.createJar(jarPath, List.of(new ZipSet(entries)));

      if (success) {
        // Create checksum for integrity verification
        fileHandling.createSha256Of(jarPath);
        return Optional.of(jarFile);
      }

    } catch (Exception e) {
      System.err.println("Unable to create runtime jar " + e.getMessage());
    }
    return Optional.empty();
  }

  /**
   * Find all classes in a package using ClassLoader and Reflections library.
   * Filters out inner classes (containing '$' in name).
   * Pattern from ClassLister.findAllClassesUsingClassLoader().
   *
   * @param packageName Package to scan (e.g., "org.ek9.lang")
   * @return Map of class canonical name to Class object
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  private Map<String, Class<?>> findAllClassesUsingClassLoader(final String packageName) {

    final var reflections = new Reflections(packageName,
        Scanners.SubTypes.filterResultsBy(_ -> true));

    final Function<Class<?>, String> classToName = Class::getCanonicalName;

    return reflections.getSubTypesOf(Object.class)
        .stream()
        .filter(this::usableClass)
        .collect(Collectors.toMap(classToName, Function.identity()));
  }

  /**
   * Filter out inner Java classes - they cannot be used directly via EK9.
   * Pattern from ClassLister.usableClass().
   *
   * @param cls Class to check
   * @return true if class is usable (no '$' in canonical name)
   */
  private Boolean usableClass(final Class<?> cls) {

    final var name = cls.getCanonicalName();
    return name != null && !name.contains("$");
  }
}
