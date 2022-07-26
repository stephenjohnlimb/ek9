package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Just increments a version number.
 */
public class Eiv extends Eve
{
	/**
	 * Rather than use a switch use a map of version vector name to a method call.
	 */
	private final Map<String, Consumer<Version>> operation = Map.of(
			"major", Version::incrementMajor,
			"minor", Version::incrementMinor,
			"patch", Version::incrementPatch,
			"build", Version::incrementBuildNumber
	);

	public Eiv(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Version+: ";
	}

	protected boolean doRun()
	{
		//Need to get from command line.
		String partToIncrement = commandLine.getOptionParameter("-IV");
		Version versionNumber = Version.withBuildNumber(commandLine.getVersion());
		log("Processing increment " + versionNumber);
		//i.e. Find the key or produce a no-op, this is in place of a switch.
		operation.getOrDefault(partToIncrement, v -> {}).accept(versionNumber);

		return setVersionNewNumber(versionNumber);
	}
}
