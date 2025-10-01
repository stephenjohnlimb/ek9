package org.ek9lang.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Ensure that the file handling class functions as expected.
 * As these tests add, create delete and manipulate files, this is single threaded.
 */
//Specific tests that manipulate files and specifics in ek9 must not run in parallel.
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
final class FileHandlingTest {
  private final FileHandling underTest = new FileHandling(new OsSupport(true));

  private final TargetArchitecture targetArchitecture = TargetArchitecture.JVM;

  @AfterEach
  void tidyUp() {
    String testHomeDirectory = underTest.getUsersHomeDirectory();
    assertNotNull(testHomeDirectory);
    //As this is a test delete from process id and below
    underTest.deleteContentsAndBelow(new File(new File(testHomeDirectory).getParent()), true);
  }

  @Test
  void testEK9DirectoryNaming() {
    String testHomeDirectory = underTest.getUsersHomeDirectory();
    assertNotNull(testHomeDirectory);
    String testHomeEK9Directory = underTest.getUsersHomeEk9Directory();
    assertNotNull(testHomeEK9Directory);

    File testHomeEK9LibDirectory = underTest.getUsersHomeEk9LibDirectory();
    assertNotNull(testHomeEK9LibDirectory);
  }

  @Test
  void testPackagedModuleZipFileName() {
    String result = underTest.makePackagedModuleZipFileName("some.module.name", "2.5.1");
    assertEquals("some.module.name-2.5.1.zip", result);
  }

  @Test
  void testFileStructure() throws IOException {
    //So this creates a full .ek9 structure under the developers home directory
    underTest.validateHomeEk9Directory(TargetArchitecture.JVM);

    //We need a project directory so that we can try out the other capabilities.
    //i.e. This is a project you will have checked out, here it is empty, we just create it
    String testHomeDirectory = underTest.getUsersHomeDirectory();
    File aProjectDirectory =
        FileSystems.getDefault().getPath(testHomeDirectory, "src", "aProject").toFile();
    underTest.makeDirectoryIfNotExists(aProjectDirectory);

    //Dummy source file
    File sampleEK9 =
        FileSystems.getDefault().getPath(aProjectDirectory.getPath(), "sample.ek9").toFile();
    assertTrue(sampleEK9.createNewFile());

    Digest.CheckSum checkSum = underTest.createSha256Of(sampleEK9.getPath());
    assertNotNull(checkSum);
    String checkSumFileName = sampleEK9.getPath() + ".sha256";
    File sampleEK9Sha256 = new File(checkSumFileName);
    assertTrue(sampleEK9Sha256.exists());
    Digest.check(sampleEK9, sampleEK9Sha256);

    //Now get the .ek9 directory under that, this is where we will store the built artefacts.
    String projectDotEK9Directory = underTest.getDotEk9Directory(aProjectDirectory);

    //This will check or make the whole .ek9 tree.
    underTest.validateEk9Directory(projectDotEK9Directory, targetArchitecture);

    File generatedOutputDir =
        underTest.getMainGeneratedOutputDirectory(projectDotEK9Directory, targetArchitecture);
    assertNotNull(generatedOutputDir);

    File finalOutputDir = underTest.getMainFinalOutputDirectory(projectDotEK9Directory, targetArchitecture);
    assertNotNull(finalOutputDir);

    File devGeneratedOutputDir =
        underTest.getDevGeneratedOutputDirectory(projectDotEK9Directory, targetArchitecture);
    assertNotNull(devGeneratedOutputDir);

    File devFinalOutputDir = underTest.getDevFinalOutputDirectory(projectDotEK9Directory, targetArchitecture);
    assertNotNull(devFinalOutputDir);

    File targetArtefact = underTest.getTargetExecutableArtefact(sampleEK9.getPath(), targetArchitecture);
    //Simulate a build of the target
    assertTrue(targetArtefact.createNewFile());
    assertNotNull(targetArtefact);

    File targetProperties = underTest.getTargetPropertiesArtefact(sampleEK9.getPath());
    //Simulate a build of the target properties
    assertTrue(targetProperties.createNewFile());
    assertNotNull(targetProperties);

    //Clean out
    underTest.cleanEk9DirectoryStructureFor(sampleEK9, targetArchitecture);

    //make sure they've gone
    targetArtefact = underTest.getTargetExecutableArtefact(sampleEK9.getPath(), targetArchitecture);
    assertFalse(targetArtefact.exists());
    targetProperties = underTest.getTargetPropertiesArtefact(sampleEK9.getPath());
    assertFalse(targetProperties.exists());

    //Ensure no stale zips.
    underTest.deleteStalePackages(aProjectDirectory.getPath(), "any.mod.name");
  }

