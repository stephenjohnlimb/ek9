package org.ek9lang.core.utils;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class OsSupportTest
{
	OsSupport underTest = new OsSupport();

	@Test
	public void testNumberOfProcessors()
	{
		int count = underTest.getNumberOfProcessors();
		TestCase.assertTrue(count > 1);
	}

	@Test
	public void checkDirectories()
	{
		String cwd = underTest.getCurrentWorkingDirectory();
		TestCase.assertNotNull(cwd);

		String home = underTest.getUsersHomeDirectory();
		TestCase.assertNotNull(home);

		String temp = underTest.getTempDirectory();
		TestCase.assertNotNull(temp);
	}

	@Test
	public void testRecursiveListing()
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
		TestCase.assertNotNull(rootDirectoryForTest);
		String testPath = rootDirectoryForTest.getPath();

		List<File> files = underTest.getFilesRecursivelyFrom(new File(testPath));
		TestCase.assertNotNull(files);
		TestCase.assertNotSame(0, files.size());
	}

	@Test
	public void testDirectoriesInDirectory()
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
		TestCase.assertNotNull(rootDirectoryForTest);
		String testPath = rootDirectoryForTest.getPath();

		//Needs the correct directory structure in /forFileFindTests
		List<File> directories = underTest.getDirectoriesInDirectory(new File(testPath), "cooper");
		TestCase.assertNotNull(directories);
		TestCase.assertEquals(2, directories.size());
	}

	@Test
	public void testGlobFileInDirectory()
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
		TestCase.assertNotNull(rootDirectoryForTest);
		String testPath = rootDirectoryForTest.getPath();

		//Using globs not normal file wild cards.
		Glob searchCondition = new Glob("**.file", "**/a.file");

		//Needs the correct directory structure in /forFileFindTests
		List<File> files = underTest.getFilesRecursivelyFrom(new File(testPath), searchCondition);
		TestCase.assertNotNull(files);
		TestCase.assertEquals(2, files.size());
	}

	@Test
	public void testGlobListFileInDirectory()
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
		TestCase.assertNotNull(rootDirectoryForTest);
		String testPath = rootDirectoryForTest.getPath();

		//Using globs not normal file wild cards.
		List<String> includes = Arrays.asList("**.file", "**/*.notfile");
		List<String> excludes = Arrays.asList("**/*.junk", "**/*.txt");
		Glob searchCondition = new Glob(includes, excludes);

		//Needs the correct directory structure in /forFileFindTests
		List<File> files = underTest.getFilesRecursivelyFrom(new File(testPath), searchCondition);
		TestCase.assertNotNull(files);
		TestCase.assertEquals(4, files.size());
	}

	@Test
	public void testDeleteDirectory() throws IOException
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
		TestCase.assertNotNull(rootDirectoryForTest);
		String testPath = rootDirectoryForTest.getPath();

		//Using globs not normal file wild cards.
		Glob searchCondition = new Glob("**.newfile");

		File rootDir = new File(testPath);
		File newDir = new File(rootDir, "toBeRemoved");
		TestCase.assertTrue(newDir.mkdir());
		File newFile = new File(newDir, "a.newfile");
		TestCase.assertTrue(newFile.createNewFile());

		File extraFile = new File(newDir, "a.extrafile");
		TestCase.assertTrue(extraFile.createNewFile());

		//Now check that new file can be found
		List<File> files = underTest.getFilesRecursivelyFrom(rootDir, searchCondition);
		TestCase.assertNotNull(files);
		TestCase.assertEquals(1, files.size());
		FileHandling fileHandling = new FileHandling(underTest);
		fileHandling.deleteMatchingFiles(newDir, "a\\.newfile");
		//Now check it has gone
		files = underTest.getFilesRecursivelyFrom(rootDir, searchCondition);
		TestCase.assertNotNull(files);
		TestCase.assertEquals(0, files.size());

		fileHandling.deleteContentsAndBelow(newDir, true);

	}

	@Test
	public void testFileListing()
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
		TestCase.assertNotNull(rootDirectoryForTest);
		String testPath = rootDirectoryForTest.getPath();
		Collection<File> subdirectories = underTest.getAllSubdirectories(testPath);
		
		Collection<File> sourceFiles = underTest.getFilesFromDirectories(subdirectories, ".file");
		TestCase.assertNotNull(sourceFiles);
		TestCase.assertFalse(sourceFiles.isEmpty());
		
		//only three because some directories are empty and only three files have suffix ".file"
		TestCase.assertEquals(3, sourceFiles.size());
		
		sourceFiles.forEach(dir -> System.out.println("File [" + dir.getPath() + "]"));
	}
	
	@Test
	public void testDirectoryListing()
	{
		URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
		TestCase.assertNotNull(rootDirectoryForTest);
		String testPath = rootDirectoryForTest.getPath();
		TestCase.assertNotNull(testPath);
		
		Collection<File> subdirectories = underTest.getAllSubdirectories(testPath);
		TestCase.assertEquals(6, subdirectories.size());
		
		subdirectories.forEach(dir -> System.out.println("Directory [" + dir.getPath() + "]"));
	}
	
	@Test
	public void testFileNameProcessing()
	{
		String result = underTest.getFileNameWithoutPath(null);
		TestCase.assertEquals("", result);
		
		result = underTest.getFileNameWithoutPath("/");
		TestCase.assertEquals("", result);
		
		result = underTest.getFileNameWithoutPath("/root.file");
		TestCase.assertEquals("root.file", result);

		result = underTest.getFileNameWithoutPath("/single/s1.file");
		TestCase.assertEquals("s1.file", result);
		
		result = underTest.getFileNameWithoutPath("/several/items/in/a/path/s3.file");
		TestCase.assertEquals("s3.file", result);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testDuffDirectoryListing()
	{
		underTest.getAllSubdirectories(null);
	}
}
