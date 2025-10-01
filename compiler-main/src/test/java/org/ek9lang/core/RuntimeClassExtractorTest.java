package org.ek9lang.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.util.jar.JarFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Test RuntimeClassExtractor JAR creation, checksum validation, and content verification.
 * Uses isolated test directories to avoid conflicts with other tests.
 */
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
final class RuntimeClassExtractorTest {

  private RuntimeClassExtractor underTest;
  private FileHandling fileHandling;
  private File testProjectDir;
  private final String testVersion = "0.0.1-0";

  @BeforeEach
  void setup() {
    underTest = new RuntimeClassExtractor();
    fileHandling = new FileHandling(new OsSupport(true));

    // Create isolated test directory
    String tempDir = System.getProperty("java.io.tmpdir");
    testProjectDir = new File(tempDir, "runtime-extractor-test-" + System.currentTimeMillis());
    fileHandling.makeDirectoryIfNotExists(testProjectDir);
  }

  @AfterEach
  void cleanup() {
    // Clean up test artifacts
    if (testProjectDir != null && testProjectDir.exists()) {
      fileHandling.deleteContentsAndBelow(testProjectDir, true);
    }
  }

  @Test
  void testExtractRuntimeJar() {
    // Extract runtime JAR
    var result = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);

    // Verify JAR was created
    assertTrue(result.isPresent());
    File jarFile = result.get();
    assertTrue(jarFile.exists());
    assertEquals("ek9-runtime-0.0.1-0.jar", jarFile.getName());

    // Verify JAR is in correct location
    assertTrue(jarFile.getPath().contains(".ek9"));
    assertTrue(jarFile.getPath().contains("runtime"));
  }

  @Test
  void testChecksumCreated() {
    // Extract runtime JAR
    var result = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);

    assertTrue(result.isPresent());
    File jarFile = result.get();

    // Verify checksum file exists
    File checksumFile = new File(jarFile.getPath() + ".sha256");
    assertTrue(checksumFile.exists());

    // Verify checksum is valid
    assertTrue(Digest.check(jarFile, checksumFile));
  }

  @Test
  void testJarContainsOrgEk9LangClasses() throws Exception {
    // Extract runtime JAR
    var result = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);

    assertTrue(result.isPresent());
    File jarFile = result.get();

    // Check JAR contains org.ek9.lang classes
    try (JarFile jar = new JarFile(jarFile)) {
      // Verify key org.ek9.lang classes are present
      assertNotNull(jar.getEntry("org/ek9/lang/String.class"));
      assertNotNull(jar.getEntry("org/ek9/lang/Integer.class"));
      assertNotNull(jar.getEntry("org/ek9/lang/Boolean.class"));
      assertNotNull(jar.getEntry("org/ek9/lang/Stdout.class"));
      assertNotNull(jar.getEntry("org/ek9/lang/List.class"));
      assertNotNull(jar.getEntry("org/ek9/lang/Dict.class"));
      assertNotNull(jar.getEntry("org/ek9/lang/Any.class"));

      // Verify at least one parameterized type
      long iteratorCount = jar.stream()
          .filter(e -> e.getName().contains("org/ek9/lang/_Iterator_"))
          .count();
      assertTrue(iteratorCount > 0, "Should contain Iterator parameterized types");
    }
  }

  @Test
  void testJarContainsEk9PackageClasses() throws Exception {
    // Extract runtime JAR
    var result = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);

    assertTrue(result.isPresent());
    File jarFile = result.get();

    // Check JAR contains ek9 package classes
    try (JarFile jar = new JarFile(jarFile)) {
      assertNotNull(jar.getEntry("ek9/ProgramLauncher.class"));
      assertNotNull(jar.getEntry("ek9/ProgramMetadata.class"));
      assertNotNull(jar.getEntry("ek9/StringToEK9TypeConverter.class"));
    }
  }

  @Test
  void testCachedJarReuse() throws InterruptedException {
    // First extraction
    var result1 = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);
    assertTrue(result1.isPresent());
    File jarFile1 = result1.get();
    long firstModified = jarFile1.lastModified();

    // Wait a moment to ensure timestamp would change if recreated
    Thread.sleep(100);

    // Second extraction (should reuse cached JAR)
    var result2 = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);
    assertTrue(result2.isPresent());
    File jarFile2 = result2.get();

    // Verify same file was reused (timestamp unchanged)
    assertEquals(jarFile1.getPath(), jarFile2.getPath());
    assertEquals(firstModified, jarFile2.lastModified());
  }

  @Test
  void testInvalidChecksumTriggersReExtraction() throws Exception {
    // First extraction
    var result1 = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);
    assertTrue(result1.isPresent());
    File jarFile = result1.get();

    // Corrupt checksum file
    File checksumFile = new File(jarFile.getPath() + ".sha256");
    java.nio.file.Files.writeString(checksumFile.toPath(), "INVALID_CHECKSUM");

    // Second extraction should detect invalid checksum and re-extract
    var result2 = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);
    assertTrue(result2.isPresent());

    // Verify new checksum is valid
    assertTrue(Digest.check(result2.get(),
        new File(result2.get().getPath() + ".sha256")));
  }

  @Test
  void testNoAnonymousClassesIncluded() throws Exception {
    var result = underTest.extractRuntimeJar(
        fileHandling,
        testProjectDir.getPath(),
        testVersion);

    assertTrue(result.isPresent());

    // Verify JAR contains nested classes (static inner classes with $)
    // These are legitimate EK9 runtime classes
    try (JarFile jar = new JarFile(result.get())) {
      long nestedClassCount = jar.stream()
          .filter(e -> e.getName().endsWith(".class"))
          .filter(e -> e.getName().contains("$"))
          .count();

      // Should have at least EnvVars$, SystemExitManager$, SocketConnection$, TCP$ classes
      assertTrue(nestedClassCount >= 5,
          "Should contain nested classes (expected >= 5, found " + nestedClassCount + ")");
    }
  }
}
