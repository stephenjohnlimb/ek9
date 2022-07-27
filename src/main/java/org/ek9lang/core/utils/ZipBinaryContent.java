package org.ek9lang.core.utils;

/**
 * Very simple wrapper for binary contents.
 */
public final record ZipBinaryContent(String entryName, byte[] content)
{
	public String getEntryName()
	{
		return entryName;
	}

	public byte[] getContent()
	{
		return content;
	}
}
