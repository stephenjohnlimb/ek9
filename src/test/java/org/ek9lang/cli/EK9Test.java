package org.ek9lang.cli;

import junit.framework.TestCase;
import org.ek9lang.core.exception.ExitException;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.ek9lang.core.utils.SigningKeyPair;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.net.URL;

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

		assertCompilationArtefactsPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));
	}

	@Test
	public void testGenerateKeys()
	{
		String[] command = new String[]{"-Gk"};

		File home = new File(osSupport.getUsersHomeDirectory());
		TestCase.assertTrue(home.exists());

		assertKeysPresent(assertResult(EK9.SUCCESS_EXIT_CODE, command));
	}

	@Test
	public void testRunSingleProgram()
	{
		String sourceName = "HelloWorld.ek9";
		String[] command = new String[]{sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/basics/", sourceName);
		TestCase.assertNotNull(sourceFile);

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
		//Now check that it can also be incremented and appropriate resets on patch and build no.
		assertPackageVersionChange("3.9.0-0", "-IV minor", "PackageNoDeps.ek9");
	}

	@Test
	public void testSetFeatureVersioningOfPackage()
	{
		assertPackageVersionChange("3.8.6-specials-0", "-SF 3.8.6-specials", "PackageNoDeps.ek9");
		//Now check that it can also be incremented and patch/build number is reset to 0.
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

	@Test
	public void testBadPackage()
	{
		String sourceName = "BadPackage.ek9";
		String[] command = new String[]{"-I " + sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This should Fail with a file issue, because it should not parse.
		CommandLineDetails commandLine = new CommandLineDetails(fileHandling, osSupport);

		int result = commandLine.processCommandLine(command);
		TestCase.assertEquals(EK9.FILE_ISSUE_EXIT_CODE, result);
	}

	/**
	 * Check that standard includes works.
	 */
	@Test
	public void testPackageWithStandardIncludes()
	{
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
	public void testSemanticVersionBreach()
	{
		installPackage("SupportUtils.ek9");
		installPackage("SupportUtils2.ek9");
		installPackage("InterPackage1.ek9");
		installPackage("InterPackage2.ek9");

		installPackage(EK9.BAD_COMMANDLINE_EXIT_CODE, "SemanticBreach.ek9");
	}

	@Test
	public void testSemanticVersionPromotion()
	{
		installPackage("SupportUtils.ek9");
		installPackage("SupportUtils3.ek9");
		installPackage("InterPackage1.ek9");
		installPackage("InterPackage3.ek9");

		installPackage("SemanticPromotion.ek9");
	}

	@Test
	public void testSemanticVersionOptimisation()
	{
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

	@Test
	public void testCircularDependencies()
	{
		//We have to cheat like a developer would do and put some stuff in the place where it would have been packaged!
		//So put mangled version of both deps in and then we can resolve both to build a new one.
		//Then we can see if we can detect those circular references.

		//make sure the structure for packages exists.
		fileHandling.validateHomeEK9Directory("java");

		simulatePackagedInstallation("Circular2", "net.circular.two", "1.0.1-0");
		simulatePackagedInstallation("Circular1", "net.circular.one", "1.0.0-0");

		//Only now we've simulated how a developer might try and build circular refs can we test our next check.

		installPackage(EK9.BAD_COMMANDLINE_EXIT_CODE, "Circular1.ek9");
	}

	@Test
	public void testCircularDependenciesDifferentVersions()
	{
		//make sure the structure for packages exists.
		fileHandling.validateHomeEK9Directory("java");

		//Now these should install OK
		installPackage(EK9.SUCCESS_EXIT_CODE, "CircularVersions3.ek9");
		installPackage(EK9.SUCCESS_EXIT_CODE, "CircularVersions2.ek9");

		//But now we rework 'net.circular.verone' but introduce a circular dependency
		//So in EK9 in a single build there can only be one version of a package named module
		//It is not possible to have multiple versions of the same named package
		//The can all exist in the repository or in the lib, but it cannot depend on previous versions
		//of itself.
		installPackage(EK9.BAD_COMMANDLINE_EXIT_CODE, "CircularVersions1.ek9");
	}

	@Test
	public void testCircularDependencyResolution()
	{
		//Similar to above but just checking dependency resolution for circular detections.
		//make sure the structure for packages exists.
		fileHandling.validateHomeEK9Directory("java");

		simulatePackagedInstallation("Circular2", "net.circular.two", "1.0.1-0");
		simulatePackagedInstallation("Circular1", "net.circular.one", "1.0.0-0");

		//Only now we've simulated how a developer might try and build circular refs can we test our next check.
		String sourceName = "Circular1.ek9";
		String[] command = new String[]{"-Dp " + sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		TestCase.assertNotNull(sourceFile);
		assertResult(EK9.BAD_COMMANDLINE_EXIT_CODE, command);
	}

	private void installPackage(String sourceName)
	{
		installPackage(EK9.SUCCESS_EXIT_CODE, sourceName);

		File libDir = fileHandling.getUsersHomeEK9LibDirectory();
		TestCase.assertNotNull(libDir);
		TestCase.assertTrue((libDir.exists()));
	}

	private void installPackage(int expectation, String sourceName)
	{
		String[] command = new String[]{"-I " + sourceName};

		File sourceFile = sourceFileSupport.copyFileToTestCWD("/examples/constructs/packages/", sourceName);
		TestCase.assertNotNull(sourceFile);

		//This should succeed and install the package in users lib directory
		assertResult(expectation, command);
	}

	private void simulatePackagedInstallation(String sourceName, String moduleName, String version)
	{
		String dependencyVector = fileHandling.makeDependencyVector(moduleName, version);
		File homeEK9Lib = fileHandling.getUsersHomeEK9LibDirectory();

		//Let's check if it is unpacked already, if not we can unpack it.
		File unpackedDir = new File(homeEK9Lib, dependencyVector);
		if(!unpackedDir.exists())
			TestCase.assertTrue(unpackedDir.mkdirs());

		URL simulatedProperties = getClass().getResource("/examples/constructs/packages/"+sourceName+".package.properties");
		TestCase.assertNotNull(simulatedProperties);
		fileHandling.copy(new File(simulatedProperties.getPath()), new File(unpackedDir, ".package.properties"));

		//Now the source file for that package
		URL simulatedSource = getClass().getResource("/examples/constructs/packages/"+sourceName+".ek9");
		TestCase.assertNotNull(simulatedSource);
		fileHandling.copy(new File(simulatedSource.getPath()), new File(unpackedDir, sourceName+".ek9"));
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
