package org.ek9lang.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.ek9lang.core.Digest;
import org.ek9lang.core.ZipSet;

/**
 * Do packaging for package / all packages inside the project directory.
 */
final class Ep extends E {
  Ep(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Package : ";
  }

  @Override
  boolean preConditionCheck() {
    if (compilationContext.commandLine().noPackageIsPresent()) {
      report("File " + compilationContext.commandLine().getSourceFileName()
          + " does not define a package");
      return false;
    }
    return super.preConditionCheck();
  }

  @Override
  protected boolean doRun() {
    log("- Compile!");
    //Need to ensure a full compile works.
    Efc execution = new Efc(compilationContext);

    if (!execution.run()) {
      report("Failed");
    } else {
      getFileHandling().deleteStalePackages(
          compilationContext.commandLine().getSourceFileDirectory(),
          compilationContext.commandLine().getModuleName());

      File projectDirectory = new File(compilationContext.commandLine().getSourceFileDirectory());
      Path fromPath = projectDirectory.toPath();
      List<File> listOfFiles = compilationContext.sourceFileCache().getPackageFiles();

      if (compilationContext.commandLine().isVerbose()) {
        listOfFiles.forEach(file -> log("Zip: " + file.toString()));
      }

      String zipFileName =
          getFileHandling().makePackagedModuleZipFileName(
              compilationContext.commandLine().getModuleName(),
              compilationContext.commandLine().getVersion());
      String fileName =
          getFileHandling().getDotEk9Directory(
              compilationContext.commandLine().getSourceFileDirectory()) + zipFileName;

      if (getFileHandling().createZip(fileName, new ZipSet(fromPath, listOfFiles),
          compilationContext.commandLine().getSourcePropertiesFile())) {
        log("Check summing");
        Digest.CheckSum checkSum = getFileHandling().createSha256Of(fileName);
        log(fileName + " created, checksum " + checkSum);
        return true;
      }
    }
    return false;
  }
}