package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Eve extends E
{
	public Eve(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	public boolean setVersionNewNumber(Version newVersion)
	{
		if(commandLine.isPackagePresent())
		{
			List<String> output = new ArrayList<>();
			//Now for processing of existing to get the line number.
			Integer versionLineNumber = commandLine.processEK9FileProperties(true);
			if(versionLineNumber != null)
			{
				File sourceFileToModify = new File(commandLine.getFullPathToSourceFileName());
				int lineCount = 0;
				try(BufferedReader br = new BufferedReader(new FileReader(sourceFileToModify)))
				{
					String line;
					while((line = br.readLine()) != null)
					{
						lineCount++;
						if(lineCount == versionLineNumber)
						{
							Pattern p = Pattern.compile("^(?<ver>[ a-z]+)(.*)$");
							Matcher m = p.matcher(line);
							if(!m.find())
								throw new RuntimeException("Unable to find 'version' in line [" + line + "]");
							String prefix = m.group("ver");
							line = prefix + "<- " + newVersion.toString();
						}
						output.add(line);
					}
				}
				catch(Throwable th)
				{
					report("Failed to read " + commandLine.getFullPathToSourceFileName() + " " + th.getMessage());
					return false;
				}
				//Now write it back out
				try(PrintWriter writer = new PrintWriter(sourceFileToModify, StandardCharsets.UTF_8))
				{
					output.forEach(writer::println);
				}
				catch(Throwable th)
				{
					report("Failed to update " + commandLine.getFullPathToSourceFileName() + " " + th.getMessage());
					return false;
				}
				return true;
			}
			else
			{
				report("Cannot determine line number of version in " + commandLine.getSourceFileName());
			}
		}
		else
		{
			report("File " + commandLine.getSourceFileName() + " does not define a package");
		}
		return false;
	}

	public static class Version
	{
		private int major = 0;
		private int minor = 0;
		private int patch = 0;
		private java.lang.String feature = null;
		private int buildNumber = 0;

		public static Version withNoBuildNumber(java.lang.String value)
		{
			Version rtn = new Version();

			Pattern p = Pattern.compile("^(?<major>\\d+)(\\.)(?<minor>\\d+)(\\.)(?<patch>\\d+)((-)(?<feature>[a-zA-Z]+[a-zA-Z0-9]*))?$");
			Matcher m = p.matcher(value);

			if(!m.find())
				throw new RuntimeException("Unable to use " + value + " as a VersionNumber");
			rtn.major = java.lang.Integer.parseInt(m.group("major"));
			rtn.minor = java.lang.Integer.parseInt(m.group("minor"));
			rtn.patch = java.lang.Integer.parseInt(m.group("patch"));
			//might not be present
			rtn.feature = m.group("feature");
			rtn.buildNumber = 0;
			return rtn;
		}

		public Version()
		{

		}

		public Version(String ofString)
		{
			this.parse(ofString);
		}

		protected void parse(java.lang.String value)
		{
			Pattern p = Pattern.compile("^(?<major>\\d+)(\\.)(?<minor>\\d+)(\\.)(?<patch>\\d+)((-)(?<feature>[a-zA-Z]+[a-zA-Z0-9]*))?(-)(?<buildNumber>\\d+)$");
			Matcher m = p.matcher(value);

			if(!m.find())
				throw new RuntimeException("Unable to use " + value + " as a VersionNumber");
			this.major = java.lang.Integer.parseInt(m.group("major"));
			this.minor = java.lang.Integer.parseInt(m.group("minor"));
			this.patch = java.lang.Integer.parseInt(m.group("patch"));
			//might not be present
			this.feature = m.group("feature");
			this.buildNumber = java.lang.Integer.parseInt(m.group("buildNumber"));
		}

		public Integer major()
		{
			return major;
		}

		public void incrementMajor()
		{
			major++;
			minor = 0;
			patch = 0;
			buildNumber = 0;
		}

		public Integer minor()
		{
			return minor;
		}

		public void incrementMinor()
		{
			minor++;
		}

		public Integer patch()
		{
			return patch;
		}

		public void incrementPatch()
		{
			patch++;
		}


		public String feature()
		{
			return feature;
		}

		public Integer buildNumber()
		{
			return buildNumber;
		}

		public void incrementBuildNumber()
		{
			buildNumber++;
		}

		@Override
		public String toString()
		{
			//Format
			StringBuilder buffer = new StringBuilder();
			buffer.append(major).append(".").append(minor).append(".").append(patch);
			if(feature != null)
				buffer.append("-").append(feature);
			buffer.append("-").append(buildNumber);
			return buffer.toString();
		}
	}
}