  @Test
  void testKeySigningPairPersistence() {
    //Ensure it is there
    underTest.validateHomeEk9Directory(targetArchitecture);

    String testHomeDirectory = underTest.getUsersHomeDirectory();
    assertNotNull(testHomeDirectory);
    SigningKeyPair keyPair = SigningKeyPair.generate(2048);
    boolean saved = underTest.saveToHomeEk9Directory(keyPair);
    assertTrue(saved);
    assertTrue(underTest.isUsersSigningKeyPairPresent());

    SigningKeyPair reloadedKeyPair = underTest.getUsersSigningKeyPair();

    assertEquals(keyPair.getPrivateKeyInBase64(), reloadedKeyPair.getPrivateKeyInBase64());
    assertEquals(keyPair.getPublicKeyInBase64(), reloadedKeyPair.getPublicKeyInBase64());
  }

  @Test
  void testGetRuntimeJarFile() {
    // Setup test project directory
    String testHomeDirectory = underTest.getUsersHomeDirectory();
    File projectDir = FileSystems.getDefault()
        .getPath(testHomeDirectory, "src", "testRuntimeProject")
        .toFile();
    underTest.makeDirectoryIfNotExists(projectDir);

    // Test getRuntimeJarFile() path construction
    String version = "0.0.1-0";
    File runtimeJar = underTest.getRuntimeJarFile(projectDir.getPath(), version);

    // Verify path structure: <projectDir>/.ek9/runtime/ek9-runtime-<version>.jar
    assertNotNull(runtimeJar);
    assertEquals("ek9-runtime-0.0.1-0.jar", runtimeJar.getName());
    assertTrue(runtimeJar.getPath().contains(".ek9"));
    assertTrue(runtimeJar.getPath().contains("runtime"));

    // Test with different version
    File runtimeJar2 = underTest.getRuntimeJarFile(projectDir.getPath(), "1.0.0");
    assertEquals("ek9-runtime-1.0.0.jar", runtimeJar2.getName());
    assertNotEquals(runtimeJar.getPath(), runtimeJar2.getPath());
  }

  @Test
  void testZippingAndPackaging() throws IOException {
    underTest.validateHomeEk9Directory(targetArchitecture);

    //We need a project directory so that we can try out the other capabilities.
    //i.e. This is a project you will have checked out, here it is empty, we just create it
    String testHomeDirectory = underTest.getUsersHomeDirectory();
    File aProjectDirectory =
        FileSystems.getDefault().getPath(testHomeDirectory, "src", "aProject").toFile();
    underTest.makeDirectoryIfNotExists(aProjectDirectory);

    //Dummy source file
    File sampleEK9 =
        FileSystems.getDefault().getPath(aProjectDirectory.getPath(), "sample.ek9").toFile();
    assertTrue(sampleEK9.createNewFile());

    List<File> files = new OsSupport().getFilesRecursivelyFrom(new File(sampleEK9.getParent()));

    //Now get the .ek9 directory under that, this is where we will store the built artefacts.
    String projectDotEK9Directory = underTest.getDotEk9Directory(aProjectDirectory);

    //This will check or make the whole .ek9 tree.
    underTest.validateEk9Directory(projectDotEK9Directory, targetArchitecture);

    String zipFileName = underTest.makePackagedModuleZipFileName("some.mod.name", "2.3.1");
    String fileName = projectDotEK9Directory + zipFileName;

    File propsFile = underTest.getTargetPropertiesArtefact(sampleEK9.getPath());
    assertTrue(propsFile.createNewFile());

    ZipSet fileSet = new ZipSet(aProjectDirectory.toPath(), files);
    boolean created = underTest.createZip(fileName, fileSet, propsFile);
    assertTrue(created);
    assertTrue(new File(fileName).exists());
    assertTrue(new File(fileName).delete());
    assertFalse(new File(fileName).exists());

    byte[] someBinaryData = "The Quick Brown fox".getBytes(StandardCharsets.UTF_8);
    List<ZipBinaryContent> entries = new ArrayList<>();
    entries.add(new ZipBinaryContent("text", someBinaryData));

    ZipSet binarySet = new ZipSet(entries);
    created = underTest.createZip(fileName, binarySet, propsFile);
    assertTrue(created);
    File zipFile = new File(fileName);
    assertTrue(zipFile.exists());
    assertTrue(zipFile.delete());
    assertFalse(zipFile.exists());

    //Now try jar functionality
    created = underTest.createJar(fileName, Arrays.asList(fileSet, binarySet));
    assertTrue(created);
    assertTrue(zipFile.exists());

    //unpack that zip
    boolean unzipped = underTest.unZipFileTo(zipFile, underTest.getTempDirectory());
    assertTrue(unzipped);

    //Now remove zip file
    zipFile = new File(fileName);
    assertTrue(zipFile.delete());
    assertFalse(zipFile.exists());

    //Check what was unzipped.
    File unPackedSampleEK9 =
        FileSystems.getDefault().getPath(underTest.getTempDirectory(), "sample.ek9").toFile();
    assertTrue(unPackedSampleEK9.exists());

    File unPackedText =
        FileSystems.getDefault().getPath(underTest.getTempDirectory(), "text").toFile();
    assertTrue(unPackedText.exists());
  }

