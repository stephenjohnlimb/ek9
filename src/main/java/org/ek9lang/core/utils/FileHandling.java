package org.ek9lang.core.utils;

import org.ek9lang.core.exception.AssertValue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Designed to abstract out all file handling for the compiler.
 */
public class FileHandling
{
	private final OsSupport osSupport;
	private final Packager packager;
	private final EK9DirectoryStructure directoryStructure;

	/**
	 * Create File Handling with the appropriately configured OS support.
	 * <p>
	 * Quite a few of these methods just delegate to OsSupport and the Packager.
	 */
	public FileHandling(OsSupport osSupport)
	{
		this.osSupport = osSupport;
		this.packager = new Packager(this);
		this.directoryStructure = new EK9DirectoryStructure(this);
	}

	public String getUsersHomeDirectory()
	{
		return osSupport.getUsersHomeDirectory();
	}

	public File getUsersHomeEK9LibDirectory()
	{
		return new File(getUsersHomeEK9Directory(), "lib");
	}

	public String getUsersHomeEK9Directory()
	{
		return getDotEK9Directory(osSupport.getUsersHomeDirectory());
	}

	public String getDotEK9Directory(File directory)
	{
		AssertValue.checkDirectoryReadable("Directory not readable", directory);
		return directory.getAbsolutePath() + File.separatorChar + ".ek9" + File.separatorChar;
	}

	public String getDotEK9Directory(String fromDirectory)
	{
		AssertValue.checkNotEmpty("FromDirectory is empty", fromDirectory);
		return getDotEK9Directory(new File(fromDirectory));
	}

	public String makePackagedModuleZipFileName(String moduleName, String version)
	{
		return makePackagedModuleZipFileName(makeDependencyVector(moduleName, version));
	}

	public String makePackagedModuleZipFileName(String dependencyVector)
	{
		AssertValue.checkNotEmpty("Dependency Vector is empty", dependencyVector);
		return dependencyVector + ".zip";
	}

	public String makeDependencyVector(String moduleName, String version)
	{
		AssertValue.checkNotEmpty("ModuleName is empty", moduleName);
		AssertValue.checkNotEmpty("Version is empty", version);
		return moduleName + "-" + version;
	}

