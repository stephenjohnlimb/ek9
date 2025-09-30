package org.ek9lang.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Operating System support and generic stuff for directories and files.
 */
public final class OsSupport implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * When is stub mode the users home directory and current working directory are altered.
   */
  private boolean stubMode = false;

  public OsSupport() {

  }

  /**
   * Used as and when you want to use a stub for testing.
   * You don't always want to create or access the actual users
   * home directory or current working directory.
   */
  public OsSupport(final boolean testStubMode) {

    this.stubMode = testStubMode;

  }

  public static int numberOfProcessors() {

    return Runtime.getRuntime().availableProcessors();
  }

  /**
   * Is this configured to be in stub mode.
   */
  public boolean isInStubMode() {

    return stubMode;
  }

  /**
   * Provides the current process id of this running application.
   */
  public long getPid() {

    return ProcessHandle.current().pid();
  }

  /**
   * Create a simulated tmp, home and current working directory under
   * the temp directory for a particular process.
   * This enables us to run tests with real files being created but in a safe way.
   */
  private String createStubbedDirectory(final String forDir) {

    final var rtn = System.getProperty("java.io.tmpdir")
        + FileSystems.getDefault().getSeparator()
        + getPid()
        + FileSystems.getDefault().getSeparator()
        + forDir;

    makeDirectoryIfNotExists(new File(rtn));

    return rtn;
  }

  public synchronized String makeSubDirectoryIfNotExists(final File directory, final String subDirectory) {
    final var newDirectory = new File(directory, subDirectory);
    makeDirectoryIfNotExists(newDirectory);
    return newDirectory.getAbsolutePath();
  }

  /**
   * Create directory if it does not exist or exception if failed.
   */
  public synchronized void makeDirectoryIfNotExists(final File directory) {

    AssertValue.checkNotNull("Directory cannot be null", directory);
    final var exists = directory.exists();
    if (!exists && !directory.mkdirs()) {
      throw new CompilerException("Unable to create directory [" + directory.getPath() + "]");
    }

  }


  /**
   * Get name of temporary directory.
   */
  public String getTempDirectory() {

    if (stubMode) {
      return createStubbedDirectory("tmp");
    }

    return System.getProperty("java.io.tmpdir");
  }

  /**
   * Get the users home directory.
   */
  public String getUsersHomeDirectory() {

    if (stubMode) {
      return createStubbedDirectory("home");
    }

    return System.getProperty("user.home");
  }

  /**
   * Get current working directory for this running process.
   */
  public String getCurrentWorkingDirectory() {

    if (stubMode) {
      return createStubbedDirectory("cwd");
    }

    return System.getProperty("user.dir");
  }

  /**
   * How many CPU's/Core reported.
   */
  public int getNumberOfProcessors() {

    return OsSupport.numberOfProcessors();
  }

  /**
   * Extract just the final part of a file name.
   */
  public String getFileNameWithoutPath(final String fileNameWithPath) {

    if (fileNameWithPath == null || fileNameWithPath.isEmpty()) {
      return "";
    }
    final var f = new File(fileNameWithPath);

    return f.getName();
  }

  public boolean isFileReadable(final String fileName) {

    return fileName != null && isFileReadable(new File(fileName));
  }

  public boolean isFileReadable(final File file) {

    return file != null && file.isFile() && !file.isDirectory() && file.canRead();
  }

  /**
   * Load up a file into a String. Option empty if not possible to load.
   */
  public Optional<String> getFileContent(final File file) {

    final var builder = new StringBuilder();
    final Processor<Boolean> processor = () -> {
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
        builder.append(new String(is.readAllBytes(), StandardCharsets.UTF_8));
      }
      return true;
    };

    new ExceptionConverter<Boolean>().apply(processor);
    return Optional.of(builder.toString());
  }

  public boolean isDirectoryReadable(final String directoryName) {

    return directoryName != null && isDirectoryReadable(new File(directoryName));
  }

  public boolean isDirectoryReadable(final File directory) {

    return directory != null && directory.isDirectory() && directory.canRead();
  }

  public boolean isDirectoryWritable(final String directoryName) {

    return isDirectoryWritable(new File(directoryName));
  }

  public boolean isDirectoryWritable(final File directory) {

    return directory != null && directory.isDirectory() && directory.canWrite();
  }

  /**
   * Get List of all the files in a set of directories with a particular suffix.
   */
  public List<File> getFilesFromDirectories(final Collection<File> inDirectories, final String fileSuffix) {

    AssertValue.checkNotNull("InDirectories cannot be null", inDirectories);
    AssertValue.checkNotNull("FileSuffix cannot be null", fileSuffix);

    return inDirectories
        .stream()
        .map(dir -> getFilesFromDirectory(dir, fileSuffix))
        .flatMap(Collection::stream)
        .toList();
  }

  /**
   * Get files in a particular directory, but not if they start with a prefix.
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  public List<File> getDirectoriesInDirectory(final File inDirectory, final String excludeStartingWith) {

    assertInDirectoryValid(inDirectory);
    AssertValue.checkNotNull("ExcludeStartingWith cannot be null", excludeStartingWith);

    final var files = inDirectory.listFiles((_, name) -> !name.startsWith(excludeStartingWith));

    return Optional.ofNullable(files).stream().flatMap(Arrays::stream)
        .filter(File::isDirectory)
        .toList();
  }

  /**
   * Search down a directory structure matching a 'Glob".
   */
  public List<File> getFilesRecursivelyFrom(final File inDirectory, final Glob searchCondition) {

    assertInDirectoryValid(inDirectory);
    AssertValue.checkNotNull("SearchCondition cannot be null", searchCondition);

    return getFilesRecursivelyFrom(inDirectory)
        .stream()
        .filter(file -> searchCondition.isAcceptable(inDirectory.toPath().relativize(file.toPath())))
        .toList();
  }

  /**
   * Get all files down a directory structure.
   */
  public List<File> getFilesRecursivelyFrom(final File inDirectory) {

    assertInDirectoryValid(inDirectory);

    final ArrayList<File> rtn = new ArrayList<>();
    final var files = inDirectory.listFiles();

    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          rtn.addAll(getFilesRecursivelyFrom(f));
        } else {
          rtn.add(f);
        }
      }
    }

    return rtn;
  }

  /**
   * Get all files in a directory with a specific suffix.
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  public Collection<File> getFilesFromDirectory(final File inDirectory, final String fileSuffix) {

    assertInDirectoryValid(inDirectory);
    AssertValue.checkNotNull("FileSuffix cannot be null", fileSuffix);

    final var files = inDirectory.listFiles((_, name) -> name.endsWith(fileSuffix));

    return Optional.ofNullable(files).stream().flatMap(Arrays::stream)
        .toList();
  }

  /**
   * Get all subdirectories from a root directory.
   */
  public Collection<File> getAllSubdirectories(final String directoryRoot) {

    AssertValue.checkNotNull("DirectoryRoot cannot be null", directoryRoot);

    final var dir = new File(directoryRoot);
    final ArrayList<File> rtn = new ArrayList<>();
    rtn.add(dir);
    rtn.addAll(doGetAllSubdirectories(dir));

    return rtn;
  }

  private Collection<File> doGetAllSubdirectories(final File dir) {

    AssertValue.checkNotNull("Dir cannot be null", dir);

    final ArrayList<File> rtn = new ArrayList<>();
    Optional.ofNullable(dir.listFiles()).stream().flatMap(Arrays::stream)
        .filter(file -> file.isDirectory() && file.canRead())
        .forEach(file -> {
          rtn.add(file);
          rtn.addAll(doGetAllSubdirectories(file));
        });

    return rtn;
  }

  private void assertInDirectoryValid(final File path) {

    AssertValue.checkNotNull("InDirectory cannot be null", path);
  }
}