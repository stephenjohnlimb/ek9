package org.ek9lang.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Some tests to check the command line processing.
 * <p>
 * Always difficult and error-prone to handle a range of different command line options.
 * <p>
 * So if there are issues, always add another test.
 */
//Specific tests that manipulate files and specifics in ek9 must not run in parallel.
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
final class CommandLineDetailsTest {
  private final LanguageMetaData languageMetaData = new LanguageMetaData("0.0.1-0");
  private final OsSupport osSupport = new OsSupport(true);
  private final FileHandling fileHandling = new FileHandling(osSupport);
  private final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);

  private final Function<String, String> copyFileToTestCWD = sourceName -> {
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
    return sourceName;
  };

  private final Function<String, Integer> processStringCommandLine =
      commandLine -> createClassUnderTest().processCommandLine(commandLine);

  private final Function<String[], Integer> processStringArrayCommandLine =
      commandLine -> createClassUnderTest().processCommandLine(commandLine);

  private final Function<String, CommandLineDetails> processStringCommandLineExpectSuccess =
      commandLine -> {
        var commandLineDetails = createClassUnderTest();
        assertEquals(0, commandLineDetails.processCommandLine(commandLine));
        return commandLineDetails;
      };

  private final Function<String, Function<String, CommandLineDetails>> makeProcess = command ->
      copyFileToTestCWD
          .andThen(fileName -> command + " " + fileName)
          .andThen(processStringCommandLineExpectSuccess);

  private final Consumer<CommandLineDetails> assertIncrementalCompilation =
      commandLineDetails -> assertTrue(commandLineDetails.isIncrementalCompile());

  private final Consumer<CommandLineDetails> assertDevBuild =
      commandLineDetails -> assertTrue(commandLineDetails.isDevBuild());

  private final Consumer<CommandLineDetails> assertFullCompilation =
      commandLineDetails -> assertTrue(commandLineDetails.isFullCompile());

  private final Consumer<CommandLineDetails> assertDebug =
      commandLineDetails -> assertTrue(commandLineDetails.isDebuggingInstrumentation());

  private final Consumer<CommandLineDetails> assertVerbose =
      commandLineDetails -> assertTrue(commandLineDetails.isVerbose());

  private final Consumer<CommandLineDetails> assertCheckCompile =
      commandLineDetails -> assertTrue(commandLineDetails.isCheckCompileOnly());

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

  private CommandLineDetails createClassUnderTest() {
    return new CommandLineDetails(languageMetaData, fileHandling, osSupport);
  }

  @Test
  void testCommandLineHelpText() {
    assertNotNull(CommandLineDetails.getCommandLineHelp());
  }

  @Test
  void testEmptyCommandLine() {
    String aNull = null;
    assertEquals(2, processStringCommandLine.apply(aNull));
    assertEquals(2, processStringCommandLine.apply(""));

    String[] aNullArray = null;
    assertEquals(2, processStringArrayCommandLine.apply(aNullArray));
    assertEquals(2, processStringArrayCommandLine.apply(new String[0]));
  }

  @Test
  void testCommandLineVersion() {
    assertEquals(1, processStringCommandLine.apply("-V"));
  }

  @Test
  void testDefaultPackageSettings() {
    String sourceName = "TCPExample.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/fullPrograms/networking/", sourceName);

    //As part of the package mechanism we have defaults when not specified.
    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-c " + sourceName));

    assertFalse(underTest.applyStandardIncludes());
    assertTrue(underTest.applyStandardExcludes());

    assertEquals(1, underTest.getIncludeFiles().size());
    assertEquals(1, underTest.getExcludeFiles().size());
  }

  private void runCommandLineExpecting(final String commandToTest,
                                       final Consumer<CommandLineDetails> assertion) {
    Consumer<String> toTest =
        item -> assertion.accept(processStringCommandLineExpectSuccess.apply(item));
    toTest.accept(commandToTest);
  }

  @Test
  void testLanguageServerCommandLine() {
    runCommandLineExpecting("-ls",
        commandLineDetails -> assertTrue(commandLineDetails.isRunEk9AsLanguageServer()));
  }

  @Test
  void testLanguageServerWithHoverHelpCommandLine() {
    Consumer<CommandLineDetails> assertion1 =
        commandLineDetails -> assertTrue(commandLineDetails.isRunEk9AsLanguageServer());
    Consumer<CommandLineDetails> assertion2 =
        commandLineDetails -> assertTrue(commandLineDetails.isEk9LanguageServerHelpEnabled());

    runCommandLineExpecting("-ls -lsh", assertion1.andThen(assertion2));
  }

  @Test
  void testCommandLineHelp() {
    assertEquals(1, processStringCommandLine.apply("-h"));
  }

  @Test
  void testCommandLineInvalidIncrementalCompile() {
    assertEquals(3, processStringCommandLine.apply("-c"));
  }

  @Test
  void testSimulationOfSourceFileAccess() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    //Now we can put a dummy source file in the simulated/test cwd and then try and process it.
    File cwd = new File(osSupport.getCurrentWorkingDirectory());

    assertEquals(0, underTest.processCommandLine("-c " + sourceName));

    //Now a few directories and files now should exist.
    File propsFile = new File(fileHandling.getDotEk9Directory(cwd), "SinglePackage.properties");
    assertTrue(propsFile.exists());

    //Now we can test the reloading. Without forced reloading
    assertNull(underTest.processEk9FileProperties(false));
    //Now with forced reloading
    assertNotNull(underTest.processEk9FileProperties(true));
    //TODO
  }

  @Test
  void testHandlingEK9Package() {
    String sourceName = "TCPExample.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/fullPrograms/networking/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    //Now we can put a dummy source file in the simulated/test cwd and then try and process it.

    assertEquals(0, underTest.processCommandLine("-c " + sourceName));

    assertEquals("2.3.14-20", underTest.getVersion());
    assertEquals("example.networking", underTest.getModuleName());
    assertEquals(4, underTest.numberOfProgramsInSourceFile());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineIncrementalCompile() {
    var process = makeProcess.apply("-c");
    assertIncrementalCompilation
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineIncrementalCheckCompileOnly() {
    var process = makeProcess.apply("-ch");
    assertIncrementalCompilation
        .andThen(assertCheckCompile)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineIncrementalDebugCompile() {

    var process = makeProcess.apply("-cg");
    assertIncrementalCompilation
        .andThen(assertDebug)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineIncrementalDevCompile() {

    var process = makeProcess.apply("-cd");
    assertIncrementalCompilation
        .andThen(assertDebug)
        .andThen(assertDevBuild)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineIncrementalDevCheckCompile() {

    var process = makeProcess.apply("-cdh");
    assertIncrementalCompilation
        .andThen(assertDebug)
        .andThen(assertDevBuild)
        .andThen(assertCheckCompile)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @ParameterizedTest
  @ValueSource(strings = {"-C", "-Cp PARSING", "-Cdp PARSING"})
  @SuppressWarnings("java:S2699")
  void testCommandLineFullCompile(final String flag) {
    var process = makeProcess.apply(flag);
    assertFullCompilation
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testInvalidCommandLineFullPhasedCompile() {
    assertEquals(2, processStringCommandLine.apply("-Cp parse HelloWorld.ek9"));
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineFullCheckCompileOnly() {
    var process = makeProcess.apply("-Ch");
    assertFullCompilation
        .andThen(assertCheckCompile)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineFullVerboseCompile() {
    var process = makeProcess.apply("-C -v");
    assertFullCompilation
        .andThen(assertVerbose)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineFullDebugCompile() {

    var process = makeProcess.apply("-Cg");
    assertFullCompilation
        .andThen(assertDebug)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineFullDevCompile() {

    var process = makeProcess.apply("-Cd");
    assertFullCompilation
        .andThen(assertDebug)
        .andThen(assertDevBuild)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineFullDevCheckCompile() {

    var process = makeProcess.apply("-Cdh");
    assertFullCompilation
        .andThen(assertDebug)
        .andThen(assertDevBuild)
        .andThen(assertCheckCompile)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  void testCommandLineClean() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-Cl " + sourceName));
    assertTrue(underTest.isCleanAll());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLinePackageMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-P"));
  }

  @Test
  void testCommandLineResolveDependencies() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-Dp " + sourceName));
    assertTrue(underTest.isResolveDependencies());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLinePackage() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-P " + sourceName));
    assertTrue(underTest.isPackaging());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineInstallMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-I"));
  }

  @Test
  void testCommandLineInstall() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-I " + sourceName));
    assertTrue(underTest.isInstall());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineGenerateKeys() {
    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-Gk"));
    assertTrue(underTest.isGenerateSigningKeys());
  }

  @Test
  void testCommandLineDeployMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-D"));
  }

  @Test
  void testCommandLineDeploy() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-D " + sourceName));
    assertTrue(underTest.isDeployment());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineIncrementVersionMissingParamAndSourceFile() {
    assertEquals(2, processStringCommandLine.apply("-IV"));
  }

  @Test
  void testCommandLineIncrementVersionMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-IV major"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-IV", "-d", "-T wasm"})
  void testBadCommandLine(String commandOption) {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    assertEquals(2, processStringCommandLine.apply(commandOption + " " + sourceName));
  }

  @Test
  void testCommandLineInvalidSetVersionParam() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      String sourceName = "SinglePackage.ek9";
      //We will copy this into a working directory and process it.
      sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

      processStringCommandLine.apply("-SV 10.3.A " + sourceName);
    });
  }

  @Test
  void testCommandLineSetVersionParam() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-SV 10.3.1 " + sourceName));
    assertTrue(underTest.isSetReleaseVector());
    assertEquals("10.3.1", underTest.getOptionParameter("-SV"));
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineInvalidSetFeatureParam() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      String sourceName = "SinglePackage.ek9";
      //We will copy this into a working directory and process it.
      sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

      processStringCommandLine.apply("-SF 10.3.2-1bogus " + sourceName);
    });
  }

  @Test
  void testCommandLineSetFeatureVersionParam() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-SF 10.3.1-special " + sourceName));
    assertTrue(underTest.isSetFeatureVector());
    assertEquals("10.3.1-special", underTest.getOptionParameter("-SF"));
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLinePrintVersionMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-PV"));
  }

  @Test
  void testCommandLineTestMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-t"));
  }

  @Test
  void testCommandLineTest() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-t " + sourceName));
    assertTrue(underTest.isUnitTestExecution());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineDebugMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-d"));
  }

  @Test
  void testInvalidBuildCommandLineDebug() {
    assertEquals(2, processStringCommandLine.apply("-Cg -d 9000 HelloWorld.ek9"));
  }

  @Test
  void testInvalidManagementCommandLineDebug() {
    assertEquals(2, processStringCommandLine.apply("-Up -d 9000 HelloWorld.ek9"));
  }

  @Test
  void testInvalidReleaseVectorCommandLineDebug() {
    assertEquals(2, processStringCommandLine.apply("-PV -d 9000 HelloWorld.ek9"));
  }

  @Test
  void testInvalidRunCommandLineDebug() {
    assertEquals(2, processStringCommandLine.apply("-d 5600 -d 9000 HelloWorld.ek9"));
  }

  @Test
  void testInvalidUnitTestCommandLineDebug() {
    assertEquals(2, processStringCommandLine.apply("-t -d 9000 HelloWorld.ek9"));
  }

  @Test
  void testCommandLineDebug() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-d 9000 " + sourceName));
    assertTrue(underTest.isRunDebugMode());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineRun() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine(sourceName));
    assertTrue(underTest.isRunNormalMode());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineInvalidBuildRunProgram() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    assertEquals(4, processStringCommandLine.apply("-c " + sourceName + " -r SomeProgram"));
  }


  @Test
  void testUnspecifiedRunProgram() {
    String sourceName = "HelloWorlds.ek9";

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    assertEquals(6, processStringCommandLine.apply(sourceName));
  }

  @Test
  void testIncorrectRunProgram() {
    assertSimpleRunResults(" -r NonSuch", 4);
  }

  @Test
  void testRunHelloWorld() {
    assertSimpleRunResults(" -r HelloEarth", 0);
  }

  @Test
  void testRunHelloMars() {
    assertSimpleRunResults(" -r HelloMars", 0);
  }

  private void assertSimpleRunResults(final String runCommand, final int expectedResult) {
    final String sourceName = "HelloWorlds.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    assertEquals(expectedResult, processStringCommandLine.apply(sourceName + runCommand));
  }

  @ParameterizedTest
  @CsvSource({"SinglePackage.ek9,-c -C,4",
      "SinglePackage.ek9,-GK -C,4",
      "SinglePackage.ek9,-Gk,4",
      "SinglePackage.ek9,-C -t,4",
      "SinglePackage.ek9,-C -PV,4",
      "SinglePackage.ek9,--c -d 9000,4"})
  void testBadCommandLine(String sourceName, String command, int expectedErrorCode) {

    var process = copyFileToTestCWD
        .andThen(fileName -> command + " " + fileName)
        .andThen(processStringCommandLine);

    var actual = Optional.of(sourceName).map(process).orElse(-1);
    assertEquals(expectedErrorCode, actual);
  }

  @Test
  void testCommandLineInvalidReleaseRunProgram() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    assertEquals(4, processStringCommandLine.apply("-IV patch " + sourceName + " -r SomeProgram"));
  }

  @Test
  void testCommandLineRunProgram() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    assertEquals(5, processStringCommandLine.apply(sourceName + " -r SomeProgram"));
  }

  @Test
  void testCommandLineEnvironment() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0,
        underTest.processCommandLine("-e checker=false -e vogons='Bear Tree' " + sourceName));
    assertTrue(underTest.getEk9AppDefines().contains("checker=false"));
    //Not the move to double quotes
    assertTrue(underTest.getEk9AppDefines().contains("vogons=\"Bear Tree\""));
  }

  @Test
  void testCommandLineRunAsJavaTarget() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-T java " + sourceName));
    assertTrue(underTest.isRunNormalMode());
    assertEquals("HelloWorld", underTest.getProgramToRun());
    assertEquals("java", underTest.getTargetArchitecture());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testEnvironmentVariableRunAsJavaTarget() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
    assertNotNull(sourceFile);

    //Simulate picking up from environment variables.
    CommandLineDetails.addDefaultSetting();

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine(sourceName));
    assertTrue(underTest.isRunNormalMode());
    assertEquals("HelloWorld", underTest.getProgramToRun());
    assertEquals("java", underTest.getTargetArchitecture());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLinePrintVersion() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-PV " + sourceName));
    assertTrue(underTest.isPrintReleaseVector());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineIncrementVersion() {
    assertIVParam("major");
    assertIVParam("minor");
    assertIVParam("patch");
    assertIVParam("build");
  }

  private void assertIVParam(String param) {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

    CommandLineDetails underTest = createClassUnderTest();
    assertEquals(0, underTest.processCommandLine("-IV " + param + " " + sourceName));
    assertTrue(underTest.isIncrementReleaseVector());
    assertEquals(param, underTest.getOptionParameter("-IV"));
    assertEquals(sourceName, underTest.getSourceFileName());
  }

}
