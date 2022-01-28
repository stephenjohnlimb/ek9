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
	private Map<String, Consumer<Version>> operation = Map.of(
			"major", v -> v.incrementMajor(),
			"minor", v -> v.incrementMinor(),
			"patch", v -> v.incrementPatch(),
			"build", v -> v.incrementBuildNumber()
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
		//i.e. Find the key or produce a no-op, this is in place of a switch.
		operation.getOrDefault(partToIncrement, v -> {}).accept(versionNumber);

		return setVersionNewNumber(versionNumber);
	}
}
