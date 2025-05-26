package org.ek9lang.cli;

/**
 * Just does an incremental build if possible.
 */
final class Eic extends Ec {
  Eic(final CompilationContext compilationContext) {

    super(compilationContext);

  }

  @Override
  protected String messagePrefix() {

    return "Compile : ";
  }

  @Override
  protected boolean doRun() {

    boolean artefactPresent = compilationContext.sourceFileCache().isTargetExecutableArtefactPresent();

    if (artefactPresent) {
      if (compilationContext.sourceFileCache().isTargetExecutableArtefactCurrent()) {
        log("Target already in date");
        return true;
      } else {
        return triggerIncrementalCompilation();
      }
    }

    return triggerFullCompilation();
  }

  private boolean triggerIncrementalCompilation() {

    prepareCompilation();
    //We still get all the compilable project files.
    //The compiler will only generate new artefacts if they are out of date.
    final var compilationSuccessful = compile(compilationContext.sourceFileCache().getAllCompilableProjectFiles());

    final var changesToPackage = compilationContext.sourceFileCache().getIncrementalFilesPartOfBuild().size();
    log(changesToPackage + " changed file(s)");

    if (compilationSuccessful) {
      return repackageTargetArtefact();
    }

    return false;
  }

  private boolean triggerFullCompilation() {

    log("Missing target - Compile!");
    final var execution = new Efc(compilationContext);
    //may have been forced in, and so we must pass on.
    execution.setCompilerFlags(getCompilerFlags());

    return execution.run();
  }
}
