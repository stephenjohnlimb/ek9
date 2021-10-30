package org.ek9lang.core.utils;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class DigestTest
{
	private static String testFileName = "assertTest.txt";
	private static String sha256FileName = "assertTest.sha256";
	
	@Before
	public void removeFile()
	{
		File newFile = new File(System.getProperty("java.io.tmpdir"), testFileName);
		//Remove it if already there
		if(newFile.exists())
			newFile.delete();
		File sha256File = new File(System.getProperty("java.io.tmpdir"), sha256FileName);
		if(sha256File.exists())
			sha256File.delete();
	}

	@Test
	public void testNullCheckSums()
	{
		Digest.CheckSum cksum = new Digest.CheckSum(new byte[1]);
		TestCase.assertFalse(cksum.equals(null));
	}

	@Test(expected = java.lang.RuntimeException.class)
	public void missingFile()
	{
		File nonSuch = new File(System.getProperty("java.io.tmpdir"), "nonSuch.txt");
		Digest.digest(nonSuch);
	}

	@Test(expected = java.lang.RuntimeException.class)
	public void missingChecksumFile()
	{
		File nonSuch = new File(System.getProperty("java.io.tmpdir"), "nonSuch.txt");
		File nonSuchSha256 = new File(System.getProperty("java.io.tmpdir"), "nonSuch.sha256");
		Digest.check(nonSuch, nonSuchSha256);
	}

	@Test
	public void quickCheck()
	{
		//checked against https://xorbin.com/tools/sha256-hash-calculator
		String data = "The quick brown fox";
		Digest.CheckSum ckSum = Digest.digest(data);

		String check = ckSum.toString();
		TestCase.assertEquals("5cac4f980fedc3d3f1f99b4be3472c9b30d56523e632d151237ec9309048bda9".toUpperCase(), check);
	}

	@Test
	public void testDigestEmptyFile() throws IOException
	{
		File newFile = new File(System.getProperty("java.io.tmpdir"), testFileName);
		newFile.createNewFile();

		File sha256File = new File(System.getProperty("java.io.tmpdir"), sha256FileName);

		Digest.CheckSum ckSum1 = Digest.digest(newFile);
		ckSum1.saveToFile(sha256File);

		Digest.check(newFile, sha256File);

		Digest.CheckSum ckSum2 = Digest.digest(newFile);

		TestCase.assertTrue(ckSum1.equals(ckSum1));
		TestCase.assertTrue(ckSum1.equals(ckSum2));

		//There is no content so check sum always the same
		TestCase.assertEquals("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855", ckSum1.toString());

		byte[] validBytes = Hex.toByteArray("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855");
		TestCase.assertTrue(ckSum1.equals(validBytes));

		byte[] inValidBytes = Hex.toByteArray("X3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855");
		TestCase.assertFalse(ckSum1.equals(inValidBytes));

		TestCase.assertFalse(ckSum1.equals(new byte[0]));

		TestCase.assertFalse(ckSum1.equals(new String()));

		if (newFile.exists())
			newFile.delete();
	}
}
