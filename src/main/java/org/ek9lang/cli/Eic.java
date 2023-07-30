package org.ek9lang.cli;

/**
 * Just does an incremental build if possible.
 */
public class Eic extends Ec {
  public Eic(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Compile : ";
  }

  protected boolean doRun() {
    boolean rtn = compilationContext.sourceFileCache().isTargetExecutableArtefactPresent();
    //Check if it even exists - if not then that is a full build that is needed.
    if (!rtn) {
      log("Missing target - Compile!");
      Efc execution = new Efc(compilationContext);
      //may have been forced in, and so we must pass on.
      execution.setCompilerFlags(getCompilerFlags());
      rtn = execution.run();
    } else {
      //Yes we can do an incremental build.
      if (compilationContext.sourceFileCache().isTargetExecutableArtefactCurrent()) {
        log("Target already in date");
      } else {
        prepareCompilation();

        //We still get all the compilable project files.
        //The compiler will only generate new artefacts if they are out of date.
        rtn = compile(compilationContext.sourceFileCache().getAllCompilableProjectFiles());

        int changesToPackage =
            compilationContext.sourceFileCache().getIncrementalFilesPartOfBuild().size();
        log(changesToPackage + " changed file(s)");

        if (rtn) {
          rtn = repackageTargetArtefact();
        }
      }
    }
    return rtn;
  }
}
