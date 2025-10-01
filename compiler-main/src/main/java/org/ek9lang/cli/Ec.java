package org.ek9lang.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.TargetArchitecture;
import org.ek9lang.core.ZipSet;

/**
 * HERE FOR {@link org.ek9lang.compiler.Ek9Compiler} (COMPILER) ENTRY.
 * An abstract base for creating the target artefact.
 * Normally extended by compile commands.
 */
abstract class Ec extends E {

  private CompilerFlags compilerFlags = new CompilerFlags(CompilationPhase.APPLICATION_PACKAGING);

  Ec(final CompilationContext compilationContext) {

    super(compilationContext);
    compilerFlags.setVerbose(compilationContext.commandLine().options().isVerbose());
    compilerFlags.setTargetArchitecture(compilationContext.commandLine().getTargetArchitecture());

  }

  void setDebuggingInstrumentation(final boolean debuggingInstrumentation) {

    compilerFlags.setDebuggingInstrumentation(debuggingInstrumentation);

  }

  void setDevBuild(final boolean devBuild) {

    compilerFlags.setDevBuild(devBuild);
    compilationContext.sourceFileCache().setDevBuild(devBuild);

  }

  void setCheckCompilationOnly(final boolean checkCompilationOnly) {

    compilerFlags.setCheckCompilationOnly(checkCompilationOnly);

  }

  void setPhaseToCompileTo(final CompilationPhase phaseToCompileTo) {

    compilerFlags.setCompileToPhase(phaseToCompileTo);

  }

  CompilerFlags getCompilerFlags() {

    return compilerFlags;
  }

  void setCompilerFlags(final CompilerFlags compilerFlags) {

    this.compilerFlags = compilerFlags;

  }

  protected void prepareCompilation() {

    log("Preparing");
    // Set if not already set
    if (!compilerFlags.isDebuggingInstrumentation()) {
      setDebuggingInstrumentation(compilationContext.commandLine().options().isDebuggingInstrumentation());
    }
    if (!compilerFlags.isDevBuild()) {
      setDevBuild(compilationContext.commandLine().options().isDevBuild());
    }

    if (!compilerFlags.isCheckCompilationOnly()) {
      setCheckCompilationOnly(compilationContext.commandLine().options().isCheckCompileOnly());
    }

    if (compilationContext.commandLine().options().isPhasedCompileOnly()) {
      final var requiredPhase = compilationContext.commandLine().options().isDevBuild()
          ? CompilationPhase.valueOf(compilationContext.commandLine().options().getOptionParameter("-Cdp")) :
          CompilationPhase.valueOf(compilationContext.commandLine().options().getOptionParameter("-Cp"));
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
  protected boolean compile(final List<File> compilableProjectFiles) {

    log(compilableProjectFiles.size() + " source file(s)");

    final var workspace = new Workspace(compilationContext.commandLine().getSourceFileDirectory());
    compilableProjectFiles.forEach(file -> {
      log(file.getAbsolutePath());
      workspace.addSource(file);
    });

    /*
     * HERE FOR COMPILER ENTRY.
     * HERE for triggering the compilation of the workspace
     */
    final var compilationResult = compilationContext.compiler().compile(workspace, compilerFlags);

    if (compilationResult) {
      final var generatedOutputDirectory = getMainGeneratedOutputDirectory();
      AssertValue.checkNotNull("Main generated out file null", generatedOutputDirectory);
      //This may be some sort of intermediate form (i.e. java we then need to actually compile).

      if (compilerFlags.isDevBuild()) {
        final var devGeneratedOutputDirectory = getDevGeneratedOutputDirectory();
        AssertValue.checkNotNull("Dev generated out file null", devGeneratedOutputDirectory);
        //This may be some sort of intermediate form (i.e. java we then need to actually compile).
      }
    }

    return compilationResult; //or false if compilation failed
  }

  protected boolean repackageTargetArtefact() {

    if (compilerFlags.isCheckCompilationOnly()) {
      log("Check Compilation so NOT creating target");
      return true;
    }

    final var isJavaBuild = TargetArchitecture.JVM.equals(compilationContext.commandLine().targetArchitecture);

    //We can only build a jar for java at present.
    if (isJavaBuild) {
      log("Creating target");

      final List<ZipSet> zipSets = new ArrayList<>();
      addProjectResources(zipSets);
      addClassesFrom(getMainFinalOutputDirectory(), zipSets);
      //Do go through the deps and locate the jar file for each dependency and pull that in.

      if (compilerFlags.isDevBuild()) {
        addClassesFrom(getDevFinalOutputDirectory(), zipSets);
        //Do go through the dev-deps and locate the jar file for each dependency and pull that in.
      }

      //The parts of the EK9 runtime that we need to package in the jar.
      zipSets.add(getCoreComponents());
      final var targetFileName = compilationContext.sourceFileCache().getTargetExecutableArtefact().getAbsolutePath();

      return getFileHandling().createJar(targetFileName, zipSets, "ek9.Main");
    }

    return false;
  }

  /**
   * This will be the stock set of runtime code that we need to bundle.
   * Extracts EK9 runtime classes (org.ek9.lang, ek9 packages) into a versioned JAR
   * and returns the contents as a ZipSet for merging into the fat JAR.
   */
  private ZipSet getCoreComponents() {

    final var version = compilationContext.commandLine().getLanguageMetaData().version();
    final var projectDir = compilationContext.commandLine().getSourceFileDirectory();

    final var extractor = new org.ek9lang.core.RuntimeClassExtractor();
    final var runtimeJarOpt = extractor.extractRuntimeJar(getFileHandling(), projectDir, version);

    if (runtimeJarOpt.isPresent()) {
      log("Using runtime: " + runtimeJarOpt.get().getName());
      return extractJarContentsAsZipSet(runtimeJarOpt.get());
    }

    log("Warning: Could not extract runtime JAR");
    return new ZipSet();
  }

  /**
   * Extract the contents of a JAR file and return as ZipSet entries.
   * This allows merging the runtime JAR into the final fat JAR.
   */
  private ZipSet extractJarContentsAsZipSet(final File jarFile) {

    final var entries = new java.util.ArrayList<org.ek9lang.core.ZipBinaryContent>();

    try (final var jarStream = new java.util.jar.JarInputStream(new java.io.FileInputStream(jarFile))) {
      java.util.jar.JarEntry entry;
      while ((entry = jarStream.getNextJarEntry()) != null) {
        if (!entry.isDirectory()) {
          final var bytes = jarStream.readAllBytes();
          entries.add(new org.ek9lang.core.ZipBinaryContent(entry.getName(), bytes));
        }
        jarStream.closeEntry();
      }
    } catch (java.io.IOException e) {
      log("Warning: Could not read runtime JAR: " + e.getMessage());
    }

    return new ZipSet(entries);
  }

  private void addClassesFrom(final File classesDir, final List<ZipSet> zipSetList) {

    log("Including classes from " + classesDir.getAbsolutePath());

    final var listOfFiles = getOsSupport().getFilesRecursivelyFrom(classesDir);
    zipSetList.add(new ZipSet(classesDir.toPath(), listOfFiles));

  }

  private void addProjectResources(final List<ZipSet> zipSetList) {

    final var projectDirectory = new File(compilationContext.commandLine().getSourceFileDirectory());
    final var fromPath = projectDirectory.toPath();
    final var listOfFiles = compilationContext.sourceFileCache().getAllNonCompilableProjectFiles();

    zipSetList.add(new ZipSet(fromPath, listOfFiles));

  }
}