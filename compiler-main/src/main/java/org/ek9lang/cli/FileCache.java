package org.ek9lang.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.ek9lang.core.Glob;

/**
 * Because we need access to the full file list or partial file list filtered.
 * We need to load up references to the files once and then filter as needed.
 * Obviously for a Language Server this would be done with listeners and watchers.
 * But for a command line process - we have to reload all the time.
 * This enables us to keep the base set of full files and then provide cut down
 * lists depending on what command is to be used.
 */
final class FileCache {

  private final CommandLine commandLine;

  /**
   * We hold dev-build here, because we may have forced it, it was not in compiler command line.
   */
  private boolean devBuild;
  private List<File> cachedFileList = null;

  /**
   * Create a new FileCache referencing a specific commandLine.
   */
  FileCache(final CommandLine commandLine) {

    this.commandLine = commandLine;
    this.devBuild = commandLine.options().isDevBuild();

  }

  /**
   * Configure to be a development based build, this includes files from the 'dev/'
   * directory then.
   */
  void setDevBuild(final boolean devBuild) {

    this.devBuild = devBuild;

  }

  /**
   * true if the target artefact exists - but it maybe out of date.
   */
  boolean isTargetExecutableArtefactPresent() {

    return getTargetExecutableArtefact().exists();
  }

  /**
   * true if the target executable exists and is newer than any of the
   * files that would make up this artefact. i.e. compilable ek9 files or other resources.
   */
  boolean isTargetExecutableArtefactCurrent() {

    return isTargetExecutableArtefactPresent() && getIncrementalCompilableProjectFiles().isEmpty();
  }

  /**
   * Might not actually exist, but here is a handle to it.
   *
   * @return The File reference to the target executable artefact (jar file for java).
   */
  File getTargetExecutableArtefact() {

    return commandLine.getFileHandling()
        .getTargetExecutableArtefact(commandLine.getFullPathToSourceFileName(),
            commandLine.getTargetArchitecture());
  }

  /**
   * Removes any final target executable that has been generated.
   */
  void deleteTargetExecutableArtefact() {

    commandLine.getFileHandling().deleteFileIfExists(getTargetExecutableArtefact());
  }

  /**
   * Supplies a list of standard file to include.
   * These are typically **.ek9, **.properties, etc.
   * Note uses GLOB format.
   */
  List<String> getStandardIncludes() {

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
  List<String> getStandardExcludes() {

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

    return Stream.of(directoriesToIgnore, filesToIgnore).flatMap(List::stream).toList();
  }

  /**
   * Supplies a list of all ek9 compilable source files that have been modified after the
   * target executable has been created. i.e. what source file has been modified that should
   * trigger a new build.
   */
  List<File> getIncrementalFilesPartOfBuild() {

    final var targetArtefact = getTargetExecutableArtefact();
    if (targetArtefact.exists()) {
      return filterListBy(getAllFilesPartOfBuild(),
          file -> file.lastModified() >= targetArtefact.lastModified());
    }

    return getAllFilesPartOfBuild();
  }

  /**
   * Supplies a list of all project files that have been modified after the
   * target executable has been created. i.e. what file has been modified that should
   * trigger a new build.
   */
  List<File> getIncrementalCompilableProjectFiles() {

    final var targetArtefact = getTargetExecutableArtefact();
    if (targetArtefact.exists()) {
      return filterListBy(getAllCompilableProjectFiles(),
          file -> file.lastModified() >= targetArtefact.lastModified());
    }

    return getAllCompilableProjectFiles();
  }

  /**
   * Supplies a list of all files that are compilable by the ek9 compiler.
   * This also applies any includes and exclude directives from any 'package' construct.
   * Honours the idea of including or excluding 'dev/' files.
   */
  List<File> getAllCompilableProjectFiles() {

    return filterListBy(getAllFilesPartOfBuild(), file -> file.getPath().endsWith(".ek9"));
  }

  /**
   * Supplies a list of all files that are not compilable by the ek9 compiler.
   * This also applies any includes and exclude directives from any 'package' construct.
   * Honours the idea of including or excluding 'dev/' files.
   */
  List<File> getAllNonCompilableProjectFiles() {

    //Now need to filter for files not ending with just only ek9 files
    return filterListBy(getAllFilesPartOfBuild(), file -> !file.getPath().endsWith(".ek9"));
  }

  /**
   * Supplies the list of all files that are to be part of the build.
   * If this is a 'dev' build then the 'dev/' directory is also included.
   * This also applies any includes and exclude directives from any 'package' construct.
   */
  List<File> getAllFilesPartOfBuild() {

    if (devBuild) {
      return getPackageFiles();
    }

    //Need to exclude files starting with "dev/"
    final var fromDirectory = new File(commandLine.getSourceFileDirectory());

    return filterListBy(getPackageFiles(),
        file -> !fromDirectory.toPath().relativize(file.toPath()).startsWith("dev/"));
  }

  /**
   * Uses what was specified on the command line and in any 'package' construct
   * to determine which files should be packaged for use.
   */
  List<File> getPackageFiles() {

    if (cachedFileList != null) {
      return cachedFileList;
    }

    //Force reprocessing, so we parse actual source to get includes and excludes.
    final List<String> includes = new ArrayList<>(commandLine.getIncludeFiles());
    final List<String> excludes = new ArrayList<>(commandLine.getExcludeFiles());

    //Standard options is for medium projects with a large range of files.
    //Typically, all ek9 files will be included.

    //For small projects even one file - we just add in the single file name
    //The developer can then create a 'package' and list the additional files
    //Up to the point where really they need all EK9 files then move to standard options.
    if (commandLine.applyStandardIncludes()) {
      includes.addAll(getStandardIncludes());
    } else {
      includes.add(commandLine.getSourceFileName());
    }

    if (commandLine.applyStandardExcludes()) {
      excludes.addAll(getStandardExcludes());
    }

    //This will update cachedFileList.
    return getMatchingFiles(includes, excludes);
  }

  /**
   * Gets the list of files from the directory where the source file is located recursively.
   * But applies any includes and exclude 'Glob' specifications.
   */
  private List<File> getMatchingFiles(final List<String> includeFiles, final List<String> excludeFiles) {

    if (cachedFileList == null) {
      cachedFileList = commandLine.getOsSupport()
          .getFilesRecursivelyFrom(new File(commandLine.getSourceFileDirectory()),
              new Glob(includeFiles, excludeFiles))
          .stream()
          .sorted(Comparator.comparingLong(File::lastModified).reversed())
          .toList();
    }

    return cachedFileList;
  }

  /**
   * Just applies the predicate filter to the list.
   * the returns a new list - which may be empty.
   */
  private List<File> filterListBy(final List<File> list, final Predicate<File> predicate) {

    return list
        .stream()
        .filter(predicate)
        .toList();
  }
}
