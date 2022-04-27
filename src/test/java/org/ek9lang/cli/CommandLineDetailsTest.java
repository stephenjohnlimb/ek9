package org.ek9lang.cli;

import org.ek9lang.LanguageMetaData;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Some tests to check the command line processing.
 * 
 * Always difficult and error-prone to handle a range of different command line options.
 * 
 * So if there are issues, always add another test.
 */
public class CommandLineDetailsTest
{
	private final LanguageMetaData languageMetaData = new LanguageMetaData("0.0.1-0");
	private final OsSupport osSupport = new OsSupport(true);
	private final FileHandling fileHandling = new FileHandling(osSupport);
	private final SourceFileSupport sourceFileSupport = new SourceFileSupport(fileHandling, osSupport);
	
	@AfterEach
	public void tidyUp()
	{
		String testHomeDirectory = fileHandling.getUsersHomeDirectory();
		assertNotNull(testHomeDirectory);
		//As this is a test delete from process id and below
		fileHandling.deleteContentsAndBelow(new File(new File(testHomeDirectory).getParent()), true);
	}

	private CommandLineDetails createClassUnderTest()
	{
		return new CommandLineDetails(languageMetaData, fileHandling, osSupport);
	}
	@Test
	public void testCommandLineHelpText()
	{
		assertNotNull(CommandLineDetails.getCommandLineHelp());
	}
	
	@Test
	public void testEmptyCommandLine()
	{
		CommandLineDetails underTest = createClassUnderTest();
		assertEquals(2, underTest.processCommandLine((String)null));
		assertEquals(2, underTest.processCommandLine(""));

		assertEquals(2, underTest.processCommandLine((String[])null));
		String[] argv = {};
		assertEquals(2, underTest.processCommandLine(argv));
	}
	
