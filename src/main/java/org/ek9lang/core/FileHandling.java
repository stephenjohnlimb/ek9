package org.ek9lang.core;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Designed to abstract out all file handling for the compiler.
 */
public final class FileHandling {
  private final OsSupport osSupport;
  private final Packager packager;
  private final Ek9DirectoryStructure directoryStructure;

  private final ExceptionConverter<Boolean> writer = new ExceptionConverter<>();

  /**
   * Create File Handling with the appropriately configured OS support.
   * Quite a few of these methods just delegate to OsSupport and the Packager.
   */
  public FileHandling(final OsSupport osSupport) {

    this.osSupport = osSupport;
    this.packager = new Packager(this);
    this.directoryStructure = new Ek9DirectoryStructure(this);

  }

  public String getTempDirectory() {

    return osSupport.getTempDirectory();
  }

  public String getUsersHomeDirectory() {

    return osSupport.getUsersHomeDirectory();
  }

  public File getUsersHomeEk9LibDirectory() {

    return new File(getUsersHomeEk9Directory(), "lib");
  }

  public String getUsersHomeEk9Directory() {

    return getDotEk9Directory(osSupport.getUsersHomeDirectory());
  }

  public String getDotEk9Directory(final File directory) {

    AssertValue.checkDirectoryReadable("Directory not readable", directory);

    return directory.getAbsolutePath() + File.separatorChar + ".ek9" + File.separatorChar;
  }

  public String getDotEk9Directory(final String fromDirectory) {

    AssertValue.checkNotEmpty("FromDirectory is empty", fromDirectory);

    return getDotEk9Directory(new File(fromDirectory));
  }

  public String makePackagedModuleZipFileName(final String moduleName, final String version) {

    return makePackagedModuleZipFileName(makeDependencyVector(moduleName, version));
  }

  public String makePackagedModuleZipFileName(final String dependencyVector) {

    AssertValue.checkNotEmpty("Dependency Vector is empty", dependencyVector);

    return dependencyVector + ".zip";
  }

  /**
   * Create a full dependency vector out of module name and verssion.
   */
  public String makeDependencyVector(final String moduleName, final String version) {

    AssertValue.checkNotEmpty("ModuleName is empty", moduleName);
    AssertValue.checkNotEmpty("Version is empty", version);

    return moduleName + "-" + version;
  }

  /**
   * Copy a named file from a source directory to a destination directory.
   */
  public boolean copy(final File sourceDir, final File destinationDir, final String fileName) {

    //Let exception break everything here - these are precondition.
    AssertValue.checkNotEmpty("Filename empty", fileName);
    AssertValue.checkDirectoryReadable("Source directory not readable", sourceDir);
    AssertValue.checkDirectoryWritable("Destination directory not writable", destinationDir);

    return copy(new File(sourceDir, fileName), new File(destinationDir, fileName));
  }

