package org.ek9lang.core.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Just a common base directory and a list of files with that common base.
 * But also can just contain binary contents i.e. one or the other.
 * Makes it easy to take a range of files from various locations and drop them into a zip.
 */
public final class ZipSet
{
	private Path relativePath;
	private Collection<File> files;
	private Collection<ZipBinaryContent> entries;

	public ZipSet()
	{

	}

	public ZipSet(Path relativePath, Collection<File> files)
	{
		this.relativePath = relativePath;
		this.files = files;
	}

	public ZipSet(Collection<ZipBinaryContent> entries)
	{
		this.entries = entries;
	}

	public boolean isEmpty()
	{
		return !isFileBased() && !isEntryBased();
	}

	public boolean isFileBased()
	{
		return files != null;
	}

	public boolean isEntryBased()
	{
		return entries != null;
	}

	public Path getRelativePath()
	{
		return relativePath;
	}

	public Collection<File> getFiles()
	{
		return files;
	}

	public Collection<ZipBinaryContent> getEntries()
	{
		return entries;
	}
}