	/**
	 * Copy a named file from a source directory to a destination directory.
	 */
	public boolean copy(File sourceDir, File destinationDir, String fileName)
	{
		//Let exception break everything here - these are precondition.
		AssertValue.checkNotEmpty("Filename empty", fileName);
		AssertValue.checkDirectoryReadable("Source directory not readable", sourceDir);
		AssertValue.checkDirectoryWritable("Destination directory not writable", destinationDir);

		try
		{
			//This should normally work - unless you're out of disk space.
			Path originalPath = new File(sourceDir, fileName).toPath();
			Path targetPath = new File(destinationDir, fileName).toPath();
			Files.copy(originalPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
		catch(Throwable th)
		{
			System.err.println("File copy failed: " + th.getMessage());
			return false;
		}
		return true;
	}

	public void deleteFileIfExists(File file)
	{
		AssertValue.checkNotNull("File cannot be null", file);
		if(file.exists())
			if(!file.delete())
				throw new RuntimeException("Unable to delete [" + file.getPath() + "]");
	}

	public void makeDirectoryIfNotExists(File directory)
	{
		AssertValue.checkNotNull("Directory cannot be null", directory);
		if(!directory.exists())
			if(!directory.mkdirs())
				throw new RuntimeException("Unable to create directory [" + directory.getPath() + "]");
	}

	/**
	 * deletes matching files
	 *
	 * @param dir             the directory to look in
	 * @param fileNamePattern The pattern regex not shell so for * use .* for .txt use \\.txt
	 */
	public void deleteMatchingFiles(File dir, String fileNamePattern)
	{
		AssertValue.checkNotNull("Dir cannot be null", dir);
		AssertValue.checkNotNull("FileNamePattern cannot be null", fileNamePattern);

		File[] files = dir.listFiles((dir1, name) -> name.matches(fileNamePattern));
		Optional.ofNullable(files)
				.map(Arrays::stream)
				.orElseGet(Stream::empty)
				.forEach(this::deleteFileIfExists);
	}

	/**
	 * Does a recursive delete from this directory and below.
	 * If includeDirectoryRoot is true then it will delete that directory as well
	 */
	public void deleteContentsAndBelow(File dir, boolean includeDirectoryRoot)
	{
		File[] files = dir.listFiles();
		if(files != null)
		{
			for(File toDelete : files)
			{
				if(toDelete.isDirectory())
					deleteContentsAndBelow(toDelete, true);
				else
					deleteFileIfExists(toDelete);
			}
		}
		if(includeDirectoryRoot)
			deleteFileIfExists(dir);
	}

	public Digest.CheckSum createSha256Of(String fileName)
	{
		File sha256File = new File(fileName + ".sha256");
		File fileToCheckSum = new File(fileName);
		Digest.CheckSum checkSum = Digest.digest(fileToCheckSum);
		checkSum.saveToFile(sha256File);
		return checkSum;
	}

	/**
	 * Create a Java jar file with a list of zip sets.
	 */
	public boolean createJar(String fileName, List<ZipSet> sets)
	{
		return packager.createJar(fileName, sets);
	}

	/**
	 * Unzips a zip file into a directory, the directory will be created if it does not exist.
	 */
	public boolean unZipFileTo(File zipFile, File unpackedDir)
	{
		return packager.unZipFileTo(zipFile, unpackedDir);
	}

	/**
	 * To be used for making the zip when publishing ek9 source to artefact server.
	 *
	 * @param fileName             The name of the zip to create
	 * @param sourcePropertiesFile The properties file that describes the package.
	 * @return true if all OK.
	 */
	public boolean createZip(String fileName, ZipSet set, File sourcePropertiesFile)
	{
		return packager.createZip(fileName, set, sourcePropertiesFile);
	}

	public File getTargetExecutableArtefact(String ek9FullPathToFileName, String targetArchitecture)
	{
		return directoryStructure.getTargetExecutableArtefact(ek9FullPathToFileName, targetArchitecture);
	}

	public File getTargetPropertiesArtefact(String ek9FullPathToFileName)
	{
		return directoryStructure.getTargetPropertiesArtefact(ek9FullPathToFileName);
	}

	public void validateHomeEK9Directory(String targetArchitecture)
	{
		validateEK9Directory(getUsersHomeEK9Directory(), targetArchitecture);
	}

	public void validateEK9Directory(String directoryName, String targetArchitecture)
	{
		directoryStructure.validateEK9Directory(directoryName, targetArchitecture);
	}

	public void cleanEK9DirectoryStructureFor(File ek9File, String targetArchitecture)
	{
		cleanEK9DirectoryStructureFor(ek9File.getPath(), targetArchitecture);
	}

	public void cleanEK9DirectoryStructureFor(String ek9FullPathToFileName, String targetArchitecture)
	{
		directoryStructure.cleanEK9DirectoryStructureFor(ek9FullPathToFileName, targetArchitecture);
	}

	public File getMainGeneratedOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		return directoryStructure.getMainGeneratedOutputDirectory(fromEK9BaseDirectory, targetArchitecture);
	}

	public File getMainFinalOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		return directoryStructure.getMainFinalOutputDirectory(fromEK9BaseDirectory, targetArchitecture);
	}

	public File getDevGeneratedOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		return directoryStructure.getDevGeneratedOutputDirectory(fromEK9BaseDirectory, targetArchitecture);
	}

	public File getDevFinalOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		return directoryStructure.getDevFinalOutputDirectory(fromEK9BaseDirectory, targetArchitecture);
	}

	public void deleteStalePackages(String ek9FileNameDirectory, String moduleName)
	{
		directoryStructure.deleteStalePackages(ek9FileNameDirectory, moduleName);
	}

	public boolean isUsersSigningKeyPairPresent()
	{
		return directoryStructure.isUsersSigningKeyPairPresent();
	}

	public SigningKeyPair getUsersSigningKeyPair()
	{
		return directoryStructure.getUsersSigningKeyPair();
	}

	public boolean saveToHomeEK9Directory(SigningKeyPair keyPair)
	{
		return directoryStructure.saveToHomeEK9Directory(keyPair);
	}
}
