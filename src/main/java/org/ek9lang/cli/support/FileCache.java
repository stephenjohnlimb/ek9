package org.ek9lang.cli.support;

import org.ek9lang.cli.CommandLineDetails;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.Glob;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Because we need access to the full file list or partial file list filtered.
 * We need to load up references to the files once and then filter as needed.
 * Obviously for a Language Server this would be done with listeners and watchers.
 * But for a command line process - we have to reload all the time.
 * <p>
 * This calls enables up to keep the base set of full files and then provide cut down
 * lists depending on what command is to be used.
 */
public class FileCache
{
	private final OsSupport osSupport;
	private final FileHandling fileHandling;
	private final CommandLineDetails commandLine;

	private boolean devBuild = false;
	private List<File> cachedFileList = null;

	public FileCache(CommandLineDetails commandLine, OsSupport osSupport)
	{
		this.osSupport = osSupport;
		this.commandLine = commandLine;
		this.fileHandling = new FileHandling(osSupport);
	}

	/**
	 * Configure to be a development based build, this includes files from the 'dev/'
	 * directory then.
	 */
	public void setDevBuild(boolean devBuild)
	{
		this.devBuild = devBuild;
	}

	/**
	 * true if the target artefact exists - but it maybe out of date.
	 */
	public boolean isTargetExecutableArtefactPresent()
	{
		return getTargetExecutableArtefact().exists();
	}

	/**
	 * true if the target executable exists and is newer than any of the
	 * files that would make up this artefact. i.e. compilable ek9 files or other resources.
	 */
	public boolean isTargetExecutableArtefactCurrent()
	{
		return isTargetExecutableArtefactPresent() && getIncrementalCompilableProjectFiles().size() == 0;
	}

	/**
	 * Might not actually exist, but here is a handle to it.
	 *
	 * @return The File reference to the target executable artefact (jar file for java).
	 */
	public File getTargetExecutableArtefact()
	{
		return fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(), commandLine.getTargetArchitecture());
	}

	/**
	 * Supplies a list of standard file to include.
	 * These are typically **.ek9, **.properties, etc.
	 * Note uses GLOB format.
	 */
	public List<String> getStandardIncludes()
	{
		return Arrays.asList("**.ek9",
				"**.properties",
				"**.png",
				"**.gif",
				"**.js",
				"**.jpg",
				"**.css");
	}

	/**
	 * Supplies a list of standard files to exclude.
	 * These are typically 'dot' directories and files.
	 * Note uses GLOB format.
	 */
	public List<String> getStandardExcludes()
	{
		List<String> directoriesToIgnore = Arrays.asList(".ek9/",
				"**.ek9/**",
				".git",
				"**.git/**",
				".settings",
				"**.settings/**"
		);

		List<String> filesToIgnore = Arrays.asList("**.gitignore",
				"**.project",
				"**.classpath"
		);
		return Stream.of(directoriesToIgnore, filesToIgnore).flatMap(List::stream).collect(Collectors.toList());
	}

	/**
	 * Supplies a list of all ek9 compilable source files that have been modified after the
	 * target executable has been created. i.e. what source file has been modified that should trigger a new build.
	 */
	public List<File> getIncrementalFilesPartOfBuild()
	{
		File targetArtefact = getTargetExecutableArtefact();
		if(targetArtefact.exists())
			return filterListBy(getAllFilesPartOfBuild(), file -> file.lastModified() >= targetArtefact.lastModified());
		return getAllFilesPartOfBuild();
	}

	/**
	 * Supplies a list of all project files that have been modified after the
	 * target executable has been created. i.e. what file has been modified that should trigger a new build.
	 */
	public List<File> getIncrementalCompilableProjectFiles()
	{
		File targetArtefact = getTargetExecutableArtefact();
		if(targetArtefact.exists())
			return filterListBy(getAllCompilableProjectFiles(), file -> file.lastModified() >= targetArtefact.lastModified());
		return getAllCompilableProjectFiles();
	}

	/**
	 * Supplies a list of all files that are compilable by the ek9 compiler.
	 * This also applies any includes and exclude directives from any 'package' construct.
	 * Honours the idea of including or excluding 'dev/' files.
	 */
	public List<File> getAllCompilableProjectFiles()
	{
		return filterListBy(getAllFilesPartOfBuild(), file -> file.getPath().endsWith(".ek9"));
	}

	/**
	 * Supplies a list of all files that are not compilable by the ek9 compiler.
	 * This also applies any includes and exclude directives from any 'package' construct.
	 * Honours the idea of including or excluding 'dev/' files.
	 */
	public List<File> getAllNonCompilableProjectFiles()
	{
		//Now need to filter for files not ending with just only ek9 files
		return filterListBy(getAllFilesPartOfBuild(), file -> !file.getPath().endsWith(".ek9"));
	}

	/**
	 * Supplies the list of all files that are to be part of the build.
	 * If this is a 'dev' build then the 'dev/' directory is also included.
	 * This also applies any includes and exclude directives from any 'package' construct.
	 */
	public List<File> getAllFilesPartOfBuild()
	{
		if(devBuild)
			return getPackageFiles();

		//Need to exclude files starting with "dev/"
		File fromDirectory = new File(commandLine.getSourceFileDirectory());
		return filterListBy(getPackageFiles(), file -> !fromDirectory.toPath().relativize(file.toPath()).startsWith("dev/"));
	}

	/**
	 * Uses what was specified on the command line and in any 'package' construct
	 * to determine which files should be packaged for use.
	 */
	public List<File> getPackageFiles()
	{
		if(cachedFileList != null)
			return cachedFileList;

		//Force reprocessing, so we parse actual source to get includes and excludes.
		List<String> includes = new ArrayList<>(commandLine.getIncludeFiles());
		List<String> excludes = new ArrayList<>(commandLine.getExcludeFiles());

		//Standard options is for medium projects with a large range of files.
		//Typically, all ek9 files will be included.

		//For small projects even one file - we just add in the single file name
		//The developer can then create a 'package' and list the additional files
		//Up to the point where really they need all EK9 files then move to standard options.
		if(commandLine.applyStandardIncludes())
			includes.addAll(getStandardIncludes());
		else
			includes.add(commandLine.getSourceFileName());
		if(commandLine.applyStandardExcludes())
			excludes.addAll(getStandardExcludes());

		//This will update cachedFileList.
		return getMatchingFiles(includes, excludes);
	}

	/**
	 * Gets the list of files from the directory where the source file is located recursively.
	 * But applies any includes and exclude 'Glob' specifications.
	 */
	private List<File> getMatchingFiles(List<String> includeFiles, List<String> excludeFiles)
	{
		if(cachedFileList == null)
		{
			cachedFileList = osSupport.getFilesRecursivelyFrom(new File(commandLine.getSourceFileDirectory()), new Glob(includeFiles, excludeFiles))
					.stream()
					.sorted(Comparator.comparingLong(File::lastModified).reversed())
					.collect(Collectors.toList());
		}
		return cachedFileList;
	}

	/**
	 * Just applies the predicate filter to the list.
	 * the returns a new list - which may be empty.
	 */
	private List<File> filterListBy(List<File> list, Predicate<File> predicate)
	{
		return list
				.stream()
				.filter(predicate)
				.collect(Collectors.toList());
	}
}
