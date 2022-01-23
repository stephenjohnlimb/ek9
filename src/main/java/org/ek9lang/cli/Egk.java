package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;
import org.ek9lang.core.utils.SigningKeyPair;

/**
 * Generate signing keys.
 */
public class Egk extends E
{
	public Egk(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	@Override
	protected String messagePrefix()
	{
		return "Keys    : ";
	}

	public boolean run()
	{
		log("Prepare");

		if(!fileHandling.isUsersSigningKeyPairPresent())
		{
			log("Generating new signing keys");

			//Clients only use short key lengths, server uses 2048.
			if(!fileHandling.saveToHomeEK9Directory(SigningKeyPair.generate(1024)))
			{
				report("Failed to regenerate keys");
				return false;
			}
		}
		else
		{
			log("Already present - not regenerating");
		}

		log("Complete");
		return true;
	}
}
