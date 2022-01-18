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
import java.util.stream.Collectors;

/**
 * Because we need access to the full file list or partial file list filtered.
 * We need to load up references to the files once and then filter as needed.
 * Obviously for a Language Server this would be done with listeners and watchers.
 * But for a command line process - we have to reload all the time.
 *
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

    public void setDevBuild(boolean devBuild)
    {
        this.devBuild = devBuild;
    }

    public boolean isTargetExecutableArtefactPresent()
    {
        return getTargetExecutableArtefact().exists();
    }

    public boolean isTargetExecutableArtefactCurrent()
    {
        File target = getTargetExecutableArtefact();
        if(!target.exists())
            return false;
        return getIncrementalFilesPartOfBuild().size() == 0;
    }

    /**
     * Might not actually exist, but here is a handle to it.
     * @return The File reference to the target executable artefact (jar file for java).
     */
    public File getTargetExecutableArtefact()
    {
			return fileHandling.getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(), commandLine.getTargetArchitecture());
    }

    protected List<File> getFiles(List<String> includeFiles, List<String> excludeFiles)
    {
        if(cachedFileList == null)
        {
            Glob searchCondition = new Glob(includeFiles, excludeFiles);
            cachedFileList = osSupport.getFilesRecursivelyFrom(new File(commandLine.getFullPathToSourceFileName()), searchCondition);
            cachedFileList.sort(Comparator.comparingLong(File::lastModified).reversed());
        }
        return cachedFileList;
    }

    protected List<File> getPackageFiles()
    {
        if(cachedFileList == null)
        {
            //Force reprocessing, so we parse actual source to get includes and excludes.
            List<String> includes = new ArrayList<>(commandLine.getIncludeFiles());
            List<String> excludes = new ArrayList<>(commandLine.getExcludeFiles());

            //Standard options is for medium projects with a large range of files.
            //Typically, all ek9 files will be included.

            //For small projects even one file - we just add in the single file name
            //The developer can then create a 'package' and list the additional files
            //Up to the point where really they need all EK9 files then move to standard options.
            if (commandLine.applyStandardIncludes())
                includes.addAll(getStandardIncludes());
            else
                includes.add(commandLine.getSourceFileName());
            if (commandLine.applyStandardExcludes())
                excludes.addAll(getStandardExcludes());
            return getFiles(includes, excludes);
        }
        return cachedFileList;
    }

    protected List<String> getStandardIncludes()
    {
        return Arrays.asList("**.ek9", "**.properties", "**.png", "**.gif", "**.js", "**.jpg", "**.css");
    }

    protected List<String> getStandardExcludes()
    {
        List<String> directoriesToIgnore = Arrays.asList(".ek9/", "**.ek9/**", ".git", "**.git/**", ".settings", "**.settings/**");
        List<String> filesToIgnore = Arrays.asList("**.gitignore", "**.project", "**.classpath");

        List<String> rtn = new ArrayList<>();
        rtn.addAll(directoriesToIgnore);
        rtn.addAll(filesToIgnore);
        return rtn;
    }

    protected List<File> getIncrementalFilesPartOfBuild()
    {
        File targetArtefact = getTargetExecutableArtefact();
        List<File> returnList = getAllFilesPartOfBuild();
        returnList = returnList.stream().filter(file -> file.lastModified() >= targetArtefact.lastModified()).collect(Collectors.toList());

        return returnList;
    }

    protected List<File> getAllFilesPartOfBuild()
    {
        File fromDirectory = new File(commandLine.getSourceFileDirectory());
        List<File> packageList = getPackageFiles();

        if(!devBuild)
        {
            //Need to exclude files starting with "dev/"
            packageList = packageList.stream().filter(file -> !fromDirectory.toPath().relativize(file.toPath()).startsWith("dev/")).collect(Collectors.toList());
        }
        return packageList;
    }

    protected List<File> getAllCompilableProjectFiles()
    {
        List<File> returnList = getAllFilesPartOfBuild();
        //Now need to filter for files ending with just only ek9 files
        returnList = returnList.stream().filter(file -> file.getPath().endsWith(".ek9")).collect(Collectors.toList());

        return returnList;
    }

    protected List<File> getAllNonCompilableProjectFiles()
    {
        List<File> returnList = getAllFilesPartOfBuild();
        //Now need to filter for files not ending with just only ek9 files
        returnList = returnList.stream().filter(file -> !file.getPath().endsWith(".ek9")).collect(Collectors.toList());

        return returnList;
    }

    protected List<File> getIncrementalCompilableProjectFiles()
    {
        File targetArtefact = getTargetExecutableArtefact();
        List<File> returnList = getAllCompilableProjectFiles();

        //Now need to filter for files modified after target
        returnList = returnList.stream().filter(file -> file.lastModified() >= targetArtefact.lastModified()).collect(Collectors.toList());

        return returnList;
    }
}
