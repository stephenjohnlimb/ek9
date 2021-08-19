package org.ek9lang.cli;

import org.junit.Test;

import junit.framework.TestCase;

/**
 * Some tests to check the command line processing.
 * 
 * Always difficult and error prone to handle a range of different command line options.
 * 
 * So if there are issues, always add another test.
 */
public class CommandLineDetailsTest
{

	@Test
	public void testCommandLineHelpText()
	{
		TestCase.assertNotNull(CommandLineDetails.getCommandLineHelp());
	}
	
	@Test
	public void testEmptyCommandLine()
	{
		CommandLineDetails underTest = new CommandLineDetails();
		TestCase.assertEquals(2, underTest.processCommandLine(null));
		TestCase.assertEquals(2, underTest.processCommandLine(""));		
	}
	
	@Test
	public void testCommandLineVersion()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-V"));
	}
	
	@Test
	public void testLanguageServerCommandLine()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-ls"));
		TestCase.assertTrue(underTest.isRunEK9AsLanguageServer());		
	}
	
	@Test
	public void testLanguageServerWithHoverHelpCommandLine()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-ls -lsh"));
		TestCase.assertTrue(underTest.isRunEK9AsLanguageServer());
		TestCase.assertTrue(underTest.isEK9LanguageServerHelpEnabled());
	}
	
	@Test
	public void testCommandLineHelp()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-h"));
	}
	
	@Test
	public void testCommandLineInvalidIncrementalCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-c"));
	}
	
	@Test
	public void testCommandLineIncrementalCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-c someFile.ek9"));		
		TestCase.assertTrue(underTest.isIncrementalCompile());
		TestCase.assertFalse(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertFalse(underTest.isDebuggingInstrumentation());
		TestCase.assertFalse(underTest.isDevBuild());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineIncrementalDebugCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-cg someFile.ek9"));		
		TestCase.assertTrue(underTest.isIncrementalCompile());
		TestCase.assertFalse(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertFalse(underTest.isDevBuild());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineIncrementalDevCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-cd someFile.ek9"));		
		TestCase.assertTrue(underTest.isIncrementalCompile());
		TestCase.assertFalse(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertTrue(underTest.isDevBuild());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineFullCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-C someFile.ek9"));		
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineFullVerboseCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-C -v someFile.ek9"));		
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertTrue(underTest.isVerbose());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineFullDebugCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-Cg someFile.ek9"));		
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertFalse(underTest.isDevBuild());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineFullDevCompile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-Cd someFile.ek9"));		
		TestCase.assertFalse(underTest.isIncrementalCompile());
		TestCase.assertTrue(underTest.isFullCompile());
		TestCase.assertFalse(underTest.isVerbose());
		TestCase.assertTrue(underTest.isDebuggingInstrumentation());
		TestCase.assertTrue(underTest.isDevBuild());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineClean()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-Cl someFile.ek9"));		
		TestCase.assertTrue(underTest.isCleanAll());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLinePackageMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-P"));				
	}
	
	@Test
	public void testCommandLineResolveDependencies()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-Dp someFile.ek9"));		
		TestCase.assertTrue(underTest.isResolveDependencies());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLinePackage()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-P someFile.ek9"));		
		TestCase.assertTrue(underTest.isPackaging());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineInstallMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-I"));		
	}
	
	@Test
	public void testCommandLineInstall()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-I someFile.ek9"));		
		TestCase.assertTrue(underTest.isInstall());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}

	@Test
	public void testCommandLineGenerateKeys()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-Gk"));		
		TestCase.assertTrue(underTest.isGenerateSigningKeys());
		
	}
	
	@Test
	public void testCommandLineGenerateKeysAdditionalSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(4, underTest.processCommandLine("-Gk someFile.ek9"));
	}

	@Test
	public void testCommandLineDeployMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-D"));		
	}
	
	@Test
	public void testCommandLineDeploy()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-D someFile.ek9"));		
		TestCase.assertTrue(underTest.isDeployment());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineIncrementVersionMissingParamAndSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-IV"));		
	}

	@Test
	public void testCommandLineIncrementVersionMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-IV major"));		
	}
	
	@Test
	public void testCommandLineIncrementVersionMissingParam()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-IV someFile.ek9"));		
	}

	@Test
	public void testCommandLineInvalidSetVersionParam()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-SV 10.3.A someFile.ek9"));
	}
	
	@Test
	public void testCommandLineSetVersionParam()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-SV 10.3.1 someFile.ek9"));
		TestCase.assertTrue(underTest.isSetReleaseVector());
		TestCase.assertTrue("10.3.1".equals(underTest.getOptionParameter("-SV")));
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineInvalidSetFeatureParam()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-SF 10.3.2-1bogus someFile.ek9"));
	}
	
	@Test
	public void testCommandLineSetFeatureVersionParam()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-SF 10.3.1-special someFile.ek9"));
		TestCase.assertTrue(underTest.isSetFeatureVector());
		TestCase.assertTrue("10.3.1-special".equals(underTest.getOptionParameter("-SF")));
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLinePrintVersionMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-PV"));	
	}
	
	@Test
	public void testCommandLineTestMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-t"));	
	}
	
	@Test
	public void testCommandLineTest()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-t someFile.ek9"));
		TestCase.assertTrue(underTest.isUnitTestExecution());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineDebugMissingSourceFile()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(3, underTest.processCommandLine("-d"));	
	}
	
	@Test
	public void testCommandLineDebugMissingPort()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-d someFile.ek9"));		
	}
	
	@Test
	public void testCommandLineDebug()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-d 9000 someFile.ek9"));
		TestCase.assertTrue(underTest.isRunDebugMode());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineRun()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("someFile.ek9"));
		TestCase.assertTrue(underTest.isRunNormalMode());
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}

	@Test
	public void testCommandLineInvalidBuildRunProgram()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(4, underTest.processCommandLine("-c someFile.ek9 -r SomeProgram"));		
	}
	
	@Test
	public void testCommandLineConflictingBuild1()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-c -C someFile.ek9"));		
	}
	
	@Test
	public void testCommandLineConflictingBuild2()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-GK -C someFile.ek9"));		
	}
	
	@Test
	public void testCommandLineConflictingBuild3()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-C -t someFile.ek9"));		
	}
	
	@Test
	public void testCommandLineConflictingBuild4()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-C -PV someFile.ek9"));		
	}
	
	@Test
	public void testCommandLineConflictingBuild5()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-c -d 9000 someFile.ek9"));		
	}
	
	@Test
	public void testCommandLineInvalidReleaseRunProgram()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(4, underTest.processCommandLine("-IV patch someFile.ek9 -r SomeProgram"));		
	}
	
	@Test
	public void testCommandLineRunProgram()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("someFile.ek9 -r SomeProgram"));
		TestCase.assertTrue(underTest.isRunNormalMode());
		TestCase.assertTrue("SomeProgram".equals(underTest.getProgramToRun()));
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
	@Test
	public void testCommandLineRunAsInvalidTarget()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(2, underTest.processCommandLine("-T wasm someFile.ek9"));
	}
	
	@Test
	public void testCommandLineEnvironment()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-e checker=false -e vogons='Bear Tree' someFile.ek9"));
		TestCase.assertTrue(underTest.getEk9AppDefines().contains("checker=false"));
		//Not the move to double quotes
		TestCase.assertTrue(underTest.getEk9AppDefines().contains("vogons=\"Bear Tree\""));
	}
	
	@Test
	public void testCommandLineRunAsJavaTarget()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-T java someFile.ek9 -r"));
		TestCase.assertTrue(underTest.isRunNormalMode());
		TestCase.assertNull(underTest.getProgramToRun());
		TestCase.assertTrue("java".equals(underTest.getTargetArchitecture()));
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}

	@Test
	public void testCommandLinePrintVersion()
	{
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-PV someFile.ek9"));
		TestCase.assertTrue(underTest.isPrintReleaseVector());		
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
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
		CommandLineDetails underTest = new CommandLineDetails();				
		TestCase.assertEquals(0, underTest.processCommandLine("-IV " + param + " someFile.ek9"));
		TestCase.assertTrue(underTest.isIncrementReleaseVector());
		TestCase.assertTrue(param.equals(underTest.getOptionParameter("-IV")));
		TestCase.assertTrue("someFile.ek9".equals(underTest.getSourceFileName()));
	}
	
}
