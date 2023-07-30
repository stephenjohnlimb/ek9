package org.ek9lang.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.LanguageMetaData;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.OsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Test that the fileCache pulls in just what's needed based on EK9 package, dev, etc.
 * So uses a command line with real test examples and configurations of simple ek9 code.
 * We're only interested in checking the right files get identified here.
 * This is so we can add them to a workspace.
 * Quite tricky, when you include dev builds, packages, named source, and default includes.
 */
class FileCacheTest {
  private final LanguageMetaData languageMetaData = new LanguageMetaData("0.0.1-0");
  private final OsSupport osSupport = new OsSupport(true);
  private final FileHandling fileHandling = new FileHandling(osSupport);
  private final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);

  private final Supplier<Optional<SourceResource>> helloWorldSource = () ->
      Optional.of(new SourceResource(false, "/examples/basics/",
          "HelloWorld.ek9", "."));

  @AfterEach
  void tidyUp() {
    String testHomeDirectory = fileHandling.getUsersHomeDirectory();
    assertNotNull(testHomeDirectory);
    //As this is a test delete from process id and below
    fileHandling.deleteContentsAndBelow(new File(new File(testHomeDirectory).getParent()), true);
  }

  @Test
  void checkSingleEk9SourceFileOnly() {
    var resourceDirectory = "/examples/constructs/constants/";
    var ek9SourceFilename = "SingleConstant.ek9";

    testSourceFilesIdentified("-c",
        new SourceResource(resourceDirectory, ek9SourceFilename, "."),
        helloWorldSource.get());
  }

  @Test
  void checkMultipleNamedSourceFiles() {
    var resourceDirectory = "/examples/constructs/references/";
    var ek9SourceFilename = "ConstantRef1.ek9";

    testSourceFilesIdentified("-c", new SourceResource(resourceDirectory, ek9SourceFilename,
            List.of("ConstantRef2.ek9", "ConstantRef3.ek9", "ConstantRef4.ek9"), "."),
        helloWorldSource.get());
  }

  /**
   * So even though we only reference PairPackage.ek9,
   * the other files also get identified.
   * PairPackage does not list them, but states us standard Includes.
   */
  @Test
  void checkStandardIncludesSourceFiles() {
    var resourceDirectory = "/examples/constructs/references/";
    var ek9SourceFilename = "PairPackage.ek9";

    testSourceFilesIdentified("-c", new SourceResource(resourceDirectory, ek9SourceFilename,
        List.of("Pair1.ek9", "Pair2.ek9", "Pair3.ek9"), "."), Optional.empty());
  }

  /**
   * Now copy a file into a dev subdirectory.
   * For a non development build this must not be detected.
   */
  @Test
  void checkStandardIncludesSourceFilesNoneDev() {
    var resourceDirectory = "/examples/constructs/references/";
    var ek9SourceFilename = "PairPackage.ek9";

    testSourceFilesIdentified("-c", new SourceResource(resourceDirectory, ek9SourceFilename,
            List.of("Pair1.ek9", "Pair2.ek9", "Pair3.ek9"), "."),
        Optional.of(new SourceResource(false, resourceDirectory, "DevPair.ek9", "dev")));
  }

  /**
   * Now with '-cd' this is a development compile, so now DevPair.ek9 should be found and compiled.
   */
  @Test
  void checkStandardIncludesSourceFilesDev() {
    var resourceDirectory = "/examples/constructs/references/";
    var ek9SourceFilename = "PairPackage.ek9";

    testSourceFilesIdentified("-cd", new SourceResource(resourceDirectory, ek9SourceFilename,
            List.of("Pair1.ek9", "Pair2.ek9", "Pair3.ek9"), "."),
        Optional.of(new SourceResource(true, resourceDirectory, "DevPair.ek9", "dev")));
  }

  void testSourceFilesIdentified(final String compilerFlag, final SourceResource resource,
                                 final Optional<SourceResource> addition) {
    //Make up a command line.
    String[] command =
        new String[] {String.format("%s %s ", compilerFlag, resource.ek9SourceFilename)};

    var fullList =
        Stream.of(List.of(resource.ek9SourceFilename), resource.additionalSourceFiles)
            .flatMap(List::stream).toList();

    //We will copy this into a working directory and process it.
    fullList.forEach(sourceFile -> assertNotNull(
        sourceFileSupport.copyFileToTestCWD(resource.resourceDirectory, sourceFile)));

    if (addition.isPresent()) {
      //Note we also copy in a rogue file that should not be found
      var additionalResource = addition.get();
      assertNotNull(
          sourceFileSupport.copyFileToTestDirectoryUnderCWD(additionalResource.resourceDirectory,
              additionalResource.ek9SourceFilename, additionalResource.relativeDirectory));
      //Add in the additional resources as these are expected now.
      if (additionalResource.expectToBeCompiled) {
        fullList = Stream
            .of(fullList,
                List.of(additionalResource.ek9SourceFilename),
                additionalResource.additionalSourceFiles)
            .flatMap(List::stream).toList();
      }
    }
    assertCommandResultsInCorrectSources(command, fullList);
  }

  private void assertCommandResultsInCorrectSources(final String[] command,
                                                    final List<String> fullList) {
    //Now check the commandline is valid, and then we can actually test the FileCache.
    CommandLineDetails commandLine =
        new CommandLineDetails(languageMetaData, fileHandling, osSupport);
    int result = commandLine.processCommandLine(command);
    assertTrue(result <= Ek9.SUCCESS_EXIT_CODE);

    FileCache underTest = new FileCache(commandLine);
    var compilableSources = underTest.getAllCompilableProjectFiles();

    assertSourcesIdentified(fullList, compilableSources);
  }

  private void assertSourcesIdentified(final List<String> expectedFiles,
                                       final List<File> identifiedFiles) {

    var justFileNames = identifiedFiles.stream().map(File::getName).toList();
    assertEquals(expectedFiles.size(), justFileNames.size());

    var allPresent = justFileNames.containsAll(expectedFiles);

    assertTrue(allPresent);
  }

  private record SourceResource(boolean expectToBeCompiled, String resourceDirectory,
                                String ek9SourceFilename,
                                List<String> additionalSourceFiles,
                                String relativeDirectory) {

    public SourceResource(String resourceDirectory, String ek9SourceFilename,
                          String relativeDirectory) {
      this(true, resourceDirectory, ek9SourceFilename, List.of(), relativeDirectory);
    }

    public SourceResource(String resourceDirectory, String ek9SourceFilename,
                          List<String> additionalSourceFiles,
                          String relativeDirectory) {
      this(true, resourceDirectory, ek9SourceFilename, additionalSourceFiles, relativeDirectory);
    }

    SourceResource(boolean expectToBeCompiled, String resourceDirectory, String ek9SourceFilename,
                   String relativeDirectory) {
      this(expectToBeCompiled, resourceDirectory, ek9SourceFilename, List.of(), relativeDirectory);
    }
  }
}
