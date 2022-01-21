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

	public boolean run()
	{
		log("Keys: Prepare");

		if(!fileHandling.isUsersSigningKeyPairPresent())
		{
			log("Keys: Generating new signing keys");

			//Clients only use short key lengths, server uses 2048.
			if(!fileHandling.saveToHomeEK9Directory(SigningKeyPair.generate(1024)))
			{
				report("Keys: Failed to regenerate keys");
				return false;
			}
		}
		else
		{
			log("Keys: Already present - not regenerating");
		}

		log("Keys: Complete");
		return true;
	}
}
