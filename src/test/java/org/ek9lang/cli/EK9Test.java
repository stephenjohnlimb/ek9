package org.ek9lang.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.net.URL;
import org.ek9lang.compiler.Compiler;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;
import org.ek9lang.core.SigningKeyPair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test the main command to run EK9 but from a test pint of view.
 * <p>
 * Only design to test valid command line instructions.
 * See CommandLineDetailsTest of invalid combinations.
 */
//Specific tests that manipulate files and specifics in ek9 must not run in parallel.
@Execution(SAME_THREAD)
@ResourceLock(value="file_access", mode=READ_WRITE)
final class EK9Test {
  private final LanguageMetaData languageMetaData = new LanguageMetaData("0.0.1-0");
  private final OsSupport osSupport = new OsSupport(true);
  private final FileHandling fileHandling = new FileHandling(osSupport);
  private final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);

  @BeforeAll
  static void disableLogger() {
    Logger.setMuteStderrOutput(true);
  }

  @AfterAll
  static void enableLogger() {
    Logger.setMuteStderrOutput(false);
  }

  @AfterEach
  void tidyUp() {
    String testHomeDirectory = fileHandling.getUsersHomeDirectory();
    assertNotNull(testHomeDirectory);
    //As this is a test delete from process id and below
    fileHandling.deleteContentsAndBelow(new File(new File(testHomeDirectory).getParent()), true);
  }

  @Test
  void testCommandLineHelpText() {
    assertResult(Ek9.SUCCESS_EXIT_CODE, new String[] {"-h"});
  }

  @Test
  void testCommandLineVersionText() {
    assertResult(Ek9.SUCCESS_EXIT_CODE, new String[] {"-V"});
  }

  @Test
  void testIncrementationCompilation() {
    String sourceName = "HelloWorld.ek9";
    String[] command = new String[] {"-c " + sourceName};

    //We will copy this into a working directory and process it.
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    //This will actually trigger a full compile first.
    assertCompilationArtefactsPresent(assertResult(Ek9.SUCCESS_EXIT_CODE, command));

    //Now if we do it again there will be nothing to compile
    CommandLineDetails commandLine = assertResult(Ek9.SUCCESS_EXIT_CODE, command);
    assertCompilationArtefactsPresent(commandLine);

    //If we remove the target it will trigger a full re-compile and packaging
    File targetArtefact =
        fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(),
            commandLine.targetArchitecture);
    assertTrue(targetArtefact.delete());
    assertCompilationArtefactsPresent(assertResult(Ek9.SUCCESS_EXIT_CODE, command));

    long lastModified = targetArtefact.lastModified();
    //Now simulate updating the source to ensure target gets rebuilt.
    assertTrue(sourceFile.setLastModified(lastModified + 1));

    //Trigger incremental rebuild
    assertCompilationArtefactsPresent(assertResult(Ek9.SUCCESS_EXIT_CODE, command));
    targetArtefact =
        fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(),
            commandLine.targetArchitecture);
    assertNotSame(lastModified, targetArtefact.lastModified());
  }

  @ParameterizedTest
  @ValueSource(strings = {"-Cdp IR_ANALYSIS", "-Cp IR_ANALYSIS", "-ch", "-Cl"})
  void testNoneArtifactCompilation(final String flag) {
    String sourceName = "HelloWorld.ek9";
    String[] command = new String[] {String.format("%s %s", flag, sourceName)};

    //We will copy this into a working directory and process it.
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    //This will actually trigger a full compile first, but no artefact should be created in a check.
    assertCompilationArtefactsNotPresent(assertResult(Ek9.SUCCESS_EXIT_CODE, command));
  }

  @Test
  void testGenerateKeys() {
    String[] command = new String[] {"-Gk"};

    File home = new File(osSupport.getUsersHomeDirectory());
    assertTrue(home.exists());
    var commandLineDetails = assertResult(Ek9.SUCCESS_EXIT_CODE, command);
    assertNotNull(commandLineDetails);
    assertKeysPresent();

    //Now trigger it again to ensure it does not cause any errors
    commandLineDetails = assertResult(Ek9.SUCCESS_EXIT_CODE, command);
    assertNotNull(commandLineDetails);
    assertKeysPresent();
  }

  @Test
  void testUpdateUpgrade() {
    String[] command = new String[] {"-Up"};
    var commandLineDetails = assertResult(Ek9.SUCCESS_EXIT_CODE, command);
    assertNotNull(commandLineDetails);
  }

  @Test
  void testDebugDevCompilation() {
    String sourceName = "HelloWorld.ek9";
    String[] command = new String[] {"-Cd " + sourceName};

    //We will copy this into a working directory and process it.
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    assertCompilationArtefactsPresent(assertResult(Ek9.SUCCESS_EXIT_CODE, command));
  }

  @Test
  void testRunSingleProgram() {
    String sourceName = "HelloWorld.ek9";
    String[] command = new String[] {sourceName};

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    assertCompilationArtefactsPresent(assertResult(Ek9.RUN_COMMAND_EXIT_CODE, command));
  }

  @Test
  void testRunSingleProgramInDebug() {
    String sourceName = "HelloWorld.ek9";
    String[] command = new String[] {"-d 9999 " + sourceName};

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    assertCompilationArtefactsPresent(assertResult(Ek9.RUN_COMMAND_EXIT_CODE, command));
  }

  @Test
  void testRunUnitTests() {
    String sourceName = "HelloWorlds.ek9";
    String[] command = new String[] {"-t " + sourceName};

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    //This will actually trigger a full compile and then run.
    assertCompilationArtefactsPresent(assertResult(Ek9.SUCCESS_EXIT_CODE, command));
  }

  @Test
  void testRunSelectedProgram() {
    String sourceName = "HelloWorlds.ek9";
    String[] command = new String[] {sourceName + " -r HelloMars"};

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    //This will actually trigger a full compile and then run.
    assertCompilationArtefactsPresent(assertResult(Ek9.RUN_COMMAND_EXIT_CODE, command));
  }

  @Test
  void testInstallNonSuchPackage() {
    String sourceName = "HelloWorlds.ek9";
    String[] command = new String[] {"-I " + sourceName};

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    //Should fail because this source does not define a package.
    assertResult(Ek9.BAD_COMMANDLINE_EXIT_CODE, command);
  }

  @Test
  void testValidPackage() {
    String sourceName = "PackageNoDeps.ek9";
    String[] command = new String[] {"-P " + sourceName};

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/",
        sourceName);
    assertNotNull(sourceFile);

    //Should pass because it does have a valid package.
    assertResult(Ek9.SUCCESS_EXIT_CODE, command);
  }

  @Test
  void testAlterationToDependencies() {
    var sourceName = "testFile.ek9";
    var ek9InitialSource = """
        #!ek9
        defines module net.inter1
            
          defines package
            
            publicAccess as Boolean := true
            version as Version: 1.2.0-0
            description as String = "Simulation intermediate package"
            license <- "MIT"

            tags <- [ "tools" ]

            deps <- {
              "ekopen.network.support.utils": "1.6.1-9"
              }
        //EOF""";

    //Make sure we have the dependency in place
    installPackage("SupportUtils.ek9");

    File cwd = new File(osSupport.getCurrentWorkingDirectory());
    assertTrue(cwd.exists());
    File newSourceFile = new File(cwd, sourceName);
    fileHandling.saveToOutput(newSourceFile, ek9InitialSource);
    String[] command = new String[] {"-P " + sourceName};
    assertResult(Ek9.SUCCESS_EXIT_CODE, command);
    //OK so now simulate changing the dependency

    var updatedEk9InitialSource = """
        #!ek9
        defines module net.inter1
            
          defines package
            
            publicAccess as Boolean := true
            version as Version: 1.2.0-0
            description as String = "Simulation intermediate package"
            license <- "MIT"

            tags <- [ "tools" ]

            deps <- {
              "ekopen.network.support.utils": "2.4.1-9"
              }
        //EOF""";
    //So update th source code and put in the new dependency
    fileHandling.saveToOutput(newSourceFile, updatedEk9InitialSource);
    installPackage("SupportUtils2.ek9");
    //Now check that too runs and is successful.
    assertResult(Ek9.SUCCESS_EXIT_CODE, command);
  }

  @Test
  void testEnvironmentVariable() {
    var user = "'Steve Limb'";
    String sourceName = "HelloWorlds.ek9";
    String[] command = new String[] {"-e " + user + " " + sourceName + " -r HelloMars"};

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    //Should fail because this source does not define a package.
    assertResult(Ek9.RUN_COMMAND_EXIT_CODE, command);

  }

  @Test
  void testInstallPackage() {
    installPackage("PackageNoDeps.ek9");
    File libDir = fileHandling.getUsersHomeEk9LibDirectory();
    assertNotNull(libDir.listFiles());
    //Expect a zip and a sha256 of that zip.
    var files = libDir.listFiles();
    assertNotNull(files);
    assertEquals(2, files.length);
  }

  @Test
  void testBuildVersioningOfPackage() {
    assertPackageVersionChange("1.0.0-1", "-IV build");
  }

  @Test
  void testPatchVersioningOfPackage() {
    assertPackageVersionChange("1.0.1-0", "-IV patch");
  }

  @Test
  void testMinorVersioningOfPackage() {
    assertPackageVersionChange("1.1.0-0", "-IV minor");
  }

  @Test
  void testMajorVersioningOfPackage() {
    assertPackageVersionChange("2.0.0-0", "-IV major");
  }

  @Test
  void testSetVersionButNoPackagePresent() {
    String sourceName = "HelloWorld.ek9";
    String[] incrementBuildNo = new String[] {"-SV 3.6.7 " + sourceName};

    File sourceFile =
        sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);
    CommandLineDetails commandLine = assertResult(Ek9.BAD_COMMANDLINE_EXIT_CODE, incrementBuildNo);
    assertNotNull(commandLine);
  }

  @Test
  void testSetVersioningOfPackage() {
    assertPackageVersionChange("3.8.6-0", "-SV 3.8.6");
    //Now check that it can also be incremented and appropriate resets on patch and build no.
    assertPackageVersionChange("3.9.0-0", "-IV minor");
  }

  @Test
  void testSetVersioningOfPackageUsingAsVersion() {
    assertPackageVersionOfFileChange("SinglePackage.ek9", "3.8.6-0", "-SV 3.8.6");
    //Now check that it can also be incremented and appropriate resets on patch and build no.
    assertPackageVersionOfFileChange("SinglePackage.ek9", "3.9.0-0", "-IV minor");
  }

  @Test
  void testSetFeatureVersioningOfPackage() {
    assertPackageVersionChange("3.8.6-specials-0", "-SF 3.8.6-specials");
    //Now check that it can also be incremented and patch/build number is reset to 0.
    assertPackageVersionChange("3.9.0-specials-0", "-IV minor");
  }

  @Test
  void testPrintVersioningOfPackage() {
    assertPackageVersionChange("3.8.6-specials-0", "-SF 3.8.6-specials");
    //Now check that it can also be printed.
    assertPackageVersionChange("3.8.6-specials-0", "-PV");
  }

  @Test
  void testDeploymentOfPackage() {
    deployPackage(Ek9.SUCCESS_EXIT_CODE, "PackageNoDeps.ek9");
  }

  @Test
  void testBadPackage() {
    String sourceName = "BadPackage.ek9";
    String[] command = new String[] {"-I " + sourceName};

    File sourceFile =
        sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
    assertNotNull(sourceFile);

    //This should Fail with a file issue, because it should not parse.
    CommandLineDetails commandLine =
        new CommandLineDetails(languageMetaData, fileHandling, osSupport);

    int result = commandLine.processCommandLine(command);
    assertEquals(Ek9.FILE_ISSUE_EXIT_CODE, result);
  }

  /**
   * Check that standard includes works.
   */
  @Test
  void testPackageWithStandardIncludes() {
    //So firstly we need to 'install' a number of dependent packages
    //then we can try and resolve those dependencies.
    installPackage("StandardIncludes.ek9");
  }

  /**
   * A more complex test that checks that the package resolver mechanism works.
   * For this test we actually build and package each of the dependencies locally.
   * But if they were not local builds, the resolver would (in the future)
   * try and pull these from a repository.
   */
  @Test
  void testPackageDependencyResolution() {
    //So firstly we need to 'install' a number of dependent packages
    //then we can try and resolve those dependencies.
    installPackage("SupportUtils.ek9");
    installPackage("HandyTools.ek9");
    installPackage("SupertoolsUtil.ek9");
    installPackage("ToolsMisc.ek9");

    //Now for the one with dependencies
    deployPackage(Ek9.SUCCESS_EXIT_CODE, "SinglePackage.ek9");
  }

  @Test
  void testSemanticVersionBreach() {
    installPackage("SupportUtils.ek9");
    installPackage("SupportUtils2.ek9");
    installPackage("InterPackage1.ek9");
    installPackage("InterPackage2.ek9");

    installPackage(Ek9.BAD_COMMANDLINE_EXIT_CODE, "SemanticBreach.ek9");
  }

  @Test
  void testSemanticVersionPromotion() {
    installPackage("SupportUtils.ek9");
    installPackage("SupportUtils3.ek9");
    installPackage("InterPackage1.ek9");
    installPackage("InterPackage3.ek9");

    installPackage("SemanticPromotion.ek9");
  }

  @Test
  void testSemanticVersionOptimisation() {
    installPackage("SupportUtils.ek9");
    installPackage("SupertoolsUtil.ek9");
    installPackage("SupportUtils3.ek9");
    installPackage("InterPackage1.ek9");
    installPackage("InterPackage3.ek9");
    installPackage("InterPackage4.ek9");
    installPackage("InterPackage5.ek9");
    installPackage("InterPackage6.ek9");

    installPackage("SemanticVersionOptimisation.ek9");
  }

  @Test
  void testMissingPackageDependencyResolution() {
    //So firstly we need to 'install' a number of dependent packages
    //then we can try and resolve those dependencies.
    installPackage("SupportUtils.ek9");
    //We omit this package and the overall resolution should fail
    //installPackage("HandyTools.ek9");
    installPackage("SupertoolsUtil.ek9");
    installPackage("ToolsMisc.ek9");

    //Now for the one with dependencies - but we've not got one present.
    deployPackage(Ek9.BAD_COMMANDLINE_EXIT_CODE, "SinglePackage.ek9");
  }

  @Test
  void testCircularDependencies() {
    //We have to cheat like a developer would do and put some stuff in the place where it would have been packaged!
    //So put mangled version of both deps in; and then we can resolve both to build a new one.
    //Then we can see if we can detect those circular references.

    //make sure the structure for packages exists.
    fileHandling.validateHomeEk9Directory("java");

    simulatePackagedInstallation("Circular2", "net.circular.two", "1.0.1-0");
    simulatePackagedInstallation("Circular1", "net.circular.one", "1.0.0-0");

    //Only now we've simulated how a developer might try and build circular refs can we test our next check.

    installPackage(Ek9.BAD_COMMANDLINE_EXIT_CODE, "Circular1.ek9");
  }

  @Test
  void testCircularDependenciesDifferentVersions() {
    //make sure the structure for packages exists.
    fileHandling.validateHomeEk9Directory("java");

    //Now these should install OK
    installPackage(Ek9.SUCCESS_EXIT_CODE, "CircularVersions3.ek9");
    installPackage(Ek9.SUCCESS_EXIT_CODE, "CircularVersions2.ek9");

    //But now we rework 'net.circular.verone' but introduce a circular dependency
    //So in EK9 in a single build there can only be one version of a package named module
    //It is not possible to have multiple versions of the same named package
    //The can all exist in the repository or in the lib, but it cannot depend on previous versions
    //of itself.
    installPackage(Ek9.BAD_COMMANDLINE_EXIT_CODE, "CircularVersions1.ek9");
  }

  @Test
  void testCircularDependencyResolution() {
    //Similar to above but just checking dependency resolution for circular detections.
    //make sure the structure for packages exists.
    fileHandling.validateHomeEk9Directory("java");

    simulatePackagedInstallation("Circular2", "net.circular.two", "1.0.1-0");
    simulatePackagedInstallation("Circular1", "net.circular.one", "1.0.0-0");

    //Only now we've simulated how a developer might try and build circular refs can we test our next check.
    String sourceName = "Circular1.ek9";
    String[] command = new String[] {"-Dp " + sourceName};

    File sourceFile =
        sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
    assertNotNull(sourceFile);
    assertResult(Ek9.BAD_COMMANDLINE_EXIT_CODE, command);
  }

  private void installPackage(String sourceName) {
    installPackage(Ek9.SUCCESS_EXIT_CODE, sourceName);

    File libDir = fileHandling.getUsersHomeEk9LibDirectory();
    assertNotNull(libDir);
    assertTrue((libDir.exists()));
  }

  private void installPackage(int expectation, String sourceName) {
    String[] command = new String[] {"-I " + sourceName};

    File sourceFile =
        sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
    assertNotNull(sourceFile);

    //This should succeed and install the package in users lib directory
    assertResult(expectation, command);
  }

  private void simulatePackagedInstallation(String sourceName, String moduleName, String version) {
    String dependencyVector = fileHandling.makeDependencyVector(moduleName, version);
    File homeEK9Lib = fileHandling.getUsersHomeEk9LibDirectory();

    //Let's check if it is unpacked already, if not we can unpack it.
    File unpackedDir = new File(homeEK9Lib, dependencyVector);
    if (!unpackedDir.exists()) {
      assertTrue(unpackedDir.mkdirs());
    }

    URL simulatedProperties = getClass().getResource(
        "/examples/constructs/packages/" + sourceName + ".package.properties");
    assertNotNull(simulatedProperties);
    fileHandling.copy(new File(simulatedProperties.getPath()),
        new File(unpackedDir, ".package.properties"));

    //Now the source file for that package
    URL simulatedSource =
        getClass().getResource("/examples/constructs/packages/" + sourceName + ".ek9");
    assertNotNull(simulatedSource);
    fileHandling.copy(new File(simulatedSource.getPath()),
        new File(unpackedDir, sourceName + ".ek9"));
  }

  private void deployPackage(int expectedExitCode, String sourceName) {
    String[] deployCommand = new String[] {"-D " + sourceName};
    File sourceFile =
        sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
    assertNotNull(sourceFile);

    //So this should just package up and deploy the artefact.
    CommandLineDetails commandLine = assertResult(expectedExitCode, deployCommand);

    if (expectedExitCode == Ek9.SUCCESS_EXIT_CODE) {
      String zipFileName = fileHandling.makePackagedModuleZipFileName(commandLine.getModuleName(),
          commandLine.getVersion());
      File sha256EncFile =
          new File(fileHandling.getDotEk9Directory(commandLine.getSourceFileDirectory()),
              zipFileName + ".sha256.enc");
      assertTrue(sha256EncFile.exists());
    }
  }

  private void assertPackageVersionChange(String expectedVersion, String command) {
    assertPackageVersionOfFileChange("PackageNoDeps.ek9", expectedVersion, command);
  }

  private void assertPackageVersionOfFileChange(String sourceName, String expectedVersion,
                                                String command) {
    String[] incrementBuildNo = new String[] {command + " " + sourceName};

    File sourceFile =
        sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
    assertNotNull(sourceFile);

    //So this should just increment the build number from '0' which is what is in PackageNoDeps.ek9 to '1'
    CommandLineDetails commandLine = assertResult(Ek9.SUCCESS_EXIT_CODE, incrementBuildNo);

    //Now get the line number
    Integer versionLineNumber = commandLine.processEk9FileProperties(true);
    assertNotNull(versionLineNumber);
    assertEquals(expectedVersion, commandLine.getVersion());
  }

  private CommandLineDetails assertResult(int expectation, String[] argv) {
    CommandLineDetails commandLine =
        new CommandLineDetails(languageMetaData, fileHandling, osSupport);
    FileCache sourceFileCache = new FileCache(commandLine);
    Compiler compiler = new StubCompiler();
    CompilationContext compilationContext =
        new CompilationContext(commandLine, compiler, sourceFileCache, true);

    int result = commandLine.processCommandLine(argv);

    assertTrue(result <= Ek9.SUCCESS_EXIT_CODE);

    //Now should something be run and executed.
    try {
      if (result == Ek9.RUN_COMMAND_EXIT_CODE) {
        assertEquals(expectation, new Ek9(compilationContext).run());
      }
    } catch (InterruptedException exception) {
      fail(exception);
    }

    return commandLine;
  }

  private void assertKeysPresent() {
    assertTrue(fileHandling.isUsersSigningKeyPairPresent());
    SigningKeyPair signingKeyPair = fileHandling.getUsersSigningKeyPair();
    assertNotNull(signingKeyPair);
    assertNotNull(signingKeyPair.getPublicKeyInBase64());
    assertNotNull(signingKeyPair.getPrivateKeyInBase64());
    //get keys and check they are ok
  }

  private void assertCompilationArtefactsPresent(CommandLineDetails commandLine) {
    File propsFile =
        fileHandling.getTargetPropertiesArtefact(commandLine.getFullPathToSourceFileName());
    assertTrue(propsFile.exists());

    File targetArtefact =
        fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(),
            commandLine.targetArchitecture);
    assertTrue(targetArtefact.exists());
  }

  private void assertCompilationArtefactsNotPresent(CommandLineDetails commandLine) {

    File targetArtefact =
        fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(),
            commandLine.targetArchitecture);
    assertFalse(targetArtefact.exists());
  }
}
