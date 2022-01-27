package org.ek9lang.cli;

import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.junit.After;
import org.junit.Test;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Some tests to check the command line processing.
 * 
 * Always difficult and error-prone to handle a range of different command line options.
 * 
 * So if there are issues, always add another test.
 */
public class CommandLineDetailsTest
{
	private final OsSupport osSupport = new OsSupport(true);
	private final FileHandling fileHandling = new FileHandling(osSupport);
	private final SourceFileSupport sourceFileSupport = new SourceFileSupport(fileHandling, osSupport);
	
	@After
	public void tidyUp()
	{
		String testHomeDirectory = fileHandling.getUsersHomeDirectory();
		TestCase.assertNotNull(testHomeDirectory);
		//As this is a test delete from process id and below
		fileHandling.deleteContentsAndBelow(new File(new File(testHomeDirectory).getParent()), true);
	}

	@Test
	public void testCommandLineHelpText()
	{
		TestCase.assertNotNull(CommandLineDetails.getCommandLineHelp());
	}
	
	@Test
	public void testEmptyCommandLine()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		TestCase.assertEquals(2, underTest.processCommandLine((String)null));
		TestCase.assertEquals(2, underTest.processCommandLine(""));

		TestCase.assertEquals(2, underTest.processCommandLine((String[])null));
		String[] argv = {};
		TestCase.assertEquals(2, underTest.processCommandLine(argv));
	}
	
	@Test
	public void testCommandLineVersion()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(1, underTest.processCommandLine("-V"));
	}

	@Test
	public void testDefaultPackageSettings()
	{
		String sourceName = "TCPExample.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/fullPrograms/", sourceName);

		//As part of the package mechanism we have defaults when not specified.
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		TestCase.assertEquals(0, underTest.processCommandLine("-c " + sourceName));

		TestCase.assertFalse(underTest.applyStandardIncludes());
		TestCase.assertTrue(underTest.applyStandardExcludes());

		TestCase.assertEquals(1, underTest.getIncludeFiles().size());
		TestCase.assertEquals(1, underTest.getExcludeFiles().size());
	}

	@Test
	public void testLanguageServerCommandLine()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-ls"));
		TestCase.assertTrue(underTest.isRunEK9AsLanguageServer());		
	}
	
	@Test
	public void testLanguageServerWithHoverHelpCommandLine()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-ls -lsh"));
		TestCase.assertTrue(underTest.isRunEK9AsLanguageServer());
		TestCase.assertTrue(underTest.isEK9LanguageServerHelpEnabled());
	}
	
	@Test
	public void testCommandLineHelp()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(1, underTest.processCommandLine("-h"));
	}
	
	@Test
	public void testCommandLineInvalidIncrementalCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-c"));
	}

	@Test
	public void testSimulationOfSourceFileAccess() throws IOException
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		//Now we can put a dummy source file in the simulated/test cwd and then try and process it.
		File cwd = new File(osSupport.getCurrentWorkingDirectory());

		TestCase.assertEquals(0, underTest.processCommandLine("-c " + sourceName));

		//Now a few directories and files now should exist.
		File propsFile = new File(fileHandling.getDotEK9Directory(cwd), "SinglePackage.properties");
		TestCase.assertTrue(propsFile.exists());

		//Now we can test the reloading. Without forced reloading
		TestCase.assertNull(underTest.processEK9FileProperties(false));
		//Now with forced reloading
		TestCase.assertNotNull(underTest.processEK9FileProperties(true));
		//TODO
	}

	@Test
	public void testHandlingEK9Package() throws IOException
	{
		String sourceName = "TCPExample.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/fullPrograms/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		//Now we can put a dummy source file in the simulated/test cwd and then try and process it.

		TestCase.assertEquals(0, underTest.processCommandLine("-c " + sourceName));

		TestCase.assertEquals("2.3.14-20", underTest.getVersion());
		TestCase.assertEquals("example.networking", underTest.getModuleName());
		TestCase.assertEquals(4, underTest.numberOfProgramsInSourceFile());
	}

	@Test
	public void testCommandLineIncrementalCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-c " + sourceName));		
		TestCase.assertTrue(underTest.isIncrementalCompile());
		TestCase.assertFalse(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertFalse(underTest.isDebuggingInstrumentation());
		TestCase.assertFalse(underTest.isDevBuild());
		TestCase.assertEquals(sourceName,underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineIncrementalDebugCompile() throws IOException
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		TestCase.assertEquals(0, underTest.processCommandLine("-cg " + sourceName));
		TestCase.assertTrue(underTest.isIncrementalCompile());
		TestCase.assertFalse(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertFalse(underTest.isDevBuild());
		TestCase.assertEquals(sourceName,underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineIncrementalDevCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-cd " + sourceName));
		TestCase.assertTrue(underTest.isIncrementalCompile());
		TestCase.assertFalse(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertTrue(underTest.isDevBuild());
		TestCase.assertEquals(sourceName,underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-C " + sourceName));
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullVerboseCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-C -v " + sourceName));		
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertTrue(underTest.isVerbose());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullDebugCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-Cg " + sourceName));		
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertFalse(underTest.isDevBuild());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineFullDevCompile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-Cd " + sourceName));		
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertTrue(underTest.isDevBuild());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineClean()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-Cl " + sourceName));		
		TestCase.assertTrue(underTest.isCleanAll());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLinePackageMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-P"));				
	}
	
	@Test
	public void testCommandLineResolveDependencies()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-Dp " + sourceName));		
		TestCase.assertTrue(underTest.isResolveDependencies());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLinePackage()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-P " + sourceName));		
		TestCase.assertTrue(underTest.isPackaging());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineInstallMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-I"));		
	}
	
	@Test
	public void testCommandLineInstall()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-I " + sourceName));		
		TestCase.assertTrue(underTest.isInstall());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}

	@Test
	public void testCommandLineGenerateKeys()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-Gk"));		
		TestCase.assertTrue(underTest.isGenerateSigningKeys());
		
	}
	
	@Test
	public void testCommandLineGenerateKeysAdditionalSourceFile()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-Gk " + sourceName));
	}

	@Test
	public void testCommandLineDeployMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-D"));		
	}
	
	@Test
	public void testCommandLineDeploy()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-D " + sourceName));		
		TestCase.assertTrue(underTest.isDeployment());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineIncrementVersionMissingParamAndSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(2, underTest.processCommandLine("-IV"));		
	}

	@Test
	public void testCommandLineIncrementVersionMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-IV major"));		
	}
	
	@Test
	public void testCommandLineIncrementVersionMissingParam()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(2, underTest.processCommandLine("-IV " + sourceName));		
	}

	@Test(expected = RuntimeException.class)
	public void testCommandLineInvalidSetVersionParam()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		underTest.processCommandLine("-SV 10.3.A " + sourceName);
	}
	
	@Test
	public void testCommandLineSetVersionParam()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-SV 10.3.1 " + sourceName));
		TestCase.assertTrue(underTest.isSetReleaseVector());
		TestCase.assertEquals("10.3.1", underTest.getOptionParameter("-SV"));
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test(expected = RuntimeException.class)
	public void testCommandLineInvalidSetFeatureParam()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		underTest.processCommandLine("-SF 10.3.2-1bogus " + sourceName);
	}
	
	@Test
	public void testCommandLineSetFeatureVersionParam()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-SF 10.3.1-special " + sourceName));
		TestCase.assertTrue(underTest.isSetFeatureVector());
		TestCase.assertEquals("10.3.1-special", underTest.getOptionParameter("-SF"));
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLinePrintVersionMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-PV"));	
	}
	
	@Test
	public void testCommandLineTestMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-t"));	
	}
	
	@Test
	public void testCommandLineTest()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-t " + sourceName));
		TestCase.assertTrue(underTest.isUnitTestExecution());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineDebugMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(3, underTest.processCommandLine("-d"));	
	}
	
	@Test
	public void testCommandLineDebugMissingPort()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(2, underTest.processCommandLine("-d " + sourceName));		
	}
	
	@Test
	public void testCommandLineDebug()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-d 9000 " + sourceName));
		TestCase.assertTrue(underTest.isRunDebugMode());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}
	
	@Test
	public void testCommandLineRun()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine(sourceName));
		TestCase.assertTrue(underTest.isRunNormalMode());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}

	@Test
	public void testCommandLineInvalidBuildRunProgram()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-c " + sourceName + " -r SomeProgram"));
	}


	@Test
	public void testUnspecifiedRunProgram()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		TestCase.assertEquals(6, underTest.processCommandLine(sourceName));
	}

	@Test
	public void testIncorrectRunProgram()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		TestCase.assertEquals(4, underTest.processCommandLine(sourceName + " -r NonSuch"));
	}

	@Test
	public void testRunHelloWorld()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		TestCase.assertEquals(0, underTest.processCommandLine(sourceName + " -r HelloWorld"));
	}

	@Test
	public void testRunHelloMars()
	{
		String sourceName = "HelloWorlds.ek9";

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);
		TestCase.assertEquals(0, underTest.processCommandLine(sourceName + " -r HelloMars"));
	}

	@Test
	public void testCommandLineConflictingBuild1()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-c -C " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild2()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-GK -C " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild3()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-C -t " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild4()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-C -PV " + sourceName));
	}
	
	@Test
	public void testCommandLineConflictingBuild5()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-c -d 9000 " + sourceName));
	}
	
	@Test
	public void testCommandLineInvalidReleaseRunProgram()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(4, underTest.processCommandLine("-IV patch " + sourceName + " -r SomeProgram"));
	}
	
	@Test
	public void testCommandLineRunProgram()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(5, underTest.processCommandLine(sourceName + " -r SomeProgram"));
	}
	
	@Test
	public void testCommandLineRunAsInvalidTarget()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(2, underTest.processCommandLine("-T wasm " + sourceName));
	}
	
	@Test
	public void testCommandLineEnvironment()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-e checker=false -e vogons='Bear Tree' " + sourceName));
		TestCase.assertTrue(underTest.getEk9AppDefines().contains("checker=false"));
		//Not the move to double quotes
		TestCase.assertTrue(underTest.getEk9AppDefines().contains("vogons=\"Bear Tree\""));
	}
	
	@Test
	public void testCommandLineRunAsJavaTarget()
	{
		String sourceName = "HelloWorld.ek9";
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-T java " + sourceName));
		TestCase.assertTrue(underTest.isRunNormalMode());
		TestCase.assertEquals("HelloWorld", underTest.getProgramToRun());
		TestCase.assertEquals("java", underTest.getTargetArchitecture());
		TestCase.assertEquals(sourceName, underTest.getSourceFileName());
	}

	@Test
	public void testCommandLinePrintVersion()
	{
		String sourceName = "SinglePackage.ek9";
		//We will copy this into a working directory and process it.
		sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);

		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-PV " + sourceName));
		TestCase.assertTrue(underTest.isPrintReleaseVector());		
		TestCase.assertEquals(sourceName,underTest.getSourceFileName());
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
		
		CommandLineDetails underTest = new CommandLineDetails(fileHandling, osSupport);				
		TestCase.assertEquals(0, underTest.processCommandLine("-IV " + param + " " + sourceName));
		TestCase.assertTrue(underTest.isIncrementReleaseVector());
		TestCase.assertEquals(param, underTest.getOptionParameter("-IV"));
		TestCase.assertEquals(sourceName,underTest.getSourceFileName());
	}
	
}
