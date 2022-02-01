package org.ek9lang.cli.support;

import org.ek9lang.cli.CommandLineDetails;
import org.ek9lang.compiler.parsing.JustParser;

import java.io.File;
import java.util.Optional;

/**
 * Once the EK9 'Edp' dependency module has determined that a packaged module now needs to be resolved
 * it will call upon this resolver to make sure that it is available.
 * <p>
 * This resolver will initially look in the users $HOME/.ek9/lib directory for a zip file
 * matching the vector for the packages module ie 'ekopen.network.support.utils-1.6.1-9.zip'
 * <p>
 * If that is not present then it will make a https request to repo.ek9lang.com
 * to obtain that zip file, and hash of zip fingerprint.
 * <p>
 * The manual steps for this are:
 * curl https://repo.ek9lang.org/ekopen.net.handy.tools-3.2.1-0.zip -o ekopen.net.handy.tools-3.2.1-0.zip
 * curl https://repo.ek9lang.org/ekopen.net.handy.tools-3.2.1-0.zip.sha256 -o ekopen.net.handy.tools-3.2.1-0.zip.sha256
 * cat ekopen.net.handy.tools-3.2.1-0.zip | shasum -a 256 -c ekopen.net.handy.tools-3.2.1-0.zip.sha256
 * <p>
 * Note when the publisher of the package uploaded the zip, they did a couple of extra bits to be able to provide
 * a secure copy of the hash of the zip.
 * <p>
 * See SigningKeyPair.doubleEncryption as an example of this.
 * They used their private key to encrypt the hash of the zip they created.
 * They then used the public key of the repo server to encrypt that data.
 * <p>
 * So that ensures that only the server with its private key can decrypt that payload, but then
 * only by using the publishers public key can the inner payload be decrypted to reveal the hash.
 * That hash can then be checked against a re-run of hashing of the zip file.
 * <p>
 * On the repo server, the zip is taken and put to one side for virus scanning and later processing.
 * The first layer of encryption is decrypted by the repo serer using its own private key. This ensures that the
 * data decrypted (the still encrypted hash and un-encrypted client public key) has not been tampered with.
 * <p>
 * Now the provided client public key can be used to decrypt the encrypted hash. That hash value can be checked
 * against the hash calculated against the zip.
 * <p>
 * If the zip is virus free, then the zip, the encrypted (with the client private key) hash and the clients public
 * key are all stored on the S3 server.
 * <p>
 * So when the zip is downloaded, this resolver will use the same hashing routine to calculate the fingerprint.
 * It will also get the clients public key to decrypt the encrypted hash and check that the hash values match.
 * <p>
 * Anyway that's the general idea.
 */
public class PackageResolver extends Reporter
{
	private final CommandLineDetails commandLine;

	public PackageResolver(CommandLineDetails commandLine)
	{
		super(commandLine.isVerbose());
		this.commandLine = commandLine;
	}

	@Override
	protected String messagePrefix()
	{
		return "Resolve : ";
	}

	public Optional<EK9SourceVisitor> resolve(String dependencyVector)
	{
		EK9SourceVisitor visitor = null;
		String zipFileName = commandLine.getFileHandling().makePackagedModuleZipFileName(dependencyVector);
		File homeEK9Lib = commandLine.getFileHandling().getUsersHomeEK9LibDirectory();
		//Let's check if it is unpacked already, if not we can unpack it.
		File unpackedDir = new File(homeEK9Lib, dependencyVector);

		File zipFile = new File(homeEK9Lib, zipFileName);
		log("Checking '" + dependencyVector + "'");

		//See if it is already unpackaged and available
		if(commandLine.getOsSupport().isDirectoryReadable(unpackedDir))
		{
			log("Already unpacked '" + dependencyVector + "'");
			return Optional.of(processPackageProperties(unpackedDir));
		}

		if(!commandLine.getOsSupport().isFileReadable(zipFile))
		{
			//OK not there so download
			if(!downloadDependency(dependencyVector))
			{
				report("'" + dependencyVector + "' cannot be resolved!");
				return Optional.empty();
			}
		}

		//So it should now have been downloaded we can un-package.
		if(commandLine.getOsSupport().isFileReadable(zipFile))
		{
			log("Unpacking '" + zipFile + "'");
			if(unZip(zipFile, unpackedDir))
				return Optional.of(processPackageProperties(unpackedDir));
		}

		return Optional.ofNullable(visitor);
	}

	private boolean downloadDependency(String dependencyVector)
	{
		//TODO the download part
		return false;
	}

	/**
	 * Load .package.properties file from the unpacked dir and fet the property 'sourceFile'
	 * This will tell us which of the sources of ek9 files is the one containing the package directive to use.
	 *
	 * @param unpackedDir The directory where the zip is unpacked to.
	 * @return A Visitor with all the details of the package from the source file.
	 */
	private EK9SourceVisitor processPackageProperties(File unpackedDir)
	{
		File propertiesFile = new File(unpackedDir, ".package.properties");
		log("Loading '" + propertiesFile + "'");

		File src = new File(unpackedDir, new EK9ProjectProperties(propertiesFile).loadProperties().getProperty("sourceFile"));
		log("SourceFile '" + src + "'");
		if(commandLine.getOsSupport().isFileReadable(src))
			return loadFileAndVisit(src);

		report("Unable to read sourceFile '" + src + "'");
		return null;
	}

	private boolean unZip(File zipFile, File unpackedDir)
	{
		return commandLine.getFileHandling().unZipFileTo(zipFile, unpackedDir);
	}

	private EK9SourceVisitor loadFileAndVisit(File sourceFile)
	{
		EK9SourceVisitor visitor = new EK9SourceVisitor();
		if(!new JustParser().readSourceFile(sourceFile, visitor))
		{
			report("Unable to Parse source file [" + sourceFile.getAbsolutePath() + "]");
			return null;
		}
		return visitor;
	}
}
