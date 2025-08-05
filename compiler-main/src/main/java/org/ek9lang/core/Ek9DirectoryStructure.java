package org.ek9lang.core;

import java.io.File;
import java.nio.file.FileSystems;

/**
 * Responsibility of dealing with aspects of the ek9 directory structure.
 * Typically, lib, keys, artefacts and other places where EK9 expects items to be located.
 */
public final class Ek9DirectoryStructure {
  public static final String DOT_JAR = ".jar";
  public static final String DOT_EK9 = ".ek9";
  public static final String DOT_PROPERTIES = ".properties";
  public static final String MAIN = "main";
  public static final String LIB = "lib";
  public static final String DEV = "dev";
  public static final String GENERATED = "generated";

  public static final String PUBLIC_PEM = "public.pem";
  public static final String PRIVATE_PEM = "private.pem";

  private final FileHandling fileHandling;

  public Ek9DirectoryStructure(final FileHandling fileHandling) {

    this.fileHandling = fileHandling;

  }

  /**
   * Access the final built artefact that can be executed.
   */
  public File getTargetExecutableArtefact(final String ek9FullPathToFileName,
                                          final TargetArchitecture targetArchitecture) {

    assertEk9FullPathToFileNameValid(ek9FullPathToFileName);
    assertTargetArchitectureSupported(targetArchitecture);

    final var sourceFile = new File(ek9FullPathToFileName);
    final var ek9FileNameDirectory = sourceFile.getParent();
    final var ek9JustFileName = sourceFile.getName();
    final var projectEk9Directory = fileHandling.getDotEk9Directory(ek9FileNameDirectory);

    return new File(projectEk9Directory, ek9JustFileName.replace(DOT_EK9, DOT_JAR));
  }

  /**
   * Get final set of properties for the main artefact.
   */
  public File getTargetPropertiesArtefact(final String ek9FullPathToFileName) {

    assertEk9FullPathToFileNameValid(ek9FullPathToFileName);

    final var sourceFile = new File(ek9FullPathToFileName);
    final var ek9FileNameDirectory = sourceFile.getParent();
    final var fileName = sourceFile.getName().replace(DOT_EK9, DOT_PROPERTIES);
    final var projectEk9Directory = fileHandling.getDotEk9Directory(ek9FileNameDirectory);

    return new File(projectEk9Directory, fileName);
  }

  /**
   * Check the structure of an Ek9 build directory.
   */
  public void validateEk9Directory(final String directoryName, final TargetArchitecture targetArchitecture) {

    AssertValue.checkNotEmpty("DirectoryName is empty", directoryName);
    assertTargetArchitectureSupported(targetArchitecture);

    fileHandling.makeDirectoryIfNotExists(new File(directoryName));
    makeEk9DevDirectoryStructure(directoryName, targetArchitecture);

  }

  /**
   * Used to create structure under .ek9 in both a project directory and a $HOME/.ek9 directory.
   *
   * @param fromEk9BaseDirectory typically $HOME/.ek9/ or /path/to/project/.ek9/
   */
  public void makeEk9DevDirectoryStructure(final String fromEk9BaseDirectory,
                                           final TargetArchitecture targetArchitecture) {

    assertFromEk9BaseDirectoryValid(fromEk9BaseDirectory);
    assertTargetArchitectureSupported(targetArchitecture);

    //For ALL libraries still in source ek9 but under their full versioned package name.
    //Unpacked form /path/to/project/.ek9/lib/com.some.package-4.9.5-feature23-40/

    //Once the ek9 source is processed to .class/.jar/.so - the resulting lib is then stored as
    //i.e. /path/to/project/.ek9/com.some.package-4.9.5-feature23-40.jar
    //or   /path/to/project/.ek9/com.some.package-4.9.5-feature23-40.so

    //The alternative is just to compile them up in the
    //$HOME/.ek9/lib/com.some.package-4.9.5-feature23-40/ directory
    //Then the final jar/executables can be stored in $HOME/.ek9/lib/com.some.package-4.9.5-feature23-40.jar
    //So when it comes to resolving - we can quickly check if we have the package and also if it
    //is already built.
    //We still need to load the ek9 source from the package for resolving and the like.
    //But no longer need to generate .classes/.o/.jar/.so If it is already up-to-date.

    addDirectory(fromEk9BaseDirectory, LIB);

    //Where we put anything we generate - intermediate formats java/classes etc/
    final var generated = addDirectory(fromEk9BaseDirectory, GENERATED);
    final var lib = addDirectory(generated, LIB);
    addDirectory(lib, targetArchitecture.getDescription());

    final var main = addDirectory(generated, MAIN);
    addDirectory(main, targetArchitecture.getDescription());

    final var dev = addDirectory(generated, DEV);
    addDirectory(dev, targetArchitecture.getDescription());

  }

  /**
   * This expects an ek9 source file in a specific directory.
   *
   * @param ek9FullPathToFileName i.e. main.ek9
   * @param targetArchitecture    i.e. "jvm" or "llvm-go/llvm-cpp"
   */
  public void cleanEk9DirectoryStructureFor(final String ek9FullPathToFileName,
                                            final TargetArchitecture targetArchitecture) {

    AssertValue.checkNotEmpty("EK9FullPathToFileName is empty", ek9FullPathToFileName);
    assertTargetArchitectureSupported(targetArchitecture);

    final var sourceFile = new File(ek9FullPathToFileName);
    final var ek9FileNameDirectory = sourceFile.getParent();
    final var dotEk9Dir = fileHandling.getDotEk9Directory(ek9FileNameDirectory);

    fileHandling.deleteContentsAndBelow(new File(dotEk9Dir, GENERATED), false);
    fileHandling.deleteContentsAndBelow(new File(dotEk9Dir, LIB), false);

    final var target = getTargetExecutableArtefact(ek9FullPathToFileName, targetArchitecture);
    fileHandling.deleteFileIfExists(target);

    final var props = getTargetPropertiesArtefact(ek9FullPathToFileName);
    fileHandling.deleteFileIfExists(props);

  }

