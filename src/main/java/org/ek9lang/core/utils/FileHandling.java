package org.ek9lang.core.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

/**
 * Designed to abstract out all file handling for the compiler.
 */
public final class FileHandling {
  private final OsSupport osSupport;
  private final Packager packager;
  private final Ek9DirectoryStructure directoryStructure;

  /**
   * Create File Handling with the appropriately configured OS support.
   * Quite a few of these methods just delegate to OsSupport and the Packager.
   */
  public FileHandling(OsSupport osSupport) {
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

  public String getDotEk9Directory(File directory) {
    AssertValue.checkDirectoryReadable("Directory not readable", directory);
    return directory.getAbsolutePath() + File.separatorChar + ".ek9" + File.separatorChar;
  }

  public String getDotEk9Directory(String fromDirectory) {
    AssertValue.checkNotEmpty("FromDirectory is empty", fromDirectory);
    return getDotEk9Directory(new File(fromDirectory));
  }

  public String makePackagedModuleZipFileName(String moduleName, String version) {
    return makePackagedModuleZipFileName(makeDependencyVector(moduleName, version));
  }

  public String makePackagedModuleZipFileName(String dependencyVector) {
    AssertValue.checkNotEmpty("Dependency Vector is empty", dependencyVector);
    return dependencyVector + ".zip";
  }

  /**
   * Create a full dependency vector out of module name and verssion.
   */
  public String makeDependencyVector(String moduleName, String version) {
    AssertValue.checkNotEmpty("ModuleName is empty", moduleName);
    AssertValue.checkNotEmpty("Version is empty", version);
    return moduleName + "-" + version;
  }

  /**
   * Copy a named file from a source directory to a destination directory.
   */
  public boolean copy(File sourceDir, File destinationDir, String fileName) {
    //Let exception break everything here - these are precondition.
    AssertValue.checkNotEmpty("Filename empty", fileName);
    AssertValue.checkDirectoryReadable("Source directory not readable", sourceDir);
    AssertValue.checkDirectoryWritable("Destination directory not writable", destinationDir);

    return copy(new File(sourceDir, fileName), new File(destinationDir, fileName));
  }

  /**
   * Copy a file to a new destination.
   */
  public boolean copy(File fullSourcePath, File fullDestinationPath) {
    try {
      Path originalPath = fullSourcePath.toPath();
      Path targetPath = fullDestinationPath.toPath();
      Files.copy(originalPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (Exception ex) {
      Logger.error("File copy failed: " + ex.getMessage());
      return false;
    }
  }

  /**
   * Save some text to a file.
   */
  public boolean saveToOutput(File file, String value) {
    try (OutputStream output = new FileOutputStream(file)) {
      output.write(value.getBytes(StandardCharsets.UTF_8));
      return true;
    } catch (Exception ex) {
      Logger.error("Unable to save " + file.getPath() + " " + ex.getMessage());
      return false;
    }
  }

  /**
   * Deletes a file if it exists or a compiler exception if it cannot be deleted.
   */
  public void deleteFileIfExists(File file) {
    AssertValue.checkNotNull("File cannot be null", file);
    try {
      Files.deleteIfExists(file.toPath());
    } catch (Exception ex) {
      throw new CompilerException("Unable to delete [" + ex.getMessage() + "]", ex);
    }
  }

  /**
   * Create a directory.
   */
  public void makeDirectoryIfNotExists(File directory) {
    osSupport.makeDirectoryIfNotExists(directory);
  }

  /**
   * deletes matching files.
   *
   * @param dir             the directory to look in
   * @param fileNamePattern The pattern regex not shell so for * use .* for .txt use \\.txt
   */
  public void deleteMatchingFiles(File dir, String fileNamePattern) {
    AssertValue.checkNotNull("Dir cannot be null", dir);
    AssertValue.checkNotNull("FileNamePattern cannot be null", fileNamePattern);

    File[] files = dir.listFiles((dir1, name) -> name.matches(fileNamePattern));
    Optional.ofNullable(files).stream().flatMap(Arrays::stream)
        .forEach(this::deleteFileIfExists);
  }

  /**
   * Does a recursive delete from this directory and below.
   * If includeDirectoryRoot is true then it will delete that directory as well
   */
  public void deleteContentsAndBelow(File dir, boolean includeDirectoryRoot) {
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
  public Digest.CheckSum createSha256Of(String fileName) {
    File sha256File = new File(fileName + ".sha256");
    File fileToCheckSum = new File(fileName);
    Digest.CheckSum checkSum = Digest.digest(fileToCheckSum);
    checkSum.saveToFile(sha256File);
    return checkSum;
  }

  /**
   * Create a Java jar file with a list of zip sets.
   */
  public boolean createJar(String fileName, List<ZipSet> sets) {
    return packager.createJar(fileName, sets);
  }

  public boolean unZipFileTo(File zipFile, String unpackedDir) {
    return unZipFileTo(zipFile, new File(unpackedDir));
  }

  /**
   * Unzips a zip file into a directory, the directory will be created if it does not exist.
   */
  public boolean unZipFileTo(File zipFile, File unpackedDir) {
    return packager.unZipFileTo(zipFile, unpackedDir);
  }

  /**
   * To be used for making the zip when publishing ek9 source to artefact server.
   *
   * @param fileName             The name of the zip to create
   * @param sourcePropertiesFile The properties file that describes the package.
   * @return true if all OK.
   */
  public boolean createZip(String fileName, ZipSet set, File sourcePropertiesFile) {
    return packager.createZip(fileName, set, sourcePropertiesFile);
  }

  public File getTargetExecutableArtefact(String ek9FullPathToFileName, String targetArchitecture) {
    return directoryStructure.getTargetExecutableArtefact(ek9FullPathToFileName,
        targetArchitecture);
  }

  public File getTargetPropertiesArtefact(String ek9FullPathToFileName) {
    return directoryStructure.getTargetPropertiesArtefact(ek9FullPathToFileName);
  }

  public void validateHomeEk9Directory(String targetArchitecture) {
    validateEk9Directory(getUsersHomeEk9Directory(), targetArchitecture);
  }

  public void validateEk9Directory(String directoryName, String targetArchitecture) {
    directoryStructure.validateEk9Directory(directoryName, targetArchitecture);
  }

  public void cleanEk9DirectoryStructureFor(File ek9File, String targetArchitecture) {
    cleanEk9DirectoryStructureFor(ek9File.getPath(), targetArchitecture);
  }

  public void cleanEk9DirectoryStructureFor(String ek9FullPathToFileName,
                                            String targetArchitecture) {
    directoryStructure.cleanEk9DirectoryStructureFor(ek9FullPathToFileName, targetArchitecture);
  }

  public File getMainGeneratedOutputDirectory(String fromEk9BaseDirectory,
                                              String targetArchitecture) {
    return directoryStructure.getMainGeneratedOutputDirectory(fromEk9BaseDirectory,
        targetArchitecture);
  }

  public File getMainFinalOutputDirectory(String fromEk9BaseDirectory, String targetArchitecture) {
    return directoryStructure.getMainFinalOutputDirectory(fromEk9BaseDirectory, targetArchitecture);
  }

  public File getDevGeneratedOutputDirectory(String fromEk9BaseDirectory,
                                             String targetArchitecture) {
    return directoryStructure.getDevGeneratedOutputDirectory(fromEk9BaseDirectory,
        targetArchitecture);
  }

  public File getDevFinalOutputDirectory(String fromEk9BaseDirectory, String targetArchitecture) {
    return directoryStructure.getDevFinalOutputDirectory(fromEk9BaseDirectory, targetArchitecture);
  }

  public void deleteStalePackages(String ek9FileNameDirectory, String moduleName) {
    directoryStructure.deleteStalePackages(ek9FileNameDirectory, moduleName);
  }

  public boolean isUsersSigningKeyPairPresent() {
    return directoryStructure.isUsersSigningKeyPairPresent();
  }

  public SigningKeyPair getUsersSigningKeyPair() {
    return directoryStructure.getUsersSigningKeyPair();
  }

  public boolean saveToHomeEk9Directory(SigningKeyPair keyPair) {
    return directoryStructure.saveToHomeEk9Directory(keyPair);
  }
}
