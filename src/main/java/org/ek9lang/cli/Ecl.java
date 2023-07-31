package org.ek9lang.cli;

/**
 * Clean a project down.
 */
final class Ecl extends E {
  Ecl(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Clean   : ";
  }

  @Override
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