	@Test
	public void testCommandLineVersion()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(1, underTest.processCommandLine("-V"));
	}

	@Test
	public void testDefaultPackageSettings()
	{
		String sourceName = "TCPExample.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/fullPrograms/", sourceName);

		//As part of the package mechanism we have defaults when not specified.
		CommandLineDetails underTest = createClassUnderTest();
		assertEquals(0, underTest.processCommandLine("-c " + sourceName));

		assertFalse(underTest.applyStandardIncludes());
		assertTrue(underTest.applyStandardExcludes());

		assertEquals(1, underTest.getIncludeFiles().size());
		assertEquals(1, underTest.getExcludeFiles().size());
	}

	@Test
	public void testLanguageServerCommandLine()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-ls"));
		assertTrue(underTest.isRunEK9AsLanguageServer());		
	}
	
	@Test
	public void testLanguageServerWithHoverHelpCommandLine()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-ls -lsh"));
		assertTrue(underTest.isRunEK9AsLanguageServer());
		assertTrue(underTest.isEK9LanguageServerHelpEnabled());
	}
	
	@Test
	public void testCommandLineHelp()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(1, underTest.processCommandLine("-h"));
	}
	
	@Test
	public void testCommandLineInvalidIncrementalCompile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-c"));
	}

	@Test
	public void testSimulationOfSourceFileAccess()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();
		//Now we can put a dummy source file in the simulated/test cwd and then try and process it.
		File cwd = new File(osSupport.getCurrentWorkingDirectory());

		assertEquals(0, underTest.processCommandLine("-c " + sourceName));

		//Now a few directories and files now should exist.
		File propsFile = new File(fileHandling.getDotEK9Directory(cwd), "SinglePackage.properties");
		assertTrue(propsFile.exists());

		//Now we can test the reloading. Without forced reloading
		assertNull(underTest.processEK9FileProperties(false));
		//Now with forced reloading
		assertNotNull(underTest.processEK9FileProperties(true));
		//TODO
	}

	@Test
	public void testHandlingEK9Package()
	{
		String sourceName = "TCPExample.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/fullPrograms/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();
		//Now we can put a dummy source file in the simulated/test cwd and then try and process it.

		assertEquals(0, underTest.processCommandLine("-c " + sourceName));

		assertEquals("2.3.14-20", underTest.getVersion());
		assertEquals("example.networking", underTest.getModuleName());
		assertEquals(4, underTest.numberOfProgramsInSourceFile());
	}

	@Test
	public void testCommandLineIncrementalCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-c " + sourceName));		
		assertTrue(underTest.isIncrementalCompile());
		assertFalse(underTest.isFullCompile());
		assertFalse(underTest.isVerbose());
		assertFalse(underTest.isDebuggingInstrumentation());
		assertFalse(underTest.isDevBuild());
		assertEquals(sourceName,underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineIncrementalDebugCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		CommandLineDetails underTest = createClassUnderTest();
		assertEquals(0, underTest.processCommandLine("-cg " + sourceName));
		assertTrue(underTest.isIncrementalCompile());
		assertFalse(underTest.isFullCompile());
		assertFalse(underTest.isVerbose());
		assertTrue(underTest.isDebuggingInstrumentation());
		assertFalse(underTest.isDevBuild());
		assertEquals(sourceName,underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineIncrementalDevCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-cd " + sourceName));
		assertTrue(underTest.isIncrementalCompile());
		assertFalse(underTest.isFullCompile());
		assertFalse(underTest.isVerbose());
		assertTrue(underTest.isDebuggingInstrumentation());
		assertTrue(underTest.isDevBuild());
		assertEquals(sourceName,underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-C " + sourceName));
		assertFalse(underTest.isIncrementalCompile());
		assertTrue(underTest.isFullCompile());
		assertFalse(underTest.isVerbose());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullVerboseCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-C -v " + sourceName));		
		assertFalse(underTest.isIncrementalCompile());
		assertTrue(underTest.isFullCompile());
		assertTrue(underTest.isVerbose());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullDebugCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-Cg " + sourceName));		
		assertFalse(underTest.isIncrementalCompile());
		assertTrue(underTest.isFullCompile());
		assertFalse(underTest.isVerbose());
		assertTrue(underTest.isDebuggingInstrumentation());
		assertFalse(underTest.isDevBuild());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullDevCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-Cd " + sourceName));		
		assertFalse(underTest.isIncrementalCompile());
		assertTrue(underTest.isFullCompile());
		assertFalse(underTest.isVerbose());
		assertTrue(underTest.isDebuggingInstrumentation());
		assertTrue(underTest.isDevBuild());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineClean()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-Cl " + sourceName));		
		assertTrue(underTest.isCleanAll());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLinePackageMissingSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-P"));				
	}
	
	@Test
	public void testCommandLineResolveDependencies()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-Dp " + sourceName));		
		assertTrue(underTest.isResolveDependencies());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLinePackage()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-P " + sourceName));		
		assertTrue(underTest.isPackaging());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineInstallMissingSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-I"));		
	}
	
	@Test
	public void testCommandLineInstall()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-I " + sourceName));		
		assertTrue(underTest.isInstall());
		assertEquals(sourceName, underTest.getSourceFileName());
	}

	@Test
	public void testCommandLineGenerateKeys()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-Gk"));		
		assertTrue(underTest.isGenerateSigningKeys());
		
	}
	
	@Test
	public void testCommandLineGenerateKeysAdditionalSourceFile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-Gk " + sourceName));
	}

	@Test
	public void testCommandLineDeployMissingSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-D"));		
	}
	
	@Test
	public void testCommandLineDeploy()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-D " + sourceName));		
		assertTrue(underTest.isDeployment());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineIncrementVersionMissingParamAndSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(2, underTest.processCommandLine("-IV"));		
	}

	@Test
	public void testCommandLineIncrementVersionMissingSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-IV major"));		
	}
	
	@Test
	public void testCommandLineIncrementVersionMissingParam()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(2, underTest.processCommandLine("-IV " + sourceName));		
	}

	@Test
	public void testCommandLineInvalidSetVersionParam()
	{
		assertThrows(java.lang.RuntimeException.class, () -> {
			String sourceName = "SinglePackage.ek9";
			//We will copy this into a working directory and process it.
			sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

			CommandLineDetails underTest = createClassUnderTest();
			underTest.processCommandLine("-SV 10.3.A " + sourceName);
		});
	}
	
	@Test
	public void testCommandLineSetVersionParam()
	{
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
	public void testCommandLineInvalidSetFeatureParam()
	{
		assertThrows(java.lang.RuntimeException.class, () -> {
			String sourceName = "SinglePackage.ek9";
			//We will copy this into a working directory and process it.
			sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

			CommandLineDetails underTest = createClassUnderTest();
			underTest.processCommandLine("-SF 10.3.2-1bogus " + sourceName);
		});
	}
	
	@Test
	public void testCommandLineSetFeatureVersionParam()
	{
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
	public void testCommandLinePrintVersionMissingSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-PV"));	
	}
	
	@Test
	public void testCommandLineTestMissingSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-t"));	
	}
	
	@Test
	public void testCommandLineTest()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-t " + sourceName));
		assertTrue(underTest.isUnitTestExecution());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineDebugMissingSourceFile()
	{
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(3, underTest.processCommandLine("-d"));	
	}
	
	@Test
	public void testCommandLineDebugMissingPort()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(2, underTest.processCommandLine("-d " + sourceName));		
	}
	
	@Test
	public void testCommandLineDebug()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-d 9000 " + sourceName));
		assertTrue(underTest.isRunDebugMode());
		assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineRun()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine(sourceName));
		assertTrue(underTest.isRunNormalMode());
		assertEquals(sourceName, underTest.getSourceFileName());
	}

	@Test
	public void testCommandLineInvalidBuildRunProgram()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-c " + sourceName + " -r SomeProgram"));
	}


	@Test
	public void testUnspecifiedRunProgram()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();
		assertEquals(6, underTest.processCommandLine(sourceName));
	}

	@Test
	public void testIncorrectRunProgram()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();
		assertEquals(4, underTest.processCommandLine(sourceName + " -r NonSuch"));
	}

	@Test
	public void testRunHelloWorld()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();
		assertEquals(0, underTest.processCommandLine(sourceName + " -r HelloWorld"));
	}

	@Test
	public void testRunHelloMars()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();
		assertEquals(0, underTest.processCommandLine(sourceName + " -r HelloMars"));
	}

	@Test
	public void testCommandLineConflictingBuild1()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-c -C " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild2()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-GK -C " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild3()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-C -t " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild4()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-C -PV " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild5()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-c -d 9000 " + sourceName));
	}
	
	@Test
	public void testCommandLineInvalidReleaseRunProgram()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(4, underTest.processCommandLine("-IV patch " + sourceName + " -r SomeProgram"));
	}
	
	@Test
	public void testCommandLineRunProgram()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(5, underTest.processCommandLine(sourceName + " -r SomeProgram"));
	}
	
	@Test
	public void testCommandLineRunAsInvalidTarget()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(2, underTest.processCommandLine("-T wasm " + sourceName));
	}
	
	@Test
	public void testCommandLineEnvironment()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		assertNotNull(sourceFile);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-e checker=false -e vogons='Bear Tree' " + sourceName));
		assertTrue(underTest.getEk9AppDefines().contains("checker=false"));
		//Not the move to double quotes
		assertTrue(underTest.getEk9AppDefines().contains("vogons=\"Bear Tree\""));
	}
	
	@Test
	public void testCommandLineRunAsJavaTarget()
	{
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
	public void testCommandLinePrintVersion()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-PV " + sourceName));
		assertTrue(underTest.isPrintReleaseVector());		
		assertEquals(sourceName,underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineIncrementVersion()
	{
		assertIVParam("major");
		assertIVParam("minor");
		assertIVParam("patch");
		assertIVParam("build");
	}
	
	private void assertIVParam(String param)
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		
		CommandLineDetails underTest = createClassUnderTest();				
		assertEquals(0, underTest.processCommandLine("-IV " + param + " " + sourceName));
		assertTrue(underTest.isIncrementReleaseVector());
		assertEquals(param, underTest.getOptionParameter("-IV"));
		assertEquals(sourceName,underTest.getSourceFileName());
	}
	
}
