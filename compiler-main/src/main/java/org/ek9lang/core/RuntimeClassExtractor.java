package org.ek9lang.core;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Extracts EK9 runtime classes (org.ek9.lang and ek9 packages) from the running JVM
 * and packages them into a versioned JAR file with SHA-256 checksum.
 * <p>
 * This enables per-project, versioned runtime caching:
 * - First build: Extract EK9 runtime + dependency classes from compiler JVM → create JAR + checksum
 * - Subsequent builds: Reuse cached JAR if checksum valid
 * - Multiple compiler versions: Each gets its own versioned JAR
 * </p>
 * <p>
 * Extracts all EK9 runtime classes plus third-party dependencies (Jackson, JSONPath, SLF4J)
 * including inner and anonymous classes required for runtime functionality.
 * Uses direct JAR scanning with standard Java APIs (JarFile, ClassLoader).
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
   * Third-party dependency packages required by EK9 runtime classes.
   * These are libraries used by EK9 built-in types (especially JSON support).
   */
  private static final List<String> DEPENDENCY_PACKAGES = List.of(
      "com.fasterxml.jackson.core",       // Jackson Core (JSON processing)
      "com.fasterxml.jackson.databind",   // Jackson Databind (used by JSON class)
      "com.fasterxml.jackson.annotation", // Jackson Annotations
      "com.jayway.jsonpath",              // JSONPath library (used by JSON.read())
      "net.minidev.json",                 // JSON Smart (JSONPath transitive dependency)
      "org.slf4j"                         // SLF4J logging (JSONPath transitive dependency)
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

    // Extract runtime classes from EK9 packages AND dependency packages
    try {
      final var allClasses = new ArrayList<Class<?>>();

      // First: Extract EK9 runtime classes (org.ek9.lang, ek9)
      for (final var packageName : RUNTIME_PACKAGES) {
        allClasses.addAll(findAllClassesUsingClassLoader(packageName));
      }

      // Second: Extract dependency library classes (Jackson, JSONPath, etc.)
      for (final var packageName : DEPENDENCY_PACKAGES) {
        allClasses.addAll(findAllClassesUsingClassLoader(packageName));
      }

      // Convert classes to ZipBinaryContent entries
      final var entries = new ArrayList<ZipBinaryContent>();
      for (final var cls : allClasses) {
        // Use getName() not getCanonicalName() - works for all classes including anonymous
        final var resourceName = cls.getName().replace('.', '/') + ".class";
        final var classLoader = cls.getClassLoader();

        // Skip classes with null classloader (bootstrap classloader - JDK classes)
        if (classLoader == null) {
          continue;
        }

        try (final var is = classLoader.getResourceAsStream(resourceName)) {
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

      throw new CompilerException("Failed to create runtime JAR at " + jarPath);

    } catch (Exception e) {
      throw new CompilerException("Unable to create runtime JAR: " + e.getMessage(), e);
    }
  }

  /**
   * Find all classes in a package by scanning JAR files and class directories.
   * Extracts ALL classes including regular classes, inner classes, and anonymous classes.
   * Handles both JAR-based classpaths (Maven) and exploded directories (IntelliJ).
   * For dependency packages, we need everything to ensure runtime functionality.
   *
   * @param packageName Package to scan (e.g., "org.ek9.lang")
   * @return Set of Class objects found in the package
   */
  private Set<Class<?>> findAllClassesUsingClassLoader(final String packageName) {

    final var classes = new HashSet<Class<?>>();

    // Find the JAR file containing the package by looking at the classpath
    final var packagePath = packageName.replace('.', '/');
    final var classLoader = Thread.currentThread().getContextClassLoader();

    try {
      // Get the resource URL for the package
      final Enumeration<URL> resources = classLoader.getResources(packagePath);

      while (resources.hasMoreElements()) {
        final URL url = resources.nextElement();
        final String urlString = url.toString();

        // Check if this is a JAR file URL
        if (urlString.startsWith("jar:file:")) {
          // Extract JAR file path from jar:file:/path/to/file.jar!/com/package
          final var jarPath = urlString.substring("jar:file:".length(), urlString.indexOf("!"));
          scanJarForClasses(jarPath, packagePath, classLoader, classes);
        } else if (urlString.startsWith("file:")) {
          // Handle exploded class directories (IntelliJ, Maven test-classes, etc.)
          scanDirectoryForClasses(url, packagePath, classLoader, classes);
        }
      }

    } catch (IOException e) {
      System.err.println("Error scanning package " + packageName + ": " + e.getMessage());
    }

    return classes;
  }

  /**
   * Scan a JAR file for all classes in a specific package path.
   * Loads each class found and adds it to the provided set.
   *
   * @param jarPath      Path to the JAR file to scan
   * @param packagePath  Package path in JAR format (e.g., "org/ek9/lang")
   * @param classLoader  ClassLoader to use for loading classes
   * @param classes      Set to add found classes to
   */
  private void scanJarForClasses(final String jarPath,
                                 final String packagePath,
                                 final ClassLoader classLoader,
                                 final Set<Class<?>> classes) {
    try (final var jarFile = new JarFile(jarPath)) {
      final Enumeration<JarEntry> entries = jarFile.entries();

      while (entries.hasMoreElements()) {
        final JarEntry entry = entries.nextElement();
        final String entryName = entry.getName();

        // Check if this is a class file in our package
        if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
          // Convert entry name to class name
          // Example: com/fasterxml/jackson/databind/ObjectMapper.class -> com.fasterxml.jackson.databind.ObjectMapper
          final String className = entryName.replace('/', '.').replace(".class", "");

          loadClass(classLoader, classes, className);
        }
      }
    } catch (IOException e) {
      System.err.println("Error scanning JAR " + jarPath + ": " + e.getMessage());
    }
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private static void loadClass(final ClassLoader classLoader, final Set<Class<?>> classes, final String className) {
    try {
      // Load the class
      final Class<?> cls = Class.forName(className, false, classLoader);
      classes.add(cls);
    } catch (ClassNotFoundException | LinkageError | TypeNotPresentException _) {
      // Class cannot be loaded - skip it
    }
  }

  /**
   * Scan an exploded class directory for all classes in a specific package path.
   * This handles IntelliJ's classpath format and Maven's test-classes directories.
   * Loads each class found and adds it to the provided set.
   *
   * @param url          URL pointing to the directory
   * @param packagePath  Package path in directory format (e.g., "org/ek9/lang")
   * @param classLoader  ClassLoader to use for loading classes
   * @param classes      Set to add found classes to
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  private void scanDirectoryForClasses(final URL url,
                                       final String packagePath,
                                       final ClassLoader classLoader,
                                       final Set<Class<?>> classes) {
    try {
      final Path directory = Paths.get(url.toURI());

      // Convert package path to package name prefix (e.g., "org/ek9/lang" -> "org.ek9.lang.")
      final String packagePrefix = packagePath.replace('/', '.') + '.';

      // Walk the directory tree to find all .class files
      try (Stream<Path> paths = Files.walk(directory)) {
        paths.filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".class"))
            .forEach(classFile -> {
              try {
                // Convert file path to class name relative to package directory
                // Example: /path/to/org/ek9/lang/String.class -> String
                final Path relativePath = directory.relativize(classFile);
                final String relativeClassName = relativePath.toString()
                    .replace(File.separatorChar, '.')
                    .replace(".class", "");

                // Prepend package name to get fully qualified class name
                // Example: "String" -> "org.ek9.lang.String"
                final String className = packagePrefix + relativeClassName;

                // Load the class
                final Class<?> cls = Class.forName(className, false, classLoader);
                classes.add(cls);
              } catch (ClassNotFoundException | LinkageError | TypeNotPresentException _) {
                // Class cannot be loaded - skip it
              }
            });
      }
    } catch (URISyntaxException | IOException e) {
      System.err.println("Error scanning directory " + url + ": " + e.getMessage());
    }
  }
}
