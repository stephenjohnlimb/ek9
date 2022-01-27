package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base for the versioning commands.
 */
public abstract class Eve extends E
{
	public Eve(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	public boolean preConditionCheck()
	{
		if(!super.preConditionCheck() || !commandLine.isPackagePresent())
		{
			report("File " + super.commandLine.getSourceFileName() + " does not define a package");
			return false;
		}
		return true;
	}

	/**
	 * Just sets the new version into the source file.
	 */
	public boolean setVersionNewNumber(Version newVersion)
	{
		try
		{
			List<String> output = loadAndUpdateVersionFromSourceFile(newVersion);
			if(!output.isEmpty())
			{
				saveUpdatedSourceFile(output);
				return true;
			}
		}
		catch(Throwable th)
		{
			report("Failed to set version in  " + commandLine.getFullPathToSourceFileName() + " " + th.getMessage());
		}
		return false;
	}

	private void saveUpdatedSourceFile(List<String> output) throws IOException
	{
		//Now write it back out
		try(PrintWriter writer = new PrintWriter(commandLine.getFullPathToSourceFileName(), StandardCharsets.UTF_8))
		{
			output.forEach(writer::println);
		}
	}

	private List<String> loadAndUpdateVersionFromSourceFile(Version newVersion) throws IOException
	{
		List<String> output = new ArrayList<>();
		//Now for processing of existing to get the line number.
		Integer versionLineNumber = commandLine.processEK9FileProperties(true);
		if(versionLineNumber != null)
		{
			int lineCount = 0;
			try(BufferedReader br = new BufferedReader(new FileReader(commandLine.getFullPathToSourceFileName())))
			{
				String line;
				while((line = br.readLine()) != null)
				{
					lineCount++;
					if(lineCount == versionLineNumber)
					{
						Matcher m = Pattern.compile("^(?<ver>[ a-z]+)(.*)$").matcher(line);
						if(m.find())
						{
							String prefix = m.group("ver");
							if(prefix.contains(" as "))
								line = prefix + " Version := " + newVersion;
							else
								line = prefix + "<- " + newVersion;
						}
					}
					output.add(line);
				}
			}
		}
		return output;
	}

	/**
	 * Holder for the version.
	 * Takes a version text for features or plain versions.
	 * Both with or without a build number.
	 * This then allows the developer to manipulate major, minor, patch and build numbers.
	 */
	public static class Version
	{
		private static String MAJOR_MINOR_PATCH = "(?<major>\\d+)(\\.)(?<minor>\\d+)(\\.)(?<patch>\\d+)";
		private static String FEATURE = "((-)(?<feature>[a-zA-Z]+[a-zA-Z0-9]*))";
		private static String BUILD_NO = "(-)(?<buildNumber>\\d+)";
		private int major = 0;
		private int minor = 0;
		private int patch = 0;
		private String feature = null;
		private int buildNumber = 0;

		/**
		 * Parse the incoming - but expect no build number.
		 */
		public static Version withNoBuildNumber(String value)
		{
			Matcher m = matcher("^" + MAJOR_MINOR_PATCH + FEATURE + "?$", value);
			return parse(false, true, m, value);
		}

		public static Version withNoFeatureNoBuildNumber(String value)
		{
			Matcher m = matcher("^" + MAJOR_MINOR_PATCH + "$", value);
			return parse(false, false, m, value);
		}

		public static Version withFeatureNoBuildNumber(String value)
		{
			Matcher m = matcher("^" + MAJOR_MINOR_PATCH + FEATURE + "$", value);
			return parse(false, true, m, value);
		}

		/**
		 * Parse the incoming - but expect a build number.
		 */
		public static Version withBuildNumber(String value)
		{
			Matcher m = matcher("^" + MAJOR_MINOR_PATCH + FEATURE + "?" + BUILD_NO + "$", value);
			return parse(true, true, m, value);
		}

		/**
		 * Does the parsing of a version number either with or without a build number.
		 */
		private static Version parse(boolean withBuildNumber, boolean withFeature, Matcher m, String value)
		{
			Version rtn = new Version();

			if(!m.find())
				throw new RuntimeException("Unable to use " + value + " as a VersionNumber");
			rtn.major = Integer.parseInt(m.group("major"));
			rtn.minor = Integer.parseInt(m.group("minor"));
			rtn.patch = Integer.parseInt(m.group("patch"));
			//might not be present
			if(withFeature)
				rtn.feature = m.group("feature");
			if(withBuildNumber)
				rtn.buildNumber = Integer.parseInt(m.group("buildNumber"));
			return rtn;
		}

		private static Matcher matcher(String pattern, String value)
		{
			return Pattern.compile(pattern).matcher(value);
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
			patch = 0;
			buildNumber = 0;
		}

		public Integer patch()
		{
			return patch;
		}

		public void incrementPatch()
		{
			patch++;
			buildNumber = 0;
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
			buffer.append(major()).append(".").append(minor()).append(".").append(patch());
			if(feature != null)
				buffer.append("-").append(feature());
			buffer.append("-").append(buildNumber());
			return buffer.toString();
		}
	}
}