package org.ek9lang.core.utils;

import org.ek9lang.core.exception.AssertValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wraps the SHA 256 digest and the resulting byte array into objects,
 * so we can deal with check objects rather than raw bytes.
 */
public final class Digest
{
	public static MessageDigest getSha256()
	{
		try
		{
			return MessageDigest.getInstance("SHA-256");
		}
		catch(NoSuchAlgorithmException e)
		{
			throw new RuntimeException("Unable to create SHA256 Message Digest", e);
		}
	}

	public static CheckSum digest(String input)
	{
		AssertValue.checkNotNull("checksum input cannot be null", input);
		return digest(input.getBytes(StandardCharsets.UTF_8));
	}

	public static CheckSum digest(byte[] input)
	{
		AssertValue.checkNotNull("checksum input cannot be null", input);
		return new CheckSum(getSha256().digest(input));
	}

	public static CheckSum digest(File file)
	{
		try(InputStream is = new FileInputStream(file))
		{
			MessageDigest digest = getSha256();
			byte[] buffer = new byte[4096];
			int amountRead;
			while((amountRead = is.read(buffer, 0, 4096)) != -1)
				digest.update(buffer, 0, amountRead);
			return new CheckSum(digest.digest());
		}
		catch(IOException e)
		{
			throw new RuntimeException("Unable to Read file " + file.getAbsolutePath(), e);
		}
	}

	public static boolean check(File contentsFile, File checkSumFile)
	{
		CheckSum contentsSha = digest(contentsFile);
		CheckSum providedSha = new CheckSum(checkSumFile);

		return providedSha.equals(contentsSha);
	}

	public final static class CheckSum
	{
		private byte[] checksum = null;

		public CheckSum(File sha256File)
		{
			this.loadFromFile(sha256File);
		}

		public CheckSum(byte[] checksum)
		{
			AssertValue.checkNotNull("checksum bytes array cannot be null", checksum);
			this.checksum = checksum;
		}

		@Override
		public boolean equals(Object obj)
		{
			if(obj == this)
				return true;

			if(obj instanceof CheckSum)
				return checkBytesSame(((CheckSum)obj).checksum, checksum);

			if(obj instanceof byte[])
				return checkBytesSame((byte[])obj, checksum);

			return false;
		}

		private boolean checkBytesSame(byte[] checksum1, byte[] checksum2)
		{
			if(checksum1 == null || checksum2 == null)
				return false;
			if(checksum1.length != checksum2.length)
				return false;
			for(int i = 0; i < checksum1.length; i++)
				if(checksum1[i] != checksum2[i])
					return false;
			return true;
		}

		@Override
		public String toString()
		{
			return Hex.toString(checksum);
		}

		public void saveToFile(File sha256File)
		{
			try(OutputStream output = new FileOutputStream(sha256File))
			{
				//Don't include the file name - because it might be very long, and we need to
				//keep what we have to send short because it is going to use PKI to encrypt it.
				String content = this + " *-\n";
				output.write(content.getBytes());
			}
			catch(Throwable th)
			{
				System.err.println("Unable to save " + sha256File.getName() + " " + th.getMessage());
			}
		}

		private void loadFromFile(File sha256File)
		{
			try(InputStream is = new BufferedInputStream(new FileInputStream(sha256File)))
			{
				String line = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				String firstPart = line.split(" ")[0];
				checksum = Hex.toByteArray(firstPart);
			}
			catch(Throwable th)
			{
				System.err.println("Unable to load " + sha256File.getName() + " " + th.getMessage());
			}
		}
	}
}
