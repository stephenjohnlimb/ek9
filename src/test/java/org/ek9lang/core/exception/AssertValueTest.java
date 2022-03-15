package org.ek9lang.core.exception;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssertValueTest
{

	@Test
	public void testNullRange()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			Date toCheck = null;
			AssertValue.checkRange("Outside range", null, 1, 10);
		});
	}

	@Test
	public void testLowerRange()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			Date toCheck = null;
			AssertValue.checkRange("Outside range", -1, 1, null);
		});
	}

	@Test
	public void testUpperRange()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			Date toCheck = null;
			AssertValue.checkRange("Outside range", 11, null, 10);
		});
	}

	@Test
	public void testRange()
	{
		Date toCheck = null;
		AssertValue.checkRange("Outside range", 1, 0, 10);
		//No exception
	}

	@Test
	public void testOptionNotEmpty()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			Date toCheck = null;
			AssertValue.checkNotEmpty("Should not be empty", Optional.empty());
		});
	}

	@Test
	public void testOption()
	{
		Date toCheck = null;
		AssertValue.checkNotEmpty("Should not be empty", Optional.of(5));
	}

	@Test
	public void testCollectionEmpty()
	{
		Date toCheck = null;
		AssertValue.checkNotEmpty("Should not be empty", Arrays.asList("Buenos Aires", "Córdoba", "La Plata"));
	}

	@Test
	public void testCollectionNotEmpty()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			Date toCheck = null;
			AssertValue.checkNotEmpty("Should not be empty", new ArrayList<String>());
		});
	}

	@Test
	public void testNullDate()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			Date toCheck = null;
			AssertValue.checkNotNull("Some Message", toCheck);
		});
	}

	@Test
	public void testNoneDate()
	{
		Date toCheck = new Date();
		AssertValue.checkNotNull("Some Message", toCheck);
	}

	@Test
	public void testCheckTrue()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			AssertValue.checkTrue("Cannot be false", false);
		});
	}

	@Test
	public void testTrue()
	{
		AssertValue.checkTrue("Cannot be false", true);
	}

	@Test
	public void testNullString()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String toCheck = null;
			AssertValue.checkNotEmpty("Some Message", toCheck);
		});
	}

	@Test
	public void testEmptyString()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String toCheck = "";
			AssertValue.checkNotEmpty("Some Message", toCheck);
		});
	}

	@Test
	public void testNoneEmptyString()
	{
		String toCheck = "Some Text";
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}

	@Test
	public void testNullStrings()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String[] toCheck = null;
			AssertValue.checkNotEmpty("Some Message", toCheck);
		});
	}

	@Test
	public void testEmptyStrings()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String[] toCheck = new String[0];
			AssertValue.checkNotEmpty("Some Message", toCheck);
		});
	}

	@Test
	public void testPartEmptyStrings()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String[] toCheck = {"Some Text", null};
			AssertValue.checkNotEmpty("Some Message", toCheck);
		});
	}

	@Test
	public void testNoneEmptyStrings()
	{
		String[] toCheck = { "Some Text", "other text" };
		AssertValue.checkNotEmpty("Some Message", toCheck);
	}
	
	@Test
	public void testNullNotFoundFile()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			AssertValue.checkCanReadFile("File Cannot be found", (File)null);
		});
	}

	@Test
	public void testNotFoundFile()
	{
		assertThrows(IllegalArgumentException.class, () -> {
			AssertValue.checkCanReadFile("File Cannot be found", "nosuchfile");
		});
	}

	@Test
	public void testInAccessibleReadableDirectory() throws IOException
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String dir = "D";
			AssertValue.checkDirectoryReadable("Must be able to read from", dir);
		});
	}

	@Test
	public void testInAccessibleWritableDirectory() throws IOException
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String dir = "D";
			AssertValue.checkDirectoryWritable("Must be able to read from", dir);
		});
	}

	@Test
	public void testInAccessibleFileReadableDirectory() throws IOException
	{
		assertThrows(IllegalArgumentException.class, () -> {
			File dir = new File("D");
			AssertValue.checkDirectoryReadable("Must be able to read from", dir);
		});
	}

	@Test
	public void testInAccessibleFileWritableDirectory() throws IOException
	{
		assertThrows(IllegalArgumentException.class, () -> {
			File dir = new File("D");
			AssertValue.checkDirectoryWritable("Must be able to read from", dir);
		});
	}

	@Test
	public void testAccessibleDirectory() throws IOException
	{
		String tempDir = System.getProperty("java.io.tmpdir");

		AssertValue.checkDirectoryReadable("Must be able to read from", tempDir);
		AssertValue.checkDirectoryWritable("Must be able to write to", tempDir);

		File tempDirFile = new File(tempDir);
		AssertValue.checkDirectoryReadable("Must be able to read from", tempDirFile);
		AssertValue.checkDirectoryWritable("Must be able to write to", tempDirFile);
	}

	@Test
	public void testNonReadableDirectory() throws IOException
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String tempDir = " no such directory/";
			AssertValue.checkDirectoryReadable("Must be able to read", tempDir);
		});
	}

	@Test
	public void testNonWritableDirectory() throws IOException
	{
		assertThrows(IllegalArgumentException.class, () -> {
			String tempDir = " no such directory/";
			AssertValue.checkDirectoryWritable("Must be able to write", tempDir);
		});
	}

	@Test
	public void testFoundFile() throws IOException
	{
		String tempDir = System.getProperty("java.io.tmpdir");
		
		File newFile = new File(tempDir, "assertTest.txt");
		//Remove it if already there
		if(newFile.exists())
			newFile.delete();
		
		//make the file check can access
		newFile.createNewFile();
		AssertValue.checkCanReadFile("File Cannot be found", newFile.getAbsolutePath());
		if(newFile.exists())
			newFile.delete();
	}
}