  /**
   * Copy a file to a new destination.
   */
  public boolean copy(final File fullSourcePath, final File fullDestinationPath) {

    final Processor<Boolean> accessor = () -> {
      final var originalPath = fullSourcePath.toPath();
      final var targetPath = fullDestinationPath.toPath();

      Files.copy(originalPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
      return true;
    };

    return writer.apply(accessor);
  }

  /**
   * Save some text to a file.
   */
  public boolean saveToOutput(final File file, final String value) {

    final Processor<Boolean> accessor = () -> {
      try (final var output = new FileOutputStream(file)) {
        output.write(value.getBytes(StandardCharsets.UTF_8));
        return true;
      }
    };

    return writer.apply(accessor);
  }

  /**
   * Deletes a file if it exists or a compiler exception if it cannot be deleted.
   */
  public void deleteFileIfExists(final File file) {

    AssertValue.checkNotNull("File cannot be null", file);

    writer.apply(() -> Files.deleteIfExists(file.toPath()));
  }

  /**
   * Create a directory.
   */
  public void makeDirectoryIfNotExists(final File directory) {

    osSupport.makeDirectoryIfNotExists(directory);
  }

  /**
   * deletes matching files.
   *
   * @param dir             the directory to look in
   * @param fileNamePattern The pattern regex not shell so for * use .* for .txt use \\.txt
   */
  public void deleteMatchingFiles(final File dir, final String fileNamePattern) {

    AssertValue.checkNotNull("Dir cannot be null", dir);
    AssertValue.checkNotNull("FileNamePattern cannot be null", fileNamePattern);

    final var files = dir.listFiles((dir1, name) -> name.matches(fileNamePattern));
    Optional.ofNullable(files).stream().flatMap(Arrays::stream).forEach(this::deleteFileIfExists);

  }

  /**
   * Does a recursive delete from this directory and below.
   * If includeDirectoryRoot is true then it will delete that directory as well
   */
  public void deleteContentsAndBelow(final File dir, final boolean includeDirectoryRoot) {

    Optional.ofNullable(dir.listFiles())
        .stream()
        .flatMap(Arrays::stream)
        .forEach(toDelete -> {
          if (toDelete.isDirectory()) {
            deleteContentsAndBelow(toDelete, true);
          } else {
            deleteFileIfExists(toDelete);
          }
        });

    if (includeDirectoryRoot) {
      deleteFileIfExists(dir);
    }

  }

  /**
   * Create a sha256 hash of a file and save it in fileName".sha256"
   */
  public Digest.CheckSum createSha256Of(final String fileName) {

    final var sha256File = new File(fileName + ".sha256");
    final var fileToCheckSum = new File(fileName);

    Digest.CheckSum checkSum = Digest.digest(fileToCheckSum);
    checkSum.saveToFile(sha256File);

    return checkSum;
  }

  /**
   * Create a Java jar file with a list of zip sets.
   */
  public boolean createJar(final String fileName, final List<ZipSet> sets) {

    return packager.createJar(fileName, sets);
  }

  public boolean unZipFileTo(final File zipFile, final String unpackedDir) {

    return unZipFileTo(zipFile, new File(unpackedDir));
  }

  /**
   * Unzips a zip file into a directory, the directory will be created if it does not exist.
   */
  public boolean unZipFileTo(final File zipFile, final File unpackedDir) {

    return packager.unZipFileTo(zipFile, unpackedDir);
  }

  /**
   * To be used for making the zip when publishing ek9 source to artefact server.
   *
   * @param fileName             The name of the zip to create
   * @param sourcePropertiesFile The properties file that describes the package.
   * @return true if all OK.
   */
  public boolean createZip(final String fileName, final ZipSet set, final File sourcePropertiesFile) {

    return packager.createZip(fileName, set, sourcePropertiesFile);
  }

  public File getTargetExecutableArtefact(final String ek9FullPathToFileName,
                                          final TargetArchitecture targetArchitecture) {

    return directoryStructure.getTargetExecutableArtefact(ek9FullPathToFileName, targetArchitecture);
  }

  public File getTargetPropertiesArtefact(final String ek9FullPathToFileName) {

    return directoryStructure.getTargetPropertiesArtefact(ek9FullPathToFileName);
  }

  public void validateHomeEk9Directory(final TargetArchitecture targetArchitecture) {

    validateEk9Directory(getUsersHomeEk9Directory(), targetArchitecture);

  }

  public void validateEk9Directory(final String directoryName, final TargetArchitecture targetArchitecture) {

    directoryStructure.validateEk9Directory(directoryName, targetArchitecture);

  }

  public void cleanEk9DirectoryStructureFor(final File ek9File, final TargetArchitecture targetArchitecture) {

    cleanEk9DirectoryStructureFor(ek9File.getPath(), targetArchitecture);

  }

  public void cleanEk9DirectoryStructureFor(final String ek9FullPathToFileName,
                                            final TargetArchitecture targetArchitecture) {

    directoryStructure.cleanEk9DirectoryStructureFor(ek9FullPathToFileName, targetArchitecture);

  }

  public File getMainGeneratedOutputDirectory(final String fromEk9BaseDirectory,
                                              final TargetArchitecture targetArchitecture) {

    return directoryStructure.getMainGeneratedOutputDirectory(fromEk9BaseDirectory, targetArchitecture);
  }

  public File getMainFinalOutputDirectory(final String fromEk9BaseDirectory,
                                          final TargetArchitecture targetArchitecture) {

    return directoryStructure.getMainFinalOutputDirectory(fromEk9BaseDirectory, targetArchitecture);
  }

  public File getDevGeneratedOutputDirectory(final String fromEk9BaseDirectory,
                                             final TargetArchitecture targetArchitecture) {

    return directoryStructure.getDevGeneratedOutputDirectory(fromEk9BaseDirectory, targetArchitecture);
  }

  public File getDevFinalOutputDirectory(final String fromEk9BaseDirectory,
                                         final TargetArchitecture targetArchitecture) {

    return directoryStructure.getDevFinalOutputDirectory(fromEk9BaseDirectory, targetArchitecture);
  }

  public void deleteStalePackages(final String ek9FileNameDirectory, final String moduleName) {

    directoryStructure.deleteStalePackages(ek9FileNameDirectory, moduleName);

  }

  public boolean isUsersSigningKeyPairPresent() {

    return directoryStructure.isUsersSigningKeyPairPresent();
  }

  public SigningKeyPair getUsersSigningKeyPair() {

    return directoryStructure.getUsersSigningKeyPair();
  }

  public boolean saveToHomeEk9Directory(final SigningKeyPair keyPair) {

    return directoryStructure.saveToHomeEk9Directory(keyPair);
  }
}
