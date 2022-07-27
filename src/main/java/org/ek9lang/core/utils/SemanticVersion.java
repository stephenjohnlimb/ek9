package org.ek9lang.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Can be a normal version number like 6.8.2-9 i.e 9 is the build number
 * Or can be for a feature like 6.1.6-specialFeature12-19 i.e. build number 19 of specialFeature12
 */
public final class SemanticVersion implements Comparable<SemanticVersion>
{
	private int major = 0;
	private int minor = 0;
	private int patch = 0;
	private String feature = null;
	private int buildNumber = 0;

	public static SemanticVersion _of(String value)
	{
		SemanticVersion rtn = new SemanticVersion();

		if(!rtn.parse(value))
			return null;

		return rtn;
	}

	public static SemanticVersion _withNoBuildNumber(String value)
	{
		SemanticVersion rtn = new SemanticVersion();

		Pattern p = Pattern.compile("^(?<major>\\d+)(\\.)(?<minor>\\d+)(\\.)(?<patch>\\d+)((-)(?<feature>[a-zA-Z]+[a-zA-Z0-9]*))?$");
		Matcher m = p.matcher(value);

		if(!m.find())
			return null;
		rtn.major = java.lang.Integer.parseInt(m.group("major"));
		rtn.minor = java.lang.Integer.parseInt(m.group("minor"));
		rtn.patch = java.lang.Integer.parseInt(m.group("patch"));
		//might not be present
		rtn.feature = m.group("feature");
		rtn.buildNumber = 0;
		return rtn;
	}

	private SemanticVersion()
	{
	}

	public SemanticVersion(String value)
	{
		parse(value);
	}

	public int major()
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

	public int minor()
	{
		return minor;
	}

	public void incrementMinor()
	{
		minor++;
		patch = 0;
		buildNumber = 0;
	}

	public int patch()
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

	public int buildNumber()
	{
		return buildNumber;
	}

	public void incrementBuildNumber()
	{
		buildNumber++;
	}

	private boolean parse(java.lang.String value)
	{
		Pattern p = Pattern.compile("^(?<major>\\d+)(\\.)(?<minor>\\d+)(\\.)(?<patch>\\d+)((-)(?<feature>[a-zA-Z]+[a-zA-Z0-9]*))?(-)(?<buildNumber>\\d+)$");
		Matcher m = p.matcher(value);

		if(!m.find())
			return false;
		this.major = java.lang.Integer.parseInt(m.group("major"));
		this.minor = java.lang.Integer.parseInt(m.group("minor"));
		this.patch = java.lang.Integer.parseInt(m.group("patch"));
		//might not be present
		this.feature = m.group("feature");
		this.buildNumber = java.lang.Integer.parseInt(m.group("buildNumber"));
		return true;
	}

	@Override
	public int compareTo(SemanticVersion value)
	{
		if(this.major == value.major)
		{
			if(this.minor == value.minor)
			{
				if(this.patch == value.patch)
				{
					if(feature != null && value.feature != null)
					{
						int featureCompare = feature.compareTo(value.feature);
						if(featureCompare == 0)
						{
							return java.lang.Integer.compare(this.buildNumber, value.buildNumber);
						}
						return featureCompare;
					}
					else if(feature != null)
					{
						//because it has a feature it is not as important as those without.
						return -1;
					}
					else if(value.feature != null)
					{
						return 1;
					}
					return java.lang.Integer.compare(this.buildNumber, value.buildNumber);
				}
				return java.lang.Integer.compare(this.patch, value.patch);
			}
			return java.lang.Integer.compare(this.minor, value.minor);
		}
		return java.lang.Integer.compare(this.major, value.major);
	}

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

	public int hashCode()
	{
		return toString().hashCode();
	}

	public boolean equals(Object obj)
	{
		if(obj instanceof SemanticVersion)
			return toString().equals(obj.toString());
		return false;
	}
}