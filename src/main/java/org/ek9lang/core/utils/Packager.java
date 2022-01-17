package org.ek9lang.core.utils;

import org.ek9lang.core.exception.AssertValue;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Focus on the responsibility of packaging. i.e. zipping and unzipping.
 */
public class Packager
{
	private final FileHandling fileHandling;

	public Packager(FileHandling fileHandling)
	{
		this.fileHandling = fileHandling;
	}

	public boolean createJar(String fileName, List<ZipSet> sets)
	{
		//Let exception break everything here - these are precondition.
		AssertValue.checkNotEmpty("Filename empty", fileName);
		AssertValue.checkNotNull("Zip Set cannot be null", sets);
		fileHandling.deleteFileIfExists(new File(fileName));

		try(FileSystem zip = FileSystems.newFileSystem(getZipUri(fileName), getZipEnv()))
		{
			sets.forEach(set -> addZipSet(zip, set));
		}
		catch(Throwable th)
		{
			System.err.println(th.getMessage());
			return false;
		}
		return true;
	}

	public boolean unZipFileTo(File zipFile, File unpackedDir)
	{
		//Preconditions
		AssertValue.checkCanReadFile("Zip File not readable", zipFile);
		fileHandling.makeDirectoryIfNotExists(unpackedDir);

		try(FileInputStream fis = new FileInputStream(zipFile))
		{
			//buffer for read and write data to file
			byte[] buffer = new byte[1024];

			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze = zis.getNextEntry();
			while(ze != null)
			{
				String fileName = ze.getName();
				File newFile = new File(unpackedDir, fileName);
				//System.err.println("Unzipping to "+newFile.getAbsolutePath());
				//create directories for subdirectories in zip
				File parent = new File(newFile.getParent());
				if(!parent.exists() && !parent.mkdirs())
					throw new RuntimeException("Unable to create directory structure");
				if(ze.isDirectory())
				{
					if(!newFile.exists() && !newFile.mkdir())
						throw new RuntimeException("Unable to create directory structure");
				}
				else
				{
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while((len = zis.read(buffer)) > 0)
					{
						fos.write(buffer, 0, len);
					}
					fos.close();
				}
				//close this ZipEntry
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			//close last ZipEntry
			zis.closeEntry();
			zis.close();
		}
		catch(Throwable th)
		{
			System.err.println(th.getMessage());
			return false;
		}
		return true;
	}

	public boolean createZip(String fileName, ZipSet set, File sourcePropertiesFile)
	{
		//Preconditions
		AssertValue.checkNotEmpty("FileName is empty", fileName);
		AssertValue.checkNotNull("Zip Set is null", set);
		AssertValue.checkCanReadFile("Properties file not readable", sourcePropertiesFile);

		try(FileSystem zip = FileSystems.newFileSystem(getZipUri(fileName), getZipEnv()))
		{
			addZipSet(zip, set);

			//Now to include the properties file so when the zip is unpacked we can find the name of the source that
			//was used to trigger the packaging.
			Path externalTxtFile = Path.of(sourcePropertiesFile.getAbsolutePath());
			Path pathInZipFile = zip.getPath(".package.properties");
			Files.copy(externalTxtFile, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
		}
		catch(Throwable th)
		{
			System.err.println(th.getMessage());
			return false;
		}
		return true;
	}

	private URI getZipUri(String fileName)
	{
		AssertValue.checkNotEmpty("FileName is empty", fileName);
		String toFile = new File(fileName).toURI().toString();
		return URI.create("jar:" + toFile);
	}

	private Map<String, String> getZipEnv()
	{
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		return env;
	}

	private void addZipSet(FileSystem zip, ZipSet set)
	{
		AssertValue.checkNotNull("Zip is null", zip);
		AssertValue.checkNotNull("Zip Set is null", set);
		try
		{
			if(set.isFileBased())
			{
				for(File file : set.getFiles())
				{
					Path externalTxtFile = Path.of(file.getAbsolutePath());
					Path relative = set.getRelativePath().toAbsolutePath().relativize(externalTxtFile);

					Path pathInZipFile = zip.getPath(relative.toString());
					// Copy a file into the zip file
					if(pathInZipFile.getParent() != null)
						Files.createDirectories(pathInZipFile.getParent());
					Files.copy(externalTxtFile, pathInZipFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			else if(set.isEntryBased())
			{
				for(ZipBinaryContent content : set.getEntries())
				{
					Path pathInZipFile = zip.getPath(content.getEntryName());
					if(pathInZipFile.getParent() != null)
						Files.createDirectories(pathInZipFile.getParent());
					try(OutputStream out = Files.newOutputStream(pathInZipFile))
					{
						out.write(content.getContent());
					}
				}
			}
			else
			{
				throw new RuntimeException("Unsure what to add to zip");
			}
		}
		catch(IOException ioException)
		{
			throw new RuntimeException(ioException);
		}
	}
}
