package org.ek9lang.core.utils;

import org.ek9lang.core.exception.AssertValue;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class OsSupport
{
	public static int numberOfProcessors()
	{
		return Runtime.getRuntime().availableProcessors();
	}

	public String getUsersHomeDirectory()
	{
		return System.getProperty("user.home");
	}

	public String getCurrentWorkingDirectory()
	{
		return System.getProperty("user.dir");
	}

	public int getNumberOfProcessors()
	{
		return OsSupport.numberOfProcessors();
	}

	public String getFileNameWithoutPath(String fileNameWithPath)
	{
		if(fileNameWithPath == null || fileNameWithPath.isEmpty())
			return "";

		File f = new File(fileNameWithPath);
		return f.getName();
	}

	public boolean isFileReadable(String fileName)
	{
		return fileName != null && isFileReadable(new File(fileName));
	}

	public boolean isFileReadable(File file)
	{
		return file != null && file.isFile() && !file.isDirectory() && file.canRead();
	}

	public boolean isDirectoryReadable(String directoryName)
	{
		return directoryName != null && isDirectoryReadable(new File(directoryName));
	}

	public boolean isDirectoryReadable(File directory)
	{
		return directory != null && directory.isDirectory() && directory.canRead();
	}

	public boolean isDirectoryWritable(String directoryName)
	{
		return isDirectoryWritable(new File(directoryName));
	}

	public boolean isDirectoryWritable(File directory)
	{
		return directory != null && directory.isDirectory() && directory.canWrite();
	}

	/**
	 * deletes matching files
	 *
	 * @param dir             the directory to look in
	 * @param fileNamePattern The pattern regex not shell so for * use .* for .txt use \\.txt
	 */
	public void deleteMatchingFiles(File dir, String fileNamePattern)
	{
		AssertValue.checkNotNull("Dir cannot be null", dir);
		AssertValue.checkNotNull("FileNamePattern cannot be null", fileNamePattern);

		File[] files = dir.listFiles((dir1, name) -> name.matches(fileNamePattern));
		if(files != null)
		{
			for(final File file : files)
			{
				if(!file.delete())
					System.err.println("Can't remove " + file.getAbsolutePath());
			}
		}
	}

	/**
	 * Does a recursive delete from this directory and below.
	 * If includeDirectoryRoot is true then it will delete that directory as well
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void deleteContentsAndBelow(File dir, boolean includeDirectoryRoot)
	{
		File[] files = dir.listFiles();
		if(files != null)
		{
			for(File toDelete : files)
			{
				if(toDelete.isDirectory())
					deleteContentsAndBelow(toDelete, true);
				else
					toDelete.delete();
			}
		}
		if(includeDirectoryRoot)
			dir.delete();
	}

	public List<File> getFilesFromDirectories(Collection<File> inDirectories, String fileSuffix)
	{
		AssertValue.checkNotNull("InDirectories cannot be null", inDirectories);
		AssertValue.checkNotNull("FileSuffix cannot be null", fileSuffix);

		ArrayList<File> rtn = new ArrayList<>();
		inDirectories.forEach(dir -> rtn.addAll(getFilesFromDirectory(dir, fileSuffix)));
		return rtn;
	}

	public List<File> getDirectoriesInDirectory(File inDirectory, String excludeStartingWith)
	{
		AssertValue.checkNotNull("InDirectory cannot be null", inDirectory);
		AssertValue.checkNotNull("ExcludeStartingWith cannot be null", excludeStartingWith);

		ArrayList<File> rtn = new ArrayList<>();
		File[] files = inDirectory.listFiles((d, name) -> !name.startsWith(excludeStartingWith));
		if(files != null)
			for(File f : files)
				if(f.isDirectory())
					rtn.add(f);
		return rtn;
	}

	public List<File> getFilesRecursivelyFrom(File inDirectory, Glob searchCondition)
	{
		AssertValue.checkNotNull("InDirectory cannot be null", inDirectory);
		AssertValue.checkNotNull("SearchCondition cannot be null", searchCondition);

		ArrayList<File> rtn = new ArrayList<>();
		List<File> fullList = getFilesRecursivelyFrom(inDirectory);
		fullList.forEach(file -> {
			Path thePath = file.toPath();
			Path relPath = inDirectory.toPath().relativize(thePath);
			if(searchCondition.isAcceptable(relPath))
				rtn.add(file);
		});
		return rtn;
	}

	public List<File> getFilesRecursivelyFrom(File inDirectory)
	{
		AssertValue.checkNotNull("InDirectory cannot be null", inDirectory);

		ArrayList<File> rtn = new ArrayList<>();
		File[] files = inDirectory.listFiles();
		if(files != null)
		{
			for(File f : files)
			{
				if(f.isDirectory())
					rtn.addAll(getFilesRecursivelyFrom(f));
				else
					rtn.add(f);
			}
		}
		return rtn;
	}

	public Collection<File> getFilesFromDirectory(File inDirectory, String fileSuffix)
	{
		AssertValue.checkNotNull("InDirectory cannot be null", inDirectory);
		AssertValue.checkNotNull("FileSuffix cannot be null", fileSuffix);

		ArrayList<File> rtn = new ArrayList<>();
		File[] files = inDirectory.listFiles((d, name) -> name.endsWith(fileSuffix));
		if(files != null)
			Collections.addAll(rtn, files);

		return rtn;
	}

	public Collection<File> getAllSubdirectories(String directoryRoot)
	{
		AssertValue.checkNotNull("DirectoryRoot cannot be null", directoryRoot);

		ArrayList<File> rtn = new ArrayList<>();

		File dir = new File(directoryRoot);
		rtn.add(dir);
		rtn.addAll(doGetAllSubdirectories(dir));

		return rtn;
	}

	private Collection<File> doGetAllSubdirectories(File dir)
	{
		AssertValue.checkNotNull("Dir cannot be null", dir);

		ArrayList<File> rtn = new ArrayList<>();

		File[] files = dir.listFiles();
		if(files != null)
		{
			Arrays.stream(files)
					.filter(file -> file.isDirectory() && file.canRead())
					.forEach(file -> {
						rtn.add(file);
						rtn.addAll(doGetAllSubdirectories(file));
					});
		}
		return rtn;
	}
}