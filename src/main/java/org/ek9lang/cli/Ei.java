package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

import java.io.File;

/**
 * Install a package to your own $HOME/.ek9/lib directory.
 */
public class Ei extends E
{
	public Ei(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Install : ";
	}

	protected boolean doRun()
	{
		log("- Package");

		if(new Ep(commandLine, sourceFileCache).run())
		{
			//Now do deployment.
			String zipFileName = getFileHandling().makePackagedModuleZipFileName(commandLine.getModuleName(), commandLine.getVersion());
			File fromDir = new File(getFileHandling().getDotEK9Directory(commandLine.getSourceFileDirectory()));
			File destinationDir = getFileHandling().getUsersHomeEK9LibDirectory();

			return copyFile(fromDir, destinationDir, zipFileName) && copyFile(fromDir, destinationDir, zipFileName + ".sha256");
		}
		return false;
	}

	private boolean copyFile(File fromDir, File destinationDir, String fileName)
	{
		log("Copying '" + fileName + "' from '" + fromDir.toString() + "' to '" + destinationDir + "'");
		boolean rtn = getFileHandling().copy(fromDir, destinationDir, fileName);
		if(rtn)
			log("" + new File(destinationDir, fileName) + " installed");
		return rtn;
	}
}
