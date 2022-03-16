package org.ek9lang.core.utils;

import org.ek9lang.core.exception.AssertValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;

/**
 * Responsibility of dealing with aspects of the ek9 directory structure.
 * Typically, lib, keys, artefacts and other places where EK9 expects items to be located.
 */
public class EK9DirectoryStructure
{
	public static final String JAVA = "java";
	public static final String CLASSES = "classes";
	public static final String DOT_JAR = ".jar";
	public static final String DOT_EK9 = ".ek9";
	public static final String DOT_PROPERTIES = ".properties";
	public static final String MAIN = "main";
	public static final String LIB = "lib";
	public static final String DEV = "dev";
	public static final String GENERATED = "generated";

	public static final String PUBLIC_PEM = "public.pem";
	public static final String PRIVATE_PEM = "private.pem";

	private final FileHandling fileHandling;

	public EK9DirectoryStructure(FileHandling fileHandling)
	{
		this.fileHandling = fileHandling;
	}

	public File getTargetExecutableArtefact(String ek9FullPathToFileName, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("EK9FullPathToFileName is empty", ek9FullPathToFileName);
		assertTargetArchitectureSupported(targetArchitecture);

		File sourceFile = new File(ek9FullPathToFileName);
		String ek9FileNameDirectory = sourceFile.getParent();
		String ek9JustFileName = sourceFile.getName();

		String projectEK9Directory = fileHandling.getDotEK9Directory(ek9FileNameDirectory);
		return new File(projectEK9Directory, ek9JustFileName.replace(DOT_EK9, DOT_JAR));
	}

	public File getTargetPropertiesArtefact(String ek9FullPathToFileName)
	{
		AssertValue.checkNotEmpty("EK9FullPathToFileName is empty", ek9FullPathToFileName);
		File sourceFile = new File(ek9FullPathToFileName);
		String ek9FileNameDirectory = sourceFile.getParent();

		String fileName = sourceFile.getName().replace(DOT_EK9, DOT_PROPERTIES);
		String projectEK9Directory = fileHandling.getDotEK9Directory(ek9FileNameDirectory);
		return new File(projectEK9Directory, fileName);
	}

	public void validateEK9Directory(String directoryName, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("DirectoryName is empty", directoryName);
		assertTargetArchitectureSupported(targetArchitecture);

		fileHandling.makeDirectoryIfNotExists(new File(directoryName));
		makeEK9DevDirectoryStructure(directoryName, targetArchitecture);
	}

	/**
	 * Used to create structure under .ek9 in both a project directory and a $HOME/.ek9 directory.
	 *
	 * @param fromEK9BaseDirectory typically $HOME/.ek9/ or /path/to/project/.ek9/
	 */
	public void makeEK9DevDirectoryStructure(String fromEK9BaseDirectory, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("FromEK9BaseDirectory is empty", fromEK9BaseDirectory);
		assertTargetArchitectureSupported(targetArchitecture);

		//For ALL libraries still in source ek9 but under their full versioned package name.
		//This is in unpacked form /path/to/project/.ek9/lib/com.some.package-4.9.5-feature23-40/ (a directory unpacked)

		//Once the ek9 source is processed to Java/classes - the resulting jar is then stored as
		// /path/to/project/.ek9/com.some.package-4.9.5-feature23-40.jar

		//The alternative is just to compile them up in the $HOME/.ek9/lib/com.some.package-4.9.5-feature23-40/ directory
		//Then the final jar can be stored in $HOME/.ek9/lib/com.some.package-4.9.5-feature23-40.jar
		//So when it comes to resolving - we can quickly check if we have the package and also if it is already built.
		//We still need to load the ek9 source from the package for resolving and the like. But no longer need to generate
		// Java/Classes/Jar If it is already up-to-date.

		addDirectory(fromEK9BaseDirectory, LIB);

		//Where we put anything we generate - intermediate formats java/classes etc/
		String generated = addDirectory(fromEK9BaseDirectory, GENERATED);

		addDirectory(generated, LIB);

		String main = addDirectory(generated, MAIN);
		addDirectory(main, targetArchitecture);
		addDirectory(main, CLASSES);

		String dev = addDirectory(generated, DEV);
		addDirectory(dev, targetArchitecture);
		addDirectory(dev, CLASSES);
	}