  /**
   * Get the generated output directory.
   */
  public File getMainGeneratedOutputDirectory(final String fromEk9BaseDirectory,
                                              final TargetArchitecture targetArchitecture) {

    assertFromEk9BaseDirectoryValid(fromEk9BaseDirectory);
    assertTargetArchitectureSupported(targetArchitecture);

    return FileSystems.getDefault()
        .getPath(fromEk9BaseDirectory, GENERATED, MAIN, targetArchitecture.getDescription()).toFile();
  }

  /**
   * Get the final output directory - where the artefacts will be.
   */
  public File getMainFinalOutputDirectory(final String fromEk9BaseDirectory,
                                          final TargetArchitecture targetArchitecture) {

    assertFromEk9BaseDirectoryValid(fromEk9BaseDirectory);
    assertTargetArchitectureSupported(targetArchitecture);

    return FileSystems.getDefault().getPath(fromEk9BaseDirectory, GENERATED, MAIN, targetArchitecture.getDescription())
        .toFile();
  }

  /**
   * Main generated code output directory when in development.
   */
  public File getDevGeneratedOutputDirectory(final String fromEk9BaseDirectory,
                                             final TargetArchitecture targetArchitecture) {

    assertFromEk9BaseDirectoryValid(fromEk9BaseDirectory);
    assertTargetArchitectureSupported(targetArchitecture);

    return FileSystems.getDefault()
        .getPath(fromEk9BaseDirectory, GENERATED, DEV, targetArchitecture.getDescription()).toFile();
  }

  /**
   * Main final output directory when in development.
   */
  public File getDevFinalOutputDirectory(final String fromEk9BaseDirectory,
                                         final TargetArchitecture targetArchitecture) {

    assertFromEk9BaseDirectoryValid(fromEk9BaseDirectory);
    assertTargetArchitectureSupported(targetArchitecture);

    return FileSystems
        .getDefault()
        .getPath(fromEk9BaseDirectory, GENERATED, DEV, targetArchitecture.getDescription())
        .toFile();
  }

  /**
   * Ensure all stale packaged content is removed.
   */
  public void deleteStalePackages(final String ek9FileNameDirectory, final String moduleName) {

    AssertValue.checkNotEmpty("EK9FileNameDirectory is empty", ek9FileNameDirectory);
    AssertValue.checkNotEmpty("ModuleName is empty", moduleName);

    final var zipFileName = moduleName + "-.*\\.zip";
    fileHandling.deleteMatchingFiles(
        new File(fileHandling.getDotEk9Directory(ek9FileNameDirectory)), zipFileName);

  }

  /**
   * Does a key pair for signing content exist.
   */
  public boolean isUsersSigningKeyPairPresent() {

    final var dir = fileHandling.getUsersHomeEk9Directory();

    return (new File(dir, PRIVATE_PEM).exists() && new File(dir, PUBLIC_PEM).exists());
  }

  /**
   * Get the key signing pair for this user.
   */
  public SigningKeyPair getUsersSigningKeyPair() {

    final var dir = fileHandling.getUsersHomeEk9Directory();
    final var privateKeyFile = new File(dir, PRIVATE_PEM);
    final var publicKeyFile = new File(dir, PUBLIC_PEM);

    return SigningKeyPair.of(privateKeyFile, publicKeyFile);
  }

  /**
   * Save the signing key pair to the users home directory.
   */
  public boolean saveToHomeEk9Directory(final SigningKeyPair keyPair) {

    AssertValue.checkNotNull("Keypair is null", keyPair);
    final var dir = fileHandling.getUsersHomeEk9Directory();

    return fileHandling.saveToOutput(new File(dir, PRIVATE_PEM), keyPair.getPrivateKeyInBase64())
        && fileHandling.saveToOutput(new File(dir, PUBLIC_PEM), keyPair.getPublicKeyInBase64());
  }

  private String addDirectory(final String baseDir, final String newDir) {

    AssertValue.checkNotEmpty("BaseDir is empty", baseDir);
    AssertValue.checkNotEmpty("NewDir is empty", newDir);

    final var directory = new File(baseDir, newDir);
    fileHandling.makeDirectoryIfNotExists(directory);

    return directory.getAbsolutePath();
  }

  private void assertTargetArchitectureSupported(final TargetArchitecture targetArchitecture) {

    final Processor<Void> processor = () -> {
      AssertValue.checkNotNull("TargetArchitecture is null", targetArchitecture);
      return null;
    };

    new ExceptionConverter<Void>().apply(processor);
  }

  private void assertEk9FullPathToFileNameValid(final String path) {

    AssertValue.checkNotEmpty("EK9FullPathToFileName is empty", path);

  }

  private void assertFromEk9BaseDirectoryValid(final String path) {

    AssertValue.checkNotEmpty("FromEK9BaseDirectory is empty", path);

  }

}
