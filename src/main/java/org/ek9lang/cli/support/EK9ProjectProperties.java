package org.ek9lang.cli.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

/**
 * Designed only to be used on the front end of the compiler to access
 * properties files that are in the .ek9 directory just off the same directory as the
 * file that the compiler was asked to compile. This code will cause System.exit if it is
 * not possible to continue (i.e. it is not designed to be tolerant at all).
 * i.e. /some/path/to/project/MyAceProgram.ek9
 * We're looking in /some/path/to/project/.ek9/*.properties
 */
public class EK9ProjectProperties
{
	private final File file;

	public EK9ProjectProperties(File propertiesFile)
	{
		this.file = propertiesFile;
	}

	public boolean exists()
	{
		return file.exists();
	}

	public String getFileName()
	{
		return file.getName();
	}

	public String prepareListForStorage(List<String> list)
	{
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < list.size(); i++)
		{
			if(i != 0)
				buffer.append(",");
			buffer.append(list.get(i));
		}
		return buffer.toString();
	}

	public boolean isNewerThan(File sourceFile)
	{
		return file.exists() && file.lastModified() > sourceFile.lastModified();
	}

	public Properties loadProperties()
	{
		Properties properties = new Properties();
		try(FileReader reader = new FileReader(file))
		{
			properties.load(reader);
		}
		catch(Throwable th)
		{
			System.err.println("Unable to load properties " + file.getName() + " " + th.getMessage());
			System.exit(3);
		}
		return properties;
	}

	public void storeProperties(Properties properties)
	{
		try(OutputStream output = new FileOutputStream(file))
		{
			properties.store(output, "Package Properties");
		}
		catch(Throwable th)
		{
			System.err.println("Unable to save properties " + file.getName() + " " + th.getMessage());
			System.exit(3);
		}
	}
}