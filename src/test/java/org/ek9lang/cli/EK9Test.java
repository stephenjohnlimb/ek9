package org.ek9lang.cli;

import junit.framework.TestCase;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.ek9lang.core.utils.SigningKeyPair;
import org.junit.After;
import org.junit.Test;

import java.io.File;

/**
 * Test the main command to run EK9 but from a test pint of view.
 * <p>
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
		assertResult(EK9.SUCCESS_EXIT_CODE, new String[]{"-h"});
	}

	@Test
	public void testCommandLineVersionText()
	{
		assertResult(EK9.SUCCESS_EXIT_CODE, new String[]{"-V"});
	}

	@Test
	public void testIncrementationCompilation()
	{
		String sourceName = "HelloWorld.ek9";
		String[] command = new String[]{"-c " + sourceName};

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
		sourceFile.setLastModified(lastModified + 1);

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

	@Test
	public void testGenerateKeys()
	{
		String[] command = new String[]{"-Gk"};

		File home = new File(osSupport.getUsersHomeDirectory());
		TestCase.assertTrue(home.exists());

		//This will actually trigger a full compile.
		assertKeysPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));
	}

	@Test
	public void testRunSingleProgram()
	{
		String sourceName = "HelloWorld.ek9";

		String[] command = new String[]{sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This will actually trigger a full compile and then run.
		assertCompilationArtefactsPresent(assertResult(EK9.RUN_COMMAND_EXIT_CODE, command));
	}

	@Test
	public void testRunUnitTests()
	{
		String sourceName = "HelloWorlds.ek9";

		String[] command = new String[]{"-t " + sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This will actually trigger a full compile and then run.
		assertCompilationArtefactsPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));
	}

	@Test
	public void testRunSelectedProgram()
	{
		String sourceName = "HelloWorlds.ek9";

		String[] command = new String[]{sourceName + " -r HelloMars"};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This will actually trigger a full compile and then run.
		assertCompilationArtefactsPresent(assertResult(EK9.RUN_COMMAND_EXIT_CODE, command));
	}

	@Test
	public void testInstallNonSuchPackage()
	{
		String sourceName = "HelloWorlds.ek9";

		String[] command = new String[]{"-I " + sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//Should fail because this source does not define a package.
		assertResult(EK9.BAD_COMMANDLINE_EXIT_CODE, command);
	}

	@Test
	public void testInstallPackage()
	{
		installPackage("PackageNoDeps.ek9");
		File libDir = fileHandling.getUsersHomeEK9LibDirectory();
		TestCase.assertNotNull(libDir.listFiles());
		//Expect a zip and a sha256 of that zip.
		TestCase.assertEquals(2, libDir.listFiles().length);
	}

	private void installPackage(String sourceName)
	{
		String[] command = new String[]{"-I " + sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This should succeed and install the package in users lib directory
		assertResult(EK9.SUCCESS_EXIT_CODE, command);

		File libDir = fileHandling.getUsersHomeEK9LibDirectory();
		TestCase.assertNotNull(libDir);
		TestCase.assertTrue((libDir.exists()));
	}
	@Test
	public void testBuildVersioningOfPackage()
	{
		assertPackageVersionChange("1.0.0-1", "-IV build", "PackageNoDeps.ek9");
	}

	@Test
	public void testPatchVersioningOfPackage()
	{
		assertPackageVersionChange("1.0.1-0", "-IV patch", "PackageNoDeps.ek9");
	}

	@Test
	public void testMinorVersioningOfPackage()
	{
		assertPackageVersionChange("1.1.0-0", "-IV minor", "PackageNoDeps.ek9");
	}

	@Test
	public void testMajorVersioningOfPackage()
	{
		assertPackageVersionChange("2.0.0-0", "-IV major", "PackageNoDeps.ek9");
	}

	@Test
	public void testSetVersioningOfPackage()
	{
		assertPackageVersionChange("3.8.6-0", "-SV 3.8.6", "PackageNoDeps.ek9");
		//Now check that it can also be incremented.
		assertPackageVersionChange("3.9.0-0", "-IV minor", "PackageNoDeps.ek9");
	}

	@Test
	public void testSetFeatureVersioningOfPackage()
	{
		assertPackageVersionChange("3.8.6-specials-0", "-SF 3.8.6-specials", "PackageNoDeps.ek9");
		//Now check that it can also be incremented.
		assertPackageVersionChange("3.9.0-specials-0", "-IV minor", "PackageNoDeps.ek9");
	}

	@Test
	public void testPrintVersioningOfPackage()
	{
		assertPackageVersionChange("3.8.6-specials-0", "-SF 3.8.6-specials", "PackageNoDeps.ek9");
		//Now check that it can also be printed.
		assertPackageVersionChange("3.8.6-specials-0", "-PV", "PackageNoDeps.ek9");
	}

	@Test
	public void testDeploymentOfPackage()
	{
		deployPackage(EK9.SUCCESS_EXIT_CODE, "PackageNoDeps.ek9");
	}

	/**
	 * A more complex test that checks that the package resolver mechanism works.
	 * For this test we actually build and package each of the dependencies locally.
	 * But if they were not local builds, the resolver would (in the future)
	 * try and pull these from a repository.
	 */
	@Test
	public void testPackageDependencyResolution()
	{
		//So firstly we need to 'install' a number of dependent packages
		//then we can try and resolve those dependencies.
		installPackage("SupportUtils.ek9");
		installPackage("HandyTools.ek9");
		installPackage("SupertoolsUtil.ek9");
		installPackage("ToolsMisc.ek9");

		//Now for the one with dependencies
		deployPackage(EK9.SUCCESS_EXIT_CODE, "SinglePackage.ek9");
	}

	@Test
	public void testMissingPackageDependencyResolution()
	{
		//So firstly we need to 'install' a number of dependent packages
		//then we can try and resolve those dependencies.
		installPackage("SupportUtils.ek9");
		//We omit this package and the overall resolution should fail
		//installPackage("HandyTools.ek9");
		installPackage("SupertoolsUtil.ek9");
		installPackage("ToolsMisc.ek9");

		//Now for the one with dependencies - but we've not got one present.
		deployPackage(EK9.BAD_COMMANDLINE_EXIT_CODE, "SinglePackage.ek9");
	}

	private void deployPackage(int expectedExitCode, String sourceName)
	{
		String[] deployCommand = new String[]{"-v -D " + sourceName};
		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//So this should just package up and deploy the artefact.
		CommandLineDetails commandLine = assertResult(expectedExitCode, deployCommand);

		if(expectedExitCode == EK9.SUCCESS_EXIT_CODE)
		{
			String zipFileName = fileHandling.makePackagedModuleZipFileName(commandLine.getModuleName(), commandLine.getVersion().toString());
			File sha256EncFile = new File(fileHandling.getDotEK9Directory(commandLine.getSourceFileDirectory()), zipFileName + ".sha256.enc");
			TestCase.assertTrue(sha256EncFile.exists());
		}
	}

	private void assertPackageVersionChange(String expectedVersion, String command, String sourceName)
	{
		String[] incrementBuildNo = new String[]{command + " " + sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//So this should just increment the build number from '0' which is what is in PackageNoDeps.ek9 to '1'
		CommandLineDetails commandLine = assertResult(EK9.SUCCESS_EXIT_CODE, incrementBuildNo);

		//Now get the line number
		Integer versionLineNumber = commandLine.processEK9FileProperties(true);
		TestCase.assertEquals(expectedVersion, commandLine.getVersion());
	}

	private CommandLineDetails assertResult(int expectation, String[] argv)
	{
		CommandLineDetails commandLine = new CommandLineDetails(fileHandling, osSupport);

		int result = commandLine.processCommandLine(argv);
		TestCase.assertTrue(result <= EK9.SUCCESS_EXIT_CODE);

		//Now should something be run and executed.
		if(result == EK9.RUN_COMMAND_EXIT_CODE)
			TestCase.assertEquals(expectation, new EK9(commandLine).run());

		return commandLine;
	}

	private void assertKeysPresent(CommandLineDetails commandLine)
	{
		TestCase.assertTrue(fileHandling.isUsersSigningKeyPairPresent());
		SigningKeyPair signingKeyPair = fileHandling.getUsersSigningKeyPair();
		TestCase.assertNotNull(signingKeyPair);
		TestCase.assertNotNull(signingKeyPair.getPubBase64());
		TestCase.assertNotNull(signingKeyPair.getPvtBase64());
		//get keys and check they are ok
	}

	private void assertCompilationArtefactsPresent(CommandLineDetails commandLine)
	{
		File propsFile = fileHandling.getTargetPropertiesArtefact(commandLine.getFullPathToSourceFileName());
		TestCase.assertTrue(propsFile.exists());

		File targetArtefact = fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(), commandLine.targetArchitecture);
		TestCase.assertTrue(targetArtefact.exists());
	}
}
