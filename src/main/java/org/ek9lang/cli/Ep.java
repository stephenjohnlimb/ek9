package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.Digest;
import org.ek9lang.core.utils.OsSupport;
import org.ek9lang.core.utils.ZipSet;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Do packaging for package / all packages inside the project directory.
 */
public class Ep extends E
{
	public Ep(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	public boolean run()
	{
		//Now do packaging
		log("Package: Prepare");

		if(!commandLine.isPackagePresent())
		{
			report("File " + commandLine.getSourceFileName() + " does not define a package");
			return false;
		}

		fileHandling.deleteStalePackages(commandLine.getSourceFileDirectory(), commandLine.getModuleName());

		File projectDirectory = new File(commandLine.getSourceFileDirectory());
		Path fromPath = projectDirectory.toPath();
		List<File> listOfFiles = sourceFileCache.getPackageFiles();

		if(commandLine.isVerbose())
			listOfFiles.forEach(file -> log("Zip: " + file.toString()));

		String zipFileName = fileHandling.makePackagedModuleZipFileName(commandLine.getModuleName(), commandLine.getVersion().toString());
		String fileName = fileHandling.getDotEK9Directory(commandLine.getSourceFileDirectory()) + zipFileName;

		if(fileHandling.createZip(fileName, new ZipSet(fromPath, listOfFiles), commandLine.getSourcePropertiesFile()))
		{
			log("Package: Complete");

			log("Package: Check summing");
			Digest.CheckSum checkSum = fileHandling.createSha256Of(fileName);
			log("Package: " + fileName + " created, checksum " + checkSum);

			return true;
		}
		return false;
	}
}