  @Test
  void testJarWithMainClassManifest() throws Exception {
    underTest.validateHomeEk9Directory(targetArchitecture);

    String testHomeDirectory = underTest.getUsersHomeDirectory();
    File aProjectDirectory =
        FileSystems.getDefault().getPath(testHomeDirectory, "src", "manifestTest").toFile();
    underTest.makeDirectoryIfNotExists(aProjectDirectory);

    // Create test JAR with Main-Class manifest
    String projectDotEK9Directory = underTest.getDotEk9Directory(aProjectDirectory);
    underTest.validateEk9Directory(projectDotEK9Directory, targetArchitecture);

    String jarFileName = projectDotEK9Directory + "test-with-manifest.jar";
    byte[] testClassBytes = "fake class bytes".getBytes(StandardCharsets.UTF_8);
    List<ZipBinaryContent> entries = new ArrayList<>();
    entries.add(new ZipBinaryContent("ek9/Main.class", testClassBytes));

    ZipSet binarySet = new ZipSet(entries);
    boolean created = underTest.createJar(jarFileName, List.of(binarySet), "ek9.Main");
    assertTrue(created);

    // Verify JAR was created
    File jarFile = new File(jarFileName);
    assertTrue(jarFile.exists());

    // Verify manifest exists and has Main-Class entry
    try (var jarStream = new java.util.jar.JarInputStream(new java.io.FileInputStream(jarFile))) {
      var manifest = jarStream.getManifest();
      assertNotNull(manifest, "Manifest should exist");

      var mainAttributes = manifest.getMainAttributes();
      String mainClass = mainAttributes.getValue("Main-Class");
      assertNotNull(mainClass, "Main-Class attribute should exist");
      assertEquals("ek9.Main", mainClass);

      String manifestVersion = mainAttributes.getValue("Manifest-Version");
      assertEquals("1.0", manifestVersion);
    }

    // Clean up
    assertTrue(jarFile.delete());
  }

  @Test
  void testJarWithoutMainClassManifest() throws Exception {
    underTest.validateHomeEk9Directory(targetArchitecture);

    String testHomeDirectory = underTest.getUsersHomeDirectory();
    File aProjectDirectory =
        FileSystems.getDefault().getPath(testHomeDirectory, "src", "noManifestTest").toFile();
    underTest.makeDirectoryIfNotExists(aProjectDirectory);

    // Create test JAR WITHOUT Main-Class manifest (using original method)
    String projectDotEK9Directory = underTest.getDotEk9Directory(aProjectDirectory);
    underTest.validateEk9Directory(projectDotEK9Directory, targetArchitecture);

    String jarFileName = projectDotEK9Directory + "test-without-manifest.jar";
    byte[] testClassBytes = "fake class bytes".getBytes(StandardCharsets.UTF_8);
    List<ZipBinaryContent> entries = new ArrayList<>();
    entries.add(new ZipBinaryContent("org/ek9/lang/String.class", testClassBytes));

    ZipSet binarySet = new ZipSet(entries);
    boolean created = underTest.createJar(jarFileName, List.of(binarySet)); // No mainClass parameter
    assertTrue(created);

    // Verify JAR was created
    File jarFile = new File(jarFileName);
    assertTrue(jarFile.exists());

    // Verify manifest either doesn't exist or doesn't have Main-Class
    try (var jarStream = new java.util.jar.JarInputStream(new java.io.FileInputStream(jarFile))) {
      var manifest = jarStream.getManifest();
      if (manifest != null) {
        var mainAttributes = manifest.getMainAttributes();
        String mainClass = mainAttributes.getValue("Main-Class");
        // Should not have Main-Class entry
        assertNull(mainClass, "Should not have Main-Class when not specified");
      }
      // It's also valid for manifest to not exist at all
    }

    // Clean up
    assertTrue(jarFile.delete());
  }
}
