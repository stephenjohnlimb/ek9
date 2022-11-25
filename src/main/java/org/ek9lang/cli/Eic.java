package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Just does an incremental build if possible.
 */
public class Eic extends Ec {
  public Eic(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine, sourceFileCache);
  }

  @Override
  protected String messagePrefix() {
    return "Compile : ";
  }

  protected boolean doRun() {
    boolean rtn = sourceFileCache.isTargetExecutableArtefactPresent();
    //Check if it even exists - if not then that is a full build that is needed.
    if (!rtn) {
      log("Missing target - Compile!");
      Efc execution = new Efc(commandLine, sourceFileCache);
      //may have been forced in, and so we must pass on.
      execution.setCompilerFlags(getCompilerFlags());
      rtn = execution.run();
    } else {
      //Yes we can do an incremental build.
      if (sourceFileCache.isTargetExecutableArtefactCurrent()) {
        log("Target already in date");
      } else {
        prepareCompilation();

        //We still get all the compilable project files.
        //The compiler will only generate new artefacts if they are out of date.
        rtn = compile(sourceFileCache.getAllCompilableProjectFiles());

        int changesToPackage = sourceFileCache.getIncrementalFilesPartOfBuild().size();
        log(changesToPackage + " changed file(s)");

        if (rtn) {
          rtn = repackageTargetArtefact();
        }
      }
    }
    return rtn;
  }
}
