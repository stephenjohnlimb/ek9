package org.ek9lang.cli;

import java.io.File;

/**
 * Install a package to your own $HOME/.ek9/lib directory.
 */
final class Ei extends E {
  Ei(final CompilationContext compilationContext) {

    super(compilationContext);

  }

  @Override
  protected String messagePrefix() {

    return "Install : ";
  }

  @Override
  protected boolean doRun() {

    log("- Package");

    if (new Ep(compilationContext).run()) {
      //Now do deployment.
      final var zipFileName = getFileHandling().makePackagedModuleZipFileName(
          compilationContext.commandLine().getModuleName(),
          compilationContext.commandLine().getVersion());
      final var fromDir = new File(getFileHandling().getDotEk9Directory(
          compilationContext.commandLine().getSourceFileDirectory()));
      final var destinationDir = getFileHandling().getUsersHomeEk9LibDirectory();

      return copyFile(fromDir, destinationDir, zipFileName)
          && copyFile(fromDir, destinationDir, zipFileName + ".sha256");
    }

    return false;
  }

  private boolean copyFile(final File fromDir, final File destinationDir, final String fileName) {

    log("Copying '" + fileName + "' from '" + fromDir.toString() + "' to '" + destinationDir + "'");
    final var rtn = getFileHandling().copy(fromDir, destinationDir, fileName);
    if (rtn) {
      log(new File(destinationDir, fileName) + " installed");
    }

    return rtn;
  }
}