	/**
	 * This expects an ek9 source file in a specific directory.
	 *
	 * @param ek9FullPathToFileName i.e. main.ek9
	 * @param targetArchitecture    i.e. "java"
	 */
	public void cleanEK9DirectoryStructureFor(String ek9FullPathToFileName, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("EK9FullPathToFileName is empty", ek9FullPathToFileName);
		assertTargetArchitectureSupported(targetArchitecture);

		File sourceFile = new File(ek9FullPathToFileName);
		String ek9FileNameDirectory = sourceFile.getParent();

		String dotEk9Dir = fileHandling.getDotEK9Directory(ek9FileNameDirectory);

		fileHandling.deleteContentsAndBelow(new File(dotEk9Dir, GENERATED), false);
		fileHandling.deleteContentsAndBelow(new File(dotEk9Dir, LIB), false);

		File target = getTargetExecutableArtefact(ek9FullPathToFileName, targetArchitecture);
		fileHandling.deleteFileIfExists(target);

		File props = getTargetPropertiesArtefact(ek9FullPathToFileName);
		fileHandling.deleteFileIfExists(props);
	}

	public File getMainGeneratedOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("FromEK9BaseDirectory is empty", fromEK9BaseDirectory);
		assertTargetArchitectureSupported(targetArchitecture);

		return FileSystems.getDefault().getPath(fromEK9BaseDirectory, GENERATED, MAIN, targetArchitecture).toFile();
	}

	public File getMainFinalOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("FromEK9BaseDirectory is empty", fromEK9BaseDirectory);
		assertTargetArchitectureSupported(targetArchitecture);

		return FileSystems.getDefault().getPath(fromEK9BaseDirectory, GENERATED, MAIN, CLASSES).toFile();
	}

	public File getDevGeneratedOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("FromEK9BaseDirectory is empty", fromEK9BaseDirectory);
		assertTargetArchitectureSupported(targetArchitecture);

		return FileSystems.getDefault().getPath(fromEK9BaseDirectory, GENERATED, DEV, targetArchitecture).toFile();
	}

	public File getDevFinalOutputDirectory(String fromEK9BaseDirectory, String targetArchitecture)
	{
		AssertValue.checkNotEmpty("FromEK9BaseDirectory is empty", fromEK9BaseDirectory);
		assertTargetArchitectureSupported(targetArchitecture);

		return FileSystems.getDefault().getPath(fromEK9BaseDirectory, GENERATED, DEV, CLASSES).toFile();
	}

	public void deleteStalePackages(String ek9FileNameDirectory, String moduleName)
	{
		AssertValue.checkNotEmpty("EK9FileNameDirectory is empty", ek9FileNameDirectory);
		AssertValue.checkNotEmpty("ModuleName is empty", moduleName);

		String zipFileName = moduleName + "-.*\\.zip";
		fileHandling.deleteMatchingFiles(new File(fileHandling.getDotEK9Directory(ek9FileNameDirectory)), zipFileName);
	}

	public boolean isUsersSigningKeyPairPresent()
	{
		String dir = fileHandling.getUsersHomeEK9Directory();
		return (new File(dir, PRIVATE_PEM).exists() && new File(dir, PUBLIC_PEM).exists());
	}

	public SigningKeyPair getUsersSigningKeyPair()
	{
		String dir = fileHandling.getUsersHomeEK9Directory();
		File privateKeyFile = new File(dir, PRIVATE_PEM);
		File publicKeyFile = new File(dir, PUBLIC_PEM);
		return SigningKeyPair.of(privateKeyFile, publicKeyFile);
	}

	public boolean saveToHomeEK9Directory(SigningKeyPair keyPair)
	{
		AssertValue.checkNotNull("Keypair is null", keyPair);
		String dir = fileHandling.getUsersHomeEK9Directory();

		return saveToOutput(new File(dir, PRIVATE_PEM), keyPair.getPvtBase64()) &&
				saveToOutput(new File(dir, PUBLIC_PEM), keyPair.getPubBase64());
	}

	private boolean saveToOutput(File file, String value)
	{
		try(OutputStream output = new FileOutputStream(file))
		{
			output.write(value.getBytes());
			return true;
		}
		catch(Throwable th)
		{
			System.err.println("Unable to save " + file.getPath() + " " + th.getMessage());
			return false;
		}
	}

	private String addDirectory(String baseDir, String newDir)
	{
		AssertValue.checkNotEmpty("BaseDir is empty", baseDir);
		AssertValue.checkNotEmpty("NewDir is empty", newDir);

		File directory = new File(baseDir, newDir);
		if(!directory.exists())
		{
			if(!directory.mkdir())
			{
				System.err.println("Unable to create directory " + directory);
				System.exit(3);
			}
		}
		return directory.getAbsolutePath();
	}

	private void assertTargetArchitectureSupported(String targetArchitecture)
	{
		try
		{
			AssertValue.checkNotEmpty("TargetArchitecture is empty", targetArchitecture);
			if(!targetArchitecture.equals(JAVA))
				throw new RuntimeException("Unsupported target architecture " + targetArchitecture);
		}
		catch(RuntimeException rex)
		{
			System.err.println(rex.getMessage());
			//That's a hard fail and exit compiler
			System.exit(3);
		}
	}
}
