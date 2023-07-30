package org.ek9lang.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.Ek9DirectoryStructure;
import org.ek9lang.core.ZipSet;

/**
 * HERE FOR COMPILER ENTRY.
 * An abstract base for creating the target artefact.
 * Normally extended by compile commands.
 */
public abstract class Ec extends E {

  private CompilerFlags compilerFlags = new CompilerFlags(CompilationPhase.APPLICATION_PACKAGING);

  protected Ec(CompilationContext compilationContext) {
    super(compilationContext);
    compilerFlags.setVerbose(compilationContext.commandLine().isVerbose());
  }

  protected void prepareCompilation() {
    log("Preparing");
    // Set if not already set
    if (!compilerFlags.isDebuggingInstrumentation()) {
      setDebuggingInstrumentation(compilationContext.commandLine().isDebuggingInstrumentation());
    }
    if (!compilerFlags.isDevBuild()) {
      setDevBuild(compilationContext.commandLine().isDevBuild());
    }

    if (!compilerFlags.isCheckCompilationOnly()) {
      setCheckCompilationOnly(compilationContext.commandLine().isCheckCompileOnly());
    }

    if (compilationContext.commandLine().isPhasedCompileOnly()) {
      final CompilationPhase requiredPhase = compilationContext.commandLine().isDevBuild()
          ? CompilationPhase.valueOf(compilationContext.commandLine().getOptionParameter("-Cdp")) :
          CompilationPhase.valueOf(compilationContext.commandLine().getOptionParameter("-Cp"));
      setPhaseToCompileTo(requiredPhase);
    }

    if (compilerFlags.isDebuggingInstrumentation()) {

      log("Instrumenting");
    }
    if (compilerFlags.isDevBuild()) {

      log("Development");
    }
  }

  /**
   * HERE FOR COMPILER ENTRY.
   * This is the key entry point into the CLI compile process.
   * This creates a workspace with all the required files.
   * Then it uses the compiler from the compilationContext and flags
   * to trigger the compile process.
   */
  protected boolean compile(List<File> compilableProjectFiles) {
    log(compilableProjectFiles.size() + " source file(s)");

    //Do the actual compilation!
    Workspace workspace = new Workspace();

    //Show the list of files to be compiled (if in verbose mode).
    //Add to the workspace the compiler will use
    compilableProjectFiles.forEach(file -> {
      log(file.getAbsolutePath());
      workspace.addSource(file);
    });

    /*
     * HERE FOR COMPILER ENTRY.
     * HERE for triggering the compilation of the workspace
     */
    var compilationResult = compilationContext.compiler().compile(workspace, compilerFlags);

    if (compilationResult) {
      var generatedOutputDirectory = getMainGeneratedOutputDirectory();
      AssertValue.checkNotNull("Main generated out file null", generatedOutputDirectory);
      //This will be some sort of intermediate form (i.e. java we then need to actually compile).

      if (compilerFlags.isDevBuild()) {
        var devGeneratedOutputDirectory = getDevGeneratedOutputDirectory();
        AssertValue.checkNotNull("Dev generated out file null", devGeneratedOutputDirectory);
        //This will be some sort of intermediate form (i.e. java we then need to actually compile).
      }
    }

    return compilationResult; //or false if compilation failed
  }

  protected boolean repackageTargetArtefact() {

    if (compilerFlags.isCheckCompilationOnly()) {
      log("Check Compilation so NOT creating target");
      return true;
    }

    boolean rtn = Objects.equals(compilationContext.commandLine().targetArchitecture,
        Ek9DirectoryStructure.JAVA);

    //We can only build a jar for java at present.
    if (rtn) {
      log("Creating target");

      List<ZipSet> zipSets = new ArrayList<>();
      addProjectResources(zipSets);
      addClassesFrom(getMainFinalOutputDirectory(), zipSets);
      //Do go through the deps and locate the jar file for each dependency and pull that in.

      if (compilerFlags.isDevBuild()) {
        addClassesFrom(getDevFinalOutputDirectory(), zipSets);
        //Do go through the dev-deps and locate the jar file for each dependency and pull that in.
      }

      //The parts of the EK9 runtime that we need to package in the jar.
      zipSets.add(getCoreComponents());

      String targetFileName =
          compilationContext.sourceFileCache().getTargetExecutableArtefact().getAbsolutePath();
      rtn = getFileHandling().createJar(targetFileName, zipSets);
    }

    return rtn;
  }

  public void setDebuggingInstrumentation(boolean debuggingInstrumentation) {
    compilerFlags.setDebuggingInstrumentation(debuggingInstrumentation);
  }

  public void setDevBuild(boolean devBuild) {
    compilerFlags.setDevBuild(devBuild);
    compilationContext.sourceFileCache().setDevBuild(devBuild);
  }

  public void setCheckCompilationOnly(boolean checkCompilationOnly) {
    compilerFlags.setCheckCompilationOnly(checkCompilationOnly);
  }

  public void setPhaseToCompileTo(CompilationPhase phaseToCompileTo) {
    compilerFlags.setCompileToPhase(phaseToCompileTo);
  }

  public CompilerFlags getCompilerFlags() {
    return compilerFlags;
  }

  public void setCompilerFlags(CompilerFlags compilerFlags) {
    this.compilerFlags = compilerFlags;
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
    File projectDirectory = new File(compilationContext.commandLine().getSourceFileDirectory());
    Path fromPath = projectDirectory.toPath();
    List<File> listOfFiles = compilationContext.sourceFileCache().getAllNonCompilableProjectFiles();
    zipSetList.add(new ZipSet(fromPath, listOfFiles));
  }
}