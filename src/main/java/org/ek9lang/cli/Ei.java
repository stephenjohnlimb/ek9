package org.ek9lang.cli;

import java.io.File;

/**
 * Install a package to your own $HOME/.ek9/lib directory.
 */
public class Ei extends E {
  public Ei(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Install : ";
  }

  protected boolean doRun() {
    log("- Package");

    if (new Ep(compilationContext).run()) {
      //Now do deployment.
      String zipFileName =
          getFileHandling().makePackagedModuleZipFileName(
              compilationContext.commandLine().getModuleName(),
              compilationContext.commandLine().getVersion());
      File fromDir =
          new File(getFileHandling().getDotEk9Directory(
              compilationContext.commandLine().getSourceFileDirectory()));
      File destinationDir = getFileHandling().getUsersHomeEk9LibDirectory();

      return copyFile(fromDir, destinationDir, zipFileName)
          && copyFile(fromDir, destinationDir, zipFileName + ".sha256");
    }
    return false;
  }

  private boolean copyFile(File fromDir, File destinationDir, String fileName) {
    log("Copying '" + fileName + "' from '" + fromDir.toString() + "' to '" + destinationDir + "'");
    boolean rtn = getFileHandling().copy(fromDir, destinationDir, fileName);
    if (rtn) {
      log(new File(destinationDir, fileName) + " installed");
    }
    return rtn;
  }
}
