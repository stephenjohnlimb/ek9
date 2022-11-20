package org.ek9lang.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.utils.Ek9DirectoryStructure;
import org.ek9lang.core.utils.ZipSet;

/**
 * An abstract base for creating the target artefact.
 * Normally extended by compile commands.
 */
public abstract class Ec extends E {
  protected Ec(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine, sourceFileCache);
  }

  protected void prepareCompilation() {
    log("Preparing");
    // Set if not already set
    if (!isDebuggingInstrumentation()) {
      setDebuggingInstrumentation(commandLine.isDebuggingInstrumentation());
    }
    if (!isDevBuild()) {
      setDevBuild(commandLine.isDevBuild());
    }

    if (!isCheckCompilationOnly()) {
      setCheckCompilationOnly(commandLine.isCheckCompileOnly());
    }

    if (isDebuggingInstrumentation()) {
      log("Instrumenting");
    }
    if (isDevBuild()) {
      log("Development");
    }
  }

  protected boolean compile(List<File> compilableProjectFiles) {
    log(compilableProjectFiles.size() + " source file(s)");

    //Do the actual compilation!

    //At present if in log mode show the list of files to be compiled.
    compilableProjectFiles.forEach(file -> log(file.getAbsolutePath()));

    //Do compile these files with appropriate compiler

    //Do make sure we pass in this.isCheckCompilationOnly()

    var generatedOutputDirectory = getMainGeneratedOutputDirectory();
    AssertValue.checkNotNull("Main generated out file null", generatedOutputDirectory);
    //This will be some sort of intermediate form (i.e. java we then need to actually compile).

    if (this.isDevBuild()) {
      var devGeneratedOutputDirectory = getDevGeneratedOutputDirectory();
      AssertValue.checkNotNull("Dev generated out file null", devGeneratedOutputDirectory);
      //This will be some sort of intermediate form (i.e. java we then need to actually compile).
    }
    return true; //or false if compilation failed
  }

  protected boolean repackageTargetArtefact() {

    if (this.isCheckCompilationOnly()) {
      log("Check Compilation so NOT creating target");
      return true;
    }

    boolean rtn = Objects.equals(commandLine.targetArchitecture, Ek9DirectoryStructure.JAVA);

    //We can only build a jar for java at present.
    if (rtn) {
      log("Creating target");

      List<ZipSet> zipSets = new ArrayList<>();
      addProjectResources(zipSets);
      addClassesFrom(getMainFinalOutputDirectory(), zipSets);
      //Do go through the deps and locate the jar file for each dependency and pull that in.

      if (super.isDevBuild()) {
        addClassesFrom(getDevFinalOutputDirectory(), zipSets);
        //Do go through the dev-deps and locate the jar file for each dependency and pull that in.
      }

      //The parts of the EK9 runtime that we need to package in the jar.
      zipSets.add(getCoreComponents());

      String targetFileName = sourceFileCache.getTargetExecutableArtefact().getAbsolutePath();
      rtn = getFileHandling().createJar(targetFileName, zipSets);
    }

    return rtn;
  }

  /**
   * This will be the stock set of runtime code that we need to bundle.
   */
  private ZipSet getCoreComponents() {
    return new ZipSet();
  }

  private void addClassesFrom(File classesDir, List<ZipSet> zipSetList) {
    log("Including classes from " + classesDir.getAbsolutePath());
    List<File> listOfFiles = getOsSupport().getFilesRecursivelyFrom(classesDir);
    zipSetList.add(new ZipSet(classesDir.toPath(), listOfFiles));
  }

  private void addProjectResources(List<ZipSet> zipSetList) {
    File projectDirectory = new File(commandLine.getSourceFileDirectory());
    Path fromPath = projectDirectory.toPath();
    List<File> listOfFiles = sourceFileCache.getAllNonCompilableProjectFiles();
    zipSetList.add(new ZipSet(fromPath, listOfFiles));
  }
}