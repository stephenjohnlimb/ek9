package org.ek9lang.cli;

/**
 * Clean a project down.
 */
public class Ecl extends E {
  public Ecl(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Clean   : ";
  }

  protected boolean doRun() {
    getFileHandling().cleanEk9DirectoryStructureFor(
        compilationContext.commandLine().getFullPathToSourceFileName(),
        compilationContext.commandLine().targetArchitecture);
    //Now trigger file structure and property file regeneration.
    log("- Props");
    compilationContext.commandLine().processEk9FileProperties(true);
    return true;
  }
}
