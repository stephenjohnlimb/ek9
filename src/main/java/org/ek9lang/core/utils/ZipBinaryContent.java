package org.ek9lang.core.utils;

/**
 * Very simple wrapper for binary contents.
 */
public class ZipBinaryContent
{
	private final String entryName;
	private final byte[] content;
	
	public ZipBinaryContent(String entryName, byte[] content)
	{
		this.content = content;
		this.entryName = entryName;				
	}

	public String getEntryName()
	{
		return entryName;
	}

	public byte[] getContent()
	{
		return content;
	}
}
