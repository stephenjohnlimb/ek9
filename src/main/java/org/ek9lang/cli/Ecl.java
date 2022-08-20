package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Clean a project down.
 */
public class Ecl extends E {
  public Ecl(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine, sourceFileCache);
  }

  @Override
  protected String messagePrefix() {
    return "Clean   : ";
  }

  protected boolean doRun() {
    getFileHandling().cleanEk9DirectoryStructureFor(commandLine.getFullPathToSourceFileName(),
        commandLine.targetArchitecture);
    //Now trigger file structure and property file regeneration.
    log("- Props");
    commandLine.processEk9FileProperties(true);
    return true;
  }
}
