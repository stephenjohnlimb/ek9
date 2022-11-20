package org.ek9lang.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.Digest;
import org.ek9lang.core.utils.ZipSet;

/**
 * Do packaging for package / all packages inside the project directory.
 */
public class Ep extends E {
  public Ep(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine, sourceFileCache);
  }

  @Override
  protected String messagePrefix() {
    return "Package : ";
  }

  @Override
  public boolean preConditionCheck() {
    if (commandLine.noPackageIsPresent()) {
      report("File " + commandLine.getSourceFileName() + " does not define a package");
      return false;
    }
    return super.preConditionCheck();
  }

  protected boolean doRun() {
    log("- Compile!");
    //Need to ensure a full compile works.
    Efc execution = new Efc(commandLine, sourceFileCache);

    if (!execution.run()) {
      report("Failed");
    } else {
      getFileHandling().deleteStalePackages(commandLine.getSourceFileDirectory(),
          commandLine.getModuleName());

      File projectDirectory = new File(commandLine.getSourceFileDirectory());
      Path fromPath = projectDirectory.toPath();
      List<File> listOfFiles = sourceFileCache.getPackageFiles();

      if (commandLine.isVerbose()) {
        listOfFiles.forEach(file -> log("Zip: " + file.toString()));
      }

      String zipFileName =
          getFileHandling().makePackagedModuleZipFileName(commandLine.getModuleName(),
              commandLine.getVersion());
      String fileName =
          getFileHandling().getDotEk9Directory(commandLine.getSourceFileDirectory()) + zipFileName;

      if (getFileHandling().createZip(fileName, new ZipSet(fromPath, listOfFiles),
          commandLine.getSourcePropertiesFile())) {
        log("Check summing");
        Digest.CheckSum checkSum = getFileHandling().createSha256Of(fileName);
        log(fileName + " created, checksum " + checkSum);
        return true;
      }
    }
    return false;
  }
}