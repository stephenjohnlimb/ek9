package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.Digest;
import org.ek9lang.core.utils.OsSupport;
import org.ek9lang.core.utils.SigningKeyPair;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Deploy a package / all packages inside the project directory.
 */
public class Ed extends E
{
	public Ed(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	@Override
	protected String messagePrefix()
	{
		return "Deploy  : ";
	}

	public boolean run()
	{
		log("- Package");
		Ep ep = new Ep(commandLine, sourceFileCache, osSupport);
		if(ep.run())
		{
			//Need to ensure that the user has some signing keys.
			Egk egk = new Egk(commandLine, sourceFileCache, osSupport);
			if(egk.run())
			{
				//Now do deployment.
				log("Prepare");

				if(!prepareEncryptedZipHash())
				{
					report("Unable to complete package deployment");
					return false;
				}
				//Still TODO
				//OK now we can zip the zip, encrypted hash and clients public key and send

				//Also needs an account with some credentials to send to https://deploy.ek9lang.org
				//TODO - we will leave this for now - see SigningKeyPairTest on how we will do it.
				log("Complete");
				return true;
			}
		}
		return false;
	}

	/**
	 * Once zipped, packaging and everything is ready.
	 * This method can take the sha256 file and double encrypt it ready to accompany
	 * the zip and the clients public key.
	 */
	private boolean prepareEncryptedZipHash()
	{
		String zipFileName = fileHandling.makePackagedModuleZipFileName(commandLine.getModuleName(), commandLine.getVersion().toString());
		File zipFile = new File(fileHandling.getDotEK9Directory(commandLine.getSourceFileDirectory()), zipFileName);
		File sha256File = new File(fileHandling.getDotEK9Directory(commandLine.getSourceFileDirectory()), zipFileName + ".sha256");

		if(zipFile.exists() && sha256File.exists())
		{
			log("Signing deployment package");
			//Yes hard coded again - revisit later maybe
			String serverPublicKey = getServerPublicKey("repo.ek9lang.org");
			if(serverPublicKey == null)
			{
				report("Unable to sign package for deployment");
			}
			else
			{
				SigningKeyPair usersSigningKeyPair = fileHandling.getUsersSigningKeyPair();
				if(usersSigningKeyPair == null)
				{
					report("Unable to load users signing keys");
				}
				else
				{
					SigningKeyPair halfKeyPair = SigningKeyPair.ofPublic(serverPublicKey);
					//OK ready to go!
					String plainHashText = Digest.digest(sha256File).toString();
					String innerCipherText = usersSigningKeyPair.encryptWithPrivateKey(plainHashText);
					String finalCipherText = halfKeyPair.encryptWithPublicKey(innerCipherText);
					File sha256EncFile = new File(fileHandling.getDotEK9Directory(commandLine.getSourceFileDirectory()), zipFileName + ".sha256.enc");

					try(FileOutputStream fos = new FileOutputStream((sha256EncFile)))
					{
						fos.write(finalCipherText.getBytes(StandardCharsets.UTF_8));
						log("Deployment package signed");
						return true;
					}
					catch(Throwable th)
					{
						report("Failed to create signed package");
					}
				}
			}
		}
		else
		{
			report("Failed to find deployable zip/sha files");
		}
		return false;
	}

	private String getServerPublicKey(String serverName)
	{
		//pem file must have same name as server.
		//TODO
		return null;
	}
}
