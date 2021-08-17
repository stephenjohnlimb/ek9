package org.ek9lang.core.utils;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.ek9lang.core.utils.Digest.CheckSum;
import org.junit.Before;
import org.junit.Test;

public class DigestTest
{
	private static String testFileName = "assertTest.txt";
	
	@Before
	public void removeFile()
	{
		File newFile = new File(System.getProperty("java.io.tmpdir"), testFileName);
		//Remove it if already there
		if(newFile.exists())
			newFile.delete();
	}

	@Test
	public void quickCheck()
	{
		//checked against https://xorbin.com/tools/sha256-hash-calculator
		String data = "The quick brown fox";
		CheckSum ckSum = Digest.digest(data);

		String check = ckSum.toString();
		TestCase.assertEquals("5cac4f980fedc3d3f1f99b4be3472c9b30d56523e632d151237ec9309048bda9".toUpperCase(), check);
	}

	@Test
	public void testDigestEmptyFile() throws IOException
	{
		File newFile = new File(System.getProperty("java.io.tmpdir"), testFileName);
		newFile.createNewFile();

		CheckSum ckSum = Digest.digest(newFile);

		TestCase.assertTrue(ckSum.equals(ckSum));

		//There is not content so check sum always the same
		TestCase.assertEquals("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855", ckSum.toString());
		if (newFile.exists())
			newFile.delete();
	}
}
