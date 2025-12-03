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
import org.ek9lang.compiler.OptimizationLevel;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;
import org.ek9lang.core.TargetArchitecture;
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
final class CommandLineTest {
  private final LanguageMetaData languageMetaData = new LanguageMetaData("0.0.1-0");
  private final OsSupport osSupport = new OsSupport(true);
  private final FileHandling fileHandling = new FileHandling(osSupport);
  private final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);

  private final Function<String, String> copyFileToTestCWD = sourceName -> {
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);
    return sourceName;
  };

  private final Function<String, Integer> processStringCommandLine =
      commandLine -> createClassUnderTest().process(commandLine);

  private final Function<String[], Integer> processStringArrayCommandLine =
      commandLine -> createClassUnderTest().process(commandLine);

  private final Function<String, CommandLine> processStringCommandLineExpectSuccess =
      commandLine -> {
        var commandLineDetails = createClassUnderTest();
        assertEquals(0, commandLineDetails.process(commandLine));
        return commandLineDetails;
      };

  private final Function<String, Function<String, CommandLine>> makeProcess = command ->
      copyFileToTestCWD
          .andThen(fileName -> command + " " + fileName)
          .andThen(processStringCommandLineExpectSuccess);

  private final Consumer<CommandLine> assertIncrementalCompilation =
      commandLineDetails -> assertTrue(commandLineDetails.options().isIncrementalCompile());

  private final Consumer<CommandLine> assertDevBuild =
      commandLineDetails -> assertTrue(commandLineDetails.options().isDevBuild());

  private final Consumer<CommandLine> assertFullCompilation =
      commandLineDetails -> assertTrue(commandLineDetails.options().isFullCompile());

  private final Consumer<CommandLine> assertDebug =
      commandLineDetails -> assertTrue(commandLineDetails.options().isDebuggingInstrumentation());

  private final Consumer<CommandLine> assertVerbose =
      commandLineDetails -> assertTrue(commandLineDetails.options().isVerbose());

  private final Consumer<CommandLine> assertDebugVerbose =
      commandLineDetails -> assertTrue(commandLineDetails.options().isDebugVerbose());

  private final Consumer<CommandLine> assertErrorVerbose =
      commandLineDetails -> assertTrue(commandLineDetails.options().isErrorVerbose());

  private final Consumer<CommandLine> assertCheckCompile =
      commandLineDetails -> assertTrue(commandLineDetails.options().isCheckCompileOnly());

  private final Consumer<CommandLine> assertOptimizationO0 =
      commandLineDetails -> assertEquals(OptimizationLevel.O0,
          commandLineDetails.getOptimizationLevel());

  private final Consumer<CommandLine> assertOptimizationO2 =
      commandLineDetails -> assertEquals(OptimizationLevel.O2,
          commandLineDetails.getOptimizationLevel());

  private final Consumer<CommandLine> assertOptimizationO3 =
      commandLineDetails -> assertEquals(OptimizationLevel.O3,
          commandLineDetails.getOptimizationLevel());

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

  private CommandLine createClassUnderTest() {
    return new CommandLine(languageMetaData, fileHandling, osSupport);
  }

  @Test
  void testCommandLineHelpText() {
    assertNotNull(CommandLine.getCommandLineHelp());
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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/fullPrograms/networking/", sourceName);

    //As part of the package mechanism we have defaults when not specified.
    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-c " + sourceName));

    assertFalse(underTest.applyStandardIncludes());
    assertTrue(underTest.applyStandardExcludes());

    assertEquals(1, underTest.getIncludeFiles().size());
    assertEquals(1, underTest.getExcludeFiles().size());
  }

  private void runCommandLineExpecting(final String commandToTest,
                                       final Consumer<CommandLine> assertion) {
    Consumer<String> toTest =
        item -> assertion.accept(processStringCommandLineExpectSuccess.apply(item));
    toTest.accept(commandToTest);
  }

  @Test
  void testLanguageServerCommandLine() {
    runCommandLineExpecting("-ls",
        commandLineDetails -> assertTrue(commandLineDetails.options().isRunEk9AsLanguageServer()));
  }

  @Test
  void testLanguageServerWithHoverHelpCommandLine() {
    Consumer<CommandLine> assertion1 =
        commandLineDetails -> assertTrue(commandLineDetails.options().isRunEk9AsLanguageServer());
    Consumer<CommandLine> assertion2 =
        commandLineDetails -> assertTrue(commandLineDetails.options().isEk9LanguageServerHelpEnabled());

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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    //Now we can put a dummy source file in the simulated/test cwd and then try and process it.
    File cwd = new File(osSupport.getCurrentWorkingDirectory());

    assertEquals(0, underTest.process("-c " + sourceName));

    //Now a few directories and files now should exist.
    File propsFile = new File(fileHandling.getDotEk9Directory(cwd), "SinglePackage.properties");
    assertTrue(propsFile.exists());

    //Now we can test the reloading. Without forced reloading
    assertNull(underTest.processEk9FileProperties(false));
    //Now with forced reloading
    assertNotNull(underTest.processEk9FileProperties(true));
  }

  @Test
  void testHandlingEK9Package() {
    String sourceName = "TCPExample.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/fullPrograms/networking/", sourceName);

    CommandLine underTest = createClassUnderTest();
    //Now we can put a dummy source file in the simulated/test cwd and then try and process it.

    assertEquals(0, underTest.process("-c " + sourceName));

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
  void testCommandLineFullDebugVerboseCompile() {
    var process = makeProcess.apply("-C -dv");
    assertFullCompilation
        .andThen(assertDebugVerbose)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineFullErrorVerboseCompile() {
    var process = makeProcess.apply("-C -ve");
    assertFullCompilation
        .andThen(assertErrorVerbose)
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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-Cl " + sourceName));
    assertTrue(underTest.options().isCleanAll());
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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-Dp " + sourceName));
    assertTrue(underTest.options().isResolveDependencies());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLinePackage() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-P " + sourceName));
    assertTrue(underTest.options().isPackaging());
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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-I " + sourceName));
    assertTrue(underTest.options().isInstall());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineGenerateKeys() {
    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-Gk"));
    assertTrue(underTest.options().isGenerateSigningKeys());
  }

  @Test
  void testCommandLineDeployMissingSourceFile() {
    assertEquals(3, processStringCommandLine.apply("-D"));
  }

  @Test
  void testCommandLineDeploy() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-D " + sourceName));
    assertTrue(underTest.options().isDeployment());
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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    assertEquals(2, processStringCommandLine.apply(commandOption + " " + sourceName));
  }

  @Test
  void testCommandLineInvalidSetVersionParam() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      String sourceName = "SinglePackage.ek9";
      //We will copy this into a working directory and process it.
      sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

      processStringCommandLine.apply("-SV 10.3.A " + sourceName);
    });
  }

  @Test
  void testCommandLineSetVersionParam() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-SV 10.3.1 " + sourceName));
    assertTrue(underTest.options().isSetReleaseVector());
    assertEquals("10.3.1", underTest.options().getOptionParameter("-SV"));
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineInvalidSetFeatureParam() {
    assertThrows(java.lang.RuntimeException.class, () -> {
      String sourceName = "SinglePackage.ek9";
      //We will copy this into a working directory and process it.
      sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

      processStringCommandLine.apply("-SF 10.3.2-1bogus " + sourceName);
    });
  }

  @Test
  void testCommandLineSetFeatureVersionParam() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-SF 10.3.1-special " + sourceName));
    assertTrue(underTest.options().isSetFeatureVector());
    assertEquals("10.3.1-special", underTest.options().getOptionParameter("-SF"));
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
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-t " + sourceName));
    assertTrue(underTest.options().isUnitTestExecution());
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
  @SuppressWarnings("java:S2699")
  void testCommandLineOptimizationO0() {
    var process = makeProcess.apply("-C -O0");
    assertFullCompilation
        .andThen(assertOptimizationO0)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineOptimizationO2() {
    var process = makeProcess.apply("-C -O2");
    assertFullCompilation
        .andThen(assertOptimizationO2)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineOptimizationO3() {
    var process = makeProcess.apply("-C -O3");
    assertFullCompilation
        .andThen(assertOptimizationO3)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testCommandLineDefaultOptimizationO2() {
    var process = makeProcess.apply("-C");
    assertFullCompilation
        .andThen(assertOptimizationO2)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testIncrementalCompileOptimizationO3() {
    var process = makeProcess.apply("-c -O3");
    assertIncrementalCompilation
        .andThen(assertOptimizationO3)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testDebugCompileOptimizationO0() {
    var process = makeProcess.apply("-cg -O0");
    assertIncrementalCompilation
        .andThen(assertDebug)
        .andThen(assertOptimizationO0)
        .accept(Optional.of("SinglePackage.ek9").map(process).orElseThrow());
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
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-d 9000 " + sourceName));
    assertTrue(underTest.options().isRunDebugMode());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineRun() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process(sourceName));
    assertTrue(underTest.options().isRunNormalMode());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLineInvalidBuildRunProgram() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    assertEquals(4, processStringCommandLine.apply("-c " + sourceName + " -r SomeProgram"));
  }


  @Test
  void testUnspecifiedRunProgram() {
    String sourceName = "HelloWorlds.ek9";

    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
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
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    assertEquals(4, processStringCommandLine.apply("-IV patch " + sourceName + " -r SomeProgram"));
  }

  @Test
  void testCommandLineRunProgram() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    assertEquals(5, processStringCommandLine.apply(sourceName + " -r SomeProgram"));
  }

  @Test
  void testCommandLineEnvironment() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0,
        underTest.process("-e checker=false -e vogons='Bear Tree' " + sourceName));
    assertTrue(underTest.getEk9AppDefines().contains("checker=false"));
    //Not the move to double quotes
    assertTrue(underTest.getEk9AppDefines().contains("vogons=\"Bear Tree\""));
  }

  @Test
  void testCommandLineRunAsJavaTarget() {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-T jvm " + sourceName));
    assertTrue(underTest.options().isRunNormalMode());
    assertEquals("HelloWorld", underTest.getProgramToRun());
    assertEquals(TargetArchitecture.JVM, underTest.getTargetArchitecture());
    assertEquals(sourceName, underTest.getSourceFileName());
  }


  @ParameterizedTest
  @CsvSource({"-T llvm-cpp,LLVM_CPP", "-T jvm,JVM"})
  void testCommandLineRunAs(final String targetArchitectureOption, final String expectedArchitecture) {
    String sourceName = "HelloWorld.ek9";
    File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/basics/", sourceName);
    assertNotNull(sourceFile);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process(targetArchitectureOption + " " + sourceName));
    assertTrue(underTest.options().isRunNormalMode());
    assertEquals("HelloWorld", underTest.getProgramToRun());
    assertEquals(TargetArchitecture.valueOf(expectedArchitecture), underTest.getTargetArchitecture());
    assertEquals(sourceName, underTest.getSourceFileName());
  }

  @Test
  void testCommandLinePrintVersion() {
    String sourceName = "SinglePackage.ek9";
    //We will copy this into a working directory and process it.
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-PV " + sourceName));
    assertTrue(underTest.options().isPrintReleaseVector());
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
    sourceFileSupport.copyFileToTestCWD("/examples/parseAndCompile/constructs/packages/", sourceName);

    CommandLine underTest = createClassUnderTest();
    assertEquals(0, underTest.process("-IV " + param + " " + sourceName));
    assertTrue(underTest.options().isIncrementReleaseVector());
    assertEquals(param, underTest.options().getOptionParameter("-IV"));
    assertEquals(sourceName, underTest.getSourceFileName());
  }

}
