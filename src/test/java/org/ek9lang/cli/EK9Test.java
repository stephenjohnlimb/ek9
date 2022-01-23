package org.ek9lang.cli;

import junit.framework.TestCase;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Test the main command to run EK9 but from a test pint of view.
 *
 * Only design to test valid command line instructions.
 * See CommandLineDetailsTest of invalid combinations.
 */
public class EK9Test
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
		assertResult(EK9.SUCCESS_EXIT_CODE, new String[] {"-h"});
	}

	@Test
	public void testCommandLineVersionText()
	{
		assertResult(EK9.SUCCESS_EXIT_CODE, new String[] {"-V"});
	}

	@Test
	public void testIncrementationCompilation()
	{
		String sourceName = "HelloWorld.ek9";
		String[] command = new String[] {"-c " + sourceName};

		//We will copy this into a working directory and process it.
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This will actually trigger a full compile first.
		assertCompilationArtefactsPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));

		//Now if we do it again there will be nothing to compile
		CommandLineDetails commandLine = assertResult(EK9.SUCCESS_EXIT_CODE, command);
		assertCompilationArtefactsPresent(commandLine);

		//If we remove the target it will trigger a full re-compile and packaging
		File targetArtefact = fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(), commandLine.targetArchitecture);
		TestCase.assertTrue(targetArtefact.delete());
		assertCompilationArtefactsPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));

		long lastModified = targetArtefact.lastModified();
		//Now simulate updating the source to ensure target gets rebuilt.
		sourceFile.setLastModified(lastModified+1);

		//Trigger incremental rebuild
		assertCompilationArtefactsPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));
		targetArtefact = fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(), commandLine.targetArchitecture);
		TestCase.assertNotSame(lastModified, targetArtefact.lastModified());
	}

	@Test
	public void testDebugDevCompilation()
	{
		String sourceName = "HelloWorld.ek9";
		String[] command = new String[]{"-Cd " + sourceName};

		//We will copy this into a working directory and process it.
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This will actually trigger a full compile.
		assertCompilationArtefactsPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));
	}

	private CommandLineDetails assertResult(int expectation, String[] argv)
	{
		CommandLineDetails commandLine = new CommandLineDetails(fileHandling, osSupport);

		int result = commandLine.processCommandLine(argv);
		TestCase.assertTrue(result <= EK9.SUCCESS_EXIT_CODE);

		//Now should something be run and executed.
		if(result == EK9.RUN_COMMAND_EXIT_CODE)
			TestCase.assertEquals(expectation, new EK9(commandLine, fileHandling, osSupport).run());

		return commandLine;
	}

	private void assertCompilationArtefactsPresent(CommandLineDetails commandLine)
	{
		File propsFile = fileHandling.getTargetPropertiesArtefact(commandLine.getFullPathToSourceFileName());
		TestCase.assertTrue(propsFile.exists());

		File targetArtefact = fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(), commandLine.targetArchitecture);
		TestCase.assertTrue(targetArtefact.exists());
	}
}
