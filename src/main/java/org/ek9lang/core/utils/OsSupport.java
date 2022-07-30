package org.ek9lang.core.utils;

import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.*;

/**
 * Operating System support and generic stuff for directories and files.
 */
public final class OsSupport
{
	public static int numberOfProcessors()
	{
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * When is stub mode the users home directory and current working directory are altered.
	 */
	private boolean stubMode = false;

	public OsSupport()
	{

	}

	/**
	 * Used as and when you want to use a stub for testing.
	 * You don't always want to create or access the actual users
	 * home directory or current working directory.
	 */
	public OsSupport(boolean testStubMode)
	{
		this.stubMode = testStubMode;
	}

	/**
	 * Is this configured to be in stub mode.
	 */
	public boolean isInStubMode()
	{
		return stubMode;
	}

	/**
	 * Provides the current process id of this running application.
	 */
	public long getPid()
	{
		return ProcessHandle.current().pid();
	}

	/**
	 * Create a simulated tmp, home and current working directory under
	 * the temp directory for a particular process.
	 * This enables us to run tests with real files being created but in a safe way.
	 */
	private String createStubbedDirectory(String forDir)
	{
		String rtn = System.getProperty("java.io.tmpdir") +
				FileSystems.getDefault().getSeparator() +
				getPid() +
				FileSystems.getDefault().getSeparator() +
				forDir;

		makeDirectoryIfNotExists(new File(rtn));

		return rtn;
	}

	public void makeDirectoryIfNotExists(File directory)
	{
		AssertValue.checkNotNull("Directory cannot be null", directory);
		if(!directory.exists() && !directory.mkdirs())
			throw new CompilerException("Unable to create directory [" + directory.getPath() + "]");
	}

	public String getTempDirectory()
	{
		if(stubMode)
			return createStubbedDirectory("tmp");
		return System.getProperty("java.io.tmpdir");
	}

	public String getUsersHomeDirectory()
	{
		if(stubMode)
			return createStubbedDirectory("home");
		return System.getProperty("user.home");
	}

	public String getCurrentWorkingDirectory()
	{
		if(stubMode)
			return createStubbedDirectory("cwd");
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
		assertInDirectoryValid(inDirectory);
		AssertValue.checkNotNull("ExcludeStartingWith cannot be null", excludeStartingWith);

		File[] files = inDirectory.listFiles((d, name) -> !name.startsWith(excludeStartingWith));
		return Optional.ofNullable(files).stream().flatMap(Arrays::stream)
				.filter(File::isDirectory)
				.toList();
	}

	public List<File> getFilesRecursivelyFrom(File inDirectory, Glob searchCondition)
	{
		assertInDirectoryValid(inDirectory);
		AssertValue.checkNotNull("SearchCondition cannot be null", searchCondition);

		return getFilesRecursivelyFrom(inDirectory)
				.stream()
				.filter(file -> searchCondition.isAcceptable(inDirectory.toPath().relativize(file.toPath())))
				.toList();
	}

	public List<File> getFilesRecursivelyFrom(File inDirectory)
	{
		assertInDirectoryValid(inDirectory);

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
		assertInDirectoryValid(inDirectory);
		AssertValue.checkNotNull("FileSuffix cannot be null", fileSuffix);

		File[] files = inDirectory.listFiles((d, name) -> name.endsWith(fileSuffix));
		return Optional.ofNullable(files).stream().flatMap(Arrays::stream)
				.toList();
	}

	public Collection<File> getAllSubdirectories(String directoryRoot)
	{
		AssertValue.checkNotNull("DirectoryRoot cannot be null", directoryRoot);

		File dir = new File(directoryRoot);
		ArrayList<File> rtn = new ArrayList<>();
		rtn.add(dir);
		rtn.addAll(doGetAllSubdirectories(dir));

		return rtn;
	}

	private Collection<File> doGetAllSubdirectories(File dir)
	{
		AssertValue.checkNotNull("Dir cannot be null", dir);

		ArrayList<File> rtn = new ArrayList<>();
		Optional.ofNullable(dir.listFiles()).stream().flatMap(Arrays::stream)
				.filter(file -> file.isDirectory() && file.canRead())
				.forEach(file -> {
					rtn.add(file);
					rtn.addAll(doGetAllSubdirectories(file));
				});

		return rtn;
	}

	private void assertInDirectoryValid(File path)
	{
		AssertValue.checkNotNull("InDirectory cannot be null", path);
	}
}