package org.ek9lang.core.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ensure that the file handling class functions as expected.
 */
public class FileHandlingTest
{
	private final FileHandling underTest = new FileHandling(new OsSupport(true));

	@AfterEach
	public void tidyUp()
	{
		String testHomeDirectory = underTest.getUsersHomeDirectory();
		assertNotNull(testHomeDirectory);
		//As this is a test delete from process id and below
		underTest.deleteContentsAndBelow(new File(new File(testHomeDirectory).getParent()), true);
	}

	@Test
	public void testEK9DirectoryNaming()
	{
		String testHomeDirectory = underTest.getUsersHomeDirectory();
		assertNotNull(testHomeDirectory);
		System.out.println("Home directory is [" + testHomeDirectory + "]");
		String testHomeEK9Directory = underTest.getUsersHomeEK9Directory();
		assertNotNull(testHomeEK9Directory);

		File testHomeEK9LibDirectory = underTest.getUsersHomeEK9LibDirectory();
		assertNotNull(testHomeEK9LibDirectory);
	}

	@Test
	public void testPackagedModuleZipFileName()
	{
		String result = underTest.makePackagedModuleZipFileName("some.module.name", "2.5.1");
		assertEquals("some.module.name-2.5.1.zip", result);
	}

	@Test
	public void testFileStructure() throws IOException
	{
		//So this creates a full .ek9 structure under the developers home directory
		underTest.validateHomeEK9Directory("java");

		//We need a project directory so that we can try out the other capabilities.
		//i.e. This is a project you will have checked out, here it is empty, we just create it
		String testHomeDirectory = underTest.getUsersHomeDirectory();
		File aProjectDirectory = FileSystems.getDefault().getPath(testHomeDirectory, "src", "aProject").toFile();
		underTest.makeDirectoryIfNotExists(aProjectDirectory);

		//Dummy source file
		File sampleEK9 = FileSystems.getDefault().getPath(aProjectDirectory.getPath(), "sample.ek9").toFile();
		assertTrue(sampleEK9.createNewFile());

		Digest.CheckSum checkSum = underTest.createSha256Of(sampleEK9.getPath());
		assertNotNull(checkSum);
		String checkSumFileName = sampleEK9.getPath()+".sha256";
		File sampleEK9Sha256 = new File(checkSumFileName);
		assertTrue(sampleEK9Sha256.exists());
		Digest.check(sampleEK9, sampleEK9Sha256);

		//Now get the .ek9 directory under that, this is where we will store the built artefacts.
		String projectDotEK9Directory = underTest.getDotEK9Directory(aProjectDirectory);

		//This will check or make the whole .ek9 tree.
		underTest.validateEK9Directory(projectDotEK9Directory, "java");

		File generatedOutputDir = underTest.getMainGeneratedOutputDirectory(projectDotEK9Directory, "java");
		assertNotNull(generatedOutputDir);

		File finalOutputDir = underTest.getMainFinalOutputDirectory(projectDotEK9Directory, "java");
		assertNotNull(finalOutputDir);

		File devGeneratedOutputDir = underTest.getDevGeneratedOutputDirectory(projectDotEK9Directory, "java");
		assertNotNull(devGeneratedOutputDir);

		File devFinalOutputDir = underTest.getDevFinalOutputDirectory(projectDotEK9Directory, "java");
		assertNotNull(devFinalOutputDir);

		File targetArtefact = underTest.getTargetExecutableArtefact(sampleEK9.getPath(), "java");
		//Simulate a build of the target
		assertTrue(targetArtefact.createNewFile());
		assertNotNull(targetArtefact);

		File targetProperties = underTest.getTargetPropertiesArtefact(sampleEK9.getPath());
		//Simulate a build of the target properties
		assertTrue(targetProperties.createNewFile());
		assertNotNull(targetProperties);

		//Clean out
		underTest.cleanEK9DirectoryStructureFor(sampleEK9, "java");

		//make sure they've gone
		targetArtefact = underTest.getTargetExecutableArtefact(sampleEK9.getPath(), "java");
		assertFalse(targetArtefact.exists());
		targetProperties = underTest.getTargetPropertiesArtefact(sampleEK9.getPath());
		assertFalse(targetProperties.exists());

		//Ensure no stale zips.
		underTest.deleteStalePackages(aProjectDirectory.getPath(), "any.mod.name");
	}

	@Test
	public void testKeySigningPairPersistence()
	{
		//Ensure it is there
		underTest.validateHomeEK9Directory("java");

		String testHomeDirectory = underTest.getUsersHomeDirectory();
		assertNotNull(testHomeDirectory);
		SigningKeyPair keyPair = SigningKeyPair.generate(2048);
		boolean saved = underTest.saveToHomeEK9Directory(keyPair);
		assertTrue(saved);
		assertTrue(underTest.isUsersSigningKeyPairPresent());

		SigningKeyPair reloadedKeyPair = underTest.getUsersSigningKeyPair();

		assertEquals(keyPair.getPvtBase64(), reloadedKeyPair.getPvtBase64());
		assertEquals(keyPair.getPubBase64(), reloadedKeyPair.getPubBase64());
	}

	@Test
	public void testZippingAndPackaging() throws IOException
	{
		underTest.validateHomeEK9Directory("java");

		//We need a project directory so that we can try out the other capabilities.
		//i.e. This is a project you will have checked out, here it is empty, we just create it
		String testHomeDirectory = underTest.getUsersHomeDirectory();
		File aProjectDirectory = FileSystems.getDefault().getPath(testHomeDirectory, "src", "aProject").toFile();
		underTest.makeDirectoryIfNotExists(aProjectDirectory);

		//Dummy source file
		File sampleEK9 = FileSystems.getDefault().getPath(aProjectDirectory.getPath(), "sample.ek9").toFile();
		assertTrue(sampleEK9.createNewFile());

		List<File> files = new OsSupport().getFilesRecursivelyFrom(new File(sampleEK9.getParent()));

		//Now get the .ek9 directory under that, this is where we will store the built artefacts.
		String projectDotEK9Directory = underTest.getDotEK9Directory(aProjectDirectory);

		//This will check or make the whole .ek9 tree.
		underTest.validateEK9Directory(projectDotEK9Directory, "java");

		String zipFileName = underTest.makePackagedModuleZipFileName("some.mod.name", "2.3.1");
		String fileName = projectDotEK9Directory + zipFileName;

		File propsFile = underTest.getTargetPropertiesArtefact(sampleEK9.getPath());
		assertTrue(propsFile.createNewFile());

		ZipSet fileSet = new ZipSet(aProjectDirectory.toPath(), files);
		boolean created = underTest.createZip(fileName, fileSet, propsFile);
		assertTrue(created);
		assertTrue(new File(fileName).exists());
		assertTrue(new File(fileName).delete());
		assertFalse(new File(fileName).exists());

		byte[] someBinaryData = "The Quick Brown fox".getBytes(StandardCharsets.UTF_8);
		List<ZipBinaryContent> entries = new ArrayList<>();
		entries.add(new ZipBinaryContent("text", someBinaryData));

		ZipSet binarySet = new ZipSet(entries);
		created = underTest.createZip(fileName, binarySet, propsFile);
		assertTrue(created);
		File zipFile = new File(fileName);
		assertTrue(zipFile.exists());
		assertTrue(zipFile.delete());
		assertFalse(zipFile.exists());

		//Now try jar functionality
		created = underTest.createJar(fileName, Arrays.asList(fileSet, binarySet));
		assertTrue(created);
		assertTrue(zipFile.exists());

		//unpack that zip
		boolean unzipped = underTest.unZipFileTo(zipFile, underTest.getTempDirectory());
		assertTrue(unzipped);

		//Now remove zip file
		zipFile = new File(fileName);
		assertTrue(zipFile.delete());
		assertFalse(zipFile.exists());

		//Check what was unzipped.
		File unPackedSampleEK9 = FileSystems.getDefault().getPath(underTest.getTempDirectory(), "sample.ek9").toFile();
		assertTrue(unPackedSampleEK9.exists());

		File unPackedText = FileSystems.getDefault().getPath(underTest.getTempDirectory(), "text").toFile();
		assertTrue(unPackedText.exists());
	}
}
